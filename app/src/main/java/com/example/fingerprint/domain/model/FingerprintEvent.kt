package com.example.fingerprint.domain.model

import java.util.UUID

data class FingerprintEvent(
    val id: String = UUID.randomUUID().toString(),
    val scheduleId: String,
    val eventType: EventType,
    val scheduledTimeEpochMillis: Long,
    val cycleIndex: Int = 0,
    val status: EventStatus = EventStatus.PENDING,
    val snoozedUntilEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null
)
