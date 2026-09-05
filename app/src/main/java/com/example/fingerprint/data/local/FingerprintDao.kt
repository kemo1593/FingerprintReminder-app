package com.example.fingerprint.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FingerprintDao {

    @Query("SELECT * FROM schedule_configs ORDER BY firstEntryTimeEpochMillis DESC LIMIT 1")
    fun getLatestScheduleConfig(): Flow<ScheduleConfigEntity?>

    @Query("SELECT * FROM schedule_configs ORDER BY firstEntryTimeEpochMillis DESC LIMIT 1")
    suspend fun getLatestScheduleConfigDirect(): ScheduleConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleConfig(config: ScheduleConfigEntity)

    @Query("DELETE FROM schedule_configs")
    suspend fun clearScheduleConfigs()

    @Query("SELECT * FROM fingerprint_events WHERE scheduleId = :scheduleId ORDER BY scheduledTimeEpochMillis ASC")
    fun getEventsForSchedule(scheduleId: String): Flow<List<FingerprintEventEntity>>

    @Query("SELECT * FROM fingerprint_events WHERE scheduleId = :scheduleId ORDER BY scheduledTimeEpochMillis ASC")
    suspend fun getEventsForScheduleDirect(scheduleId: String): List<FingerprintEventEntity>

    @Query("SELECT * FROM fingerprint_events ORDER BY scheduledTimeEpochMillis ASC")
    fun getAllEvents(): Flow<List<FingerprintEventEntity>>

    @Query("SELECT * FROM fingerprint_events ORDER BY scheduledTimeEpochMillis ASC")
    suspend fun getAllEventsDirect(): List<FingerprintEventEntity>

    @Query("SELECT * FROM fingerprint_events WHERE id = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): FingerprintEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<FingerprintEventEntity>)

    @Query("UPDATE fingerprint_events SET status = :status, snoozedUntilEpochMillis = :snoozedUntil, completedAtEpochMillis = :completedAt WHERE id = :eventId")
    suspend fun updateEventStatus(eventId: String, status: String, snoozedUntil: Long? = null, completedAt: Long? = null)

    @Query("DELETE FROM fingerprint_events")
    suspend fun clearAllEvents()
}
