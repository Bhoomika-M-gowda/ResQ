package com.resq.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forwarding_log")
data class ForwardingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: String,
    val fromDevice: String,
    val toDevice: String,
    val method: String,
    val timestamp: Long,
    val result: String
)

@Entity(tableName = "map_markers")
data class MapMarker(
    @PrimaryKey val id: String,
    val markerType: String,
    val title: String,
    val lat: Double,
    val lng: Double,
    val description: String,
    val source: String,
    val createdAt: Long
)

@Entity(tableName = "device_state")
data class DeviceState(
    @PrimaryKey val deviceId: String,
    val batteryPercent: Int,
    val lastSeen: Long,
    val demoMode: Boolean
)
