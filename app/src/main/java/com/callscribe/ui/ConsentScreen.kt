package com.callscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConsentScreen(onAccept: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("CallScribe", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Приложението записва телефонни разговори и чете съобщения, превръща ги в текст " +
                "и извлича задачи с краен срок в таблица с напомняния.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(8.dp))
        Text("Преди да продължиш, прочети следното:", style = MaterialTheme.typography.titleMedium)

        Bullet("Записът на разговор без знанието на отсрещната страна е незаконен в много държави. В България записът на собствен разговор е допустим, но разпространението му без съгласие не е. Отговорността е твоя.")
        Bullet("От Android 10 системата не дава достъп до аудиото на отсрещната страна. На повечето телефони записът минава през микрофона — отсрещната страна се чува ясно само при включен високоговорител.")
        Bullet("Транскрипцията и анализът се изпращат към конфигурирания от теб сървър. Съдържанието на разговорите ти напуска телефона. Ако това не ти е приемливо, използвай локален сървър за транскрипция.")
        Bullet("Записите и текстовете се пазят само на този телефон, в частната памет на приложението.")
        Bullet("Приложението не може да бъде публикувано в Google Play — правилата забраняват запис на разговори и достъп до SMS за приложения, които не са SMS клиент по подразбиране.")

        Spacer(Modifier.height(8.dp))
        Row(checked) { checked = it }

        Button(
            onClick = onAccept,
            enabled = checked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Разбрах, продължи")
        }
    }
}

@Composable
private fun Row(checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(
            "Разбирам изискванията и поемам отговорност за законосъобразната употреба.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun Bullet(text: String) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
        Text("•  ", style = MaterialTheme.typography.bodySmall)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
