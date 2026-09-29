package com.resq.data.model

enum class EmergencyType(val label: String, val emoji: String) {
    MEDICAL("Medical", "✚"),
    FIRE("Fire", "♨"),
    FLOOD("Flood", "≋"),
    OTHER("Other", "!"),
    BLOCKED_ROAD("Blocked Road", "⛔"),
    DAMAGED_BRIDGE("Damaged Bridge", "⚠"),
    SOS("SOS", "!")
}

data class EmergencyDraft(
    val type: EmergencyType = EmergencyType.FLOOD,
    val description: String = "",
    val locationLabel: String = "Location will be added in Milestone 2",
    val imageLocalPath: String? = null
)
