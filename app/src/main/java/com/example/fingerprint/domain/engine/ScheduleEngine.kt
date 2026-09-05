package com.example.fingerprint.domain.engine

import com.example.fingerprint.domain.model.EventType
import com.example.fingerprint.domain.model.FingerprintEvent
import com.example.fingerprint.domain.model.ScheduleConfig
import com.example.fingerprint.domain.model.SystemType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ScheduleEngine {

    /**
     * Generates all fingerprint events for a given schedule configuration.
     *
     * @param config The schedule configuration.
     * @param zoneId Timezone used for date calculations (defaults to system default).
     * @return List of generated [FingerprintEvent] objects in chronological order.
     */
    fun generateSchedule(
        config: ScheduleConfig,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<FingerprintEvent> {
        val events = mutableListOf<FingerprintEvent>()

        val startZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(config.firstEntryTimeEpochMillis), zoneId)
        val endZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(config.lastEntryTimeEpochMillis), zoneId)

        if (startZdt.isAfter(endZdt)) {
            return emptyList()
        }

        when (config.systemType) {
            SystemType.SYSTEM_8_2,
            SystemType.SYSTEM_8_3,
            SystemType.SYSTEM_8_5,
            SystemType.SYSTEM_9_3,
            SystemType.SYSTEM_9_5,
            SystemType.SYSTEM_10_5 -> {
                generateSimpleDailySchedule(config, startZdt, endZdt, zoneId, events)
            }
            SystemType.SYSTEM_18_6 -> {
                generate18_6Schedule(config, startZdt, endZdt, zoneId, events)
            }
            SystemType.SYSTEM_24_6 -> {
                generate24_6Schedule(config, startZdt, endZdt, zoneId, events)
            }
            SystemType.SYSTEM_12_12 -> {
                generate12_12Schedule(config, startZdt, endZdt, zoneId, events)
            }
        }

        // Ensure the list is sorted chronologically
        var sortedEvents = events.sortedBy { it.scheduledTimeEpochMillis }

        // Guarantee that if there are events, the final event is an OUT event if not already
        if (sortedEvents.isNotEmpty()) {
            val lastEvent = sortedEvents.last()
            if (lastEvent.eventType != EventType.OUT) {
                // If last event was IN or OUT_IN, we can ensure an OUT event exists at or near end time
                val finalOutTime = if (config.customLastOutHoursOffset != null) {
                    val prevTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastEvent.scheduledTimeEpochMillis), zoneId)
                    prevTime.plusHours(config.customLastOutHoursOffset).toInstant().toEpochMilli()
                } else {
                    config.lastEntryTimeEpochMillis
                }

                if (finalOutTime > lastEvent.scheduledTimeEpochMillis) {
                    val finalOutEvent = FingerprintEvent(
                        scheduleId = config.id,
                        eventType = EventType.OUT,
                        scheduledTimeEpochMillis = finalOutTime,
                        cycleIndex = lastEvent.cycleIndex
                    )
                    sortedEvents = sortedEvents + finalOutEvent
                }
            }
        }

        // Automatically mark past events preceding the next due fingerprint as DONE
        val now = System.currentTimeMillis()
        val activeWindowStart = now - (15 * 60 * 1000L)
        val nextDueIndex = sortedEvents.indexOfFirst { it.scheduledTimeEpochMillis >= activeWindowStart }

        val finalEvents = if (nextDueIndex > 0) {
            sortedEvents.mapIndexed { index, event ->
                if (index < nextDueIndex) {
                    event.copy(
                        status = com.example.fingerprint.domain.model.EventStatus.DONE,
                        completedAtEpochMillis = event.scheduledTimeEpochMillis
                    )
                } else {
                    event
                }
            }
        } else if (nextDueIndex == -1 && sortedEvents.isNotEmpty()) {
            sortedEvents.map { 
                it.copy(
                    status = com.example.fingerprint.domain.model.EventStatus.DONE,
                    completedAtEpochMillis = it.scheduledTimeEpochMillis
                ) 
            }
        } else {
            sortedEvents
        }

        return finalEvents
    }

    private fun generateSimpleDailySchedule(
        config: ScheduleConfig,
        startZdt: ZonedDateTime,
        endZdt: ZonedDateTime,
        zoneId: ZoneId,
        events: MutableList<FingerprintEvent>
    ) {
        val defaultShiftDurationHours = when (config.systemType) {
            SystemType.SYSTEM_8_2 -> 6L
            SystemType.SYSTEM_8_3 -> 7L
            SystemType.SYSTEM_8_5 -> 9L
            SystemType.SYSTEM_9_3 -> 6L
            SystemType.SYSTEM_9_5 -> 8L
            SystemType.SYSTEM_10_5 -> 7L
            else -> 6L
        }

        val startTime = startZdt.toLocalTime()
        val endTime = endZdt.toLocalTime()
        val rawDiffMinutes = java.time.Duration.between(startTime, endTime).toMinutes()
        val shiftDurationMinutes = if (rawDiffMinutes > 0) {
            rawDiffMinutes
        } else if (rawDiffMinutes < 0) {
            rawDiffMinutes + (24 * 60)
        } else {
            defaultShiftDurationHours * 60
        }

        var currentDate = startZdt.toLocalDate()
        val endDate = endZdt.toLocalDate()

        var cycleIndex = 0

        while (!currentDate.isAfter(endDate)) {
            if (config.workDays.contains(currentDate.dayOfWeek)) {
                val inZdt = ZonedDateTime.of(currentDate, startTime, zoneId)
                val outZdt = inZdt.plusMinutes(shiftDurationMinutes)

                val inMillis = inZdt.toInstant().toEpochMilli()
                val outMillis = outZdt.toInstant().toEpochMilli()

                if (inMillis in config.firstEntryTimeEpochMillis..config.lastEntryTimeEpochMillis) {
                    events.add(
                        FingerprintEvent(
                            scheduleId = config.id,
                            eventType = EventType.IN,
                            scheduledTimeEpochMillis = inMillis,
                            cycleIndex = cycleIndex
                        )
                    )
                }

                val maxTolerance = 60 * 1000L // 1 minute tolerance
                if (outMillis in config.firstEntryTimeEpochMillis..(config.lastEntryTimeEpochMillis + maxTolerance)) {
                    events.add(
                        FingerprintEvent(
                            scheduleId = config.id,
                            eventType = EventType.OUT,
                            scheduledTimeEpochMillis = outMillis,
                            cycleIndex = cycleIndex
                        )
                    )
                }

                cycleIndex++
            }
            currentDate = currentDate.plusDays(1)
        }
    }

    private fun generate18_6Schedule(
        config: ScheduleConfig,
        startZdt: ZonedDateTime,
        endZdt: ZonedDateTime,
        zoneId: ZoneId,
        events: MutableList<FingerprintEvent>
    ) {
        var currentCycleStart = startZdt
        var cycleIndex = 0

        while (!currentCycleStart.isAfter(endZdt)) {
            val inTime = currentCycleStart
            val outInTime = currentCycleStart.plusHours(12)
            val outTime = currentCycleStart.plusHours(18)

            val inMillis = inTime.toInstant().toEpochMilli()
            val outInMillis = outInTime.toInstant().toEpochMilli()
            val outMillis = outTime.toInstant().toEpochMilli()

            if (inMillis <= config.lastEntryTimeEpochMillis) {
                events.add(
                    FingerprintEvent(
                        scheduleId = config.id,
                        eventType = EventType.IN,
                        scheduledTimeEpochMillis = inMillis,
                        cycleIndex = cycleIndex
                    )
                )
            }

            if (outInMillis in (config.firstEntryTimeEpochMillis + 1)..config.lastEntryTimeEpochMillis) {
                events.add(
                    FingerprintEvent(
                        scheduleId = config.id,
                        eventType = EventType.OUT_IN,
                        scheduledTimeEpochMillis = outInMillis,
                        cycleIndex = cycleIndex
                    )
                )
            }

            if (outMillis in (config.firstEntryTimeEpochMillis + 1)..config.lastEntryTimeEpochMillis) {
                events.add(
                    FingerprintEvent(
                        scheduleId = config.id,
                        eventType = EventType.OUT,
                        scheduledTimeEpochMillis = outMillis,
                        cycleIndex = cycleIndex
                    )
                )
            }

            // 18h work + 6h rest = 24h cycle repeat
            currentCycleStart = currentCycleStart.plusHours(24)
            cycleIndex++
        }
    }

    private fun generate24_6Schedule(
        config: ScheduleConfig,
        startZdt: ZonedDateTime,
        endZdt: ZonedDateTime,
        zoneId: ZoneId,
        events: MutableList<FingerprintEvent>
    ) {
        var currentCycleStart = startZdt
        var cycleIndex = 0

        while (!currentCycleStart.isAfter(endZdt)) {
            val inTime = currentCycleStart
            val outInTime = currentCycleStart.plusHours(12)

            // Check if this is the final cycle before Last Entry Time and custom offset is set
            val isNextCycleBeyondEnd = currentCycleStart.plusHours(30).isAfter(endZdt)
            val outHoursOffset = if (isNextCycleBeyondEnd && config.customLastOutHoursOffset != null) {
                config.customLastOutHoursOffset
            } else {
                24L
            }

            val outTime = currentCycleStart.plusHours(outHoursOffset)

            val inMillis = inTime.toInstant().toEpochMilli()
            val outInMillis = outInTime.toInstant().toEpochMilli()
            val outMillis = outTime.toInstant().toEpochMilli()

            if (inMillis <= config.lastEntryTimeEpochMillis) {
                events.add(
                    FingerprintEvent(
                        scheduleId = config.id,
                        eventType = EventType.IN,
                        scheduledTimeEpochMillis = inMillis,
                        cycleIndex = cycleIndex
                    )
                )
            }

            if (outInMillis in (config.firstEntryTimeEpochMillis + 1)..config.lastEntryTimeEpochMillis) {
                events.add(
                    FingerprintEvent(
                        scheduleId = config.id,
                        eventType = EventType.OUT_IN,
                        scheduledTimeEpochMillis = outInMillis,
                        cycleIndex = cycleIndex
                    )
                )
            }

            if (outMillis in (config.firstEntryTimeEpochMillis + 1)..config.lastEntryTimeEpochMillis) {
                events.add(
                    FingerprintEvent(
                        scheduleId = config.id,
                        eventType = EventType.OUT,
                        scheduledTimeEpochMillis = outMillis,
                        cycleIndex = cycleIndex
                    )
                )
            }

            // 24h work + 6h rest = 30h shift cycle
            currentCycleStart = currentCycleStart.plusHours(30)
            cycleIndex++
        }
    }

    private fun generate12_12Schedule(
        config: ScheduleConfig,
        startZdt: ZonedDateTime,
        endZdt: ZonedDateTime,
        zoneId: ZoneId,
        events: MutableList<FingerprintEvent>
    ) {
        var currentCycleStart = startZdt
        var cycleIndex = 0

        while (!currentCycleStart.isAfter(endZdt)) {
            val inTime = currentCycleStart
            val outTime = currentCycleStart.plusHours(12)

            val inMillis = inTime.toInstant().toEpochMilli()
            val outMillis = outTime.toInstant().toEpochMilli()

            if (inMillis <= config.lastEntryTimeEpochMillis) {
                events.add(
                    FingerprintEvent(
                        scheduleId = config.id,
                        eventType = EventType.IN,
                        scheduledTimeEpochMillis = inMillis,
                        cycleIndex = cycleIndex
                    )
                )
            }

            if (outMillis in (config.firstEntryTimeEpochMillis + 1)..config.lastEntryTimeEpochMillis) {
                events.add(
                    FingerprintEvent(
                        scheduleId = config.id,
                        eventType = EventType.OUT,
                        scheduledTimeEpochMillis = outMillis,
                        cycleIndex = cycleIndex
                    )
                )
            }

            // 12h work + 12h rest = 24h cycle repeat
            currentCycleStart = currentCycleStart.plusHours(24)
            cycleIndex++
        }
    }
}
