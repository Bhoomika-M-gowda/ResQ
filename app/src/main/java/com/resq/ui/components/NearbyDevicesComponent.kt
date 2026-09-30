package com.resq.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resq.data.model.AvailablePeer
import com.resq.data.model.BatchSendProgressState
import com.resq.data.model.PeerConnectionType
import com.resq.data.model.PeerSendStatus
import com.resq.ui.theme.CriticalRed
import com.resq.ui.theme.SafeGreen

@Composable
fun NearbyDevicesSection(
    availablePeers: List<AvailablePeer>,
    selectedPeerIds: Set<String>,
    batchProgress: BatchSendProgressState,
    onToggleSelectAll: () -> Unit,
    onTogglePeer: (String) -> Unit,
    onSendEmergency: () -> Unit,
    hasPacketSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val availablePeersOnly = availablePeers.filter { it.isAvailable }
    val isAllSelected = availablePeersOnly.isNotEmpty() && availablePeersOnly.all { it.id in selectedPeerIds }
    val selectedCount = selectedPeerIds.count { id -> availablePeers.any { it.id == id } }

    ResQCard(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Nearby Devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)

            if (availablePeers.isEmpty()) {
                Text(
                    "No nearby devices found. Turn on Bluetooth or Wi-Fi Direct and scan to discover peers.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isAllSelected,
                        onCheckedChange = { onToggleSelectAll() },
                        enabled = availablePeersOnly.isNotEmpty() && !batchProgress.isSending
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "[ Select All Available ]",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider()

                availablePeers.forEach { peer ->
                    val isChecked = peer.id in selectedPeerIds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onTogglePeer(peer.id) },
                            enabled = peer.isAvailable && !batchProgress.isSending
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (peer.connectionType == PeerConnectionType.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (peer.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(peer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    if (peer.connectionType == PeerConnectionType.BLUETOOTH) "Bluetooth" else "Wi-Fi Direct",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "• ${peer.statusText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (peer.isAvailable) SafeGreen else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Column {
                    Text(
                        "Selected:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$selectedCount devices",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (batchProgress.isSending || batchProgress.peerProgressList.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Sending:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    batchProgress.peerProgressList.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            val statusIcon = when (item.status) {
                                PeerSendStatus.SUCCESS -> "✓"
                                PeerSendStatus.FAILED -> "✗"
                                PeerSendStatus.SENDING -> "..."
                                PeerSendStatus.SKIPPED_DUPLICATE -> "↪ (already sent)"
                                PeerSendStatus.PENDING -> "..."
                            }
                            val color = when (item.status) {
                                PeerSendStatus.SUCCESS -> SafeGreen
                                PeerSendStatus.FAILED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                "${item.index}/${item.total} $statusIcon  ${item.peerName} (${if (item.connectionType == PeerConnectionType.BLUETOOTH) "Bluetooth" else "Wi-Fi Direct"})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = color
                            )
                        }
                    }

                    batchProgress.finalSummaryMessage?.let { summary ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Final:\n$summary",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SafeGreen
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onSendEmergency,
                    enabled = selectedCount > 0 && hasPacketSelected && !batchProgress.isSending,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed)
                ) {
                    if (batchProgress.isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("SENDING EMERGENCY…")
                    } else {
                        Text("[ SEND EMERGENCY ]", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
