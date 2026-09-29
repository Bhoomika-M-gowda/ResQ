package com.resq.ui.decision

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resq.ai.decision.*
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.ResQDimens

@Composable
fun DecisionScreen(
    decision: DecisionResult,
    onBack: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenWifi: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = ResQDimens.page)) {
        ResQHeader("RESQ Decision Engine", onBack)
        Text("Current Conditions", fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        ResQCard(Modifier.fillMaxWidth()) {
            decision.factors.forEachIndexed { index, factor ->
                Text(factor, style = MaterialTheme.typography.bodyMedium)
                if (index != decision.factors.lastIndex) HorizontalDivider(Modifier.padding(vertical = 6.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Recommended Path", fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        ResQCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (decision.method) {
                        CommunicationMethod.BLUETOOTH -> Icons.Default.Bluetooth
                        CommunicationMethod.WIFI_LOCAL -> Icons.Default.Wifi
                        CommunicationMethod.STORE_RETRY -> Icons.Default.Inventory2
                    },
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(decision.method.name.replace('_', ' '), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    Text(decision.reason, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        when (decision.method) {
            CommunicationMethod.BLUETOOTH -> Button(onClick = onOpenBluetooth, modifier = Modifier.fillMaxWidth()) { Text("OPEN BLUETOOTH MESH") }
            CommunicationMethod.WIFI_LOCAL -> Button(onClick = onOpenWifi, modifier = Modifier.fillMaxWidth()) { Text("OPEN WI-FI LOCAL") }
            CommunicationMethod.STORE_RETRY -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("STORED - RETRY WHEN A PEER APPEARS") }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Route, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("The rules are deterministic and visible; emergency transmission never depends on cloud AI.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
