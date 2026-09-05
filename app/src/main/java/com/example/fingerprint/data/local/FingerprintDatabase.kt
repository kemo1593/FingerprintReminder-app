package com.example.fingerprint.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScheduleConfigEntity::class, FingerprintEventEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FingerprintDatabase : RoomDatabase() {

    abstract fun fingerprintDao(): FingerprintDao

    companion object {
        @Volatile
        private var INSTANCE: FingerprintDatabase? = null

        fun getInstance(context: Context): FingerprintDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FingerprintDatabase::class.java,
                    "fingerprint_reminder_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
