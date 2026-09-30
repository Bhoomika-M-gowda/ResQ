package com.resq.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.resq.data.model.DeviceState
import com.resq.data.model.EmergencyPacket
import com.resq.data.model.ForwardingLog
import com.resq.data.model.MapMarker

@Database(
    entities = [EmergencyPacket::class, ForwardingLog::class, MapMarker::class, DeviceState::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ResQDatabase : RoomDatabase() {
    abstract fun packetDao(): EmergencyPacketDao
    abstract fun supportDao(): SupportDao

    companion object {
        @Volatile private var instance: ResQDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE emergency_packets ADD COLUMN imageLocalPath TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE emergency_packets ADD COLUMN destinationId TEXT NOT NULL DEFAULT 'RESCUE'")
            }
        }

        fun getInstance(context: Context): ResQDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ResQDatabase::class.java,
                "resq.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
    }
}
