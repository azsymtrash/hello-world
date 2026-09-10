package com.callscribe.export

import android.content.Context
import com.callscribe.R
import com.callscribe.data.TaskRow
import com.callscribe.ui.Format
import java.util.Locale

object CsvExporter {

    private val headerIds = intArrayOf(
        R.string.csv_created, R.string.csv_source, R.string.csv_contact, R.string.csv_phone,
        R.string.csv_task, R.string.csv_details, R.string.csv_due, R.string.csv_all_day,
        R.string.csv_priority, R.string.csv_category, R.string.csv_status,
        R.string.csv_confidence, R.string.csv_reminder, R.string.csv_quote
    )

    fun toCsv(context: Context, tasks: List<TaskRow>): String {
        val builder = StringBuilder()
        // BOM, за да отвори Excel кирилицата коректно.
        builder.append('﻿')
        builder.append(headerIds.joinToString(",") { escape(context.getString(it)) }).append("\r\n")

        val yes = context.getString(R.string.yes)
        val no = context.getString(R.string.no)

        for (task in tasks) {
            val row = listOf(
                Format.dateTime(task.createdAt),
                Format.source(context, task.source),
                task.contactName.orEmpty(),
                task.phone.orEmpty(),
                task.title,
                task.details.orEmpty(),
                task.dueAt?.let { Format.due(it, task.allDay) }.orEmpty(),
                if (task.allDay) yes else no,
                Format.priority(context, task.priority),
                task.category.orEmpty(),
                Format.status(context, task.status),
                String.format(Locale.US, "%.2f", task.confidence),
                task.reminderAt?.let { Format.dateTime(it) }.orEmpty(),
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
