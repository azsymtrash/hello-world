package com.callscribe.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object Kind {
    const val CALL = "CALL"
    const val SMS = "SMS"
    const val MANUAL = "MANUAL"
}

object Direction {
    const val IN = "IN"
    const val OUT = "OUT"
    const val UNKNOWN = "UNKNOWN"
}

object CaptureStatus {
    const val NEW = "NEW"
    const val TRANSCRIBING = "TRANSCRIBING"
    const val ANALYZING = "ANALYZING"
    const val DONE = "DONE"
    const val ERROR = "ERROR"
}

object TaskStatus {
    const val OPEN = "OPEN"
    const val DONE = "DONE"
    const val CANCELLED = "CANCELLED"
}

object Priority {
    const val LOW = "LOW"
    const val NORMAL = "NORMAL"
    const val HIGH = "HIGH"
}

/** Суров източник: един разговор или едно текстово съобщение. */
@Entity(tableName = "captures", indices = [Index("startedAt")])
data class Capture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val direction: String = Direction.UNKNOWN,
    val contactName: String? = null,
    val phone: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val durationSec: Int = 0,
    val audioPath: String? = null,
    val text: String? = null,
    val status: String = CaptureStatus.NEW,
    val error: String? = null,
    val tasksFound: Int = 0
)

/** Един ред в таблицата с напомняния. */
@Entity(tableName = "tasks", indices = [Index("dueAt"), Index("status")])
data class TaskRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val captureId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = Kind.MANUAL,
    val contactName: String? = null,
    val phone: String? = null,
    val title: String,
    val details: String? = null,
    val dueAt: Long? = null,
    val allDay: Boolean = false,
    val priority: String = Priority.NORMAL,
    val category: String? = null,
    val status: String = TaskStatus.OPEN,
    val confidence: Float = 1f,
    val quote: String? = null,
    val reminderAt: Long? = null
)
