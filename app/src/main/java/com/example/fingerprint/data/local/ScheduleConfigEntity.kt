package com.example.fingerprint.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.fingerprint.domain.model.ScheduleConfig
import com.example.fingerprint.domain.model.SystemType
import java.time.DayOfWeek

@Entity(tableName = "schedule_configs")
data class ScheduleConfigEntity(
    @PrimaryKey val id: String,
    val systemType: String,
    val firstEntryTimeEpochMillis: Long,
    val lastEntryTimeEpochMillis: Long,
    val workDaysCsv: String, // e.g. "SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY"
    val customLastOutHoursOffset: Long?
) {
    fun toDomain(): ScheduleConfig {
        val days = if (workDaysCsv.isBlank()) emptySet() else {
            workDaysCsv.split(",").mapNotNull {
                try { DayOfWeek.valueOf(it.trim()) } catch (e: Exception) { null }
            }.toSet()
        }
        return ScheduleConfig(
            id = id,
            systemType = SystemType.valueOf(systemType),
            firstEntryTimeEpochMillis = firstEntryTimeEpochMillis,
            lastEntryTimeEpochMillis = lastEntryTimeEpochMillis,
            workDays = days,
            customLastOutHoursOffset = customLastOutHoursOffset
        )
    }

    companion object {
        fun fromDomain(config: ScheduleConfig): ScheduleConfigEntity {
            return ScheduleConfigEntity(
                id = config.id,
                systemType = config.systemType.name,
                firstEntryTimeEpochMillis = config.firstEntryTimeEpochMillis,
                lastEntryTimeEpochMillis = config.lastEntryTimeEpochMillis,
                workDaysCsv = config.workDays.joinToString(",") { it.name },
                customLastOutHoursOffset = config.customLastOutHoursOffset
            )
        }
    }
}
