package com.example.fingerprint.ui.schedule

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fingerprint.alarm.AlarmScheduler
import com.example.fingerprint.domain.model.EventStatus
import com.example.fingerprint.domain.model.EventTimingHelper
import com.example.fingerprint.domain.model.EventType
import com.example.fingerprint.domain.model.FingerprintEvent
import com.example.fingerprint.domain.model.ScheduleConfig
import com.example.fingerprint.settings.AppSettings
import com.example.fingerprint.settings.AppSettingsManager
import com.example.ui.theme.NextEventHighlight
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ScheduleTimelineScreen(
    config: ScheduleConfig,
    events: List<FingerprintEvent>,
    nextEvent: FingerprintEvent?,
    isArabic: Boolean,
    onLanguageToggle: () -> Unit,
    onOpenAuthDialog: () -> Unit = {},
    onOpenFeedbackDialog: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onClearSchedule: () -> Unit,
    onDoneEvent: (String) -> Unit,
    onSnoozeEvent: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH
    val zoneId = ZoneId.systemDefault()
    val context = LocalContext.current
    val canScheduleExact = AlarmScheduler.canScheduleExactAlarms(context)
    val settingsState by AppSettingsManager.getInstance(context).settingsState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = if (isArabic) "جدول البصمات الحالي" else "Active Fingerprint Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${if (isArabic) "نظام" else "System"} ${if (isArabic) config.systemType.displayNameAr else config.systemType.displayNameEn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenFeedbackDialog,
                    modifier = Modifier.testTag("open_feedback_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = "Submit Feedback",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onOpenAuthDialog,
                    modifier = Modifier.testTag("open_auth_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Account & Cloud Sync",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("timeline_open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onLanguageToggle,
                    modifier = Modifier.testTag("timeline_language_toggle")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "E" else "ع",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = onClearSchedule,
                    modifier = Modifier.testTag("clear_schedule_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Schedule",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Exact Alarm Permission Alert Banner if needed
        if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = if (isArabic) "يرجى تفعيل صلاحية المنبهات الدقيقة لضمان وصول التنبيه في الموعد" else "Please enable exact alarm permission for timely reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تفعيل" else "Enable",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Next Fingerprint Hero Highlight Card
        if (nextEvent != null) {
            NextFingerprintHeroCard(
                event = nextEvent,
                isArabic = isArabic,
                locale = locale,
                zoneId = zoneId,
                settings = settingsState,
                onDone = { onDoneEvent(nextEvent.id) },
                onSnooze = { onSnoozeEvent(nextEvent.id, nextEvent.eventType.name) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = if (isArabic) "جميع المواعيد المجدولة" else "All Scheduled Fingerprints",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Chronological List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(events) { event ->
                val isNext = event.id == nextEvent?.id
                EventRowCard(
                    event = event,
                    isNext = isNext,
                    isArabic = isArabic,
                    locale = locale,
                    zoneId = zoneId,
                    settings = settingsState,
                    onDone = { onDoneEvent(event.id) },
                    onSnooze = { onSnoozeEvent(event.id, event.eventType.name) }
                )
            }
        }
    }
}

@Composable
fun NextFingerprintHeroCard(
    event: FingerprintEvent,
    isArabic: Boolean,
    locale: Locale,
    zoneId: ZoneId,
    settings: AppSettings = AppSettings(),
    onDone: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val isDark = isSystemInDarkTheme()

    val eventZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.scheduledTimeEpochMillis), zoneId)
    val dayName = eventZdt.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val dateStr = eventZdt.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", locale))
    val timeStr = eventZdt.format(DateTimeFormatter.ofPattern("hh:mm a", locale))

    val label = if (isArabic) event.eventType.arabicLabel else event.eventType.englishLabel

    val isDone = event.status == EventStatus.DONE
    val completionTime = if (isDone) (event.completedAtEpochMillis ?: event.scheduledTimeEpochMillis) else currentTime
    val lateMillis = EventTimingHelper.getLateDurationMillis(
        eventType = event.eventType,
        scheduledTimeMillis = event.scheduledTimeEpochMillis,
        completedTimeMillis = completionTime,
        graceMinutesIn = settings.graceMinutesIn,
        graceMinutesOutIn = settings.graceMinutesOutIn,
        graceMinutesOut = settings.graceMinutesOut
    )
    val isLate = lateMillis != null

    val cardBgColor = when {
        isLate -> if (isDark) Color(0xFF381818) else Color(0xFFFFF0F0)
        else -> if (isDark) Color(0xFF003831) else Color(0xFFB2DFDB)
    }

    val cardBorderColor = when {
        isLate -> if (isDark) Color(0xFFEF5350) else Color(0xFFE53935)
        else -> if (isDark) Color(0xFF1DE9B6) else Color(0xFF00897B)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, cardBorderColor, RoundedCornerShape(20.dp))
            .testTag("next_fingerprint_hero_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when {
                        isLate -> if (isDark) Color(0xFFE53935) else Color(0xFFC62828)
                        event.status == EventStatus.DONE -> if (isDark) Color(0xFF43A047) else Color(0xFF2E7D32)
                        event.status == EventStatus.SNOOZED -> if (isDark) Color(0xFFFB8C00) else Color(0xFFE65100)
                        else -> if (isDark) Color(0xFF004D40) else Color(0xFF004D40)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when {
                            isLate && isDone -> if (isArabic) "تمت البصمة (متأخر) ✓" else "DONE (LATE) ✓"
                            isLate && !isDone -> if (isArabic) "البصمة متأخرة الآن! ⚠️" else "LATE NOW! ⚠️"
                            event.status == EventStatus.DONE -> if (isArabic) "تمت البصمة ✓" else "DONE ✓"
                            event.status == EventStatus.SNOOZED -> if (isArabic) "غفوة (5 دقائق) ⏰" else "SNOOZED (5m) ⏰"
                            else -> if (isArabic) "البصمة القادمة ⚡" else "NEXT FINGERPRINT ⚡"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "$dayName - $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLate) {
                        if (isDark) Color(0xFFFF8A80) else Color(0xFFB71C1C)
                    } else {
                        if (isDark) Color(0xFFE0F2F1) else Color(0xFF00363A)
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLate && lateMillis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = EventTimingHelper.formatLateLabel(lateMillis, isArabic),
                        color = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (isDone && event.completedAtEpochMillis != null) {
                        val completedZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.completedAtEpochMillis), zoneId)
                        val completedTimeStr = completedZdt.format(DateTimeFormatter.ofPattern("hh:mm a", locale))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "(في $completedTimeStr)" else "($completedTimeStr)",
                            color = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (isDone && event.completedAtEpochMillis != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val completedZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.completedAtEpochMillis), zoneId)
                val completedTimeStr = completedZdt.format(DateTimeFormatter.ofPattern("hh:mm a", locale))
                Text(
                    text = if (isArabic) "وقت تسجيل البصمة: $completedTimeStr" else "Fingerprint recorded at: $completedTimeStr",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLate) {
                            if (isDark) Color(0xFFFF8A80) else Color(0xFFB71C1C)
                        } else {
                            if (isDark) Color(0xFFFFFFFF) else Color(0xFF00201A)
                        }
                    )
                    Text(
                        text = when (event.eventType) {
                            EventType.IN -> if (isArabic) "تنبيه المنبه قبل 15 دقيقة" else "Alarm scheduled 15m prior"
                            EventType.OUT_IN, EventType.OUT -> if (isArabic) "تنبيه المنبه في الموعد المحدد" else "Alarm scheduled at exact time"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLate) {
                            if (isDark) Color(0xFFFFAB91) else Color(0xFFC62828)
                        } else {
                            if (isDark) Color(0xFFB2DFDB) else Color(0xFF004D40)
                        }
                    )
                }

                Surface(
                    color = if (isDark) Color(0xFF162B28) else Color.White,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isLate) {
                            if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                        } else {
                            if (isDark) Color(0xFF1DE9B6) else Color(0xFF004D40)
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Interactive Actions if event is not yet DONE
            if (event.status != EventStatus.DONE) {
                val isActionable = EventTimingHelper.isEventActionable(
                    eventType = event.eventType,
                    scheduledTimeMillis = event.scheduledTimeEpochMillis,
                    status = event.status,
                    currentTimeMillis = currentTime
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDone,
                        enabled = isActionable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF00897B) else Color(0xFF00695C),
                            contentColor = Color.White,
                            disabledContainerColor = if (isDark) Color(0xFF004D40).copy(alpha = 0.5f) else Color(0xFF80CBC4).copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_done_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isActionable) Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "بصمت" else "Done",
                            color = if (isActionable) Color.White else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onSnooze,
                        enabled = isActionable,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color(0xFF1DE9B6) else Color(0xFF004D40),
                            disabledContentColor = if (isDark) Color(0xFF1DE9B6).copy(alpha = 0.4f) else Color(0xFF004D40).copy(alpha = 0.4f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isActionable) {
                                if (isDark) Color(0xFF1DE9B6).copy(alpha = 0.6f) else Color(0xFF004D40).copy(alpha = 0.5f)
                            } else {
                                if (isDark) Color(0xFF1DE9B6).copy(alpha = 0.2f) else Color(0xFF004D40).copy(alpha = 0.2f)
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hero_snooze_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = null,
                            tint = if (isActionable) {
                                if (isDark) Color(0xFF1DE9B6) else Color(0xFF004D40)
                            } else {
                                if (isDark) Color(0xFF1DE9B6).copy(alpha = 0.4f) else Color(0xFF004D40).copy(alpha = 0.4f)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "غفوة ${settings.snoozeDurationMinutes} دقائق" else "Snooze ${settings.snoozeDurationMinutes}m",
                            color = if (isActionable) {
                                if (isDark) Color(0xFF1DE9B6) else Color(0xFF004D40)
                            } else {
                                if (isDark) Color(0xFF1DE9B6).copy(alpha = 0.4f) else Color(0xFF004D40).copy(alpha = 0.4f)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (!isActionable) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val activationHint = when (event.eventType) {
                        EventType.IN -> if (isArabic) "تتفعّل الأزرار قبل موعد البصمة بـ 15 دقيقة" else "Buttons activate 15 mins before scheduled time"
                        EventType.OUT_IN, EventType.OUT -> if (isArabic) "تتفعّل الأزرار عند حلول موعد البصمة" else "Buttons activate at scheduled time"
                    }
                    Text(
                        text = "⏳ $activationHint",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF80CBC4) else Color(0xFF004D40).copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EventRowCard(
    event: FingerprintEvent,
    isNext: Boolean,
    isArabic: Boolean,
    locale: Locale,
    zoneId: ZoneId,
    settings: AppSettings = AppSettings(),
    onDone: () -> Unit,
    onSnooze: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val eventZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.scheduledTimeEpochMillis), zoneId)
    val dayName = eventZdt.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    val dateStr = eventZdt.format(DateTimeFormatter.ofPattern("dd MMM", locale))
    val timeStr = eventZdt.format(DateTimeFormatter.ofPattern("hh:mm a", locale))

    val label = if (isArabic) event.eventType.arabicLabel else event.eventType.englishLabel

    val icon = when (event.eventType) {
        EventType.IN -> Icons.AutoMirrored.Filled.Login
        EventType.OUT_IN -> Icons.Default.SyncAlt
        EventType.OUT -> Icons.AutoMirrored.Filled.Logout
    }

    val isDone = event.status == EventStatus.DONE
    val completionTime = if (isDone) (event.completedAtEpochMillis ?: event.scheduledTimeEpochMillis) else System.currentTimeMillis()
    val lateMillis = EventTimingHelper.getLateDurationMillis(
        eventType = event.eventType,
        scheduledTimeMillis = event.scheduledTimeEpochMillis,
        completedTimeMillis = completionTime,
        graceMinutesIn = settings.graceMinutesIn,
        graceMinutesOutIn = settings.graceMinutesOutIn,
        graceMinutesOut = settings.graceMinutesOut
    )
    val isLate = lateMillis != null

    val containerColor = when {
        isLate -> if (isDark) Color(0xFF2D1414) else Color(0xFFFFF0F0)
        isNext -> if (isDark) Color(0xFF16322E) else Color(0xFFE0F2F1)
        else -> MaterialTheme.colorScheme.surface
    }

    val cardBorder = when {
        isLate -> androidx.compose.foundation.BorderStroke(1.5.dp, if (isDark) Color(0xFFEF5350) else Color(0xFFEF9A9A))
        isNext -> androidx.compose.foundation.BorderStroke(1.5.dp, if (isDark) Color(0xFF1DE9B6) else Color(0xFF00897B))
        else -> null
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 4.dp else 1.dp),
        border = cardBorder,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_item_${event.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isLate -> if (isDark) Color(0xFF4A1A1A) else Color(0xFFFFCDD2)
                                    event.status == EventStatus.DONE -> if (isDark) Color(0xFF1B3820) else Color(0xFFE8F5E9)
                                    isNext -> if (isDark) Color(0xFF00897B) else Color(0xFF00695C)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isLate && isDone -> Icons.Default.CheckCircle
                                isLate && !isDone -> Icons.Default.Warning
                                event.status == EventStatus.DONE -> Icons.Default.CheckCircle
                                else -> icon
                            },
                            contentDescription = null,
                            tint = when {
                                isLate -> if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                                event.status == EventStatus.DONE -> if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
                                isNext -> Color.White
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                fontWeight = if (isNext || isLate) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isLate) {
                                    if (isDark) Color(0xFFFF8A80) else Color(0xFFB71C1C)
                                } else if (isNext) {
                                    if (isDark) Color(0xFFFFFFFF) else Color(0xFF00201A)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (isLate && lateMillis != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (isDark) Color(0xFF4A1A1A) else Color(0xFFFFCDD2),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = EventTimingHelper.formatLateLabel(lateMillis, isArabic),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                                        )
                                    }
                                }
                            } else if (event.status == EventStatus.DONE) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (isDark) Color(0xFF1B3820) else Color(0xFFC8E6C9),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "تم" else "Done",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (event.status == EventStatus.SNOOZED) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (isDark) Color(0xFF3E2723) else Color(0xFFFFE0B2),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "غفوة" else "Snoozed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "$dayName, $dateStr",
                            fontSize = 12.sp,
                            color = if (isLate) {
                                if (isDark) Color(0xFFFFAB91) else Color(0xFFC62828)
                            } else if (isNext) {
                                if (isDark) Color(0xFFB2DFDB) else Color(0xFF004D40)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (isDone && event.completedAtEpochMillis != null) {
                            val completedZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.completedAtEpochMillis), zoneId)
                            val completedTimeStr = completedZdt.format(DateTimeFormatter.ofPattern("hh:mm a", locale))
                            Text(
                                text = if (isArabic) "تمت البصمة الساعة: $completedTimeStr" else "Done at: $completedTimeStr",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLate) {
                                    if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                                } else {
                                    if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
                                }
                            )
                        }
                    }
                }

                // Time badge
                if (isLate) {
                    Surface(
                        color = if (isDark) Color(0xFFD32F2F) else Color(0xFFC62828),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = timeStr,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else if (isNext) {
                    Surface(
                        color = if (isDark) Color(0xFF00897B) else Color(0xFF00695C),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = timeStr,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Text(
                        text = timeStr,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

