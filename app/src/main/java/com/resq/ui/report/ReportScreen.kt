package com.resq.ui.report

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.resq.data.model.EmergencyDraft
import com.resq.data.model.EmergencyType
import com.resq.ai.speech.SpeechUiState
import com.resq.ui.ImageAnalysisUiState
import com.resq.ui.components.ResQCard
import com.resq.ui.components.ResQHeader
import com.resq.ui.theme.ResQDimens
import java.io.File

@Composable
fun ReportScreen(
    locationText: String,
    hasLocation: Boolean,
    onGetLocation: () -> Unit,
    speechState: SpeechUiState,
    imageState: ImageAnalysisUiState,
    onSpeak: () -> Unit,
    onAnalyzeImage: (Uri) -> Unit,
    onClearImage: () -> Unit,
    onBack: () -> Unit,
    onContinue: (EmergencyDraft) -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(EmergencyType.FLOOD) }
    var description by remember { mutableStateOf("") }
    var aiSuggestion by remember { mutableStateOf("") }
    var editingSuggestion by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onAnalyzeImage(uri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) pendingCameraUri?.let(onAnalyzeImage)
    }
    LaunchedEffect(speechState.session, speechState.transcript) {
        if (speechState.transcript.isNotBlank()) description = speechState.transcript.take(200)
    }
    LaunchedEffect(imageState.localImagePath, imageState.suggestedDescription) {
        if (imageState.suggestedDescription.isNotBlank()) {
            aiSuggestion = imageState.suggestedDescription.take(200)
            editingSuggestion = false
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ResQDimens.page)
    ) {
        ResQHeader("Report an Emergency", onBack)
        Text("Select Emergency Type", fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        val reportTypes = listOf(
            EmergencyType.MEDICAL,
            EmergencyType.FIRE,
            EmergencyType.FLOOD,
            EmergencyType.OTHER
        )
        reportTypes.chunked(2).forEach { rowTypes ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowTypes.forEach { type ->
                    FilterChip(
                        selected = selected == type,
                        onClick = { selected = type },
                        label = { Text("${type.emoji}  ${type.label}") },
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected == type,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 2.dp
                        )
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("Describe the Emergency", fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 200) description = it },
            modifier = Modifier.fillMaxWidth().height(140.dp),
            placeholder = { Text("Example: There are people trapped near the flooded road...") },
            supportingText = { Text("${description.length}/200") },
            shape = RoundedCornerShape(14.dp)
        )
        OutlinedButton(onClick = onSpeak, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Mic, null); Spacer(Modifier.width(6.dp)); Text(if (speechState.listening) "Listening…" else "Speak")
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    runCatching {
                        val uri = newCameraImageUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    }.onFailure {
                        cameraError = "No camera application is available. Choose an existing image or type the report."
                    }
                },
                enabled = !imageState.analyzing,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(5.dp)); Text("Take Photo")
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch(arrayOf("image/*")) },
                enabled = !imageState.analyzing,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Image, null); Spacer(Modifier.width(5.dp)); Text("Choose Image")
            }
        }
        cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Text(
            if (speechState.onDevice) "On-device speech recognition" else "Offline-preferred speech; typed text is always available",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        speechState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (imageState.analyzing) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Analysing the photo on this phone…", style = MaterialTheme.typography.bodySmall)
        }
        if (imageState.localImagePath != null) {
            Spacer(Modifier.height(10.dp))
            ResQCard(Modifier.fillMaxWidth()) {
                Text("AI Suggested Description", fontWeight = FontWeight.ExtraBold)
                Text("Photo saved locally • review before using", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = aiSuggestion,
                    onValueChange = { if (it.length <= 200) aiSuggestion = it },
                    enabled = editingSuggestion,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("${aiSuggestion.length}/200") }
                )
                imageState.labels.takeIf { it.isNotEmpty() }?.let {
                    Text("Detected: ${it.joinToString()}", style = MaterialTheme.typography.labelSmall)
                }
                imageState.warning?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { editingSuggestion = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, null); Spacer(Modifier.width(4.dp)); Text("Edit")
                    }
                    Button(
                        onClick = { description = aiSuggestion.take(200); editingSuggestion = false },
                        enabled = aiSuggestion.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, null); Spacer(Modifier.width(4.dp)); Text("Use Description")
                    }
                }
                TextButton(onClick = onClearImage, modifier = Modifier.align(Alignment.End)) { Text("Remove photo") }
            }
        }
        imageState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(18.dp))
        Text("Location", fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        ResQCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(locationText, fontWeight = FontWeight.Bold)
                    Text("GPS coordinates are stored inside the emergency packet.", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!hasLocation) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onGetLocation, modifier = Modifier.fillMaxWidth()) { Text("GET CURRENT LOCATION") }
            }
        }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = {
                onContinue(EmergencyDraft(selected, description.trim(), imageLocalPath = imageState.localImagePath))
            },
            enabled = description.isNotBlank() && hasLocation,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("ANALYZE EMERGENCY  →", fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.height(24.dp))
    }
}

private fun newCameraImageUri(context: Context): Uri {
    val directory = File(context.filesDir, "emergency_images").apply { mkdirs() }
    val file = File(directory, "camera-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}
