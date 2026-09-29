package com.resq.ui.mesh

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resq.data.model.EmergencyPacket
import com.resq.mesh.bluetooth.MeshUiState
import com.resq.mesh.bluetooth.PeerDevice
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.ResQDimens
import com.resq.ui.theme.SafeGreen

@Composable
fun MeshScreen(
    state: MeshUiState,
    packets: List<EmergencyPacket>,
    selectedPacketId: String?,
    onBack: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onScan: () -> Unit,
    onSelectPacket: (String) -> Unit,
    onSend: (PeerDevice) -> Unit
) {
    val selectedPacket = packets.firstOrNull { it.messageId == selectedPacketId } ?: packets.firstOrNull()
    Column(Modifier.fillMaxSize().padding(horizontal = ResQDimens.page)) {
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
        Text("Nearby / paired phones", fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        if (state.peers.isEmpty()) {
            Text("No phones found. Pair Phone A and Phone B in Android Bluetooth settings, then scan again.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(state.peers, key = { it.address }) { peer ->
                    ResQCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(peer.name, fontWeight = FontWeight.ExtraBold)
                                Text(peer.address, style = MaterialTheme.typography.bodySmall)
                                Text(if (peer.paired) "Paired" else "Not paired", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { onSend(peer) },
                                enabled = peer.paired && selectedPacket != null
                            ) { Text("SEND") }
                        }
                    }
                }
            }
        }
    }
}
