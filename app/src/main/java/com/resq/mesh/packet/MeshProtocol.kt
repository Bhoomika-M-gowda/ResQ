package com.resq.mesh.packet

import com.resq.data.model.EmergencyPacket
import org.json.JSONObject

data class MeshAck(
    val messageId: String,
    val accepted: Boolean,
    val finalDelivery: Boolean,
    val receiverId: String,
    val reason: String? = null
)

data class MeshPacketMessage(val packet: EmergencyPacket, val forwarderId: String)

object MeshProtocol {
    fun encodePacket(packet: EmergencyPacket, forwarderId: String): String = JSONObject().apply {
        put("kind", "PACKET")
        put("forwarderId", forwarderId)
        put("payload", JSONObject(PacketJsonCodec.encode(packet)))
    }.toString()

    fun decodePacket(line: String): Result<MeshPacketMessage> = runCatching {
        val envelope = JSONObject(line)
        require(envelope.getString("kind") == "PACKET") { "Expected PACKET envelope" }
        envelope.getString("forwarderId") to envelope.getJSONObject("payload").toString()
    }.fold(
        onSuccess = { (forwarderId, payload) ->
            PacketJsonCodec.decode(payload).map { MeshPacketMessage(it, forwarderId) }
        },
        onFailure = { Result.failure(IllegalArgumentException("Malformed mesh packet", it)) }
    )

    fun encodeAck(ack: MeshAck): String = JSONObject().apply {
        put("kind", "ACK")
        put("messageId", ack.messageId)
        put("accepted", ack.accepted)
        put("finalDelivery", ack.finalDelivery)
        put("receiverId", ack.receiverId)
        if (ack.reason == null) put("reason", JSONObject.NULL) else put("reason", ack.reason)
    }.toString()

    fun decodeAck(line: String): Result<MeshAck> = runCatching {
        val value = JSONObject(line)
        require(value.getString("kind") == "ACK") { "Expected ACK envelope" }
        MeshAck(
            messageId = value.getString("messageId"),
            accepted = value.getBoolean("accepted"),
            finalDelivery = value.getBoolean("finalDelivery"),
            receiverId = value.getString("receiverId"),
            reason = if (value.isNull("reason")) null else value.getString("reason")
        )
    }
}
