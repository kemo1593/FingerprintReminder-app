package com.example.fingerprint.domain.engine

import com.example.fingerprint.domain.model.EventType
import com.example.fingerprint.domain.model.ScheduleConfig
import com.example.fingerprint.domain.model.SystemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleEngineTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testSystem8_2_SimpleDailySchedule() {
        // Start Sunday 8:00 AM, End Tuesday 2:00 PM (3 work days)
        val startDateTime = LocalDateTime.of(LocalDate.of(2026, 8, 2), LocalTime.of(8, 0)) // Sunday
        val endDateTime = LocalDateTime.of(LocalDate.of(2026, 8, 4), LocalTime.of(14, 0))   // Tuesday

        val config = ScheduleConfig(
            systemType = SystemType.SYSTEM_8_2,
            firstEntryTimeEpochMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli(),
            lastEntryTimeEpochMillis = endDateTime.atZone(zoneId).toInstant().toEpochMilli(),
            workDays = setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        )

        val events = ScheduleEngine.generateSchedule(config, zoneId)

        // 3 days x 2 events = 6 events
        assertEquals(6, events.size)

        // Sunday
        assertEquals(EventType.IN, events[0].eventType)
        assertEquals(startDateTime.atZone(zoneId).toInstant().toEpochMilli(), events[0].scheduledTimeEpochMillis)
        assertEquals(EventType.OUT, events[1].eventType)
        assertEquals(startDateTime.plusHours(6).atZone(zoneId).toInstant().toEpochMilli(), events[1].scheduledTimeEpochMillis)

        // Tuesday final Out
        assertEquals(EventType.OUT, events[5].eventType)
        assertEquals(endDateTime.atZone(zoneId).toInstant().toEpochMilli(), events[5].scheduledTimeEpochMillis)
    }

    @Test
    fun testSystem18_6_FixedDailyCycle() {
        // Start Sunday 8:00 AM, End Monday 8:00 AM (1 full 24h cycle)
        val startDateTime = LocalDateTime.of(LocalDate.of(2026, 8, 2), LocalTime.of(8, 0))
        val endDateTime = LocalDateTime.of(LocalDate.of(2026, 8, 3), LocalTime.of(8, 0))

        val config = ScheduleConfig(
            systemType = SystemType.SYSTEM_18_6,
            firstEntryTimeEpochMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli(),
            lastEntryTimeEpochMillis = endDateTime.atZone(zoneId).toInstant().toEpochMilli()
        )

        val events = ScheduleEngine.generateSchedule(config, zoneId)

        // Cycle 0: IN (8 AM), OUT_IN (8 PM), OUT (2 AM next day) + Cycle 1: IN (8 AM next day)
        assertEquals(4, events.size)

        assertEquals(EventType.IN, events[0].eventType)
        assertEquals(startDateTime.atZone(zoneId).toInstant().toEpochMilli(), events[0].scheduledTimeEpochMillis)

        assertEquals(EventType.OUT_IN, events[1].eventType)
        assertEquals(startDateTime.plusHours(12).atZone(zoneId).toInstant().toEpochMilli(), events[1].scheduledTimeEpochMillis)

        assertEquals(EventType.OUT, events[2].eventType)
        assertEquals(startDateTime.plusHours(18).atZone(zoneId).toInstant().toEpochMilli(), events[2].scheduledTimeEpochMillis)

        assertEquals(EventType.IN, events[3].eventType)
        assertEquals(startDateTime.plusHours(24).atZone(zoneId).toInstant().toEpochMilli(), events[3].scheduledTimeEpochMillis)
    }

    @Test
    fun testSystem24_6_RotatingStartTimesAndMonthBoundary() {
        // Start Aug 28, 2026 at 8:00 AM. Run for 5 cycles (150h total) spanning into September.
        // Cycle 0: 8:00 AM (Aug 28)
        // Cycle 1: 2:00 PM (Aug 29) [+30h]
        // Cycle 2: 8:00 PM (Aug 30) [+60h]
        // Cycle 3: 2:00 AM (Sep 1) [+90h] -> Month boundary crossed!
        // Cycle 4: 8:00 AM (Sep 2) [+120h] -> Back to original clock time 8:00 AM!

        val startDateTime = LocalDateTime.of(LocalDate.of(2026, 8, 28), LocalTime.of(8, 0))
        val endDateTime = startDateTime.plusHours(120) // Up to start of 5th cycle

        val config = ScheduleConfig(
            systemType = SystemType.SYSTEM_24_6,
            firstEntryTimeEpochMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli(),
            lastEntryTimeEpochMillis = endDateTime.atZone(zoneId).toInstant().toEpochMilli()
        )

        val events = ScheduleEngine.generateSchedule(config, zoneId)

        // Filter IN events to verify start times
        val inEvents = events.filter { it.eventType == EventType.IN }

        assertEquals(5, inEvents.size)

        // Verify rotating clock times: 8:00 -> 14:00 -> 20:00 -> 02:00 -> 08:00
        val times = inEvents.map {
            ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.scheduledTimeEpochMillis), zoneId).toLocalTime()
        }

        assertEquals(LocalTime.of(8, 0), times[0])
        assertEquals(LocalTime.of(14, 0), times[1])
        assertEquals(LocalTime.of(20, 0), times[2])
        assertEquals(LocalTime.of(2, 0), times[3])
        assertEquals(LocalTime.of(8, 0), times[4])

        // Verify month boundary crossing for cycle 3 (Sep 1)
        val cycle3Date = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(inEvents[3].scheduledTimeEpochMillis),
            zoneId
        ).toLocalDate()

        assertEquals(LocalDate.of(2026, 9, 1), cycle3Date)
    }

    @Test
    fun testSystem12_12_RestSystem() {
        // Start Sunday Aug 2, 2026 8:00 AM, End Tuesday Aug 4, 2026 8:00 PM
        // Cycle 0: IN (Sun 8:00 AM), OUT (Sun 8:00 PM) -> Rest 12h
        // Cycle 1: IN (Mon 8:00 AM), OUT (Mon 8:00 PM) -> Rest 12h
        // Cycle 2: IN (Tue 8:00 AM), OUT (Tue 8:00 PM)
        val startDateTime = LocalDateTime.of(LocalDate.of(2026, 8, 2), LocalTime.of(8, 0))
        val endDateTime = LocalDateTime.of(LocalDate.of(2026, 8, 4), LocalTime.of(20, 0))

        val config = ScheduleConfig(
            systemType = SystemType.SYSTEM_12_12,
            firstEntryTimeEpochMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli(),
            lastEntryTimeEpochMillis = endDateTime.atZone(zoneId).toInstant().toEpochMilli()
        )

        val events = ScheduleEngine.generateSchedule(config, zoneId)

        // 3 cycles x 2 events (IN, OUT) = 6 events
        assertEquals(6, events.size)

        // Cycle 0
        assertEquals(EventType.IN, events[0].eventType)
        assertEquals(startDateTime.atZone(zoneId).toInstant().toEpochMilli(), events[0].scheduledTimeEpochMillis)
        assertEquals(EventType.OUT, events[1].eventType)
        assertEquals(startDateTime.plusHours(12).atZone(zoneId).toInstant().toEpochMilli(), events[1].scheduledTimeEpochMillis)

        // Cycle 1
        assertEquals(EventType.IN, events[2].eventType)
        assertEquals(startDateTime.plusHours(24).atZone(zoneId).toInstant().toEpochMilli(), events[2].scheduledTimeEpochMillis)
        assertEquals(EventType.OUT, events[3].eventType)
        assertEquals(startDateTime.plusHours(36).atZone(zoneId).toInstant().toEpochMilli(), events[3].scheduledTimeEpochMillis)

        // Cycle 2
        assertEquals(EventType.IN, events[4].eventType)
        assertEquals(startDateTime.plusHours(48).atZone(zoneId).toInstant().toEpochMilli(), events[4].scheduledTimeEpochMillis)
        assertEquals(EventType.OUT, events[5].eventType)
        assertEquals(endDateTime.atZone(zoneId).toInstant().toEpochMilli(), events[5].scheduledTimeEpochMillis)
    }
}
