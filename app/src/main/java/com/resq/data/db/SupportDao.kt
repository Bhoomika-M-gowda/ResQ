package com.resq.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.resq.data.model.DeviceState
import com.resq.data.model.ForwardingLog
import com.resq.data.model.MapMarker
import kotlinx.coroutines.flow.Flow

@Dao
interface SupportDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertForwardingLog(log: ForwardingLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMarker(marker: MapMarker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeviceState(state: DeviceState)

    @androidx.room.Query("SELECT * FROM forwarding_log ORDER BY timestamp DESC")
    fun observeForwardingLogs(): Flow<List<ForwardingLog>>

    @androidx.room.Query("SELECT * FROM map_markers ORDER BY createdAt DESC")
    fun observeMarkers(): Flow<List<MapMarker>>

    @androidx.room.Query("SELECT COUNT(*) FROM map_markers")
    suspend fun markerCount(): Int
}
