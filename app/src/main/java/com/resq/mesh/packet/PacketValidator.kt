package com.resq.mesh.packet

import com.resq.data.model.EmergencyPacket

object PacketValidator {
    const val MAX_TEXT_LENGTH = 200
    const val MAX_HOPS = 5

    fun validate(packet: EmergencyPacket): Result<EmergencyPacket> {
        val error = when {
            packet.messageId.isBlank() -> "Message ID is required"
            packet.senderId.isBlank() -> "Sender ID is required"
            packet.timestamp <= 0 -> "Timestamp is invalid"
            packet.text.isBlank() -> "Emergency text is required"
            packet.text.length > MAX_TEXT_LENGTH -> "Emergency text exceeds $MAX_TEXT_LENGTH characters"
            packet.latitude !in -90.0..90.0 -> "Latitude is invalid"
            packet.longitude !in -180.0..180.0 -> "Longitude is invalid"
            packet.hopCount < 0 -> "Hop count is invalid"
            else -> null
        }
        return error?.let { Result.failure(IllegalArgumentException(it)) } ?: Result.success(packet)
    }
}
