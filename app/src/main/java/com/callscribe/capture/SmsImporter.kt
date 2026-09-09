package com.callscribe.capture

import android.Manifest
import android.content.Context
import android.provider.Telephony
import com.callscribe.data.AppDatabase
import com.callscribe.data.Capture
import com.callscribe.data.CaptureStatus
import com.callscribe.data.Direction
import com.callscribe.data.Kind
import com.callscribe.work.ProcessCaptureWorker

/** Еднократен внос на съществуващи съобщения от системната база. */
object SmsImporter {

    suspend fun importSince(context: Context, sinceMillis: Long, limit: Int = 200): Int {
        if (!Contacts.hasPermission(context, Manifest.permission.READ_SMS)) return 0
        val dao = AppDatabase.get(context).captureDao()
        var imported = 0

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(sinceMillis.toString()),
            "${Telephony.Sms.DATE} DESC LIMIT $limit"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val phone = cursor.getString(0)
                val body = cursor.getString(1) ?: continue
                val date = cursor.getLong(2)
                val direction =
                    if (cursor.getInt(3) == Telephony.Sms.MESSAGE_TYPE_SENT) Direction.OUT else Direction.IN
                if (body.isBlank()) continue
                if (dao.countSimilarSms(phone, body, date) > 0) continue

                val id = dao.insert(
                    Capture(
                        kind = Kind.SMS,
                        direction = direction,
                        contactName = Contacts.nameFor(context, phone),
                        phone = phone,
                        startedAt = date,
                        text = body,
                        status = CaptureStatus.NEW
                    )
                )
                ProcessCaptureWorker.enqueue(context, id)
                imported++
            }
        }
        return imported
    }
}
