package com.callscribe.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.callscribe.data.TaskRow

object ReminderScheduler {

    const val EXTRA_TASK_ID = "task_id"

    /** Кога да звънне напомнянето за дадена задача, или null ако няма срок. */
    fun reminderTimeFor(task: TaskRow, offsetMinutes: Int): Long? {
        val due = task.dueAt ?: return null
        val offset = if (task.allDay) 0L else offsetMinutes * 60_000L
        val at = due - offset
        // Ако моментът вече е минал, но срокът е в бъдещето, напомняме веднага при насрочване.
        return when {
            at > System.currentTimeMillis() -> at
            due > System.currentTimeMillis() -> System.currentTimeMillis() + 60_000L
            else -> null
        }
    }

    fun schedule(context: Context, task: TaskRow) {
        val at = task.reminderAt ?: return
        if (at <= System.currentTimeMillis()) return

        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context, task.id)

        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
            if (canExact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
        } catch (e: SecurityException) {
            Log.w("ReminderScheduler", "Няма право за точни аларми: ${e.message}")
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun cancel(context: Context, taskId: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        manager.cancel(pendingIntent(context, taskId))
    }

    private fun pendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            .putExtra(EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
