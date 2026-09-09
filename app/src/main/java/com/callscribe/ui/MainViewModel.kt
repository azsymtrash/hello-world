package com.callscribe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callscribe.capture.SmsImporter
import com.callscribe.data.AppDatabase
import com.callscribe.data.Capture
import com.callscribe.data.CaptureStatus
import com.callscribe.data.Kind
import com.callscribe.data.Settings
import com.callscribe.data.TaskRow
import com.callscribe.reminder.ReminderScheduler
import com.callscribe.work.ProcessCaptureWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)
    val settings = Settings(application)

    val tasks: StateFlow<List<TaskRow>> = db.taskDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val captures: StateFlow<List<Capture>> = db.captureDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun clearMessage() {
        _message.value = null
    }

    fun updateTask(task: TaskRow) = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        ReminderScheduler.cancel(context, task.id)
        val reminderAt = ReminderScheduler.reminderTimeFor(task, settings.reminderOffsetMinutes)
            .takeIf { task.status == com.callscribe.data.TaskStatus.OPEN }
        val updated = task.copy(reminderAt = reminderAt)
        db.taskDao().update(updated)
        if (reminderAt != null) ReminderScheduler.schedule(context, updated)
    }

    fun deleteTask(task: TaskRow) = viewModelScope.launch(Dispatchers.IO) {
        ReminderScheduler.cancel(getApplication(), task.id)
        db.taskDao().delete(task)
    }

    fun addManualTask(title: String, dueAt: Long?, contact: String?) = viewModelScope.launch(Dispatchers.IO) {
        val task = TaskRow(
            source = Kind.MANUAL,
            title = title,
            contactName = contact?.takeIf { it.isNotBlank() },
            dueAt = dueAt,
            confidence = 1f
        )
        val reminderAt = ReminderScheduler.reminderTimeFor(task, settings.reminderOffsetMinutes)
        val id = db.taskDao().insert(task.copy(reminderAt = reminderAt))
        if (reminderAt != null) {
            ReminderScheduler.schedule(getApplication(), task.copy(id = id, reminderAt = reminderAt))
        }
    }

    fun addTextCapture(text: String, contact: String?) = viewModelScope.launch(Dispatchers.IO) {
        val id = db.captureDao().insert(
            Capture(
                kind = Kind.SMS,
                contactName = contact?.takeIf { it.isNotBlank() },
                text = text,
                status = CaptureStatus.NEW
            )
        )
        ProcessCaptureWorker.enqueue(getApplication(), id)
        _message.value = "Текстът е добавен за анализ."
    }

    fun addAudioCapture(path: String, contact: String?) = viewModelScope.launch(Dispatchers.IO) {
        val id = db.captureDao().insert(
            Capture(
                kind = Kind.CALL,
                contactName = contact?.takeIf { it.isNotBlank() },
                audioPath = path,
                status = CaptureStatus.NEW
            )
        )
        ProcessCaptureWorker.enqueue(getApplication(), id)
        _message.value = "Записът е добавен за транскрипция."
    }

    fun reprocess(capture: Capture) = viewModelScope.launch(Dispatchers.IO) {
        db.captureDao().update(capture.copy(status = CaptureStatus.NEW, error = null))
        ProcessCaptureWorker.enqueue(getApplication(), capture.id)
        _message.value = "Обработката е пусната наново."
    }

    fun deleteCapture(capture: Capture) = viewModelScope.launch(Dispatchers.IO) {
        capture.audioPath?.let { runCatching { File(it).delete() } }
        db.taskDao().byCapture(capture.id).forEach {
            ReminderScheduler.cancel(getApplication(), it.id)
            db.taskDao().delete(it)
        }
        db.captureDao().delete(capture)
    }

    fun importSms(days: Int) = viewModelScope.launch(Dispatchers.IO) {
        val since = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        val count = runCatching { SmsImporter.importSince(getApplication(), since) }.getOrDefault(0)
        _message.value = if (count > 0) "Внесени $count съобщения." else "Няма нови съобщения за внасяне."
    }

    fun rescheduleAll() = viewModelScope.launch(Dispatchers.IO) {
        db.taskDao().withPendingReminders().forEach { ReminderScheduler.schedule(getApplication(), it) }
    }
}
