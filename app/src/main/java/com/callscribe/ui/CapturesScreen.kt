package com.callscribe.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callscribe.data.Capture
import com.callscribe.data.CaptureStatus
import java.io.File

@Composable
fun CapturesScreen(model: MainViewModel) {
    val captures by model.captures.collectAsState()
    val context = LocalContext.current
    var showText by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Capture?>(null) }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val dir = File(context.filesDir, "imports").apply { mkdirs() }
            val target = File(dir, "audio-${System.currentTimeMillis()}.m4a")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            model.addAudioCapture(target.absolutePath, null)
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showText = true }, modifier = Modifier.weight(1f)) {
                Text("Добави текст")
            }
            OutlinedButton(
                onClick = { audioLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Качи аудио")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { model.importSms(7) }, modifier = Modifier.weight(1f)) {
                Text("Внеси SMS (7 дни)")
            }
            OutlinedButton(onClick = { model.importSms(30) }, modifier = Modifier.weight(1f)) {
                Text("Внеси SMS (30 дни)")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (captures.isEmpty()) {
            Text(
                "Още няма записани източници. Разговорите се появяват тук автоматично, " +
                    "ако записът е включен в настройките.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(captures, key = { it.id }) { capture ->
                    CaptureCard(capture) { detail = capture }
                }
            }
        }
    }

    if (showText) {
        var text by remember { mutableStateOf("") }
        var contact by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showText = false },
            title = { Text("Добави текст за анализ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Контакт (по избор)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Съобщение или бележка от разговор") },
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.addTextCapture(text.trim(), contact.trim())
                        showText = false
                    },
                    enabled = text.isNotBlank()
                ) { Text("Анализирай") }
            },
            dismissButton = { TextButton(onClick = { showText = false }) { Text("Отказ") } }
        )
    }

    detail?.let { capture ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(Format.source(capture.kind) + " · " + Format.dateTime(capture.startedAt)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Контакт: ${capture.contactName ?: capture.phone ?: "—"}")
                    Text("Посока: ${Format.direction(capture.direction)}")
                    if (capture.kind == com.callscribe.data.Kind.CALL) {
                        Text("Времетраене: ${Format.duration(capture.durationSec)}")
                    }
                    Text("Състояние: ${statusLabel(capture.status)}")
                    capture.error?.let {
                        Text("Грешка: $it", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Текст:", fontWeight = FontWeight.SemiBold)
                    Text(capture.text?.takeIf { it.isNotBlank() } ?: "— няма —")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    model.reprocess(capture)
                    detail = null
                }) { Text("Анализирай наново") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        model.deleteCapture(capture)
                        detail = null
                    }) { Text("Изтрий") }
                    TextButton(onClick = { detail = null }) { Text("Затвори") }
                }
            }
        )
    }
}

@Composable
private fun CaptureCard(capture: Capture, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${Format.source(capture.kind)} · ${capture.contactName ?: capture.phone ?: "неизвестен"}",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                Format.dateTime(capture.startedAt) +
                    if (capture.kind == com.callscribe.data.Kind.CALL) " · ${Format.duration(capture.durationSec)}" else "",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                statusLabel(capture.status) +
                    if (capture.status == CaptureStatus.DONE) " · ${capture.tasksFound} задачи" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (capture.status == CaptureStatus.ERROR) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            capture.text?.takeIf { it.isNotBlank() }?.let {
                Text(it.take(140), style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    CaptureStatus.NEW -> "Чака обработка"
    CaptureStatus.TRANSCRIBING -> "Транскрибира се"
    CaptureStatus.ANALYZING -> "Анализира се"
    CaptureStatus.DONE -> "Готово"
    else -> "Грешка"
}
