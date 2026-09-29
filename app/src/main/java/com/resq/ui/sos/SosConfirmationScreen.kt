package com.resq.ui.sos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.CriticalRed
import com.resq.ui.theme.ResQDimens

@Composable
fun SosConfirmationScreen(
    deviceId: String,
    locationText: String,
    hasLocation: Boolean,
    onGetLocation: () -> Unit,
    onBack: () -> Unit,
    onConfirmed: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = ResQDimens.page)) {
        ResQHeader("Confirm SOS", onBack)
        Spacer(Modifier.height(28.dp))
        Surface(
            modifier = Modifier.size(104.dp).align(Alignment.CenterHorizontally),
            shape = CircleShape,
            color = CriticalRed.copy(alpha = .12f)
        ) {
            Icon(Icons.Default.Warning, null, tint = CriticalRed, modifier = Modifier.padding(26.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Send a critical SOS?",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "This creates and permanently stores a critical emergency packet on this device.",
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        ResQCard(Modifier.fillMaxWidth()) {
            InfoRow("Priority", "CRITICAL", CriticalRed)
            InfoRow("Type", "SOS", MaterialTheme.colorScheme.primary)
            InfoRow("Sender", deviceId, MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(locationText)
            }
            if (!hasLocation) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onGetLocation, modifier = Modifier.fillMaxWidth()) { Text("GET CURRENT LOCATION") }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("CANCEL") }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onConfirmed,
            enabled = hasLocation,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CriticalRed)
        ) { Text("CONFIRM SOS", color = Color.White, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = color, fontWeight = FontWeight.ExtraBold)
    }
}
