package com.resq.ui.wifi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resq.data.model.AvailablePeer
import com.resq.data.model.BatchSendProgressState
import com.resq.data.model.EmergencyPacket
import com.resq.mesh.wifi.WifiMeshState
import com.resq.mesh.wifi.WifiPeer
import com.resq.ui.components.NearbyDevicesSection
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.ResQDimens
import com.resq.ui.theme.SafeGreen

@Composable
fun WifiDirectScreen(
    state: WifiMeshState,
    packets: List<EmergencyPacket>,
    selectedPacketId: String?,
    availablePeers: List<AvailablePeer>,
    selectedPeerIds: Set<String>,
    batchProgress: BatchSendProgressState,
    onBack: () -> Unit,
    onHost: () -> Unit,
    onDiscover: () -> Unit,
    onSelectPacket: (String) -> Unit,
    onSend: (WifiPeer) -> Unit,
    onToggleSelectAll: () -> Unit,
    onTogglePeer: (String) -> Unit,
    onSendEmergency: () -> Unit
) {
    val selected = packets.firstOrNull { it.messageId == selectedPacketId } ?: packets.firstOrNull()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ResQDimens.page)
    ) {
        ResQHeader("Wi-Fi Local Peer", onBack)
        ResQCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, null, tint = if (state.enabled || state.connected) SafeGreen else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (state.hosting) "Receiver host active" else if (state.connected) "Peer connected" else "Wi-Fi Direct", fontWeight = FontWeight.ExtraBold)
                    Text(state.status, style = MaterialTheme.typography.bodySmall)
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            state.receivedPacketId?.let { Text("Last received: $it", color = SafeGreen, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onHost, modifier = Modifier.weight(1f)) { Text("HOST / RECEIVE") }
            Button(onClick = onDiscover, enabled = !state.discovering, modifier = Modifier.weight(1f)) {
                Text(if (state.discovering) "DISCOVERING…" else "DISCOVER")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Packet", fontWeight = FontWeight.ExtraBold)
        if (packets.isEmpty()) Text("Create an SOS or report first.")
        else ResQCard(Modifier.fillMaxWidth()) {
            packets.take(4).forEach { packet ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = packet.messageId == selected?.messageId, onClick = { onSelectPacket(packet.messageId) })
                    Column {
                        Text(packet.messageId, fontWeight = FontWeight.ExtraBold)
                        Text("${packet.type.label} • ${packet.status} • Hop ${packet.hopCount}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        NearbyDevicesSection(
            availablePeers = availablePeers,
            selectedPeerIds = selectedPeerIds,
            batchProgress = batchProgress,
            onToggleSelectAll = onToggleSelectAll,
            onTogglePeer = onTogglePeer,
            onSendEmergency = onSendEmergency,
            hasPacketSelected = selected != null
        )
        Spacer(Modifier.height(24.dp))
    }
}
