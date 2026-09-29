package com.resq.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EmergencyPriority { CRITICAL, URGENT, NORMAL }
enum class PacketStatus { CREATED, STORED, FORWARDED, DELIVERED, FAILED }

@Entity(tableName = "emergency_packets")
data class EmergencyPacket(
    @PrimaryKey val messageId: String,
    val priority: EmergencyPriority,
    val type: EmergencyType,
    val text: String,
    @ColumnInfo(name = "lat") val latitude: Double,
    @ColumnInfo(name = "lng") val longitude: Double,
    val timestamp: Long,
    val senderId: String,
    val status: PacketStatus,
    val hopCount: Int,
    val lastForwardedAt: Long?,
    val imageLocalPath: String? = null
)
