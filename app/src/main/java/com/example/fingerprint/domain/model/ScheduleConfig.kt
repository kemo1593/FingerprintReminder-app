package com.example.fingerprint.domain.model

import java.time.DayOfWeek
import java.util.UUID

/**
 * Configuration for schedule generation.
 * @param id Unique ID for the schedule config.
 * @param systemType One of 8-2, 8-3, 9-3, 10-5, 18-6, 24-6.
 * @param firstEntryTimeEpochMillis Start date and time of the schedule.
 * @param lastEntryTimeEpochMillis End date and time limit of the schedule.
 * @param workDays Set of java.time.DayOfWeek for simple daily systems (defaults to Sun-Thu in MENA region).
 * @param customLastOutHoursOffset Optional override for the final Out event offset in hours.
 */
data class ScheduleConfig(
    val id: String = UUID.randomUUID().toString(),
    val systemType: SystemType,
    val firstEntryTimeEpochMillis: Long,
    val lastEntryTimeEpochMillis: Long,
    val workDays: Set<DayOfWeek> = setOf(
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY
    ),
    val customLastOutHoursOffset: Long? = null
)
