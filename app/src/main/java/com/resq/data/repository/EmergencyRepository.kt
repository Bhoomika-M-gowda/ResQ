package com.resq.data.repository

import com.resq.data.db.EmergencyPacketDao
import com.resq.data.model.*
import com.resq.location.LocationFix
import com.resq.mesh.packet.PacketValidator
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class EmergencyRepository(private val dao: EmergencyPacketDao) {
    fun observePackets(): Flow<List<EmergencyPacket>> = dao.observeAll()

    suspend fun createPacket(
        senderId: String,
        type: EmergencyType,
        text: String,
        priority: EmergencyPriority,
        location: LocationFix,
        imageLocalPath: String? = null
    ): Result<EmergencyPacket> {
        val now = System.currentTimeMillis()
        val packet = EmergencyPacket(
            messageId = "EMG-${now.toString().takeLast(6)}-${UUID.randomUUID().toString().take(4).uppercase()}",
            priority = priority,
            type = type,
            text = text.trim(),
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = now,
            senderId = senderId,
            status = PacketStatus.STORED,
            hopCount = 0,
            lastForwardedAt = null,
            imageLocalPath = imageLocalPath
        )
        return PacketValidator.validate(packet).fold(
            onSuccess = {
                val inserted = dao.insert(it)
                if (inserted == -1L) Result.failure(IllegalStateException("Duplicate message ID rejected"))
                else Result.success(it)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun receivePacket(packet: EmergencyPacket, finalDelivery: Boolean): Result<EmergencyPacket> {
        val received = packet.copy(status = if (finalDelivery) PacketStatus.DELIVERED else PacketStatus.STORED)
        return PacketValidator.validate(received).fold(
            onSuccess = {
                if (dao.insert(it) == -1L) Result.failure(IllegalStateException("Duplicate message ID rejected"))
                else Result.success(it)
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun markTransferred(messageId: String, hopCount: Int, delivered: Boolean) {
        dao.updateTransferStatus(
            messageId,
            if (delivered) PacketStatus.DELIVERED else PacketStatus.FORWARDED,
            hopCount,
            System.currentTimeMillis()
        )
    }
}
