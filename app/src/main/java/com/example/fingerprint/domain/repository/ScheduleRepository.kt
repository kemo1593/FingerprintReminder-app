package com.example.fingerprint.domain.repository

import com.example.fingerprint.domain.model.EventStatus
import com.example.fingerprint.domain.model.FingerprintEvent
import com.example.fingerprint.domain.model.ScheduleConfig
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun getLatestScheduleConfig(): Flow<ScheduleConfig?>
    fun getEventsForSchedule(scheduleId: String): Flow<List<FingerprintEvent>>
    fun getAllEvents(): Flow<List<FingerprintEvent>>
    suspend fun saveSchedule(config: ScheduleConfig, events: List<FingerprintEvent>)
    suspend fun updateEventStatus(eventId: String, status: EventStatus, snoozedUntil: Long? = null, completedAt: Long? = null)
    suspend fun clearSchedule()
}
