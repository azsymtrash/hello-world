package com.callscribe.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callscribe.R
import com.callscribe.ai.TaskExtractor
import com.callscribe.ai.Transcriber
import com.callscribe.data.AppDatabase
import com.callscribe.data.CaptureStatus
import com.callscribe.data.Settings
import com.callscribe.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Транскрибира записа, извлича задачите и насрочва напомнянията. */
class ProcessCaptureWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val captureId = inputData.getLong(KEY_CAPTURE_ID, -1L)
        if (captureId <= 0) return@withContext Result.failure()

        val context = applicationContext
        val dao = AppDatabase.get(context).captureDao()
        val taskDao = AppDatabase.get(context).taskDao()
        val settings = Settings(context)
        var capture = dao.byId(captureId) ?: return@withContext Result.failure()

        try {
            if (capture.text.isNullOrBlank() && !capture.audioPath.isNullOrBlank()) {
                if (!settings.asrConfigured) {
                    dao.update(capture.copy(status = CaptureStatus.ERROR, error = context.getString(R.string.err_no_asr)))
                    return@withContext Result.success()
                }
                dao.update(capture.copy(status = CaptureStatus.TRANSCRIBING, error = null))
                val transcript = Transcriber(context, settings).transcribe(File(capture.audioPath!!))
                capture = capture.copy(text = transcript, status = CaptureStatus.ANALYZING)
                dao.update(capture)

                if (settings.deleteAudioAfterTranscript && transcript.isNotBlank()) {
                    File(capture.audioPath!!).delete()
                    capture = capture.copy(audioPath = null)
                    dao.update(capture)
                }
            }

            val text = capture.text
            if (text.isNullOrBlank()) {
                dao.update(capture.copy(status = CaptureStatus.ERROR, error = context.getString(R.string.err_no_text)))
                return@withContext Result.success()
            }

            if (!settings.aiConfigured) {
                dao.update(capture.copy(status = CaptureStatus.ERROR, error = context.getString(R.string.err_no_ai)))
                return@withContext Result.success()
            }

            dao.update(capture.copy(status = CaptureStatus.ANALYZING, error = null))
            val tasks = TaskExtractor(context, settings).extract(capture, text)

            // Повторно обработване на един и същи източник не трябва да дублира редове.
            taskDao.byCapture(captureId).forEach { taskDao.delete(it) }

            for (task in tasks) {
                val reminderAt = ReminderScheduler.reminderTimeFor(task, settings.reminderOffsetMinutes)
                val id = taskDao.insert(task.copy(reminderAt = reminderAt))
                if (reminderAt != null) {
                    ReminderScheduler.schedule(context, task.copy(id = id, reminderAt = reminderAt))
                }
            }

            dao.update(capture.copy(status = CaptureStatus.DONE, error = null, tasksFound = tasks.size))
            Result.success()
        } catch (e: Exception) {
            val message = e.message ?: e.javaClass.simpleName
            if (runAttemptCount < 3) {
                dao.update(capture.copy(error = context.getString(R.string.err_attempt, runAttemptCount + 1, message)))
                Result.retry()
            } else {
                dao.update(capture.copy(status = CaptureStatus.ERROR, error = message))
                Result.success()
            }
        }
    }

    companion object {
        private const val KEY_CAPTURE_ID = "capture_id"

        fun enqueue(context: Context, captureId: Long) {
            val request = OneTimeWorkRequestBuilder<ProcessCaptureWorker>()
                .setInputData(Data.Builder().putLong(KEY_CAPTURE_ID, captureId).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("capture-$captureId", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
