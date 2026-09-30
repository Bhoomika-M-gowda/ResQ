package com.resq.mesh.packet

import com.resq.data.model.EmergencyPacket
import com.resq.data.model.EmergencyPriority
import com.resq.data.model.EmergencyType
import com.resq.data.model.PacketStatus
import org.json.JSONObject

object PacketJsonCodec {
    fun encode(packet: EmergencyPacket): String = JSONObject().apply {
        put("messageId", packet.messageId)
        put("priority", packet.priority.name)
        put("type", packet.type.name)
        put("text", packet.text)
        put("latitude", packet.latitude)
        put("longitude", packet.longitude)
        put("timestamp", packet.timestamp)
        put("senderId", packet.senderId)
        put("status", packet.status.name)
        put("hopCount", packet.hopCount)
        if (packet.lastForwardedAt == null) put("lastForwardedAt", JSONObject.NULL)
        else put("lastForwardedAt", packet.lastForwardedAt)
        put("destinationId", packet.destinationId)
    }.toString()

    fun decode(json: String): Result<EmergencyPacket> = runCatching {
        val value = JSONObject(json)
        EmergencyPacket(
            messageId = value.getString("messageId"),
            priority = EmergencyPriority.valueOf(value.getString("priority")),
            type = EmergencyType.valueOf(value.getString("type")),
            text = value.getString("text"),
            latitude = value.getDouble("latitude"),
            longitude = value.getDouble("longitude"),
            timestamp = value.getLong("timestamp"),
            senderId = value.getString("senderId"),
            status = runCatching { PacketStatus.valueOf(value.getString("status")) }.getOrDefault(PacketStatus.STORED),
            hopCount = value.getInt("hopCount"),
            lastForwardedAt = if (value.isNull("lastForwardedAt")) null else value.getLong("lastForwardedAt"),
            destinationId = if (value.has("destinationId") && !value.isNull("destinationId")) value.getString("destinationId") else "RESCUE"
        )
    }.fold(
        onSuccess = { PacketValidator.validate(it) },
        onFailure = { Result.failure(IllegalArgumentException("Malformed emergency packet", it)) }
    )
}
