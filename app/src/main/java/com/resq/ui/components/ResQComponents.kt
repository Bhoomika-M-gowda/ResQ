package com.resq.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resq.ui.theme.ResQBlue
import com.resq.ui.theme.ResQDimens

@Composable
fun ResQHeader(title: String? = null, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(end = 8.dp)) {
                Text("‹", fontSize = 34.sp, fontWeight = FontWeight.Light)
            }
        } else {
            Icon(
                imageVector = Icons.Default.HealthAndSafety,
                contentDescription = null,
                tint = ResQBlue,
                modifier = Modifier.size(34.dp)
            )
        }
        Text(
            text = title ?: "RESQ",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f)
        )
        if (title == null) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = .09f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .2f))
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudOff, null, Modifier.size(16.dp), tint = ResQBlue)
                    Spacer(Modifier.width(5.dp))
                    Text("Offline Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ResQCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(ResQDimens.corner),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .7f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = { Column(Modifier.padding(ResQDimens.card), content = content) }
    )
}

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(ResQDimens.corner),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .7f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .06f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun StatusItem(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(10.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.padding(7.dp).size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
