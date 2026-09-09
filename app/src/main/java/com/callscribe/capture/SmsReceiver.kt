package com.callscribe.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.callscribe.data.AppDatabase
import com.callscribe.data.Capture
import com.callscribe.data.CaptureStatus
import com.callscribe.data.Direction
import com.callscribe.data.Kind
import com.callscribe.data.Settings
import com.callscribe.work.ProcessCaptureWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val settings = Settings(context)
        if (!settings.consentAccepted || !settings.readSms) return

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            null
        } ?: return
        if (messages.isEmpty()) return

        // Дълъг SMS пристига на части — сглобяваме ги в едно съобщение.
        val phone = messages.first().originatingAddress
        val body = messages.joinToString("") { it.messageBody.orEmpty() }
        val at = messages.first().timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
        if (body.isBlank()) return

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(appContext).captureDao()
                if (dao.countSimilarSms(phone, body, at) > 0) return@launch
                val id = dao.insert(
                    Capture(
                        kind = Kind.SMS,
                        direction = Direction.IN,
                        contactName = Contacts.nameFor(appContext, phone),
                        phone = phone,
                        startedAt = at,
                        text = body,
                        status = CaptureStatus.NEW
                    )
                )
                ProcessCaptureWorker.enqueue(appContext, id)
            } finally {
                pending.finish()
            }
        }
    }
}
