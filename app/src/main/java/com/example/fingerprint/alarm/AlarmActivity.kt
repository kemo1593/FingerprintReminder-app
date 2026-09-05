package com.example.fingerprint.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fingerprint.data.local.FingerprintDatabase
import com.example.fingerprint.domain.model.EventStatus
import com.example.ui.theme.FingerprintReminderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    private var activeEventId: String = ""

    private val dismissReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            val eventId = intent?.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID)
            if (eventId == null || eventId == activeEventId) {
                AlarmSoundManager.stop()
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupLockscreenFlags()

        val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: ""
        activeEventId = eventId
        val eventTypeStr = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_TYPE) ?: "IN"
        val scheduledTimeMillis = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
        val alarmStage = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_STAGE) ?: AlarmScheduler.ALARM_STAGE_EXACT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, android.content.IntentFilter(AlarmScheduler.ACTION_DISMISS_ALARM), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dismissReceiver, android.content.IntentFilter(AlarmScheduler.ACTION_DISMISS_ALARM))
        }

        val prefs = getSharedPreferences("fingerprint_app_prefs", Context.MODE_PRIVATE)
        val isArabic = prefs.getBoolean("is_arabic", true)
        val snoozeMins = com.example.fingerprint.settings.AppSettingsManager.getInstance(this).settingsState.value.snoozeDurationMinutes

        setContent {
            FingerprintReminderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlarmScreen(
                        eventTypeStr = eventTypeStr,
                        scheduledTimeMillis = scheduledTimeMillis,
                        alarmStage = alarmStage,
                        isArabic = isArabic,
                        snoozeDurationMinutes = snoozeMins,
                        onDone = {
                            AlarmSoundManager.stop()
                            handleDone(eventId)
                        },
                        onSnooze = {
                            AlarmSoundManager.stop()
                            handleSnooze(eventId, eventTypeStr, scheduledTimeMillis)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setupLockscreenFlags()

        val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: ""
        activeEventId = eventId
        val eventTypeStr = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_TYPE) ?: "IN"
        val scheduledTimeMillis = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
        val alarmStage = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_STAGE) ?: AlarmScheduler.ALARM_STAGE_EXACT

        val prefs = getSharedPreferences("fingerprint_app_prefs", Context.MODE_PRIVATE)
        val isArabic = prefs.getBoolean("is_arabic", true)
        val snoozeMins = com.example.fingerprint.settings.AppSettingsManager.getInstance(this).settingsState.value.snoozeDurationMinutes

        setContent {
            FingerprintReminderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlarmScreen(
                        eventTypeStr = eventTypeStr,
                        scheduledTimeMillis = scheduledTimeMillis,
                        alarmStage = alarmStage,
                        isArabic = isArabic,
                        snoozeDurationMinutes = snoozeMins,
                        onDone = {
                            AlarmSoundManager.stop()
                            handleDone(eventId)
                        },
                        onSnooze = {
                            AlarmSoundManager.stop()
                            handleSnooze(eventId, eventTypeStr, scheduledTimeMillis)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun setupLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun handleDone(eventId: String) {
        AlarmSoundManager.stop()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(eventId.hashCode())
        AlarmScheduler.cancelAlarm(applicationContext, eventId)

        val completedAt = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            if (eventId.isNotEmpty()) {
                val db = FingerprintDatabase.getInstance(applicationContext)
                db.fingerprintDao().updateEventStatus(eventId, EventStatus.DONE.name, null, completedAt)
                try {
                    val syncService = com.example.fingerprint.data.remote.SupabaseSyncService(applicationContext, db.fingerprintDao())
                    syncService.syncEventStatus(eventId, EventStatus.DONE.name, completedAtEpochMillis = completedAt)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            withContext(Dispatchers.Main) {
                finishAndRemoveTask()
            }
        }
    }

    private fun handleSnooze(eventId: String, eventTypeStr: String, scheduledTimeMillis: Long) {
        AlarmSoundManager.stop()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(eventId.hashCode())

        // Cancel previous repeat, pre, and snooze stages to avoid stale alarms
        AlarmScheduler.cancelStageAlarm(applicationContext, eventId, AlarmScheduler.ALARM_STAGE_REPEAT)
        AlarmScheduler.cancelStageAlarm(applicationContext, eventId, AlarmScheduler.ALARM_STAGE_PRE)
        AlarmScheduler.cancelStageAlarm(applicationContext, eventId, AlarmScheduler.ALARM_STAGE_SNOOZE)

        val snoozeMins = com.example.fingerprint.settings.AppSettingsManager.getInstance(applicationContext).settingsState.value.snoozeDurationMinutes
        val snoozeTimeMillis = System.currentTimeMillis() + (snoozeMins * 60 * 1000L)

        CoroutineScope(Dispatchers.IO).launch {
            if (eventId.isNotEmpty()) {
                val db = FingerprintDatabase.getInstance(applicationContext)
                db.fingerprintDao().updateEventStatus(eventId, EventStatus.SNOOZED.name, snoozeTimeMillis, null)

                try {
                    val syncService = com.example.fingerprint.data.remote.SupabaseSyncService(applicationContext, db.fingerprintDao())
                    syncService.syncEventStatus(eventId, EventStatus.SNOOZED.name, snoozedUntilEpochMillis = snoozeTimeMillis)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Schedule snooze alarm
                AlarmScheduler.scheduleRawAlarm(
                    context = applicationContext,
                    eventId = eventId,
                    triggerAtMillis = snoozeTimeMillis,
                    scheduledTimeMillis = scheduledTimeMillis,
                    eventTypeLabel = eventTypeStr,
                    alarmStage = AlarmScheduler.ALARM_STAGE_SNOOZE,
                    isSnooze = true
                )

                // For IN event: If scheduled time is still in the future, ensure the exact alarm is active
                if (eventTypeStr == "IN" && scheduledTimeMillis > System.currentTimeMillis()) {
                    AlarmScheduler.scheduleRawAlarm(
                        context = applicationContext,
                        eventId = eventId,
                        triggerAtMillis = scheduledTimeMillis,
                        scheduledTimeMillis = scheduledTimeMillis,
                        eventTypeLabel = eventTypeStr,
                        alarmStage = AlarmScheduler.ALARM_STAGE_EXACT,
                        isSnooze = false
                    )
                }
            }
            withContext(Dispatchers.Main) {
                finishAndRemoveTask()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AlarmSoundManager.start(this)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            AlarmSoundManager.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(dismissReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        AlarmSoundManager.stop()
    }
}

@Composable
fun AlarmScreen(
    eventTypeStr: String,
    scheduledTimeMillis: Long,
    alarmStage: String = AlarmScheduler.ALARM_STAGE_EXACT,
    isArabic: Boolean = true,
    snoozeDurationMinutes: Int = 5,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
    val timeFormatted = SimpleDateFormat("hh:mm a", locale).format(Date(scheduledTimeMillis))
    val dateFormatted = SimpleDateFormat("EEEE, d MMMM yyyy", locale).format(Date(scheduledTimeMillis))

    val stageLabel = if (isArabic) {
        when (alarmStage) {
            AlarmScheduler.ALARM_STAGE_PRE -> "تذكير قبل 15 دقيقة من موعد البصمة"
            AlarmScheduler.ALARM_STAGE_EXACT -> "حان الآن موعد البصمة"
            AlarmScheduler.ALARM_STAGE_SNOOZE -> "تنبيه الغفوة ($snoozeDurationMinutes دقائق)"
            AlarmScheduler.ALARM_STAGE_REPEAT -> "تنبيه متكرر للبصمة"
            else -> "تذكير البصمة القادمة"
        }
    } else {
        when (alarmStage) {
            AlarmScheduler.ALARM_STAGE_PRE -> "15m Prior Reminder"
            AlarmScheduler.ALARM_STAGE_EXACT -> "Fingerprint Time Now"
            AlarmScheduler.ALARM_STAGE_SNOOZE -> "Snooze Reminder (${snoozeDurationMinutes}m)"
            AlarmScheduler.ALARM_STAGE_REPEAT -> "Repeated Fingerprint Alert"
            else -> "Upcoming Fingerprint Reminder"
        }
    }

    val eventTypeTitle = if (isArabic) {
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
            else -> "Fingerprint Scan"
        }
    }

    val instructionsText = if (isArabic) {
        "يرجى تسجيل البصمة الآن في الجهاز المعتمد"
    } else {
        "Please record your fingerprint now on the designated device"
    }

    val doneButtonText = if (isArabic) "بصمت" else "Done"
    val snoozeButtonText = if (isArabic) "غفوة $snoozeDurationMinutes دقائق" else "Snooze ${snoozeDurationMinutes}m"

    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_fingerprint")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF00201A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Top Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = Color(0xFF004D40),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stageLabel,
                    color = Color(0xFF80CBC4),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = dateFormatted,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB2DFDB)
            )
        }

        // Center Fingerprint Animated Visual
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF00363A)),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .background(Color(0xFF00695C), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint Alarm",
                        tint = Color.White,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = eventTypeTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = instructionsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF80CBC4),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00897B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("alarm_done_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = doneButtonText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                onClick = onSnooze,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFFCC80)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("alarm_snooze_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = snoozeButtonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
