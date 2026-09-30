package com.resq.data.db

import androidx.room.TypeConverter
import com.resq.data.model.EmergencyPriority
import com.resq.data.model.EmergencyType
import com.resq.data.model.PacketStatus

class Converters {
    @TypeConverter fun priorityToString(value: EmergencyPriority) = value.name
    @TypeConverter fun stringToPriority(value: String) = runCatching { EmergencyPriority.valueOf(value) }.getOrDefault(EmergencyPriority.NORMAL)
    @TypeConverter fun typeToString(value: EmergencyType) = value.name
    @TypeConverter fun stringToType(value: String) = runCatching { EmergencyType.valueOf(value) }.getOrDefault(EmergencyType.SOS)
    @TypeConverter fun statusToString(value: PacketStatus) = value.name
    @TypeConverter fun stringToStatus(value: String) = runCatching { PacketStatus.valueOf(value) }.getOrDefault(PacketStatus.STORED)
}
