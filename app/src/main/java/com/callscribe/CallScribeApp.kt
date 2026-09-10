package com.callscribe

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.callscribe.data.Settings
import com.callscribe.work.SmsSyncWorker

class CallScribeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CAPTURE,
                getString(R.string.channel_capture),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.channel_capture_desc) }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.channel_reminders_desc) }
        )

        // Фоновото засичане тръгва само по себе си при всяко стартиране на процеса.
        val settings = Settings(this)
        if (settings.consentAccepted && settings.autoSync) {
            SmsSyncWorker.schedulePeriodic(this)
            SmsSyncWorker.syncNow(this)
        }
    }

    companion object {
        const val CHANNEL_CAPTURE = "capture"
        const val CHANNEL_REMINDERS = "reminders"
    }
}
