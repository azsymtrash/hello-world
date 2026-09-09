package com.callscribe.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callscribe.CallScribeApp
import com.callscribe.R
import com.callscribe.data.AppDatabase
import com.callscribe.data.TaskStatus
import com.callscribe.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0) return
        val action = intent.action ?: return
        val appContext = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(appContext).taskDao()
                val task = dao.byId(taskId) ?: return@launch

                when (action) {
                    ACTION_FIRE -> {
                        if (task.status != TaskStatus.OPEN) return@launch
                        notify(appContext, taskId, task.title, subtitle(task.dueAt, task.contactName))
                    }
                    ACTION_DONE -> {
                        dao.update(task.copy(status = TaskStatus.DONE, reminderAt = null))
                        NotificationManagerCompat.from(appContext).cancel(taskId.toInt())
                    }
                    ACTION_SNOOZE -> {
                        val at = System.currentTimeMillis() + 60 * 60_000L
                        val updated = task.copy(reminderAt = at)
                        dao.update(updated)
                        ReminderScheduler.schedule(appContext, updated)
                        NotificationManagerCompat.from(appContext).cancel(taskId.toInt())
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun subtitle(dueAt: Long?, contact: String?): String {
        val parts = mutableListOf<String>()
        contact?.let { parts.add(it) }
        dueAt?.let { parts.add(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("bg")).format(it)) }
        return parts.joinToString(" · ")
    }

    private fun notify(context: Context, taskId: Long, title: String, text: String) {
        val open = PendingIntent.getActivity(
            context, taskId.toInt(), Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CallScribeApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Готово", action(context, taskId, ACTION_DONE))
            .addAction(0, "Отложи 1 час", action(context, taskId, ACTION_SNOOZE))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
        } catch (e: SecurityException) {
            // Липсва разрешение POST_NOTIFICATIONS.
        }
    }

    private fun action(context: Context, taskId: Long, action: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            (taskId.toInt() * 31) + action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val ACTION_FIRE = "com.callscribe.REMINDER_FIRE"
        const val ACTION_DONE = "com.callscribe.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.callscribe.REMINDER_SNOOZE"
    }
}
