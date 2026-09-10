package com.callscribe.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.callscribe.R
import com.callscribe.capture.CallWatchAccessibilityService
import com.callscribe.data.Languages

@Composable
fun SettingsScreen(model: MainViewModel, onRequestPermissions: () -> Unit) {
    val settings = model.settings
    val context = LocalContext.current

    var recordCalls by remember { mutableStateOf(settings.recordCalls) }
    var forceSpeaker by remember { mutableStateOf(settings.forceSpeaker) }
    var readSms by remember { mutableStateOf(settings.readSms) }
    var autoSync by remember { mutableStateOf(settings.autoSync) }
    var deleteAudio by remember { mutableStateOf(settings.deleteAudioAfterTranscript) }
    var offset by remember { mutableStateOf(settings.reminderOffsetMinutes.toString()) }

    var aiBase by remember { mutableStateOf(settings.aiBaseUrl) }
    var aiKey by remember { mutableStateOf(settings.aiApiKey) }
    var aiModel by remember { mutableStateOf(settings.aiModel) }

    var asrBase by remember { mutableStateOf(settings.asrBaseUrl) }
    var asrKey by remember { mutableStateOf(settings.asrApiKey) }
    var asrModel by remember { mutableStateOf(settings.asrModel) }
    var language by remember { mutableStateOf(settings.language) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionTitle(stringResource(R.string.section_capture))

        Toggle(stringResource(R.string.setting_record_calls), recordCalls) {
            recordCalls = it
            settings.recordCalls = it
        }
        Toggle(stringResource(R.string.setting_force_speaker), forceSpeaker) {
            forceSpeaker = it
            settings.forceSpeaker = it
        }
        Text(
            stringResource(R.string.setting_speaker_note),
            style = MaterialTheme.typography.bodySmall
        )
        Toggle(stringResource(R.string.setting_read_sms), readSms) {
            readSms = it
            settings.readSms = it
        }
        Toggle(stringResource(R.string.setting_auto_sync), autoSync) {
            autoSync = it
            model.setAutoSync(it)
        }
        Text(
            stringResource(R.string.setting_auto_sync_note),
            style = MaterialTheme.typography.bodySmall
        )
        Toggle(stringResource(R.string.setting_delete_audio), deleteAudio) {
            deleteAudio = it
            settings.deleteAudioAfterTranscript = it
        }

        OutlinedButton(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.request_permissions))
        }

        val accessibilityOn = CallWatchAccessibilityService.isEnabled(context)
        Text(
            stringResource(
                if (accessibilityOn) R.string.accessibility_on else R.string.accessibility_off
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (accessibilityOn) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error
        )
        OutlinedButton(
            onClick = { open(context, AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.open_accessibility))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            OutlinedButton(
                onClick = { open(context, AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.allow_exact_alarms))
            }
        }
        OutlinedButton(
            onClick = { open(context, AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.ignore_battery))
        }

        HorizontalDivider()
        SectionTitle(stringResource(R.string.section_reminders))

        OutlinedTextField(
            value = offset,
            onValueChange = {
                offset = it.filter { char -> char.isDigit() }.take(4)
                settings.reminderOffsetMinutes = offset.toIntOrNull() ?: 30
            },
            label = { Text(stringResource(R.string.reminder_offset_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()
        SectionTitle(stringResource(R.string.section_analysis))

        Field(stringResource(R.string.api_address), aiBase, { aiBase = it; settings.aiBaseUrl = it })
        Field(stringResource(R.string.api_key), aiKey, { aiKey = it; settings.aiApiKey = it }, secret = true)
        Field(stringResource(R.string.model), aiModel, { aiModel = it; settings.aiModel = it })
        Text(
            stringResource(R.string.ai_note),
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()
        SectionTitle(stringResource(R.string.section_transcription))

        Field(stringResource(R.string.server_address), asrBase, { asrBase = it; settings.asrBaseUrl = it })
        Field(stringResource(R.string.api_key), asrKey, { asrKey = it; settings.asrApiKey = it }, secret = true)
        Field(stringResource(R.string.model), asrModel, { asrModel = it; settings.asrModel = it })
        LanguageSelector(current = language) {
            language = it
            settings.language = it
        }
        Text(
            stringResource(R.string.language_note),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            stringResource(R.string.asr_note),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LanguageSelector(current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.language_label, Languages.label(current)))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Languages.all.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    secret: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun open(context: Context, action: String) {
    runCatching {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
