package com.resq.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resq.data.model.EmergencyPriority
import com.resq.ui.AnalysisUiState
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.*

@Composable
fun AiAnalysisScreen(
    state: AnalysisUiState,
    locationText: String,
    onBack: () -> Unit,
    onCreatePacket: () -> Unit
) {
    val steps = listOf(
        "Understanding message",
        "Identifying emergency type",
        "Checking location",
        "Determining priority",
        "Preparing emergency packet"
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ResQDimens.page)
    ) {
        ResQHeader("AI Emergency Analysis", onBack)
        ResQCard(Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
                Text("RESQ LOCAL ANALYSIS", fontWeight = FontWeight.Black)
                Text("Deterministic • On device • Offline", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        ResQCard(Modifier.fillMaxWidth()) {
            steps.forEach { step ->
                val complete = step in state.completedSteps
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (complete) Icon(Icons.Default.CheckCircle, null, tint = SafeGreen)
                    else if (state.processing && step == steps.getOrNull(state.completedSteps.size)) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Spacer(Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(step, fontWeight = if (complete) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        state.result?.let { result ->
            Spacer(Modifier.height(12.dp))
            Text("Analysis Result", fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            ResQCard(Modifier.fillMaxWidth()) {
                ResultRow("Type", result.type.name.replace('_', ' '), MaterialTheme.colorScheme.primary)
                ResultRow("Priority", result.priority.name, when (result.priority) {
                    EmergencyPriority.CRITICAL -> CriticalRed
                    EmergencyPriority.URGENT -> UrgentOrange
                    EmergencyPriority.NORMAL -> SafeGreen
                })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp)); Text(locationText)
                }
                Spacer(Modifier.height(10.dp))
                Text(state.draft?.description.orEmpty(), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text(result.explanation, style = MaterialTheme.typography.bodySmall)
                if (result.matchedKeywords.isNotEmpty()) {
                    Text("Matched: ${result.matchedKeywords.joinToString()}", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onCreatePacket,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("CREATE EMERGENCY PACKET  →", fontWeight = FontWeight.ExtraBold) }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResultRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = color, fontWeight = FontWeight.ExtraBold)
    }
}
