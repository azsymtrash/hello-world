package com.callscribe.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callscribe.data.AppDatabase
import com.callscribe.data.Capture
import com.callscribe.data.CaptureStatus
import com.callscribe.data.Kind
import com.callscribe.data.Settings
import com.callscribe.data.TaskRow
import com.callscribe.data.TaskStatus
import com.callscribe.reminder.ReminderScheduler
import com.callscribe.work.ProcessCaptureWorker
import com.callscribe.work.SmsSyncWorker
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
            .takeIf { task.status == TaskStatus.OPEN }
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

    /** Отмята задачата като направена — или я връща обратно на отворена. */
    fun toggleDone(task: TaskRow) {
        val next = if (task.status == TaskStatus.DONE) TaskStatus.OPEN else TaskStatus.DONE
        updateTask(task.copy(status = next))
    }

    fun markDone(task: TaskRow) {
        if (task.status != TaskStatus.DONE) updateTask(task.copy(status = TaskStatus.DONE))
    }

    /** Ръчно бутане на фоновото засичане; иначе то върви само на всеки 15 минути. */
    fun syncNow() {
        SmsSyncWorker.syncNow(getApplication())
        _message.value = "Проверявам за нови съобщения…"
    }

    fun setAutoSync(enabled: Boolean) {
        settings.autoSync = enabled
        if (enabled) {
            SmsSyncWorker.schedulePeriodic(getApplication())
            SmsSyncWorker.syncNow(getApplication())
        } else {
            SmsSyncWorker.cancelPeriodic(getApplication())
        }
    }

    fun rescheduleAll() = viewModelScope.launch(Dispatchers.IO) {
        db.taskDao().withPendingReminders().forEach { ReminderScheduler.schedule(getApplication(), it) }
    }
}
