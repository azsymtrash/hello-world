package com.callscribe.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callscribe.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Алармите не преживяват рестарт — насрочваме ги наново. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.get(appContext).taskDao().withPendingReminders().forEach { task ->
                    ReminderScheduler.schedule(appContext, task)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
