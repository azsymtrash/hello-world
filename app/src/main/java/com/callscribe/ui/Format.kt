package com.callscribe.ui

import com.callscribe.data.Direction
import com.callscribe.data.Kind
import com.callscribe.data.Priority
import com.callscribe.data.TaskStatus
import java.text.SimpleDateFormat
import java.util.Locale

private val bg = Locale("bg")
private val dateTimeFormat = SimpleDateFormat("dd.MM.yy HH:mm", bg)
private val dateOnlyFormat = SimpleDateFormat("dd.MM.yyyy", bg)

object Format {

    fun dateTime(millis: Long?): String = millis?.let { dateTimeFormat.format(it) } ?: "—"

    fun date(millis: Long?): String = millis?.let { dateOnlyFormat.format(it) } ?: "—"

    fun due(millis: Long?, allDay: Boolean): String = when {
        millis == null -> "—"
        allDay -> dateOnlyFormat.format(millis)
        else -> dateTimeFormat.format(millis)
    }

    fun duration(seconds: Int): String {
        val minutes = seconds / 60
        val rest = seconds % 60
        return String.format(bg, "%d:%02d", minutes, rest)
    }

    fun source(kind: String): String = when (kind) {
        Kind.CALL -> "Разговор"
        Kind.SMS -> "Съобщение"
        else -> "Ръчно"
    }

    fun direction(direction: String): String = when (direction) {
        Direction.IN -> "Входящ"
        Direction.OUT -> "Изходящ"
        else -> "—"
    }

    fun priority(priority: String): String = when (priority) {
        Priority.HIGH -> "Висок"
        Priority.LOW -> "Нисък"
        else -> "Нормален"
    }

    fun status(status: String): String = when (status) {
        TaskStatus.DONE -> "Готово"
        TaskStatus.CANCELLED -> "Отказано"
        else -> "Отворено"
    }

    fun confidence(value: Float): String = "${(value * 100).toInt()}%"
}
