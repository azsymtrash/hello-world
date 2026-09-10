package com.callscribe.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.callscribe.capture.CallWatchAccessibilityService

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
        SectionTitle("Заснемане")

        Toggle("Записвай разговорите", recordCalls) {
            recordCalls = it
            settings.recordCalls = it
        }
        Toggle("Включвай високоговорителя при запис", forceSpeaker) {
            forceSpeaker = it
            settings.forceSpeaker = it
        }
        Text(
            "Без високоговорител микрофонът обикновено улавя само твоя глас — Android не дава " +
                "достъп до аудиото на отсрещната страна.",
            style = MaterialTheme.typography.bodySmall
        )
        Toggle("Чети входящи съобщения", readSms) {
            readSms = it
            settings.readSms = it
        }
        Toggle("Засичай новите съобщения автоматично", autoSync) {
            autoSync = it
            model.setAutoSync(it)
        }
        Text(
            "Проверява на всеки 15 минути и при отваряне на приложението. Изключено, " +
                "новите съобщения влизат само когато натиснеш бутона в „Източници“.",
            style = MaterialTheme.typography.bodySmall
        )
        Toggle("Трий аудиото след транскрипция", deleteAudio) {
            deleteAudio = it
            settings.deleteAudioAfterTranscript = it
        }

        OutlinedButton(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
            Text("Поискай разрешенията наново")
        }

        val accessibilityOn = CallWatchAccessibilityService.isEnabled(context)
        Text(
            if (accessibilityOn) "Услугата за следене на разговори е включена."
            else "Услугата за следене на разговори е изключена — автоматичният запис няма да тръгва надеждно.",
            style = MaterialTheme.typography.bodySmall,
            color = if (accessibilityOn) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error
        )
        OutlinedButton(
            onClick = { open(context, AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Настройки за достъпност")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            OutlinedButton(
                onClick = { open(context, AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Разреши точни аларми")
            }
        }
        OutlinedButton(
            onClick = { open(context, AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Изключи оптимизацията на батерията")
        }

        HorizontalDivider()
        SectionTitle("Напомняния")

        OutlinedTextField(
            value = offset,
            onValueChange = {
                offset = it.filter { char -> char.isDigit() }.take(4)
                settings.reminderOffsetMinutes = offset.toIntOrNull() ?: 30
            },
            label = { Text("Колко минути преди срока да напомня") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()
        SectionTitle("Анализ (Claude)")

        Field("Адрес на API", aiBase, { aiBase = it; settings.aiBaseUrl = it })
        Field("API ключ", aiKey, { aiKey = it; settings.aiApiKey = it }, secret = true)
        Field("Модел", aiModel, { aiModel = it; settings.aiModel = it })
        Text(
            "Ключът се пази в паметта на приложението и не е криптиран. По-безопасно е да " +
                "насочиш адреса към собствен прокси сървър и да оставиш полето за ключ празно. " +
                "Модели: claude-opus-5 (най-точен), claude-sonnet-5 (баланс), claude-haiku-4-5 (най-евтин).",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()
        SectionTitle("Транскрипция")

        Field("Адрес на сървъра", asrBase, { asrBase = it; settings.asrBaseUrl = it })
        Field("API ключ", asrKey, { asrKey = it; settings.asrApiKey = it }, secret = true)
        Field("Модел", asrModel, { asrModel = it; settings.asrModel = it })
        Field("Език", language, { language = it; settings.language = it })
        Text(
            "Очаква се OpenAI-съвместим endpoint POST {адрес}/v1/audio/transcriptions. " +
                "Работи с whisper.cpp сървър в локалната мрежа, ако не искаш аудиото да напуска дома ти.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(24.dp))
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
