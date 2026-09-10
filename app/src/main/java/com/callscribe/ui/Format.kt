package com.callscribe.ui

import android.content.Context
import com.callscribe.R
import com.callscribe.data.CaptureStatus
import com.callscribe.data.Direction
import com.callscribe.data.Kind
import com.callscribe.data.Priority
import com.callscribe.data.TaskStatus
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Форматиране на стойности за интерфейса. Датите следват езика на телефона,
 * а надписите идват от ресурсите, за да се превеждат заедно с всичко останало.
 */
object Format {

    private fun dateTimeFormat() = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())

    private fun dateOnlyFormat() = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    fun dateTime(millis: Long?): String =
        millis?.let { dateTimeFormat().format(it) } ?: EM_DASH

    fun due(millis: Long?, allDay: Boolean): String = when {
        millis == null -> EM_DASH
        allDay -> dateOnlyFormat().format(millis)
        else -> dateTimeFormat().format(millis)
    }

    fun duration(seconds: Int): String =
        String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)

    fun source(context: Context, kind: String): String = context.getString(
        when (kind) {
            Kind.CALL -> R.string.kind_call
            Kind.SMS -> R.string.kind_sms
            else -> R.string.kind_manual
        }
    )

    fun direction(context: Context, direction: String): String = when (direction) {
        Direction.IN -> context.getString(R.string.direction_in)
        Direction.OUT -> context.getString(R.string.direction_out)
        else -> EM_DASH
    }

    fun priority(context: Context, priority: String): String = context.getString(
        when (priority) {
            Priority.HIGH -> R.string.priority_high
            Priority.LOW -> R.string.priority_low
            else -> R.string.priority_normal
        }
    )

    fun status(context: Context, status: String): String = context.getString(
        when (status) {
            TaskStatus.DONE -> R.string.status_done
            TaskStatus.CANCELLED -> R.string.status_cancelled
            else -> R.string.status_open
        }
    )

    fun captureStatus(context: Context, status: String): String = context.getString(
        when (status) {
            CaptureStatus.NEW -> R.string.capture_new
            CaptureStatus.TRANSCRIBING -> R.string.capture_transcribing
            CaptureStatus.ANALYZING -> R.string.capture_analyzing
            CaptureStatus.DONE -> R.string.capture_done
            else -> R.string.capture_error
        }
    )

    fun confidence(value: Float): String = "${(value * 100).toInt()}%"

    const val EM_DASH = "—"
}
