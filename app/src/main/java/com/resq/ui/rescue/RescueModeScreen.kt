package com.resq.ui.rescue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resq.data.model.EmergencyPacket
import com.resq.data.model.EmergencyPriority
import com.resq.data.model.PacketStatus
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.CriticalRed
import com.resq.ui.theme.ResQDimens
import com.resq.ui.theme.SafeGreen
import com.resq.ui.theme.UrgentOrange
import java.text.DateFormat
import java.util.Date

@Composable
fun RescueModeScreen(
    enabled: Boolean,
    packets: List<EmergencyPacket>,
    onEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val delivered = packets.filter { it.status == PacketStatus.DELIVERED }
    Column(Modifier.fillMaxSize().padding(horizontal = ResQDimens.page)) {
        ResQHeader("Rescue Mode", onBack)
        ResQCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HealthAndSafety, null, tint = if (enabled) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (enabled) "Rescue node active" else "Standard relay device", fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (enabled) "Incoming packets receive final delivery acknowledgement."
                        else "Enable only on Phone C / rescue device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityCount("Critical", delivered.count { it.priority == EmergencyPriority.CRITICAL }, CriticalRed, Modifier.weight(1f))
            PriorityCount("Urgent", delivered.count { it.priority == EmergencyPriority.URGENT }, UrgentOrange, Modifier.weight(1f))
            PriorityCount("Normal", delivered.count { it.priority == EmergencyPriority.NORMAL }, SafeGreen, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Text("Delivered emergencies", fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        if (delivered.isEmpty()) {
            Text("No emergency has reached this rescue node yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(delivered, key = { it.messageId }) { packet ->
                    ResQCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(packet.priority.name, color = when (packet.priority) {
                                EmergencyPriority.CRITICAL -> CriticalRed
                                EmergencyPriority.URGENT -> UrgentOrange
                                EmergencyPriority.NORMAL -> SafeGreen
                            }, fontWeight = FontWeight.ExtraBold)
                            Text("Hop ${packet.hopCount}", fontWeight = FontWeight.Bold)
                        }
                        Text(packet.type.label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(packet.text)
                        Text("${"%.5f".format(packet.latitude)}, ${"%.5f".format(packet.longitude)}", style = MaterialTheme.typography.bodySmall)
                        Text(DateFormat.getDateTimeInstance().format(Date(packet.timestamp)), style = MaterialTheme.typography.bodySmall)
                        Text(packet.messageId, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityCount(label: String, count: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(modifier = modifier, color = color.copy(alpha = .12f), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), color = color, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
