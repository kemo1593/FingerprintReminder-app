package com.example.fingerprint.ui.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fingerprint.domain.model.SystemType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleCreationScreen(
    isArabic: Boolean,
    onLanguageToggle: () -> Unit,
    onOpenAuthDialog: () -> Unit = {},
    onOpenFeedbackDialog: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onGenerateSchedule: (
        systemType: SystemType,
        firstEntry: LocalDateTime,
        lastEntry: LocalDateTime,
        workDays: Set<DayOfWeek>,
        customOffset: Long?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locale = if (isArabic) Locale.forLanguageTag("ar") else Locale.ENGLISH

    var firstDate by remember { mutableStateOf(LocalDate.now()) }
    var firstTime by remember { mutableStateOf(LocalTime.of(8, 0)) }

    var lastDate by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    var lastTime by remember { mutableStateOf(LocalTime.of(14, 0)) }

    var selectedSystem by remember { mutableStateOf(SystemType.SYSTEM_8_2) }

    var workDays by remember {
        mutableStateOf(
            setOf(
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY
            )
        )
    }

    val isFirstAm = firstTime.hour < 12
    val isLastAm = lastTime.hour < 12

    val datePickerFirst = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            firstDate = LocalDate.of(year, month + 1, dayOfMonth)
        },
        firstDate.year,
        firstDate.monthValue - 1,
        firstDate.dayOfMonth
    )

    val timePickerFirst = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            firstTime = LocalTime.of(hourOfDay, minute)
        },
        firstTime.hour,
        firstTime.minute,
        false
    )

    val datePickerLast = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            lastDate = LocalDate.of(year, month + 1, dayOfMonth)
        },
        lastDate.year,
        lastDate.monthValue - 1,
        lastDate.dayOfMonth
    )

    val timePickerLast = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            lastTime = LocalTime.of(hourOfDay, minute)
        },
        lastTime.hour,
        lastTime.minute,
        false
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar Header
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
                Text(
                    text = if (isArabic) "إنشاء جدول البصمة" else "Create Fingerprint Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenFeedbackDialog,
                    modifier = Modifier.testTag("creation_open_feedback_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = "Submit Feedback",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onOpenAuthDialog,
                    modifier = Modifier.testTag("creation_open_auth_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Account & Cloud Sync",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("creation_open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onLanguageToggle,
                    modifier = Modifier.testTag("language_toggle_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Switch Language",
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
            }
        }

        // Section 1: Fingerprint System Selection
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isArabic) "1. نظام البصمة" else "1. Fingerprint System",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SystemType.values().forEach { sys ->
                        val label = when (sys) {
                            SystemType.SYSTEM_18_6 -> SystemType.get18_6Label(isFirstAm, isArabic)
                            SystemType.SYSTEM_24_6 -> SystemType.get24_6Label(isFirstAm, isArabic)
                            SystemType.SYSTEM_12_12 -> SystemType.get12_12Label(isFirstAm, isArabic)
                            else -> if (isArabic) sys.displayNameAr else sys.displayNameEn
                        }

                        val isSelected = selectedSystem == sys

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSystem = sys
                                when (sys) {
                                    SystemType.SYSTEM_8_2 -> {
                                        firstTime = LocalTime.of(8, 0)
                                        lastTime = LocalTime.of(14, 0)
                                    }
                                    SystemType.SYSTEM_8_3 -> {
                                        firstTime = LocalTime.of(8, 0)
                                        lastTime = LocalTime.of(15, 0)
                                    }
                                    SystemType.SYSTEM_8_5 -> {
                                        firstTime = LocalTime.of(8, 0)
                                        lastTime = LocalTime.of(17, 0)
                                    }
                                    SystemType.SYSTEM_9_3 -> {
                                        firstTime = LocalTime.of(9, 0)
                                        lastTime = LocalTime.of(15, 0)
                                    }
                                    SystemType.SYSTEM_9_5 -> {
                                        firstTime = LocalTime.of(9, 0)
                                        lastTime = LocalTime.of(17, 0)
                                    }
                                    SystemType.SYSTEM_10_5 -> {
                                        firstTime = LocalTime.of(10, 0)
                                        lastTime = LocalTime.of(17, 0)
                                    }
                                    SystemType.SYSTEM_18_6 -> {
                                        firstTime = LocalTime.of(8, 0)
                                        lastTime = LocalTime.of(2, 0)
                                        if (lastDate.isBefore(firstDate.plusDays(1))) {
                                            lastDate = firstDate.plusDays(1)
                                        }
                                    }
                                    SystemType.SYSTEM_24_6 -> {
                                        firstTime = LocalTime.of(8, 0)
                                        lastTime = LocalTime.of(8, 0)
                                        if (lastDate.isBefore(firstDate.plusDays(1))) {
                                            lastDate = firstDate.plusDays(1)
                                        }
                                    }
                                    SystemType.SYSTEM_12_12 -> {
                                        firstTime = LocalTime.of(8, 0)
                                        lastTime = LocalTime.of(20, 0)
                                        if (lastDate.isBefore(firstDate.plusDays(1))) {
                                            lastDate = firstDate.plusDays(1)
                                        }
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("system_chip_${sys.name}")
                        )
                    }
                }
            }
        }

        // Section 2: First Entry Time
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isArabic) "2. أول بصمة (تاريخ ووقت البدء)" else "2. First Entry Time (Start Date & Time)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date Card
                    OutlinedCard(
                        onClick = { datePickerFirst.show() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("first_entry_date_picker")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = firstDate.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = firstDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", locale)),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Time Card
                    OutlinedCard(
                        onClick = { timePickerFirst.show() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("first_entry_time_picker")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = if (isArabic) "الوقت" else "Time",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = firstTime.format(DateTimeFormatter.ofPattern("hh:mm", locale)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Highlighted AM / PM Toggle Buttons for First Entry Time
                AmPmSegmentedButtons(
                    isAm = isFirstAm,
                    isArabic = isArabic,
                    onAmSelected = {
                        if (!isFirstAm) {
                            firstTime = firstTime.minusHours(12)
                        }
                    },
                    onPmSelected = {
                        if (isFirstAm) {
                            firstTime = firstTime.plusHours(12)
                        }
                    },
                    testTagPrefix = "first_entry",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 3: Work Days Selection (Only for Daily systems)
        AnimatedVisibility(visible = selectedSystem.isDailyShift) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "أيام العمل الأسبوعية" else "Work Days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val orderedDays = listOf(
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY,
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        orderedDays.forEach { day ->
                            val dayName = day.getDisplayName(TextStyle.SHORT, locale)
                            val isSelected = workDays.contains(day)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    workDays = if (isSelected) workDays - day else workDays + day
                                },
                                label = {
                                    Text(
                                        text = dayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("work_day_chip_${day.name}")
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Last Entry Time
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isArabic) "3. آخر بصمة (نهاية الجدول)" else "3. Last Entry Time (Schedule End)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedCard(
                        onClick = { datePickerLast.show() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("last_entry_date_picker")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = lastDate.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", locale)),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    OutlinedCard(
                        onClick = { timePickerLast.show() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("last_entry_time_picker")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = if (isArabic) "الوقت" else "Time",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastTime.format(DateTimeFormatter.ofPattern("hh:mm", locale)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Highlighted AM / PM Toggle Buttons for Last Entry Time
                AmPmSegmentedButtons(
                    isAm = isLastAm,
                    isArabic = isArabic,
                    onAmSelected = {
                        if (!isLastAm) {
                            lastTime = lastTime.minusHours(12)
                        }
                    },
                    onPmSelected = {
                        if (isLastAm) {
                            lastTime = lastTime.plusHours(12)
                        }
                    },
                    testTagPrefix = "last_entry",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Generate Schedule Action Button
        Button(
            onClick = {
                val firstLdt = LocalDateTime.of(firstDate, firstTime)
                val lastLdt = LocalDateTime.of(lastDate, lastTime)
                onGenerateSchedule(selectedSystem, firstLdt, lastLdt, workDays, null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate_schedule_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = if (isArabic) "توليد الجدول الآن" else "Generate Schedule Now",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AmPmSegmentedButtons(
    isAm: Boolean,
    isArabic: Boolean,
    onAmSelected: () -> Unit,
    onPmSelected: () -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val activeColor = MaterialTheme.colorScheme.primary
            val activeContentColor = MaterialTheme.colorScheme.onPrimary
            val inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant

            // AM Button
            Surface(
                onClick = onAmSelected,
                shape = RoundedCornerShape(8.dp),
                color = if (isAm) activeColor else Color.Transparent,
                shadowElevation = if (isAm) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_am_button")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    Text(
                        text = if (isArabic) "صباحاً" else "AM",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (isAm) activeContentColor else inactiveContentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // PM Button
            Surface(
                onClick = onPmSelected,
                shape = RoundedCornerShape(8.dp),
                color = if (!isAm) activeColor else Color.Transparent,
                shadowElevation = if (!isAm) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_pm_button")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    Text(
                        text = if (isArabic) "مساءً" else "PM",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = if (!isAm) activeContentColor else inactiveContentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
