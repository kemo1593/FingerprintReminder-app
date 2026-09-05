package com.example.fingerprint.data.remote

import android.content.Context
import com.example.fingerprint.alarm.AlarmScheduler
import com.example.fingerprint.auth.AuthSessionManager
import com.example.fingerprint.data.local.FingerprintDao
import com.example.fingerprint.data.local.FingerprintEventEntity
import com.example.fingerprint.data.local.ScheduleConfigEntity
import com.example.fingerprint.data.remote.model.SupabaseScheduleDto
import com.example.fingerprint.data.remote.model.SupabaseScheduleEventDto
import com.example.fingerprint.domain.engine.ScheduleEngine
import com.example.fingerprint.domain.model.EventStatus
import com.example.fingerprint.domain.model.EventType
import com.example.fingerprint.domain.model.SystemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupabaseSyncService(
    private val context: Context,
    private val dao: FingerprintDao,
    private val api: SupabaseApi = SupabaseClientManager.api
) {

    private val sessionManager = AuthSessionManager(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun parseToEpochMillis(dateStr: String?, timeStr: String? = null): Long? {
        if (dateStr.isNullOrBlank()) return null
        val combined = if (!timeStr.isNullOrBlank()) {
            "${dateStr.trim()} ${timeStr.trim()}"
        } else {
            dateStr.trim()
        }

        combined.toLongOrNull()?.let { return it }

        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val parsed = sdf.parse(combined)
                if (parsed != null) return parsed.time
            } catch (e: Exception) {
                // Try next pattern
            }
        }
        return null
    }

    suspend fun sync(
        userId: String,
        accessToken: String,
        isExplicitDeletion: Boolean = false
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (userId.isBlank() || accessToken.isBlank() || accessToken.startsWith("local_")) {
                return@withContext Result.success(true)
            }

            val apiKey = SupabaseClientManager.getAnonKey()
            val bearer = if (accessToken.startsWith("Bearer ")) accessToken else "Bearer $accessToken"
            val userIdFilter = "eq.$userId"

            if (isExplicitDeletion) {
                // User explicitly cleared schedule
                try {
                    api.deleteEvents(apiKey = apiKey, bearerToken = bearer, userIdFilter = userIdFilter)
                    api.deleteSchedules(apiKey = apiKey, bearerToken = bearer, userIdFilter = userIdFilter)
                } catch (e: Exception) {
                    // Ignore deletion network error
                }
                return@withContext Result.success(true)
            }

            val lastSyncedUser = sessionManager.getLastSyncedUserId()
            val isDifferentUser = lastSyncedUser != null && lastSyncedUser != userId

            val localConfig = dao.getLatestScheduleConfigDirect()
            val allLocalEvents = dao.getAllEventsDirect()
            val localEvents = if (localConfig != null) {
                val matching = allLocalEvents.filter { it.scheduleId == localConfig.id }
                if (matching.isNotEmpty()) matching else allLocalEvents
            } else {
                allLocalEvents
            }

            // 1. Fetch remote schedules from Supabase
            val remoteSchedulesResp = try {
                api.getSchedules(apiKey = apiKey, bearerToken = bearer, userIdFilter = userIdFilter)
            } catch (e: Exception) {
                null
            }

            val remoteSchedules = if (remoteSchedulesResp?.isSuccessful == true) {
                remoteSchedulesResp.body() ?: emptyList()
            } else {
                null
            }

            // 2. Fetch remote events from Supabase
            val remoteEventsResp = try {
                api.getEvents(apiKey = apiKey, bearerToken = bearer, userIdFilter = userIdFilter)
            } catch (e: Exception) {
                null
            }

            val remoteEvents = if (remoteEventsResp?.isSuccessful == true) {
                remoteEventsResp.body() ?: emptyList()
            } else {
                emptyList()
            }

            if (remoteSchedules != null && remoteSchedules.isNotEmpty()) {
                val remoteSchedule = remoteSchedules.first()

                // If local DB has no schedule, or local schedule belongs to a different schedule ID / user:
                // RESTORE SCHEDULE FROM SUPABASE TO THIS DEVICE
                if (localConfig == null || localConfig.id != remoteSchedule.id || isDifferentUser) {
                    val firstEpoch = parseToEpochMillis(remoteSchedule.firstEntryDate, remoteSchedule.firstEntryTime)
                        ?: System.currentTimeMillis()
                    val lastEpoch = parseToEpochMillis(remoteSchedule.lastEntryDate, remoteSchedule.lastEntryTime)
                        ?: (firstEpoch + 7 * 24 * 3600 * 1000L)

                    val systemTypeStr = try {
                        SystemType.valueOf(remoteSchedule.systemType).name
                    } catch (e: Exception) {
                        SystemType.SYSTEM_8_2.name
                    }

                    val restoredConfig = ScheduleConfigEntity(
                        id = remoteSchedule.id.ifBlank { "sched_" + System.currentTimeMillis() },
                        systemType = systemTypeStr,
                        firstEntryTimeEpochMillis = firstEpoch,
                        lastEntryTimeEpochMillis = lastEpoch,
                        workDaysCsv = "SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY",
                        customLastOutHoursOffset = null
                    )

                    val restoredEvents: List<FingerprintEventEntity> = if (remoteEvents.isNotEmpty()) {
                        remoteEvents
                            .filter { it.scheduleId == remoteSchedule.id || it.scheduleId.isBlank() }
                            .mapIndexed { index, evDto ->
                                val schedEpoch = parseToEpochMillis(evDto.scheduledDateTime) ?: System.currentTimeMillis()
                                val compEpoch = parseToEpochMillis(evDto.completedAt)
                                val snoozeEpoch = parseToEpochMillis(evDto.snoozedUntil)

                                val evTypeStr = try {
                                    EventType.valueOf(evDto.eventType).name
                                } catch (e: Exception) {
                                    EventType.IN.name
                                }
                                val statusStr = try {
                                    EventStatus.valueOf(evDto.status).name
                                } catch (e: Exception) {
                                    EventStatus.PENDING.name
                                }

                                FingerprintEventEntity(
                                    id = evDto.id.ifBlank { "ev_${restoredConfig.id}_$index" },
                                    scheduleId = restoredConfig.id,
                                    eventType = evTypeStr,
                                    scheduledTimeEpochMillis = schedEpoch,
                                    cycleIndex = index,
                                    status = statusStr,
                                    snoozedUntilEpochMillis = snoozeEpoch,
                                    completedAtEpochMillis = compEpoch
                                )
                            }
                    } else {
                        val generatedDomain = ScheduleEngine.generateSchedule(restoredConfig.toDomain())
                        generatedDomain.map { FingerprintEventEntity.fromDomain(it) }
                    }

                    dao.clearScheduleConfigs()
                    dao.clearAllEvents()
                    dao.insertScheduleConfig(restoredConfig)
                    dao.insertEvents(restoredEvents)

                    sessionManager.setLastSyncedUserId(userId)

                    // Link alarms to this new phone
                    AlarmScheduler.scheduleAllAlarms(context, restoredEvents.map { it.toDomain() })

                    return@withContext Result.success(true)
                } else {
                    // Local schedule already exists with matching ID:
                    // Push local changes to ensure Supabase has the latest status
                    if (localEvents.isNotEmpty()) {
                        pushScheduleToRemote(userId, bearer, apiKey, localConfig, localEvents)
                    }
                    sessionManager.setLastSyncedUserId(userId)
                    AlarmScheduler.scheduleAllAlarms(context, localEvents.map { it.toDomain() })
                    return@withContext Result.success(true)
                }
            } else if (remoteSchedules != null && remoteSchedules.isEmpty()) {
                // Remote account has no schedule
                if (localConfig != null) {
                    // Previous schedule is saved on local phone memory -> sync with the account
                    val targetScheduleId = if (isDifferentUser || localConfig.id.isBlank()) {
                        "sched_" + System.currentTimeMillis()
                    } else {
                        localConfig.id
                    }

                    val sourceEvents = if (localEvents.isNotEmpty()) {
                        localEvents
                    } else {
                        val generatedDomain = ScheduleEngine.generateSchedule(localConfig.toDomain())
                        generatedDomain.map { FingerprintEventEntity.fromDomain(it) }
                    }

                    val finalConfig = if (targetScheduleId != localConfig.id) {
                        localConfig.copy(id = targetScheduleId)
                    } else {
                        localConfig
                    }

                    val finalEvents = sourceEvents.mapIndexed { index, ev ->
                        val newId = if (ev.id.isBlank()) "ev_${targetScheduleId}_$index" else ev.id
                        ev.copy(id = newId, scheduleId = targetScheduleId)
                    }

                    if (targetScheduleId != localConfig.id || localEvents.isEmpty()) {
                        dao.clearScheduleConfigs()
                        dao.clearAllEvents()
                        dao.insertScheduleConfig(finalConfig)
                        dao.insertEvents(finalEvents)
                    }

                    // Push retrieved local schedule to Supabase for this user
                    pushScheduleToRemote(userId, bearer, apiKey, finalConfig, finalEvents)

                    sessionManager.setLastSyncedUserId(userId)

                    // Ensure alarms are scheduled for this active user
                    AlarmScheduler.scheduleAllAlarms(context, finalEvents.map { it.toDomain() })

                    return@withContext Result.success(true)
                } else {
                    // No schedule exists remotely or locally
                    AlarmScheduler.cancelAllAlarms(context)
                    dao.clearScheduleConfigs()
                    dao.clearAllEvents()
                    sessionManager.setLastSyncedUserId(userId)
                    return@withContext Result.success(true)
                }
            } else {
                // Offline or network error: keep existing local data
                return@withContext Result.success(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "sync error", e)
            Result.success(true)
        }
    }

    private suspend fun pushScheduleToRemote(
        userId: String,
        bearer: String,
        apiKey: String,
        config: ScheduleConfigEntity,
        events: List<FingerprintEventEntity>
    ) {
        val scheduleDto = SupabaseScheduleDto(
            id = config.id,
            userId = userId,
            systemType = config.systemType,
            firstEntryDate = dateFormat.format(Date(config.firstEntryTimeEpochMillis)),
            firstEntryTime = timeFormat.format(Date(config.firstEntryTimeEpochMillis)),
            lastEntryDate = dateFormat.format(Date(config.lastEntryTimeEpochMillis)),
            lastEntryTime = timeFormat.format(Date(config.lastEntryTimeEpochMillis)),
            createdAt = System.currentTimeMillis(),
            isActive = true,
            syncTimestamp = System.currentTimeMillis()
        )

        try {
            val schedResp = api.upsertSchedules(
                apiKey = apiKey,
                bearerToken = bearer,
                body = listOf(scheduleDto)
            )
            if (!schedResp.isSuccessful) {
                android.util.Log.e("SupabaseSync", "upsertSchedules failed: ${schedResp.code()} - ${schedResp.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseSync", "Exception in upsertSchedules", e)
        }

        if (events.isNotEmpty()) {
            val eventDtos = events.map { ev ->
                SupabaseScheduleEventDto(
                    id = ev.id,
                    scheduleId = ev.scheduleId,
                    userId = userId,
                    scheduledDateTime = isoFormat.format(Date(ev.scheduledTimeEpochMillis)),
                    eventType = ev.eventType,
                    status = ev.status,
                    completedAt = ev.completedAtEpochMillis?.let { isoFormat.format(Date(it)) },
                    snoozedUntil = ev.snoozedUntilEpochMillis?.let { isoFormat.format(Date(it)) },
                    syncTimestamp = System.currentTimeMillis()
                )
            }

            var eventsUpserted = false
            // Attempt 1: Upsert with merge-duplicates
            try {
                val resp = api.upsertEvents(
                    apiKey = apiKey,
                    bearerToken = bearer,
                    body = eventDtos
                )
                if (resp.isSuccessful) {
                    eventsUpserted = true
                    android.util.Log.d("SupabaseSync", "upsertEvents succeeded for ${eventDtos.size} events")
                } else {
                    android.util.Log.e("SupabaseSync", "upsertEvents failed: ${resp.code()} - ${resp.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseSync", "Exception in upsertEvents", e)
            }

            // Attempt 2: Direct insert (without on_conflict query param)
            if (!eventsUpserted) {
                try {
                    val insertResp = api.insertEvents(
                        apiKey = apiKey,
                        bearerToken = bearer,
                        body = eventDtos
                    )
                    if (insertResp.isSuccessful) {
                        eventsUpserted = true
                        android.util.Log.d("SupabaseSync", "insertEvents succeeded for ${eventDtos.size} events")
                    } else {
                        android.util.Log.e("SupabaseSync", "insertEvents failed: ${insertResp.code()} - ${insertResp.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseSync", "Exception in insertEvents", e)
                }
            }

            // Attempt 3: If batch fails, try item-by-item
            if (!eventsUpserted) {
                for (item in eventDtos) {
                    try {
                        api.insertEvents(apiKey, bearer, listOf(item))
                    } catch (e: Exception) {
                        // ignore individual errors
                    }
                }
            }
        }
    }

    suspend fun syncEventStatus(
        eventId: String,
        status: String,
        completedAtEpochMillis: Long? = null,
        snoozedUntilEpochMillis: Long? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = sessionManager.getUserId() ?: return@withContext Result.success(true)
            val accessToken = sessionManager.getAccessToken() ?: return@withContext Result.success(true)
            if (accessToken.startsWith("local_")) return@withContext Result.success(true)

            val apiKey = SupabaseClientManager.getAnonKey()
            val bearer = if (accessToken.startsWith("Bearer ")) accessToken else "Bearer $accessToken"
            val idFilter = "eq.$eventId"

            val body = mutableMapOf<String, Any?>(
                "status" to status,
                "completed_at" to completedAtEpochMillis?.let { isoFormat.format(Date(it)) },
                "snoozed_until" to snoozedUntilEpochMillis?.let { isoFormat.format(Date(it)) },
                "sync_timestamp" to System.currentTimeMillis()
            )

            val resp = api.updateEvent(
                apiKey = apiKey,
                bearerToken = bearer,
                idFilter = idFilter,
                body = body
            )

            if (!resp.isSuccessful) {
                // If PATCH was not successful, fallback to full sync
                sync(userId, accessToken)
            }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success(true)
        }
    }
}
