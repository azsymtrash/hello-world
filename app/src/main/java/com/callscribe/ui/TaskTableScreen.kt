package com.callscribe.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callscribe.data.Priority
import com.callscribe.data.TaskRow
import com.callscribe.data.TaskStatus
import com.callscribe.export.CsvExporter
import java.util.Calendar

private enum class SortKey(val label: String) {
    CREATED("Създадено"),
    SOURCE("Източник"),
    CONTACT("Контакт"),
    TITLE("Задача"),
    DUE("Краен срок"),
    PRIORITY("Приоритет"),
    STATUS("Статус"),
    CONFIDENCE("Увереност")
}

/** Колоната с отметката „направено“ стои преди подредимите колони. */
private val doneColumnWidth = 52.dp

private val columnWidths = mapOf(
    SortKey.CREATED to 108.dp,
    SortKey.SOURCE to 96.dp,
    SortKey.CONTACT to 132.dp,
    SortKey.TITLE to 260.dp,
    SortKey.DUE to 124.dp,
    SortKey.PRIORITY to 96.dp,
    SortKey.STATUS to 96.dp,
    SortKey.CONFIDENCE to 88.dp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTableScreen(model: MainViewModel) {
    val tasks by model.tasks.collectAsState()
    val context = LocalContext.current

    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(TaskStatus.OPEN) }
    var sortKey by remember { mutableStateOf(SortKey.DUE) }
    var ascending by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<TaskRow?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(CsvExporter.toCsv(tasks).toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    val visible = remember(tasks, query, statusFilter, sortKey, ascending) {
        filterAndSort(tasks, query, statusFilter, sortKey, ascending)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Търсене") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                )
                IconButton(onClick = { exportLauncher.launch("napomnyania.csv") }) {
                    Icon(Icons.Filled.Download, contentDescription = "Експорт в CSV")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == TaskStatus.OPEN,
                    onClick = { statusFilter = TaskStatus.OPEN },
                    label = { Text("Отворени") }
                )
                FilterChip(
                    selected = statusFilter == TaskStatus.DONE,
                    onClick = { statusFilter = TaskStatus.DONE },
                    label = { Text("Готови") }
                )
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("Всички") }
                )
                Text(
                    "${visible.size} реда",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 10.dp, start = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            if (visible.isEmpty()) {
                EmptyState(tasks.isEmpty())
            } else {
                val horizontal = rememberScrollState()
                val tableWidth = columnWidths.values.fold(doneColumnWidth) { acc, dp -> acc + dp }

                Column(Modifier.horizontalScroll(horizontal)) {
                    HeaderRow(
                        width = tableWidth,
                        sortKey = sortKey,
                        ascending = ascending,
                        onSort = { key ->
                            if (key == sortKey) ascending = !ascending else {
                                sortKey = key
                                ascending = true
                            }
                        }
                    )
                    HorizontalDivider()
                    LazyColumn(Modifier.width(tableWidth)) {
                        items(visible, key = { it.id }) { task ->
                            TaskRowView(
                                task = task,
                                onToggleDone = { model.toggleDone(task) },
                                onClick = { selected = task }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Нова задача")
        }
    }

    selected?.let { task ->
        TaskDialog(
            task = task,
            onDismiss = { selected = null },
            onSave = {
                model.updateTask(it)
                selected = null
            },
            onDelete = {
                model.deleteTask(task)
                selected = null
            }
        )
    }

    if (showAdd) {
        AddTaskDialog(
            onDismiss = { showAdd = false },
            onAdd = { title, contact, dueAt ->
                model.addManualTask(title, dueAt, contact)
                showAdd = false
            }
        )
    }
}

@Composable
private fun EmptyState(noTasksAtAll: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (noTasksAtAll) "Още няма извлечени задачи." else "Няма редове по този филтър.",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        if (noTasksAtAll) {
            Text(
                "Задачите се появяват тук автоматично след разговор или съобщение. " +
                    "Можеш да добавиш и текст ръчно от раздел „Източници“.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun HeaderRow(
    width: androidx.compose.ui.unit.Dp,
    sortKey: SortKey,
    ascending: Boolean,
    onSort: (SortKey) -> Unit
) {
    Row(
        modifier = Modifier
            .width(width)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = "Готово",
            modifier = Modifier.width(doneColumnWidth).padding(horizontal = 6.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        SortKey.entries.forEach { key ->
            val arrow = if (key == sortKey) (if (ascending) " ▲" else " ▼") else ""
            Text(
                text = key.label + arrow,
                modifier = Modifier
                    .width(columnWidths.getValue(key))
                    .clickable { onSort(key) }
                    .padding(horizontal = 8.dp),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TaskRowView(task: TaskRow, onToggleDone: () -> Unit, onClick: () -> Unit) {
    val done = task.status == TaskStatus.DONE
    val overdue = task.dueAt != null &&
        task.dueAt < System.currentTimeMillis() &&
        task.status == TaskStatus.OPEN
    val decoration = if (done) TextDecoration.LineThrough else TextDecoration.None

    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(doneColumnWidth)) {
            Checkbox(checked = done, onCheckedChange = { onToggleDone() })
        }
        Cell(Format.dateTime(task.createdAt), SortKey.CREATED, decoration = decoration)
        Cell(Format.source(task.source), SortKey.SOURCE, decoration = decoration)
        Cell(task.contactName ?: task.phone ?: "—", SortKey.CONTACT, decoration = decoration)
        Cell(task.title, SortKey.TITLE, maxLines = 2, decoration = decoration)
        Cell(
            Format.due(task.dueAt, task.allDay),
            SortKey.DUE,
            color = if (overdue) MaterialTheme.colorScheme.error else Color.Unspecified,
            decoration = decoration
        )
        Cell(Format.priority(task.priority), SortKey.PRIORITY, decoration = decoration)
        Cell(Format.status(task.status), SortKey.STATUS, decoration = decoration)
        Cell(Format.confidence(task.confidence), SortKey.CONFIDENCE, decoration = decoration)
    }
}

@Composable
private fun Cell(
    text: String,
    key: SortKey,
    maxLines: Int = 1,
    color: Color = Color.Unspecified,
    decoration: TextDecoration = TextDecoration.None
) {
    Text(
        text = text,
        modifier = Modifier
            .width(columnWidths.getValue(key))
            .padding(horizontal = 8.dp),
        fontSize = 13.sp,
        color = color,
        textDecoration = decoration,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

private fun filterAndSort(
    tasks: List<TaskRow>,
    query: String,
    statusFilter: String?,
    sortKey: SortKey,
    ascending: Boolean
): List<TaskRow> {
    val needle = query.trim().lowercase()
    val filtered = tasks.filter { task ->
        (statusFilter == null || task.status == statusFilter) &&
            (needle.isEmpty() ||
                task.title.lowercase().contains(needle) ||
                task.contactName.orEmpty().lowercase().contains(needle) ||
                task.phone.orEmpty().lowercase().contains(needle) ||
                task.details.orEmpty().lowercase().contains(needle))
    }

    val comparator = when (sortKey) {
        SortKey.CREATED -> compareBy<TaskRow> { it.createdAt }
        SortKey.SOURCE -> compareBy { it.source }
        SortKey.CONTACT -> compareBy { (it.contactName ?: it.phone ?: "").lowercase() }
        SortKey.TITLE -> compareBy { it.title.lowercase() }
        SortKey.DUE -> compareBy { it.dueAt ?: Long.MAX_VALUE }
        SortKey.PRIORITY -> compareBy {
            when (it.priority) {
                Priority.HIGH -> 0
                Priority.NORMAL -> 1
                else -> 2
            }
        }
        SortKey.STATUS -> compareBy { it.status }
        SortKey.CONFIDENCE -> compareBy { it.confidence }
    }

    return if (ascending) filtered.sortedWith(comparator) else filtered.sortedWith(comparator.reversed())
}

@Composable
private fun TaskDialog(
    task: TaskRow,
    onDismiss: () -> Unit,
    onSave: (TaskRow) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf(task) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Задача") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    label = { Text("Задача") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.contactName.orEmpty(),
                    onValueChange = { draft = draft.copy(contactName = it) },
                    label = { Text("Контакт") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Източник: ${Format.source(draft.source)} · ${Format.dateTime(draft.createdAt)}",
                    style = MaterialTheme.typography.bodySmall)

                Button(
                    onClick = {
                        draft = draft.copy(
                            status = if (draft.status == TaskStatus.DONE) TaskStatus.OPEN else TaskStatus.DONE
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (draft.status == TaskStatus.DONE) "Върни като отворена" else "Направено")
                }

                TextButton(onClick = {
                    pickDateTime(context, draft.dueAt) { draft = draft.copy(dueAt = it, allDay = false) }
                }) {
                    Text("Краен срок: " + Format.due(draft.dueAt, draft.allDay))
                }
                if (draft.dueAt != null) {
                    TextButton(onClick = { draft = draft.copy(dueAt = null) }) {
                        Text("Премахни срока")
                    }
                }

                Selector(
                    label = "Приоритет",
                    current = Format.priority(draft.priority),
                    options = listOf(Priority.HIGH, Priority.NORMAL, Priority.LOW),
                    render = { Format.priority(it) },
                    onSelect = { draft = draft.copy(priority = it) }
                )
                Selector(
                    label = "Статус",
                    current = Format.status(draft.status),
                    options = listOf(TaskStatus.OPEN, TaskStatus.DONE, TaskStatus.CANCELLED),
                    render = { Format.status(it) },
                    onSelect = { draft = draft.copy(status = it) }
                )

                draft.details?.takeIf { it.isNotBlank() }?.let {
                    Text("Детайли: $it", style = MaterialTheme.typography.bodySmall)
                }
                draft.quote?.takeIf { it.isNotBlank() }?.let {
                    Text("Цитат: „$it“", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Увереност на модела: ${Format.confidence(draft.confidence)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }) { Text("Запази") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Изтрий") }
                TextButton(onClick = onDismiss) { Text("Отказ") }
            }
        }
    )
}

@Composable
private fun <T> Selector(
    label: String,
    current: String,
    options: List<T>,
    render: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text("$label: $current") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(render(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String?, Long?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нова задача") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Какво трябва да се направи") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Контакт (по избор)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                TextButton(onClick = { pickDateTime(context, dueAt) { dueAt = it } }) {
                    Text("Краен срок: " + Format.due(dueAt, false))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title.trim(), contact.trim().ifBlank { null }, dueAt) },
                enabled = title.isNotBlank()
            ) { Text("Добави") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отказ") } }
    )
}

private fun pickDateTime(context: Context, current: Long?, onPicked: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = current ?: System.currentTimeMillis() }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val picked = Calendar.getInstance().apply {
                        set(year, month, day, hour, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(picked.timeInMillis)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
