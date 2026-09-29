package com.resq.ai.classifier

import com.resq.data.model.EmergencyPriority
import com.resq.data.model.EmergencyType

data class ClassificationResult(
    val type: EmergencyType,
    val priority: EmergencyPriority,
    val matchedKeywords: List<String>,
    val explanation: String
)

object EmergencyClassifier {
    private val typeRules = linkedMapOf(
        EmergencyType.FLOOD to listOf("flood", "flooded", "water rising", "drowning", "washed away"),
        EmergencyType.FIRE to listOf("fire", "smoke", "burning", "flames", "explosion"),
        EmergencyType.MEDICAL to listOf("medical", "injured", "injury", "bleeding", "unconscious", "first aid", "heart attack"),
        EmergencyType.BLOCKED_ROAD to listOf(
            "blocked road", "road blocked", "road damage", "damaged road", "road surface", "landslide", "debris"
        ),
        EmergencyType.DAMAGED_BRIDGE to listOf("damaged bridge", "bridge collapse", "bridge broken"),
        EmergencyType.SOS to listOf("sos", "help me", "immediate help")
    )
    private val criticalRules = listOf(
        "trapped", "drowning", "unconscious", "severe bleeding", "building collapse",
        "bridge collapse", "cannot escape", "people inside", "children trapped", "life threatening"
    )
    private val urgentRules = listOf(
        "injured", "bleeding", "fire", "flood", "flooded", "medical", "blocked",
        "damaged", "help", "smoke", "landslide"
    )

    fun classify(text: String, manualFallback: EmergencyType = EmergencyType.OTHER): ClassificationResult {
        val normalized = text.lowercase().trim()
        val typeMatch = typeRules.entries.firstOrNull { (_, words) -> words.any(normalized::contains) }
        val type = typeMatch?.key ?: manualFallback
        val critical = criticalRules.filter(normalized::contains)
        val urgent = urgentRules.filter(normalized::contains)
        val priority = when {
            critical.isNotEmpty() -> EmergencyPriority.CRITICAL
            urgent.isNotEmpty() || type in listOf(EmergencyType.FLOOD, EmergencyType.FIRE, EmergencyType.MEDICAL) -> EmergencyPriority.URGENT
            else -> EmergencyPriority.NORMAL
        }
        val matched = ((typeMatch?.value?.filter(normalized::contains) ?: emptyList()) + critical + urgent).distinct()
        val explanation = when (priority) {
            EmergencyPriority.CRITICAL -> "Critical-risk language indicates an immediate threat to life or people unable to escape."
            EmergencyPriority.URGENT -> "Emergency or hazard language requires rapid local assistance."
            EmergencyPriority.NORMAL -> "No immediate life-threatening phrase was detected; manual review remains available."
        }
        return ClassificationResult(type, priority, matched, explanation)
    }
}
