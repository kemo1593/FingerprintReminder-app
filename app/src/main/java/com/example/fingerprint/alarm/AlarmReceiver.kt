package com.example.fingerprint.alarm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.fingerprint.auth.AuthSessionManager
import com.example.fingerprint.data.local.FingerprintDatabase
import com.example.fingerprint.domain.model.EventStatus
import com.example.fingerprint.domain.model.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: return
        val eventTypeStr = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_TYPE) ?: "IN"
        val scheduledTimeMillis = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
        val alarmStage = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_STAGE) ?: AlarmScheduler.ALARM_STAGE_EXACT

        when (action) {
            AlarmScheduler.ACTION_TRIGGER_ALARM -> {
                val sessionManager = AuthSessionManager(context)
                if (!sessionManager.isLoggedIn()) {
                    // User is logged out: Do not set off alarms
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(eventId.hashCode())
                    AlarmScheduler.cancelAlarm(context, eventId)
                    return
                }

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = FingerprintDatabase.getInstance(context)
                        val event = db.fingerprintDao().getEventById(eventId)
                        if (event == null || event.status == EventStatus.DONE.name) {
                            // Event is already marked DONE or deleted - cancel alarm and do not ring
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(eventId.hashCode())
                            AlarmScheduler.cancelAlarm(context, eventId)
                            return@launch
                        }

                        // If this was an auto-repeat alarm and the user already snoozed or completed it, do not ring
                        if (alarmStage == AlarmScheduler.ALARM_STAGE_REPEAT && event.status != EventStatus.PENDING.name) {
                            return@launch
                        }

                        showAlarmNotification(context, eventId, eventTypeStr, scheduledTimeMillis, alarmStage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            AlarmScheduler.ACTION_DONE -> {
                handleDoneAction(context, eventId)
            }
            AlarmScheduler.ACTION_SNOOZE -> {
                handleSnoozeAction(context, eventId, eventTypeStr, scheduledTimeMillis)
            }
        }
    }

    private fun showAlarmNotification(
        context: Context,
        eventId: String,
        eventTypeStr: String,
        scheduledTimeMillis: Long,
        alarmStage: String
    ) {
        AlarmScheduler.createNotificationChannel(context)

        val prefs = context.getSharedPreferences("fingerprint_app_prefs", Context.MODE_PRIVATE)
        val isArabic = prefs.getBoolean("is_arabic", true)
        val timeLocale = if (isArabic) Locale("ar") else Locale.ENGLISH
        val timeFormatted = SimpleDateFormat("hh:mm a", timeLocale).format(Date(scheduledTimeMillis))

        val eventTypeLabel = if (isArabic) {
            when (eventTypeStr) {
                "IN" -> "بصمة دخول"
                "OUT_IN" -> "بصمة خروج / دخول"
                "OUT" -> "بصمة خروج"
                else -> "تنبيه موعد البصمة"
            }
        } else {
            when (eventTypeStr) {
                "IN" -> "Fingerprint IN"
                "OUT_IN" -> "Fingerprint OUT / IN"
                "OUT" -> "Fingerprint OUT"
                else -> "Fingerprint Reminder"
            }
        }

        val title = if (isArabic) {
            when (alarmStage) {
                AlarmScheduler.ALARM_STAGE_PRE -> "تذكير قبل 15 دقيقة: موعد البصمة $timeFormatted"
                AlarmScheduler.ALARM_STAGE_EXACT -> "حان موعد البصمة الآن: $timeFormatted"
                AlarmScheduler.ALARM_STAGE_SNOOZE -> "تنبيه الغفوة: موعد البصمة $timeFormatted"
                AlarmScheduler.ALARM_STAGE_REPEAT -> "تذكير متكرر: موعد البصمة $timeFormatted"
                else -> "تذكير البصمة القادمة: $timeFormatted"
            }
        } else {
            when (alarmStage) {
                AlarmScheduler.ALARM_STAGE_PRE -> "15m Prior Reminder: Fingerprint at $timeFormatted"
                AlarmScheduler.ALARM_STAGE_EXACT -> "Fingerprint Time Now: $timeFormatted"
                AlarmScheduler.ALARM_STAGE_SNOOZE -> "Snooze Reminder: Fingerprint at $timeFormatted"
                AlarmScheduler.ALARM_STAGE_REPEAT -> "Repeated Reminder: Fingerprint at $timeFormatted"
                else -> "Next Fingerprint Reminder: $timeFormatted"
            }
        }

        val doneButtonLabel = if (isArabic) "بصمت" else "Done"
        val snoozeMins = com.example.fingerprint.settings.AppSettingsManager.getInstance(context).settingsState.value.snoozeDurationMinutes
        val snoozeButtonLabel = if (isArabic) "غفوة $snoozeMins دقائق" else "Snooze ${snoozeMins}m"

        // Fullscreen Intent to AlarmActivity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
            putExtra(AlarmScheduler.EXTRA_EVENT_TYPE, eventTypeStr)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
            putExtra(AlarmScheduler.EXTRA_ALARM_STAGE, alarmStage)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Done PendingIntent
        val doneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_DONE
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode() + 10,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze PendingIntent
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
            putExtra(AlarmScheduler.EXTRA_EVENT_TYPE, eventTypeStr)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTimeMillis)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode() + 20,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with high priority heads-up banner and actionable buttons
        val notification = NotificationCompat.Builder(context, AlarmScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(eventTypeLabel)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(null)
            .setVibrate(longArrayOf(0, 500, 500, 500, 500, 500))
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, doneButtonLabel, donePendingIntent)
            .addAction(0, snoozeButtonLabel, snoozePendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(eventId.hashCode(), notification)

        // Ensure single audio source starts playback
        AlarmSoundManager.start(context)

        // Schedule auto-repeat alarm after snoozeMins if user doesn't tap Done/Snooze
        AlarmScheduler.scheduleRawAlarm(
            context = context,
            eventId = eventId,
            triggerAtMillis = System.currentTimeMillis() + (snoozeMins * 60 * 1000L),
            scheduledTimeMillis = scheduledTimeMillis,
            eventTypeLabel = eventTypeStr,
            alarmStage = AlarmScheduler.ALARM_STAGE_REPEAT,
            isSnooze = true
        )
    }

    private fun handleDoneAction(context: Context, eventId: String) {
        AlarmSoundManager.stop()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(eventId.hashCode())
        AlarmScheduler.cancelAlarm(context, eventId)

        // Broadcast to close full screen AlarmActivity if visible
        val dismissIntent = Intent(AlarmScheduler.ACTION_DISMISS_ALARM).apply {
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
        }
        context.sendBroadcast(dismissIntent)

        val pendingResult = goAsync()
        val completedAt = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FingerprintDatabase.getInstance(context)
                db.fingerprintDao().updateEventStatus(eventId, EventStatus.DONE.name, null, completedAt)
                try {
                    val syncService = com.example.fingerprint.data.remote.SupabaseSyncService(context, db.fingerprintDao())
                    syncService.syncEventStatus(eventId, EventStatus.DONE.name, completedAtEpochMillis = completedAt)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnoozeAction(
        context: Context,
        eventId: String,
        eventTypeStr: String,
        scheduledTimeMillis: Long
    ) {
        AlarmSoundManager.stop()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(eventId.hashCode())

        // Cancel previous repeat and pre stages to avoid stale triggers
        AlarmScheduler.cancelStageAlarm(context, eventId, AlarmScheduler.ALARM_STAGE_REPEAT)
        AlarmScheduler.cancelStageAlarm(context, eventId, AlarmScheduler.ALARM_STAGE_PRE)
        AlarmScheduler.cancelStageAlarm(context, eventId, AlarmScheduler.ALARM_STAGE_SNOOZE)

        // Broadcast to close full screen AlarmActivity if visible
        val dismissIntent = Intent(AlarmScheduler.ACTION_DISMISS_ALARM).apply {
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
        }
        context.sendBroadcast(dismissIntent)

        val snoozeMins = com.example.fingerprint.settings.AppSettingsManager.getInstance(context).settingsState.value.snoozeDurationMinutes
        val snoozeTimeMillis = System.currentTimeMillis() + (snoozeMins * 60 * 1000L)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FingerprintDatabase.getInstance(context)
                db.fingerprintDao().updateEventStatus(eventId, EventStatus.SNOOZED.name, snoozeTimeMillis, null)

                try {
                    val syncService = com.example.fingerprint.data.remote.SupabaseSyncService(context, db.fingerprintDao())
                    syncService.syncEventStatus(eventId, EventStatus.SNOOZED.name, snoozedUntilEpochMillis = snoozeTimeMillis)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Schedule snooze alarm
                AlarmScheduler.scheduleRawAlarm(
                    context = context,
                    eventId = eventId,
                    triggerAtMillis = snoozeTimeMillis,
                    scheduledTimeMillis = scheduledTimeMillis,
                    eventTypeLabel = eventTypeStr,
                    alarmStage = AlarmScheduler.ALARM_STAGE_SNOOZE,
                    isSnooze = true
                )

                // For IN event: Ensure the exact IN alarm at scheduledTimeMillis remains active even after snooze
                if (eventTypeStr == "IN" && scheduledTimeMillis > System.currentTimeMillis()) {
                    AlarmScheduler.scheduleRawAlarm(
                        context = context,
                        eventId = eventId,
                        triggerAtMillis = scheduledTimeMillis,
                        scheduledTimeMillis = scheduledTimeMillis,
                        eventTypeLabel = eventTypeStr,
                        alarmStage = AlarmScheduler.ALARM_STAGE_EXACT,
                        isSnooze = false
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
