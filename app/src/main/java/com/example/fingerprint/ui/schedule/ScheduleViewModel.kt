package com.example.fingerprint.ui.schedule

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fingerprint.alarm.AlarmScheduler
import com.example.fingerprint.alarm.AlarmSoundManager
import com.example.fingerprint.auth.AuthSessionManager
import com.example.fingerprint.data.local.FingerprintDatabase
import com.example.fingerprint.data.remote.SupabaseSyncService
import com.example.fingerprint.data.repository.ScheduleRepositoryImpl
import com.example.fingerprint.domain.engine.ScheduleEngine
import com.example.fingerprint.domain.model.EventStatus
import com.example.fingerprint.domain.model.FingerprintEvent
import com.example.fingerprint.domain.model.ScheduleConfig
import com.example.fingerprint.domain.model.SystemType
import com.example.fingerprint.domain.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

data class ScheduleUiState(
    val activeConfig: ScheduleConfig? = null,
    val events: List<FingerprintEvent> = emptyList(),
    val nextEvent: FingerprintEvent? = null,
    val isArabic: Boolean = true,
    val isLoading: Boolean = false
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScheduleRepository = ScheduleRepositoryImpl(
        FingerprintDatabase.getInstance(application).fingerprintDao()
    )

    private val prefs = application.getSharedPreferences("fingerprint_app_prefs", android.content.Context.MODE_PRIVATE)
    private val _isArabic = MutableStateFlow(prefs.getBoolean("is_arabic", true))
    val isArabic: StateFlow<Boolean> = _isArabic

    val uiState: StateFlow<ScheduleUiState> = combine(
        repository.getLatestScheduleConfig(),
        repository.getAllEvents(),
        _isArabic
    ) { config, events, isAr ->
        val now = System.currentTimeMillis()
        val activeWindowStart = now - (15 * 60 * 1000L)

        val next = events.firstOrNull { event ->
            if (event.status == EventStatus.DONE) return@firstOrNull false
            val targetTime = if (event.status == EventStatus.SNOOZED && event.snoozedUntilEpochMillis != null) {
                event.snoozedUntilEpochMillis
            } else {
                event.scheduledTimeEpochMillis
            }
            targetTime >= activeWindowStart
        }
            ?: events.firstOrNull { it.status != EventStatus.DONE }
            ?: events.lastOrNull()

        ScheduleUiState(
            activeConfig = config,
            events = events,
            nextEvent = next,
            isArabic = isAr,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleUiState()
    )

    init {
        viewModelScope.launch {
            repository.getAllEvents().collect { events ->
                if (events.isNotEmpty()) {
                    AlarmScheduler.scheduleAllAlarms(getApplication(), events)
                }
            }
        }
    }

    fun setLanguage(isArabic: Boolean) {
        _isArabic.value = isArabic
        prefs.edit().putBoolean("is_arabic", isArabic).apply()
    }

    private val syncService = SupabaseSyncService(
        context = application,
        dao = FingerprintDatabase.getInstance(application).fingerprintDao()
    )
    private val sessionManager = AuthSessionManager(application)

    private fun triggerBackgroundSync(isExplicitDeletion: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = sessionManager.getUserId()
            val token = sessionManager.getAccessToken()
            if (!userId.isNullOrBlank() && !token.isNullOrBlank()) {
                syncService.sync(userId, token, isExplicitDeletion = isExplicitDeletion)
            }
        }
    }

    fun generateAndSaveSchedule(
        systemType: SystemType,
        firstEntryLdt: LocalDateTime,
        lastEntryLdt: LocalDateTime,
        workDays: Set<DayOfWeek>,
        customLastOutHoursOffset: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        viewModelScope.launch {
            val firstMillis = firstEntryLdt.atZone(zoneId).toInstant().toEpochMilli()
            val lastMillis = lastEntryLdt.atZone(zoneId).toInstant().toEpochMilli()

            val config = ScheduleConfig(
                systemType = systemType,
                firstEntryTimeEpochMillis = firstMillis,
                lastEntryTimeEpochMillis = lastMillis,
                workDays = workDays,
                customLastOutHoursOffset = customLastOutHoursOffset
            )

            val events = ScheduleEngine.generateSchedule(config, zoneId)
            repository.saveSchedule(config, events)

            // Schedule alarms for generated schedule
            AlarmScheduler.scheduleAllAlarms(getApplication(), events)
            triggerBackgroundSync()
        }
    }

    fun markEventDone(eventId: String) {
        viewModelScope.launch {
            val completedAt = System.currentTimeMillis()
            repository.updateEventStatus(eventId, EventStatus.DONE, null, completedAt)
            AlarmSoundManager.stop()
            AlarmScheduler.cancelAlarm(getApplication(), eventId)
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(eventId.hashCode())
            syncService.syncEventStatus(eventId, EventStatus.DONE.name, completedAtEpochMillis = completedAt)
            triggerBackgroundSync()
        }
    }

    fun snoozeEvent(eventId: String, eventTypeLabel: String, minutes: Int = 5) {
        viewModelScope.launch {
            AlarmSoundManager.stop()
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(eventId.hashCode())

            val snoozeMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)
            repository.updateEventStatus(eventId, EventStatus.SNOOZED, snoozeMillis)
            val currentEvent = uiState.value.events.firstOrNull { it.id == eventId }
            val scheduledTime = currentEvent?.scheduledTimeEpochMillis ?: snoozeMillis

            // Cancel any old repeat/pre/snooze alarms
            AlarmScheduler.cancelStageAlarm(getApplication(), eventId, AlarmScheduler.ALARM_STAGE_REPEAT)
            AlarmScheduler.cancelStageAlarm(getApplication(), eventId, AlarmScheduler.ALARM_STAGE_PRE)
            AlarmScheduler.cancelStageAlarm(getApplication(), eventId, AlarmScheduler.ALARM_STAGE_SNOOZE)

            AlarmScheduler.scheduleRawAlarm(
                context = getApplication(),
                eventId = eventId,
                triggerAtMillis = snoozeMillis,
                scheduledTimeMillis = scheduledTime,
                eventTypeLabel = eventTypeLabel,
                alarmStage = AlarmScheduler.ALARM_STAGE_SNOOZE,
                isSnooze = true
            )

            if (eventTypeLabel == "IN" && scheduledTime > System.currentTimeMillis()) {
                AlarmScheduler.scheduleRawAlarm(
                    context = getApplication(),
                    eventId = eventId,
                    triggerAtMillis = scheduledTime,
                    scheduledTimeMillis = scheduledTime,
                    eventTypeLabel = eventTypeLabel,
                    alarmStage = AlarmScheduler.ALARM_STAGE_EXACT,
                    isSnooze = false
                )
            }
            syncService.syncEventStatus(eventId, EventStatus.SNOOZED.name, snoozedUntilEpochMillis = snoozeMillis)
            triggerBackgroundSync()
        }
    }

    fun clearSchedule() {
        viewModelScope.launch {
            uiState.value.events.forEach { event ->
                AlarmScheduler.cancelAlarm(getApplication(), event.id)
            }
            repository.clearSchedule()
            triggerBackgroundSync(isExplicitDeletion = true)
        }
    }
}
