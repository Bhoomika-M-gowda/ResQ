package com.resq.ui.mesh

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resq.data.model.AvailablePeer
import com.resq.data.model.BatchSendProgressState
import com.resq.data.model.EmergencyPacket
import com.resq.mesh.bluetooth.MeshUiState
import com.resq.mesh.bluetooth.PeerDevice
import com.resq.ui.components.NearbyDevicesSection
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.ResQDimens
import com.resq.ui.theme.SafeGreen

@Composable
fun MeshScreen(
    state: MeshUiState,
    packets: List<EmergencyPacket>,
    selectedPacketId: String?,
    availablePeers: List<AvailablePeer>,
    selectedPeerIds: Set<String>,
    batchProgress: BatchSendProgressState,
    onBack: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onScan: () -> Unit,
    onSelectPacket: (String) -> Unit,
    onSend: (PeerDevice) -> Unit,
    onToggleSelectAll: () -> Unit,
    onTogglePeer: (String) -> Unit,
    onSendEmergency: () -> Unit
) {
    val selectedPacket = packets.firstOrNull { it.messageId == selectedPacketId } ?: packets.firstOrNull()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ResQDimens.page)
    ) {
        ResQHeader("Bluetooth Mesh", onBack)
        ResQCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, null, tint = if (state.enabled) SafeGreen else MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (state.enabled) "Bluetooth active" else "Bluetooth off", fontWeight = FontWeight.ExtraBold)
                    Text(state.status, style = MaterialTheme.typography.bodySmall)
                }
                if (state.listening) AssistChip(onClick = {}, label = { Text("Listening") })
            }
            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            state.receivedPacketId?.let {
                Spacer(Modifier.height(8.dp))
                Text("Last received: $it", color = SafeGreen, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (!state.enabled) {
            Button(onClick = onEnableBluetooth, modifier = Modifier.fillMaxWidth()) { Text("TURN ON BLUETOOTH") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMakeDiscoverable, modifier = Modifier.weight(1f)) { Text("BE VISIBLE") }
                Button(onClick = onScan, enabled = !state.scanning, modifier = Modifier.weight(1f)) {
                    Text(if (state.scanning) "SCANNING…" else "SCAN")
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Packet ready to send", fontWeight = FontWeight.ExtraBold)
        if (packets.isEmpty()) {
            Text("Create an SOS or report first", style = MaterialTheme.typography.bodySmall)
        } else {
            ResQCard(Modifier.fillMaxWidth()) {
                packets.take(4).forEach { packet ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = packet.messageId == selectedPacket?.messageId,
                            onClick = { onSelectPacket(packet.messageId) }
                        )
                        Column {
                            Text(packet.messageId, fontWeight = FontWeight.ExtraBold)
                            Text("${packet.type.label} • ${packet.status} • Hop ${packet.hopCount}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        NearbyDevicesSection(
            availablePeers = availablePeers,
            selectedPeerIds = selectedPeerIds,
            batchProgress = batchProgress,
            onToggleSelectAll = onToggleSelectAll,
            onTogglePeer = onTogglePeer,
            onSendEmergency = onSendEmergency,
            hasPacketSelected = selectedPacket != null
        )
        Spacer(Modifier.height(24.dp))
    }
}
