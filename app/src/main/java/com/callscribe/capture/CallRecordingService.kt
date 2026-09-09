package com.callscribe.capture

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.callscribe.CallScribeApp
import com.callscribe.R
import com.callscribe.data.AppDatabase
import com.callscribe.data.Capture
import com.callscribe.data.CaptureStatus
import com.callscribe.data.Direction
import com.callscribe.data.Kind
import com.callscribe.data.Settings
import com.callscribe.ui.MainActivity
import com.callscribe.work.ProcessCaptureWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground услуга, която записва аудио по време на разговор.
 *
 * Важно ограничение на Android: от Android 10 нататък `AudioSource.VOICE_CALL` е
 * недостъпен за приложения извън системните. Услугата опитва наличните източници по ред
 * и на повечето устройства реално записва през микрофона — тоест отсрещната страна се чува
 * добре само при включен високоговорител.
 */
class CallRecordingService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt: Long = 0L
    private var previousSpeakerState: Boolean? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> {
                stopRecording()
                stopSelf()
            }
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (recorder != null) return

        goToForeground()

        if (!Contacts.hasPermission(this, Manifest.permission.RECORD_AUDIO)) {
            Log.w(TAG, "Няма разрешение за запис на аудио.")
            stopSelf()
            return
        }

        val settings = Settings(this)
        if (settings.forceSpeaker) enableSpeaker()

        val dir = File(filesDir, "calls").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "call-$stamp.m4a")

        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_CALL,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )

        for (source in sources) {
            val candidate = newRecorder()
            try {
                candidate.setAudioSource(source)
                candidate.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                candidate.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                candidate.setAudioSamplingRate(16000)
                candidate.setAudioEncodingBitRate(64000)
                candidate.setAudioChannels(1)
                candidate.setOutputFile(file.absolutePath)
                candidate.prepare()
                candidate.start()
                recorder = candidate
                outputFile = file
                startedAt = System.currentTimeMillis()
                Log.i(TAG, "Записът започна с източник $source")
                return
            } catch (e: Exception) {
                Log.w(TAG, "Източник $source не работи: ${e.message}")
                try {
                    candidate.reset()
                    candidate.release()
                } catch (ignored: Exception) {
                }
            }
        }

        Log.e(TAG, "Нито един аудио източник не проработи.")
        file.delete()
        stopSelf()
    }

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()

    private fun stopRecording() {
        val active = recorder ?: return
        recorder = null
        try {
            active.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Спирането на записа хвърли грешка: ${e.message}")
        } finally {
            try {
                active.release()
            } catch (ignored: Exception) {
            }
        }
        restoreSpeaker()

        val file = outputFile ?: return
        outputFile = null
        if (!file.exists() || file.length() < 4096) {
            file.delete()
            Log.w(TAG, "Записът е празен, файлът е изтрит.")
            return
        }
        persist(file)
    }

    private fun persist(file: File) {
        val context = applicationContext
        val recordedAt = startedAt
        val durationSec = ((System.currentTimeMillis() - recordedAt) / 1000).toInt()

        CoroutineScope(Dispatchers.IO).launch {
            // Дневникът на обажданията се обновява с малко закъснение след края на разговора.
            kotlinx.coroutines.delay(1500)
            val entry = Contacts.lastCall(context)
            val phone = entry?.phone
            val capture = Capture(
                kind = Kind.CALL,
                direction = entry?.direction ?: Direction.UNKNOWN,
                contactName = Contacts.nameFor(context, phone),
                phone = phone,
                startedAt = recordedAt,
                durationSec = entry?.durationSec?.takeIf { it > 0 } ?: durationSec,
                audioPath = file.absolutePath,
                status = CaptureStatus.NEW
            )
            val id = AppDatabase.get(context).captureDao().insert(capture)
            ProcessCaptureWorker.enqueue(context, id)
        }
    }

    @Suppress("DEPRECATION")
    private fun enableSpeaker() {
        try {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            previousSpeakerState = audio.isSpeakerphoneOn
            audio.mode = AudioManager.MODE_IN_CALL
            audio.isSpeakerphoneOn = true
        } catch (e: Exception) {
            Log.w(TAG, "Не може да включи високоговорителя: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun restoreSpeaker() {
        val previous = previousSpeakerState ?: return
        previousSpeakerState = null
        try {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.isSpeakerphoneOn = previous
        } catch (ignored: Exception) {
        }
    }

    private fun goToForeground() {
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, CallScribeApp.CHANNEL_CAPTURE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Записва се разговор")
            .setContentText("CallScribe записва текущия разговор.")
            .setOngoing(true)
            .setContentIntent(intent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CallRecordingService"
        private const val NOTIFICATION_ID = 4201
        const val ACTION_START = "com.callscribe.START_RECORDING"
        const val ACTION_STOP = "com.callscribe.STOP_RECORDING"

        fun start(context: Context) {
            val settings = Settings(context)
            if (!settings.consentAccepted || !settings.recordCalls) return
            if (!Contacts.hasPermission(context, Manifest.permission.RECORD_AUDIO)) return
            val intent = Intent(context, CallRecordingService::class.java).setAction(ACTION_START)
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Услугата не може да стартира: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).setAction(ACTION_STOP)
            try {
                context.startService(intent)
            } catch (ignored: Exception) {
            }
        }
    }
}
