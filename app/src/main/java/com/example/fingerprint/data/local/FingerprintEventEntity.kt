package com.example.fingerprint.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.fingerprint.domain.model.EventStatus
import com.example.fingerprint.domain.model.EventType
import com.example.fingerprint.domain.model.FingerprintEvent

@Entity(tableName = "fingerprint_events")
data class FingerprintEventEntity(
    @PrimaryKey val id: String,
    val scheduleId: String,
    val eventType: String,
    val scheduledTimeEpochMillis: Long,
    val cycleIndex: Int,
    val status: String,
    val snoozedUntilEpochMillis: Long?,
    val completedAtEpochMillis: Long? = null
) {
    fun toDomain(): FingerprintEvent {
        return FingerprintEvent(
            id = id,
            scheduleId = scheduleId,
            eventType = EventType.valueOf(eventType),
            scheduledTimeEpochMillis = scheduledTimeEpochMillis,
            cycleIndex = cycleIndex,
            status = EventStatus.valueOf(status),
            snoozedUntilEpochMillis = snoozedUntilEpochMillis,
            completedAtEpochMillis = completedAtEpochMillis
        )
    }

    companion object {
        fun fromDomain(event: FingerprintEvent): FingerprintEventEntity {
            return FingerprintEventEntity(
                id = event.id,
                scheduleId = event.scheduleId,
                eventType = event.eventType.name,
                scheduledTimeEpochMillis = event.scheduledTimeEpochMillis,
                cycleIndex = event.cycleIndex,
                status = event.status.name,
                snoozedUntilEpochMillis = event.snoozedUntilEpochMillis,
                completedAtEpochMillis = event.completedAtEpochMillis
            )
        }
    }
}
