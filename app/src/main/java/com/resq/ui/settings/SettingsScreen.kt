package com.resq.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.ResQDimens
import com.resq.ui.theme.ThemeMode

@Composable
fun SettingsScreen(deviceId: String, themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = ResQDimens.page)) {
        ResQHeader("Settings")
        Text("Appearance", fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        ResQCard(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEach { mode ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                    RadioButton(selected = themeMode == mode, onClick = { onThemeChange(mode) })
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Device", fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        ResQCard(Modifier.fillMaxWidth()) {
            Text("Temporary device ID", style = MaterialTheme.typography.labelMedium)
            Text(deviceId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text("Stored locally and reused after app restart.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
