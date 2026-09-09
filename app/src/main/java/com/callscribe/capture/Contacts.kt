package com.callscribe.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.callscribe.data.Direction

data class CallLogEntry(
    val phone: String?,
    val direction: String,
    val durationSec: Int,
    val startedAt: Long
)

object Contacts {

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun nameFor(context: Context, phone: String?): String? {
        if (phone.isNullOrBlank()) return null
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phone)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Последният запис в дневника на обажданията — единственият надежден източник за номер след Android 10. */
    fun lastCall(context: Context): CallLogEntry? {
        if (!hasPermission(context, Manifest.permission.READ_CALL_LOG)) return null
        return try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE),
                null, null,
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val direction = when (cursor.getInt(1)) {
                    CallLog.Calls.OUTGOING_TYPE -> Direction.OUT
                    CallLog.Calls.INCOMING_TYPE -> Direction.IN
                    else -> Direction.UNKNOWN
                }
                CallLogEntry(
                    phone = cursor.getString(0),
                    direction = direction,
                    durationSec = cursor.getInt(2),
                    startedAt = cursor.getLong(3)
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
