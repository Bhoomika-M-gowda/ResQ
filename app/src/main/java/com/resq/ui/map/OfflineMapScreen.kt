package com.resq.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resq.data.model.MapMarker
import com.resq.map.OfflineMapProjector
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import java.text.SimpleDateFormat
import java.util.*

private data class MarkerStyle(val color: Color, val icon: ImageVector, val label: String)

private fun markerStyle(type: String) = when (type.uppercase()) {
    "SOS" -> MarkerStyle(Color(0xFFE53935), Icons.Default.Warning, "SOS")
    "HAZARD" -> MarkerStyle(Color(0xFFF57C00), Icons.Default.ReportProblem, "Hazard")
    "SAFE" -> MarkerStyle(Color(0xFF16A36A), Icons.Default.Shield, "Safe zone")
    "MEDICAL" -> MarkerStyle(Color(0xFF1565C0), Icons.Default.LocalHospital, "Medical")
    else -> MarkerStyle(Color(0xFF6A43B8), Icons.Default.HealthAndSafety, "Rescue")
}

@Composable
fun OfflineMapScreen(
    markers: List<MapMarker>,
    centerLatitude: Double?,
    centerLongitude: Double?,
    onSeedMarkers: () -> Unit,
    onGetLocation: () -> Unit
) {
    var selectedType by remember { mutableStateOf("ALL") }
    var selected by remember { mutableStateOf<MapMarker?>(null) }
    val centerLat = centerLatitude ?: markers.firstOrNull()?.lat ?: 12.97160
    val centerLng = centerLongitude ?: markers.firstOrNull()?.lng ?: 77.59460
    val visible = if (selectedType == "ALL") markers else markers.filter { it.markerType == selectedType }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ResQHeader("Offline Disaster Map")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(7.dp))
            Text("Saved on this phone • works without internet", fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf("ALL", "SOS", "HAZARD", "SAFE", "MEDICAL", "RESCUE")) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(if (type == "ALL") "All" else markerStyle(type).label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).background(Color(0xFFE8F4EA), RoundedCornerShape(18.dp))
        ) {
            OfflineMapCanvas()
            visible.forEach { marker ->
                val point = OfflineMapProjector.project(marker.lat, marker.lng, centerLat, centerLng)
                val style = markerStyle(marker.markerType)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = maxWidth * point.x - 19.dp, y = maxHeight * point.y - 19.dp)
                        .clickable { selected = marker },
                    shape = CircleShape,
                    color = style.color,
                    shadowElevation = 5.dp
                ) {
                    Icon(style.icon, marker.title, tint = Color.White, modifier = Modifier.padding(8.dp).size(22.dp))
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.TopStart).offset(12.dp, 12.dp),
                color = Color.White.copy(alpha = .88f),
                shape = RoundedCornerShape(10.dp)
            ) { Text("${visible.size} offline markers", Modifier.padding(9.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SmallFloatingActionButton(onClick = onGetLocation) { Icon(Icons.Default.MyLocation, "Get location") }
                SmallFloatingActionButton(onClick = onSeedMarkers) { Icon(Icons.Default.AddLocationAlt, "Add demo markers") }
            }
        }
        selected?.let { marker ->
            Spacer(Modifier.height(10.dp))
            MarkerDetails(marker) { selected = null }
        } ?: run {
            Spacer(Modifier.height(10.dp))
            Text("Tap a marker to see its saved details.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun OfflineMapCanvas() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFFEAF4E7))
        val river = Path().apply {
            moveTo(size.width * .12f, 0f)
            cubicTo(size.width * .45f, size.height * .25f, size.width * .12f, size.height * .62f, size.width * .7f, size.height)
        }
        drawPath(river, Color(0xFF8FD4EC), style = Stroke(size.width * .08f, cap = StrokeCap.Round))
        val roadColor = Color.White
        drawLine(roadColor, Offset(0f, size.height * .32f), Offset(size.width, size.height * .62f), 18f, StrokeCap.Round)
        drawLine(Color(0xFFD3D7D7), Offset(0f, size.height * .32f), Offset(size.width, size.height * .62f), 2f)
        drawLine(roadColor, Offset(size.width * .62f, 0f), Offset(size.width * .33f, size.height), 15f, StrokeCap.Round)
        repeat(5) { i ->
            drawRoundRect(
                color = if (i % 2 == 0) Color(0xFFCEE6C9) else Color(0xFFD9EACB),
                topLeft = Offset(size.width * (.05f + i * .18f), size.height * (.72f - (i % 3) * .24f)),
                size = Size(size.width * .12f, size.height * .1f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
            )
        }
    }
}

@Composable
private fun MarkerDetails(marker: MapMarker, onClose: () -> Unit) {
    val style = markerStyle(marker.markerType)
    ResQCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = style.color, shape = CircleShape) {
                Icon(style.icon, null, tint = Color.White, modifier = Modifier.padding(7.dp).size(20.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(marker.title, fontWeight = FontWeight.ExtraBold)
                Text(style.label, color = style.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
        }
        Text(marker.description, fontSize = 13.sp)
        Text("${"%.5f".format(marker.lat)}, ${"%.5f".format(marker.lng)} • ${marker.source}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(marker.createdAt)), fontSize = 11.sp)
    }
}
