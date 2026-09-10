package com.callscribe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callscribe.R

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
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.consent_intro), style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.consent_heading), style = MaterialTheme.typography.titleMedium)

        Bullet(stringResource(R.string.consent_legal))
        Bullet(stringResource(R.string.consent_audio))
        Bullet(stringResource(R.string.consent_network))
        Bullet(stringResource(R.string.consent_storage))
        Bullet(stringResource(R.string.consent_store))

        Spacer(Modifier.height(8.dp))
        ConsentCheck(checked) { checked = it }

        Button(
            onClick = onAccept,
            enabled = checked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.consent_continue))
        }
    }
}

@Composable
private fun ConsentCheck(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(
            stringResource(R.string.consent_checkbox),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("•  ", style = MaterialTheme.typography.bodySmall)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
