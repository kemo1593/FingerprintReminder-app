package com.example.fingerprint.data.repository

import com.example.fingerprint.data.local.FingerprintDao
import com.example.fingerprint.data.local.FingerprintEventEntity
import com.example.fingerprint.data.local.ScheduleConfigEntity
import com.example.fingerprint.domain.model.EventStatus
import com.example.fingerprint.domain.model.FingerprintEvent
import com.example.fingerprint.domain.model.ScheduleConfig
import com.example.fingerprint.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduleRepositoryImpl(
    private val dao: FingerprintDao
) : ScheduleRepository {

    override fun getLatestScheduleConfig(): Flow<ScheduleConfig?> {
        return dao.getLatestScheduleConfig().map { it?.toDomain() }
    }

    override fun getEventsForSchedule(scheduleId: String): Flow<List<FingerprintEvent>> {
        return dao.getEventsForSchedule(scheduleId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllEvents(): Flow<List<FingerprintEvent>> {
        return dao.getAllEvents().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveSchedule(config: ScheduleConfig, events: List<FingerprintEvent>) {
        dao.clearScheduleConfigs()
        dao.clearAllEvents()
        dao.insertScheduleConfig(ScheduleConfigEntity.fromDomain(config))
        dao.insertEvents(events.map { FingerprintEventEntity.fromDomain(it) })
    }

    override suspend fun updateEventStatus(eventId: String, status: EventStatus, snoozedUntil: Long?, completedAt: Long?) {
        dao.updateEventStatus(eventId, status.name, snoozedUntil, completedAt)
    }

    override suspend fun clearSchedule() {
        dao.clearScheduleConfigs()
        dao.clearAllEvents()
    }
}
