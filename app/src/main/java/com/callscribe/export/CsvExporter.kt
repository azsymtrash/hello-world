package com.callscribe.export

import com.callscribe.data.TaskRow
import java.text.SimpleDateFormat
import java.util.Locale

object CsvExporter {

    private val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("bg"))

    private val headers = listOf(
        "Създадено", "Източник", "Контакт", "Телефон", "Задача", "Детайли",
        "Краен срок", "Цял ден", "Приоритет", "Категория", "Статус", "Увереност",
        "Напомняне", "Цитат"
    )

    fun toCsv(tasks: List<TaskRow>): String {
        val builder = StringBuilder()
        // BOM, за да отвори Excel кирилицата коректно.
        builder.append('﻿')
        builder.append(headers.joinToString(",") { escape(it) }).append("\r\n")

        for (task in tasks) {
            val row = listOf(
                formatter.format(task.createdAt),
                task.source,
                task.contactName.orEmpty(),
                task.phone.orEmpty(),
                task.title,
                task.details.orEmpty(),
                task.dueAt?.let { formatter.format(it) }.orEmpty(),
                if (task.allDay) "да" else "не",
                task.priority,
                task.category.orEmpty(),
                task.status,
                String.format(Locale.US, "%.2f", task.confidence),
                task.reminderAt?.let { formatter.format(it) }.orEmpty(),
                task.quote.orEmpty()
            )
            builder.append(row.joinToString(",") { escape(it) }).append("\r\n")
        }
        return builder.toString()
    }

    private fun escape(value: String): String {
        val cleaned = value.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ')
        return "\"" + cleaned.replace("\"", "\"\"") + "\""
    }
}
