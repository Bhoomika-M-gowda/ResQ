package com.resq.ui.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resq.data.model.EmergencyPacket
import com.resq.data.model.ForwardingLog
import com.resq.data.model.PacketStatus
import com.resq.mesh.packet.PacketValidator
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.CriticalRed
import com.resq.ui.theme.ResQDimens
import com.resq.ui.theme.SafeGreen
import com.resq.ui.theme.UrgentOrange
import java.text.DateFormat
import java.util.Date

@Composable
fun PacketHistoryScreen(packets: List<EmergencyPacket>, logs: List<ForwardingLog>) {
    Column(Modifier.fillMaxSize().padding(horizontal = ResQDimens.page)) {
        ResQHeader("Emergency Packets")
        if (packets.isEmpty() && logs.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(12.dp))
                Text("No emergency packets yet", fontWeight = FontWeight.ExtraBold)
                Text("Create an SOS or emergency report.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                item {
                    Text("${packets.size} packet(s) stored locally", style = MaterialTheme.typography.labelLarge)
                }
                items(packets, key = { it.messageId }) { packet ->
                    val priorityColor = when (packet.priority.name) {
                        "CRITICAL" -> CriticalRed
                        "URGENT" -> UrgentOrange
                        else -> SafeGreen
                    }
                    ResQCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(packet.messageId, fontWeight = FontWeight.ExtraBold)
                            Text(packet.priority.name, color = priorityColor, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(packet.type.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(packet.text, maxLines = 2)
                        if (packet.imageLocalPath != null) {
                            Text("Photo saved locally on reporting device", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("${"%.5f".format(packet.latitude)}, ${"%.5f".format(packet.longitude)}", style = MaterialTheme.typography.bodySmall)
                        Text(DateFormat.getDateTimeInstance().format(Date(packet.timestamp)), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        when (packet.status) {
                            PacketStatus.DELIVERED, PacketStatus.CLOSED -> {
                                Column {
                                    Text("✓ Reached Rescue Node • Delivered • ACK Sent • Closed", color = SafeGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text("Status: ${packet.status.name} • Hop ${packet.hopCount}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            PacketStatus.EXPIRED -> {
                                Column {
                                    Text("⚠ Hop Limit Exceeded (${packet.hopCount}/${PacketValidator.MAX_HOPS})", color = UrgentOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text("🔴 EXPIRED — HOP LIMIT EXCEEDED", color = CriticalRed, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodySmall)
                                    Text("Status: EXPIRED • Hop ${packet.hopCount}", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            else -> {
                                Text("Status: ${packet.status.name} • Hop ${packet.hopCount}/${PacketValidator.MAX_HOPS}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                if (logs.isNotEmpty()) {
                    item {
                        Text("Forwarding activity", fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 10.dp))
                    }
                    items(logs.take(30), key = { "log-${it.id}" }) { log ->
                        ResQCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.result.replace('_', ' '), fontWeight = FontWeight.ExtraBold)
                                Text(log.method, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(log.messageId, style = MaterialTheme.typography.bodySmall)
                            Text("${log.fromDevice} → ${log.toDevice}", style = MaterialTheme.typography.bodySmall)
                            Text(DateFormat.getDateTimeInstance().format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
