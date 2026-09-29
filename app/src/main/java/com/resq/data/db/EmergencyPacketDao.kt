package com.resq.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.resq.data.model.EmergencyPacket
import com.resq.data.model.PacketStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyPacketDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(packet: EmergencyPacket): Long

    @Query("SELECT * FROM emergency_packets ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<EmergencyPacket>>

    @Query("SELECT * FROM emergency_packets WHERE messageId = :id LIMIT 1")
    suspend fun findById(id: String): EmergencyPacket?

    @Query("SELECT COUNT(*) FROM emergency_packets")
    fun observeCount(): Flow<Int>

    @Query("UPDATE emergency_packets SET status = :status, hopCount = :hopCount, lastForwardedAt = :time WHERE messageId = :id")
    suspend fun updateTransferStatus(id: String, status: PacketStatus, hopCount: Int, time: Long)
}
