package com.callscribe.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callscribe.data.AppDatabase
import com.callscribe.data.Settings
import com.callscribe.work.SmsSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Алармите и фоновият цикъл не преживяват рестарт — пускаме ги наново. */
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
                val settings = Settings(appContext)
                if (settings.consentAccepted && settings.autoSync) {
                    SmsSyncWorker.schedulePeriodic(appContext)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
