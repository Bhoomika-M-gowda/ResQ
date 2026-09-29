package com.resq.ai.vision

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

data class EmergencyImageResult(
    val description: String,
    val labels: List<String>,
    val localImagePath: String,
    val warning: String? = null
)

/**
 * Offline image understanding using ML Kit's model bundled in the APK.
 * Output is deliberately conservative: it describes visible labels and never invents
 * injuries, victims, causes, location, priority, or the user's personal situation.
 */
class EmergencyImageAnalyzer(private val context: Context) {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.55f)
            .build()
    )

    suspend fun analyze(source: Uri): Result<EmergencyImageResult> {
        val stored = runCatching { persistImage(source) }.getOrElse { return Result.failure(it) }
        return runCatching {
            val image = InputImage.fromFilePath(context, Uri.fromFile(stored))
            val labels = process(image).sortedByDescending { it.confidence }.take(8)
            EmergencyImageResult(
                description = EmergencyVisionDescription.from(labels),
                labels = labels.map { it.text },
                localImagePath = stored.absolutePath
            )
        }.recover {
            EmergencyImageResult(
                description = "",
                labels = emptyList(),
                localImagePath = stored.absolutePath,
                warning = "The photo was saved, but on-device analysis was unavailable. Please enter the emergency description manually."
            )
        }
    }

    fun close() = labeler.close()

    private fun persistImage(source: Uri): File {
        val directory = File(context.filesDir, "emergency_images").apply { mkdirs() }
        val destination = File(directory, "emergency-${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "The selected image could not be opened" }
            destination.outputStream().use(input::copyTo)
        }
        return destination
    }

    private suspend fun process(image: InputImage): List<ImageLabel> =
        suspendCancellableCoroutine { continuation ->
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    if (continuation.isActive) continuation.resume(labels)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWith(Result.failure(error))
                }
        }
}

internal object EmergencyVisionDescription {
    fun from(labels: List<ImageLabel>): String = fromWords(labels.map { it.text })

    fun fromWords(labelTexts: List<String>): String {
        val words = labelTexts.map { it.lowercase() }.toSet()
        fun has(vararg candidates: String) = candidates.any { candidate ->
            words.any { it.contains(candidate) }
        }

        return when {
            has("fire", "flame") && has("building", "house", "structure") ->
                "Flames are visible near a structure. Please confirm the affected area and add any known details."
            has("fire", "flame") ->
                "Fire or flames are visible in the image. Please confirm the affected area and add any known details."
            has("smoke") ->
                "Smoke is visible in the area. The image alone does not confirm its cause; please add known details."
            has("flood") ->
                "Floodwater appears to cover part of the visible area, which may restrict normal movement."
            has("water", "body of water") && has("road", "street", "asphalt", "highway") ->
                "Water is visible across or beside the roadway and may restrict normal movement. Please confirm local conditions."
            has("rubble", "debris", "ruins") && has("road", "street", "asphalt", "highway") ->
                "Debris is visible on or near the roadway and may obstruct vehicle passage."
            has("rubble", "ruins") && has("building", "house", "structure", "architecture") ->
                "A structure appears damaged, with visible debris in the surrounding area."
            has("crack", "cracked") && has("road", "street", "asphalt", "highway") ->
                "The road surface shows visible cracking or damage that may make passage difficult."
            labelTexts.isNotEmpty() -> {
                val visible = labelTexts.take(3).joinToString(", ") { it.lowercase() }
                "The image appears to show $visible. No specific emergency condition can be confirmed automatically; please describe the visible hazard."
            }
            else -> "No emergency condition could be identified from the image. Please enter a description manually."
        }
    }
}
