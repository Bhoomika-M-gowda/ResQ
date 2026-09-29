package com.resq.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resq.ui.components.*
import com.resq.ui.theme.*

@Composable
fun HomeScreen(
    deviceId: String,
    storedPacketCount: Int,
    locationText: String,
    onGetLocation: () -> Unit,
    onMesh: () -> Unit,
    onRescue: () -> Unit,
    onWifi: () -> Unit,
    onDecision: () -> Unit,
    onSos: () -> Unit,
    onReport: () -> Unit,
    onMap: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ResQDimens.page)
    ) {
        ResQHeader()
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(18.dp))
            Surface(
                onClick = onSos,
                modifier = Modifier.size(142.dp).shadow(18.dp, CircleShape),
                shape = CircleShape,
                color = CriticalRed,
                border = androidx.compose.foundation.BorderStroke(10.dp, Color.White.copy(alpha = .8f))
            ) {
                Column(
                    Modifier.fillMaxSize().background(
                        Brush.radialGradient(listOf(Color(0xFFFF6A68), CriticalRed))
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(42.dp))
                    Text("SOS", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("SEND EMERGENCY", color = CriticalRed, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Text("Get help. Save lives.", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("Report Emergency", Icons.Default.EditNote, Modifier.weight(1f), onReport)
            ActionCard("Offline Map", Icons.Default.Map, Modifier.weight(1f), onMap)
        }
        Spacer(Modifier.height(10.dp))
        ActionCard("Bluetooth Mesh Network", Icons.Default.Bluetooth, Modifier.fillMaxWidth(), onMesh)
        Spacer(Modifier.height(10.dp))
        ActionCard("Rescue Node Mode", Icons.Default.HealthAndSafety, Modifier.fillMaxWidth(), onRescue)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("Wi-Fi Local", Icons.Default.Wifi, Modifier.weight(1f), onWifi)
            ActionCard("Decision Engine", Icons.Default.Route, Modifier.weight(1f), onDecision)
        }

        Spacer(Modifier.height(18.dp))
        Text("System Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        ResQCard(Modifier.fillMaxWidth()) {
            Row {
                StatusItem("Nearby devices", "Not scanned yet", Icons.Default.Groups, ResQBlue, Modifier.weight(1f))
                StatusItem("Forwarding", "Bluetooth ready", Icons.Default.Forward, ResQBlue, Modifier.weight(1f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .4f))
            Row {
                StatusItem("Stored packets", "$storedPacketCount local", Icons.Default.Warning, CriticalRed, Modifier.weight(1f))
                StatusItem("GPS", locationText, Icons.Default.LocationOn, SafeGreen, Modifier.weight(1f))
            }
        }
        TextButton(onClick = onGetLocation, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Refresh GPS location")
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Temporary device: $deviceId",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}
