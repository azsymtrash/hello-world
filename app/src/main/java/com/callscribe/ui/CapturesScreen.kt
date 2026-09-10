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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callscribe.R
import com.callscribe.data.Capture
import com.callscribe.R
import com.callscribe.data.CaptureStatus
import com.callscribe.data.Kind
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
                Text(stringResource(R.string.add_text))
            }
            OutlinedButton(
                onClick = { audioLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.upload_audio))
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { model.syncNow() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.check_now))
        }
        Text(
            stringResource(R.string.auto_sync_note),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(12.dp))

        if (captures.isEmpty()) {
            Text(
                stringResource(R.string.empty_no_sources),
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
            title = { Text(stringResource(R.string.add_text_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text(stringResource(R.string.contact_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(R.string.text_hint)) },
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
                ) { Text(stringResource(R.string.analyze)) }
            },
            dismissButton = { TextButton(onClick = { showText = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    detail?.let { capture ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(Format.source(context, capture.kind) + " · " + Format.dateTime(capture.startedAt)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        stringResource(
                            R.string.contact_line,
                            capture.contactName ?: capture.phone ?: Format.EM_DASH
                        )
                    )
                    Text(
                        stringResource(
                            R.string.direction_line,
                            Format.direction(context, capture.direction)
                        )
                    )
                    if (capture.kind == Kind.CALL) {
                        Text(
                            stringResource(
                                R.string.duration_line,
                                Format.duration(capture.durationSec)
                            )
                        )
                    }
                    Text(
                        stringResource(
                            R.string.state_line,
                            Format.captureStatus(context, capture.status)
                        )
                    )
                    capture.error?.let {
                        Text(
                            stringResource(R.string.error_line, it),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.text_label), fontWeight = FontWeight.SemiBold)
                    Text(capture.text?.takeIf { it.isNotBlank() } ?: stringResource(R.string.text_none))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    model.reprocess(capture)
                    detail = null
                }) { Text(stringResource(R.string.analyze_again)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        model.deleteCapture(capture)
                        detail = null
                    }) { Text(stringResource(R.string.delete)) }
                    TextButton(onClick = { detail = null }) { Text(stringResource(R.string.close)) }
                }
            }
        )
    }
}

@Composable
private fun CaptureCard(capture: Capture, onClick: () -> Unit) {
    val context = LocalContext.current
    val who = capture.contactName ?: capture.phone ?: stringResource(R.string.unknown_contact)

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${Format.source(context, capture.kind)} · $who",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                Format.dateTime(capture.startedAt) +
                    if (capture.kind == Kind.CALL) {
                        " · ${Format.duration(capture.durationSec)}"
                    } else {
                        ""
                    },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                Format.captureStatus(context, capture.status) +
                    if (capture.status == CaptureStatus.DONE) {
                        " · " + stringResource(R.string.tasks_found, capture.tasksFound)
                    } else {
                        ""
                    },
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
