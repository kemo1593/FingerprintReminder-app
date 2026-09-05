package com.example.fingerprint.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.fingerprint.domain.model.EventStatus
import com.example.fingerprint.domain.model.EventType
import com.example.fingerprint.domain.model.FingerprintEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AlarmScheduler {

    const val CHANNEL_ID = "fingerprint_alarm_channel_v3"
    const val CHANNEL_NAME_AR = "تنبيهات البصمة"
    const val CHANNEL_NAME_EN = "Fingerprint Alarms"

    const val EXTRA_EVENT_ID = "extra_event_id"
    const val EXTRA_EVENT_TYPE = "extra_event_type"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    const val EXTRA_ALARM_STAGE = "extra_alarm_stage"

    const val ALARM_STAGE_PRE = "STAGE_PRE_15M"
    const val ALARM_STAGE_EXACT = "STAGE_EXACT"
    const val ALARM_STAGE_SNOOZE = "STAGE_SNOOZE"
    const val ALARM_STAGE_REPEAT = "STAGE_REPEAT"

    const val ACTION_TRIGGER_ALARM = "com.example.fingerprint.ACTION_TRIGGER_ALARM"
    const val ACTION_DONE = "com.example.fingerprint.ACTION_DONE"
    const val ACTION_SNOOZE = "com.example.fingerprint.ACTION_SNOOZE"
    const val ACTION_DISMISS_ALARM = "com.example.fingerprint.ACTION_DISMISS_ALARM"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME_AR,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming fingerprint scan reminders"
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500, 500, 500)
                enableLights(true)
                setShowBadge(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun getRequestCode(eventId: String, stage: String): Int {
        val base = eventId.hashCode()
        return when (stage) {
            ALARM_STAGE_PRE -> base * 31 + 1
            ALARM_STAGE_EXACT -> base * 31 + 2
            ALARM_STAGE_SNOOZE -> base * 31 + 3
            ALARM_STAGE_REPEAT -> base * 31 + 4
            else -> base
        }
    }

    fun scheduleRawAlarm(
        context: Context,
        eventId: String,
        triggerAtMillis: Long,
        scheduledTimeMillis: Long,
        eventTypeLabel: String,
        alarmStage: String,
        isSnooze: Boolean = false
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        createNotificationChannel(context)

        val now = System.currentTimeMillis()
        val finalTriggerMillis = if (triggerAtMillis <= now) now + 500L else triggerAtMillis

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TYPE, eventTypeLabel)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
            putExtra(EXTRA_ALARM_STAGE, alarmStage)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }

        val requestCode = getRequestCode(eventId, alarmStage)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, AlarmActivity::class.java).apply {
                    putExtra(EXTRA_EVENT_ID, eventId)
                    putExtra(EXTRA_EVENT_TYPE, eventTypeLabel)
                    putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
                    putExtra(EXTRA_ALARM_STAGE, alarmStage)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode + 100,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(finalTriggerMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, finalTriggerMillis, pendingIntent)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun scheduleEventAlarms(context: Context, event: FingerprintEvent) {
        if (event.status == EventStatus.DONE) {
            cancelAlarm(context, event.id)
            return
        }

        val now = System.currentTimeMillis()

        // 1. If currently SNOOZED, schedule the snooze alarm
        if (event.status == EventStatus.SNOOZED && event.snoozedUntilEpochMillis != null) {
            scheduleRawAlarm(
                context = context,
                eventId = event.id,
                triggerAtMillis = event.snoozedUntilEpochMillis,
                scheduledTimeMillis = event.scheduledTimeEpochMillis,
                eventTypeLabel = event.eventType.name,
                alarmStage = ALARM_STAGE_SNOOZE,
                isSnooze = true
            )
        }

        // 2. Schedule stage alarms based on event type
        if (event.eventType == EventType.IN) {
            val preAlarmTime = event.scheduledTimeEpochMillis - (15 * 60 * 1000L)
            val exactTime = event.scheduledTimeEpochMillis

            // Pre-alarm logic:
            // - If now < preAlarmTime: triggers at 15m before IN time.
            // - If schedule is set and IN time is within 15 minutes (preAlarmTime <= now <= exactTime),
            //   and event is PENDING, trigger pre-alarm IMMEDIATELY!
            if (event.status == EventStatus.PENDING) {
                if (now < preAlarmTime) {
                    scheduleRawAlarm(
                        context = context,
                        eventId = event.id,
                        triggerAtMillis = preAlarmTime,
                        scheduledTimeMillis = event.scheduledTimeEpochMillis,
                        eventTypeLabel = event.eventType.name,
                        alarmStage = ALARM_STAGE_PRE,
                        isSnooze = false
                    )
                } else if (now in preAlarmTime..exactTime) {
                    // Pre-alarm condition is active right now: trigger immediately
                    triggerAlarmDirectly(
                        context = context,
                        eventId = event.id,
                        eventTypeLabel = event.eventType.name,
                        scheduledTimeMillis = event.scheduledTimeEpochMillis,
                        alarmStage = ALARM_STAGE_PRE,
                        isSnooze = false
                    )
                }
            }

            // Exact IN-time alarm logic:
            // ALWAYS scheduled to go off at the exact IN time, even if user snoozed earlier!
            if (exactTime > now) {
                scheduleRawAlarm(
                    context = context,
                    eventId = event.id,
                    triggerAtMillis = exactTime,
                    scheduledTimeMillis = event.scheduledTimeEpochMillis,
                    eventTypeLabel = event.eventType.name,
                    alarmStage = ALARM_STAGE_EXACT,
                    isSnooze = false
                )
            } else if (now >= exactTime && event.status == EventStatus.PENDING && (now - exactTime) < (12 * 60 * 60 * 1000L)) {
                // Exact time arrived/overdue while still pending: trigger immediately
                triggerAlarmDirectly(
                    context = context,
                    eventId = event.id,
                    eventTypeLabel = event.eventType.name,
                    scheduledTimeMillis = event.scheduledTimeEpochMillis,
                    alarmStage = ALARM_STAGE_EXACT,
                    isSnooze = false
                )
            }
        } else {
            // OUT_IN and OUT: Exact scheduled time alarm
            val exactTime = event.scheduledTimeEpochMillis
            if (event.status == EventStatus.PENDING) {
                if (exactTime > now) {
                    scheduleRawAlarm(
                        context = context,
                        eventId = event.id,
                        triggerAtMillis = exactTime,
                        scheduledTimeMillis = event.scheduledTimeEpochMillis,
                        eventTypeLabel = event.eventType.name,
                        alarmStage = ALARM_STAGE_EXACT,
                        isSnooze = false
                    )
                } else if (now >= exactTime && (now - exactTime) < (12 * 60 * 60 * 1000L)) {
                    triggerAlarmDirectly(
                        context = context,
                        eventId = event.id,
                        eventTypeLabel = event.eventType.name,
                        scheduledTimeMillis = event.scheduledTimeEpochMillis,
                        alarmStage = ALARM_STAGE_EXACT,
                        isSnooze = false
                    )
                }
            }
        }
    }

    fun triggerAlarmDirectly(
        context: Context,
        eventId: String,
        eventTypeLabel: String,
        scheduledTimeMillis: Long,
        alarmStage: String,
        isSnooze: Boolean = false
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_EVENT_TYPE, eventTypeLabel)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
            putExtra(EXTRA_ALARM_STAGE, alarmStage)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Checks if any loaded event meets alarm conditions right now (e.g. on login or schedule load):
     * - An IN fingerprint is due or within 15 minutes of its due time
     * - An OUT fingerprint is due and not recorded as Done
     * - A snooze was hit and the snooze time is due
     */
    fun checkAndTriggerDueAlarm(context: Context, events: List<FingerprintEvent>) {
        val sessionManager = com.example.fingerprint.auth.AuthSessionManager(context)
        if (!sessionManager.isLoggedIn()) return

        val now = System.currentTimeMillis()
        val maxOverdueWindow = 12 * 60 * 60 * 1000L

        // 1. Snooze hit and snooze time has arrived
        val dueSnooze = events.firstOrNull { ev ->
            ev.status == EventStatus.SNOOZED &&
            ev.snoozedUntilEpochMillis != null &&
            now >= ev.snoozedUntilEpochMillis &&
            (now - ev.snoozedUntilEpochMillis) <= maxOverdueWindow
        }

        if (dueSnooze != null) {
            triggerAlarmDirectly(
                context = context,
                eventId = dueSnooze.id,
                eventTypeLabel = dueSnooze.eventType.name,
                scheduledTimeMillis = dueSnooze.scheduledTimeEpochMillis,
                alarmStage = ALARM_STAGE_SNOOZE,
                isSnooze = true
            )
            return
        }

        // 2. IN fingerprint within 15 minutes of its due time (pre-alarm)
        val duePreIn = events.firstOrNull { ev ->
            ev.eventType == EventType.IN &&
            ev.status == EventStatus.PENDING &&
            now in (ev.scheduledTimeEpochMillis - 15 * 60 * 1000L)..ev.scheduledTimeEpochMillis
        }

        if (duePreIn != null) {
            triggerAlarmDirectly(
                context = context,
                eventId = duePreIn.id,
                eventTypeLabel = duePreIn.eventType.name,
                scheduledTimeMillis = duePreIn.scheduledTimeEpochMillis,
                alarmStage = ALARM_STAGE_PRE,
                isSnooze = false
            )
            return
        }

        // 3. Current due fingerprint (IN or OUT) that has reached scheduled time and is not Done
        val dueExact = events
            .filter { ev ->
                ev.status == EventStatus.PENDING &&
                now >= ev.scheduledTimeEpochMillis &&
                (now - ev.scheduledTimeEpochMillis) <= maxOverdueWindow
            }
            .maxByOrNull { it.scheduledTimeEpochMillis }

        if (dueExact != null) {
            triggerAlarmDirectly(
                context = context,
                eventId = dueExact.id,
                eventTypeLabel = dueExact.eventType.name,
                scheduledTimeMillis = dueExact.scheduledTimeEpochMillis,
                alarmStage = ALARM_STAGE_EXACT,
                isSnooze = false
            )
        }
    }

    fun scheduleAlarm(
        context: Context,
        eventId: String,
        scheduledTimeMillis: Long,
        eventTypeLabel: String,
        isSnooze: Boolean = false
    ) {
        if (isSnooze) {
            scheduleRawAlarm(
                context = context,
                eventId = eventId,
                triggerAtMillis = scheduledTimeMillis,
                scheduledTimeMillis = scheduledTimeMillis,
                eventTypeLabel = eventTypeLabel,
                alarmStage = ALARM_STAGE_SNOOZE,
                isSnooze = true
            )
        } else {
            val eventType = try { EventType.valueOf(eventTypeLabel) } catch (e: Exception) { EventType.IN }
            scheduleEventAlarms(
                context = context,
                event = FingerprintEvent(
                    id = eventId,
                    scheduleId = "",
                    eventType = eventType,
                    scheduledTimeEpochMillis = scheduledTimeMillis,
                    status = EventStatus.PENDING
                )
            )
        }
    }

    fun scheduleAllAlarms(context: Context, events: List<FingerprintEvent>) {
        events.forEach { event ->
            if (event.status == EventStatus.DONE) {
                cancelAlarm(context, event.id)
            } else {
                scheduleEventAlarms(context, event)
            }
        }
        checkAndTriggerDueAlarm(context, events)
    }

    fun cancelStageAlarm(context: Context, eventId: String, stage: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(eventId, stage),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAlarm(context: Context, eventId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val stages = listOf(ALARM_STAGE_PRE, ALARM_STAGE_EXACT, ALARM_STAGE_SNOOZE, ALARM_STAGE_REPEAT)

        stages.forEach { stage ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getRequestCode(eventId, stage),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(eventId.hashCode())
        AlarmSoundManager.stop()

        val dismissIntent = Intent(ACTION_DISMISS_ALARM).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        context.sendBroadcast(dismissIntent)
    }

    fun cancelAllAlarms(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        AlarmSoundManager.stop()

        // Cancel any pending AlarmManager intents for all events in the local database
        try {
            val db = com.example.fingerprint.data.local.FingerprintDatabase.getInstance(context)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val stages = listOf(ALARM_STAGE_PRE, ALARM_STAGE_EXACT, ALARM_STAGE_SNOOZE, ALARM_STAGE_REPEAT)

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val events = db.fingerprintDao().getAllEventsDirect()
                events.forEach { event ->
                    stages.forEach { stage ->
                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                            action = ACTION_TRIGGER_ALARM
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            getRequestCode(event.id, stage),
                            intent,
                            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                        )
                        if (pendingIntent != null) {
                            alarmManager.cancel(pendingIntent)
                            pendingIntent.cancel()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
