package com.example.fingerprint.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fingerprint.auth.AuthState
import com.example.fingerprint.settings.AppSettings
import com.example.fingerprint.settings.DeviceRingtone

enum class SettingsTab {
    ALARMS,
    ACCOUNT,
    SUBSCRIPTION
}

@Composable
fun SettingsDialog(
    isOpen: Boolean,
    isRingtonePickerOpen: Boolean,
    settings: AppSettings,
    availableRingtones: List<DeviceRingtone>,
    currentlyPlayingUri: String?,
    isArabic: Boolean,
    authState: AuthState = AuthState.Unauthenticated,
    onDismiss: () -> Unit,
    onOpenRingtonePicker: () -> Unit,
    onCloseRingtonePicker: () -> Unit,
    onSetMixCount: (Int) -> Unit,
    onToggleRingtone: (String) -> Unit,
    onSetSnoozeDuration: (Int) -> Unit,
    onSetGraceIn: (Int) -> Unit,
    onSetGraceOutIn: (Int) -> Unit,
    onSetGraceOut: (Int) -> Unit,
    onToggleAudioPreview: (String) -> Unit,
    onOpenAuthDialog: () -> Unit = {},
    onChangePassword: (String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onDeleteAccount: ((Boolean, String) -> Unit) -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    if (!isOpen) return

    val isDark = isSystemInDarkTheme()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("settings_modal_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF131B1A) else Color(0xFFF9FBFB),
            tonalElevation = 8.dp
        ) {
            if (isRingtonePickerOpen) {
                RingtonePickerView(
                    availableRingtones = availableRingtones,
                    selectedUris = settings.selectedRingtoneUris,
                    maxMixCount = settings.alarmMixCount,
                    currentlyPlayingUri = currentlyPlayingUri,
                    isArabic = isArabic,
                    isDark = isDark,
                    onBack = onCloseRingtonePicker,
                    onToggleRingtone = onToggleRingtone,
                    onToggleAudioPreview = onToggleAudioPreview
                )
            } else {
                MainSettingsView(
                    settings = settings,
                    isArabic = isArabic,
                    isDark = isDark,
                    authState = authState,
                    onClose = onDismiss,
                    onOpenRingtonePicker = onOpenRingtonePicker,
                    onSetMixCount = onSetMixCount,
                    onSetSnoozeDuration = onSetSnoozeDuration,
                    onSetGraceIn = onSetGraceIn,
                    onSetGraceOutIn = onSetGraceOutIn,
                    onSetGraceOut = onSetGraceOut,
                    onOpenAuthDialog = onOpenAuthDialog,
                    onChangePassword = onChangePassword,
                    onDeleteAccount = onDeleteAccount,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun MainSettingsView(
    settings: AppSettings,
    isArabic: Boolean,
    isDark: Boolean,
    authState: AuthState,
    onClose: () -> Unit,
    onOpenRingtonePicker: () -> Unit,
    onSetMixCount: (Int) -> Unit,
    onSetSnoozeDuration: (Int) -> Unit,
    onSetGraceIn: (Int) -> Unit,
    onSetGraceOutIn: (Int) -> Unit,
    onSetGraceOut: (Int) -> Unit,
    onOpenAuthDialog: () -> Unit,
    onChangePassword: (String, String, (Boolean, String) -> Unit) -> Unit,
    onDeleteAccount: ((Boolean, String) -> Unit) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isArabic) "الإعدادات العامة" else "App Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF1A2A28)
                    )
                    Text(
                        text = if (isArabic) "التنبيهات، الحساب والأمان، وباقات الاشتراك" else "Alarms, Account & Security, Subscription",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFB0BEC5) else Color(0xFF546E7A)
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF263238) else Color(0xFFECEFF1))
                    .testTag("close_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (isDark) Color(0xFFCFD8DC) else Color(0xFF455A64),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Tabs (Alarms, Account & Security, Subscription)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = if (isDark) Color(0xFF1B2624) else Color(0xFFE0F2F1),
            contentColor = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                    height = 3.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .testTag("settings_tab_row")
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = if (isArabic) "التنبيهات" else "Alarms",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("tab_alarms")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = if (isArabic) "الحساب والأمان" else "Account & Security",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("tab_account_security")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        text = if (isArabic) "الاشتراك" else "Subscription",
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CardMembership,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("tab_subscription")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Contents
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AlarmsSettingsTabContent(
                    settings = settings,
                    isArabic = isArabic,
                    isDark = isDark,
                    onOpenRingtonePicker = onOpenRingtonePicker,
                    onSetMixCount = onSetMixCount,
                    onSetSnoozeDuration = onSetSnoozeDuration,
                    onSetGraceIn = onSetGraceIn,
                    onSetGraceOutIn = onSetGraceOutIn,
                    onSetGraceOut = onSetGraceOut
                )
                1 -> AccountAndSecurityTabContent(
                    authState = authState,
                    isArabic = isArabic,
                    isDark = isDark,
                    onOpenAuthDialog = onOpenAuthDialog,
                    onChangePassword = onChangePassword,
                    onDeleteAccount = onDeleteAccount,
                    onSignOut = onSignOut
                )
                2 -> SubscriptionTabContent(
                    authState = authState,
                    isArabic = isArabic,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun AlarmsSettingsTabContent(
    settings: AppSettings,
    isArabic: Boolean,
    isDark: Boolean,
    onOpenRingtonePicker: () -> Unit,
    onSetMixCount: (Int) -> Unit,
    onSetSnoozeDuration: (Int) -> Unit,
    onSetGraceIn: (Int) -> Unit,
    onSetGraceOutIn: (Int) -> Unit,
    onSetGraceOut: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // SECTION 1: ALARM TONE MIXER
        SettingsSectionCard(
            title = if (isArabic) "1. خلط نغمات التنبيه" else "1. Alarm Tones Mixer",
            subtitle = if (isArabic) "اختر عدد النغمات للتنويع العشوائي عند موعد البصمة" else "Select number of tones to randomize at alarm time",
            icon = Icons.Default.LibraryMusic,
            isDark = isDark
        ) {
            Text(
                text = if (isArabic) "عدد النغمات في قائمة الخلط:" else "Number of tones to mix from:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFE0F2F1) else Color(0xFF1E3A34)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(1, 2, 3).forEach { count ->
                    val isSelected = settings.alarmMixCount == count
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetMixCount(count) },
                        label = {
                            Text(
                                text = when (count) {
                                    1 -> if (isArabic) "نغمة واحدة (افتراضي)" else "1 Tone (Default)"
                                    2 -> if (isArabic) "نغمتين (عشوائي)" else "2 Tones (Mix)"
                                    3 -> if (isArabic) "3 نغمات (عشوائي)" else "3 Tones (Mix)"
                                    else -> "$count"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) Color(0xFF00796B) else Color(0xFF80CBC4),
                            selectedLabelColor = if (isDark) Color.White else Color(0xFF004D40)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mix_count_chip_$count")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Button to open Device Ringtones Picker
            OutlinedButton(
                onClick = onOpenRingtonePicker,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_ringtone_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "تخصيص نغمات التنبيه من الهاتف" else "Choose Device Ringtones",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (isArabic) {
                            "المحدد حالياً: ${settings.selectedRingtoneUris.size} من أصل ${settings.alarmMixCount}"
                        } else {
                            "Currently chosen: ${settings.selectedRingtoneUris.size} of ${settings.alarmMixCount}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFB0BEC5) else Color(0xFF546E7A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2: SNOOZE DURATION
        SettingsSectionCard(
            title = if (isArabic) "2. مدة الغفوة (Snooze)" else "2. Snooze Duration",
            subtitle = if (isArabic) "حدد الفترة الفاصلة عند تأجيل المنبه" else "Set interval when snoozing an active alarm",
            icon = Icons.Default.Snooze,
            isDark = isDark
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3, 5, 10, 15).forEach { mins ->
                    val isSelected = settings.snoozeDurationMinutes == mins
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetSnoozeDuration(mins) },
                        label = {
                            Text(
                                text = if (isArabic) "$mins دقائق" else "$mins min",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isDark) Color(0xFF00796B) else Color(0xFF80CBC4),
                            selectedLabelColor = if (isDark) Color.White else Color(0xFF004D40)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("snooze_chip_$mins")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 3: GRACE PERIOD / LATE CALCULATION
        SettingsSectionCard(
            title = if (isArabic) "3. فترة السماح وحساب التأخير" else "3. Grace Period & Late Calculation",
            subtitle = if (isArabic) "تحديد الدقائق المسموحة قبل احتساب التأخير لكل نوع بصمة" else "Allowed minutes before marking as late for each event",
            icon = Icons.Default.HourglassBottom,
            isDark = isDark
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. IN / الدخول
                GraceStepperRow(
                    label = if (isArabic) "بصمة الدخول (IN)" else "Fingerprint IN",
                    description = if (isArabic) "الافتراضي: 15 دقيقة" else "Default: 15 minutes",
                    currentValue = settings.graceMinutesIn,
                    options = listOf(0, 5, 10, 15, 20, 30),
                    isArabic = isArabic,
                    isDark = isDark,
                    onSelect = onSetGraceIn
                )

                HorizontalDivider(color = if (isDark) Color(0xFF263238) else Color(0xFFEEEEEE))

                // 2. OUT_IN / خروج ودخول
                GraceStepperRow(
                    label = if (isArabic) "بصمة خروج / دخول (Out/In)" else "Fingerprint Out/In",
                    description = if (isArabic) "الافتراضي: 15 دقيقة" else "Default: 15 minutes",
                    currentValue = settings.graceMinutesOutIn,
                    options = listOf(0, 5, 10, 15, 20, 30),
                    isArabic = isArabic,
                    isDark = isDark,
                    onSelect = onSetGraceOutIn
                )

                HorizontalDivider(color = if (isDark) Color(0xFF263238) else Color(0xFFEEEEEE))

                // 3. OUT / الخروج
                GraceStepperRow(
                    label = if (isArabic) "بصمة الخروج (Out)" else "Fingerprint OUT",
                    description = if (isArabic) "الافتراضي: 30 دقيقة" else "Default: 30 minutes",
                    currentValue = settings.graceMinutesOut,
                    options = listOf(0, 10, 15, 20, 30, 45, 60),
                    isArabic = isArabic,
                    isDark = isDark,
                    onSelect = onSetGraceOut
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AccountAndSecurityTabContent(
    authState: AuthState,
    isArabic: Boolean,
    isDark: Boolean,
    onOpenAuthDialog: () -> Unit,
    onChangePassword: (String, String, (Boolean, String) -> Unit) -> Unit,
    onDeleteAccount: ((Boolean, String) -> Unit) -> Unit,
    onSignOut: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isChangingPassword by remember { mutableStateOf(false) }
    var oldPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var passwordErrorMessage by remember { mutableStateOf<String?>(null) }
    var passwordSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isSubmittingPassword by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        if (authState is AuthState.Authenticated) {
            // User Profile Overview Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1B2624) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = authState.displayName.ifEmpty { "User" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF1A2A28)
                            )
                            Text(
                                text = authState.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color(0xFFB0BEC5) else Color(0xFF546E7A)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)
                        ) {
                            Text(
                                text = if (isArabic) "حساب موثق" else "Verified",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Password Management Card
            SettingsSectionCard(
                title = if (isArabic) "تغيير كلمة المرور" else "Change Password",
                subtitle = if (isArabic) "قم بتحديث كلمة المرور لحماية حسابك وجدولك" else "Update your password to secure your schedule",
                icon = Icons.Default.Key,
                isDark = isDark
            ) {
                if (!isChangingPassword) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (passwordSuccessMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF004D40).copy(alpha = 0.6f) else Color(0xFFE0F2F1),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF1DE9B6) else Color(0xFF00897B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = passwordSuccessMessage ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                isChangingPassword = true
                                passwordErrorMessage = null
                                passwordSuccessMessage = null
                                oldPasswordInput = ""
                                newPasswordInput = ""
                                confirmPasswordInput = ""
                                showOldPassword = false
                                showNewPassword = false
                                showConfirmPassword = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF00897B) else Color(0xFF00796B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_change_password_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "تغيير كلمة المرور الآن" else "Change Password",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = oldPasswordInput,
                            onValueChange = { oldPasswordInput = it },
                            label = { Text(if (isArabic) "كلمة المرور الحالية" else "Current Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showOldPassword = !showOldPassword }) {
                                    Icon(
                                        imageVector = if (showOldPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showOldPassword) "Hide password" else "Show password"
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (showOldPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("current_password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = newPasswordInput,
                            onValueChange = { newPasswordInput = it },
                            label = { Text(if (isArabic) "كلمة المرور الجديدة" else "New Password") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                    Icon(
                                        imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showNewPassword) "Hide password" else "Show password"
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            label = { Text(if (isArabic) "تأكيد كلمة المرور الجديدة" else "Confirm New Password") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                    Icon(
                                        imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_new_password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (passwordErrorMessage != null) {
                            Text(
                                text = passwordErrorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (passwordSuccessMessage != null) {
                            Text(
                                text = passwordSuccessMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isChangingPassword = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isArabic) "إلغاء" else "Cancel")
                            }

                            Button(
                                onClick = {
                                    if (oldPasswordInput.isBlank()) {
                                        passwordErrorMessage = if (isArabic) "يرجى إدخال كلمة المرور الحالية" else "Please enter your current password"
                                        return@Button
                                    }
                                    if (newPasswordInput.length < 6) {
                                        passwordErrorMessage = if (isArabic) "كلمة المرور يجب أن تكون 6 أحرف على الأقل" else "Password must be at least 6 characters"
                                        return@Button
                                    }
                                    if (newPasswordInput != confirmPasswordInput) {
                                        passwordErrorMessage = if (isArabic) "كلمتا المرور غير متطابقتين" else "Passwords do not match"
                                        return@Button
                                    }
                                    isSubmittingPassword = true
                                    passwordErrorMessage = null
                                    onChangePassword(oldPasswordInput, newPasswordInput) { success, msg ->
                                        isSubmittingPassword = false
                                        if (success) {
                                            passwordSuccessMessage = msg
                                            isChangingPassword = false
                                        } else {
                                            passwordErrorMessage = msg
                                        }
                                    }
                                },
                                enabled = !isSubmittingPassword && newPasswordInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF00897B) else Color(0xFF00796B),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("submit_change_password_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSubmittingPassword) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (isArabic) "حفظ التغيير" else "Save",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sign Out Option
            SettingsSectionCard(
                title = if (isArabic) "تسجيل الخروج" else "Sign Out",
                subtitle = if (isArabic) "إنهاء الجلسة الحالية على هذا الهاتف" else "End active session on this device",
                icon = Icons.Default.Close,
                isDark = isDark
            ) {
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_sign_out_button")
                ) {
                    Text(
                        text = if (isArabic) "تسجيل الخروج من الحساب" else "Sign Out",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Danger Zone: Delete Account
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF2C1515) else Color(0xFFFFEBEE)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFFEF5350) else Color(0xFFFFCDD2)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "منطقة الخطر: حذف الحساب" else "Danger Zone: Delete Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) {
                            "سيؤدي حذف الحساب إلى مسح بياناتك وجداول البصمة وسجل التنبيهات نهائياً ولا يمكن استرجاعها."
                        } else {
                            "Permanently delete your account and all associated fingerprint schedules and backup data."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFFFCDD2) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "حذف الحساب نهائياً" else "Delete Account Permanently",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Unauthenticated Banner
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1B2624) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isArabic) "حسابك غير متصل حالياً" else "Not Signed In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1A2A28)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isArabic) {
                            "سجل الدخول بالبريد الإلكتروني لحماية إعداداتك ومزامنة جداول البصمة وتأمين بياناتك سحابياً."
                        } else {
                            "Sign in with your email to enable secure cloud backups, schedule syncing, and account management."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFB0BEC5) else Color(0xFF546E7A),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onOpenAuthDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF00796B) else Color(0xFF00897B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_open_login_button")
                    ) {
                        Text(
                            text = if (isArabic) "تسجيل الدخول / إنشاء حساب جديد" else "Sign In / Create Account",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Delete Account Confirmation Alert Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isArabic) "تأكيد حذف الحساب نهائياً" else "Confirm Permanent Deletion",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = if (isArabic) {
                        "هل أنت متأكد تماماً من رغبتك في حذف الحساب؟ سيتم مسح كافة مواعيد البصمة المسجلة وإعدادات التنبيه ولا يمكن التراجع عن هذا الإجراء."
                    } else {
                        "Are you absolutely sure you want to delete your account? All saved fingerprint schedules and alarm preferences will be erased permanently."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletingAccount = true
                        onDeleteAccount { _, _ ->
                            isDeletingAccount = false
                            showDeleteConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (isArabic) "نعم، حذف الحساب" else "Yes, Delete")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isDeletingAccount
                ) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun SubscriptionTabContent(
    authState: AuthState,
    isArabic: Boolean,
    isDark: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Status Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1E3025) else Color(0xFFF1F8E9)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF81C784) else Color(0xFF8BC34A)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isArabic) "الباقة التجريبية نشطة (Free Trial)" else "Free Trial Active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFC8E6C9) else Color(0xFF2E7D32)
                            )
                            Text(
                                text = if (isArabic) "متبقي 60 يوماً مع كافة الميزات مفعلة" else "60 days remaining with all features unlocked",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF388E3C)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF2E7D32) else Color(0xFFC8E6C9)
                    ) {
                        Text(
                            text = if (isArabic) "نشط" else "ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1B5E20),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Subscription Plans Grid / Cards
        Text(
            text = if (isArabic) "خيارات الترقية والاشتراك" else "Subscription Plans",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color(0xFF1E3A34)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Tier 1: Pro Monthly
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (isDark) Color(0xFF152320) else Color(0xFFF9FBE7)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0xFF37474F) else Color(0xFFDCEDC8)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "باقة Pro الشهرية" else "Pro Monthly Plan",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isDark) Color.White else Color(0xFF1E3A34)
                        )
                        Text(
                            text = if (isArabic) "تجديد شهري مرن مع إلغاء في أي وقت" else "Flexible monthly billing, cancel anytime",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFB0BEC5) else Color(0xFF546E7A)
                        )
                    }
                    Text(
                        text = "$4.99",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isArabic) {
                        "✓ مزامنة سحابية غير محدودة\n✓ خلط ذكي متقدم لنغمات الهاتف\n✓ تنبيهات فورية للمناوبات والمواعيد"
                    } else {
                        "✓ Unlimited cloud backup & sync\n✓ Advanced smart ringtone mixer\n✓ Priority shift & fingerprint reminders"
                    },
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFCFD8DC) else Color(0xFF37474F),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { /* Placeholder */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF00796B) else Color(0xFF00897B)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_pro_monthly_button")
                ) {
                    Text(if (isArabic) "اختيار الباقة الشهرية" else "Select Pro Monthly")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tier 2: VIP Annual (Recommended)
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (isDark) Color(0xFF242215) else Color(0xFFFFFDE7)
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isArabic) "باقة VIP السنوية" else "VIP Annual Plan",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isDark) Color.White else Color(0xFF1E3A34)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFB300)
                            ) {
                                Text(
                                    text = if (isArabic) "توفير 33%" else "SAVE 33%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isArabic) "الخيار الأفضل والأكثر قيمة للمستخدمين" else "Best value for continuous protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFB0BEC5) else Color(0xFF546E7A)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$39.99",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFFFFB300)
                        )
                        Text(
                            text = if (isArabic) "سنوياً" else "/ year",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFFFE082) else Color(0xFFF57F17)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isArabic) {
                        "✓ جميع ميزات باقة Pro الشهرية\n✓ أولوية الوصول للميزات والذكاء الاصطناعي\n✓ دعم فني مخصص وسريع\n✓ ترخيص على عدة أجهزة بنفس الحساب"
                    } else {
                        "✓ Everything in Pro monthly\n✓ Priority access to new AI & features\n✓ Dedicated direct support\n✓ Multi-device license on same account"
                    },
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFCFD8DC) else Color(0xFF37474F),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { /* Placeholder */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_vip_annual_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "ترقية إلى VIP السنوية" else "Upgrade to VIP Annual",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Manage / Restore Subscription Placeholder
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { /* Placeholder */ },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("restore_purchases_placeholder_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "استعادة المشتريات" else "Restore Purchases",
                    fontSize = 12.sp
                )
            }

            OutlinedButton(
                onClick = { /* Placeholder */ },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("manage_plan_placeholder_button")
            ) {
                Text(
                    text = if (isArabic) "إدارة الاشتراك" else "Manage Plan",
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1B2624) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1E3A34)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFB0BEC5) else Color(0xFF546E7A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun GraceStepperRow(
    label: String,
    description: String,
    currentValue: Int,
    options: List<Int>,
    isArabic: Boolean,
    isDark: Boolean,
    onSelect: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF1A2A28)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF90A4AE) else Color(0xFF78909C)
                )
            }

            // Current selected badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)
            ) {
                Text(
                    text = if (currentValue == 0) {
                        if (isArabic) "بدون سماح (0 د)" else "No Grace (0m)"
                    } else {
                        if (isArabic) "$currentValue دقيقة" else "$currentValue min"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Options Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { mins ->
                val isSelected = currentValue == mins
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(mins) },
                    label = {
                        Text(
                            text = if (mins == 0) "0" else "$mins",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isDark) Color(0xFF00796B) else Color(0xFF80CBC4),
                        selectedLabelColor = if (isDark) Color.White else Color(0xFF004D40)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RingtonePickerView(
    availableRingtones: List<DeviceRingtone>,
    selectedUris: Set<String>,
    maxMixCount: Int,
    currentlyPlayingUri: String?,
    isArabic: Boolean,
    isDark: Boolean,
    onBack: () -> Unit,
    onToggleRingtone: (String) -> Unit,
    onToggleAudioPreview: (String) -> Unit
) {
    val isLimitReached = selectedUris.size >= maxMixCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag("ringtone_picker_view")
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF263238) else Color(0xFFECEFF1))
                        .testTag("ringtone_picker_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isArabic) "نغمات الهاتف للتنبيه" else "Device Alarm Ringtones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF1E3A34)
                    )
                    Text(
                        text = if (isArabic) {
                            "اختر حتى $maxMixCount نغمات للتنويع العشوائي"
                        } else {
                            "Select up to $maxMixCount tones to randomize"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF80CBC4) else Color(0xFF546E7A)
                    )
                }
            }

            // Counter Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isLimitReached) {
                    if (isDark) Color(0xFF00796B) else Color(0xFF00897B)
                } else {
                    if (isDark) Color(0xFF263238) else Color(0xFFCFD8DC)
                }
            ) {
                Text(
                    text = "${selectedUris.size} / $maxMixCount",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Info Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF162B27) else Color(0xFFE8F5E9),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF1DE9B6) else Color(0xFF2E7D32),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isArabic) {
                        "انقر زر التشغيل للاستماع لأي نغمة، وحدد المربع لاختيارها للمنبه."
                    } else {
                        "Tap play icon to audition any tone, and check the box to select it."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFFC8E6C9) else Color(0xFF1B5E20)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = if (isDark) Color(0xFF263238) else Color(0xFFEEEEEE))
        Spacer(modifier = Modifier.height(6.dp))

        // Ringtones List
        if (availableRingtones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isArabic) "جاري تحميل نغمات الهاتف..." else "Loading device ringtones...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color(0xFF90A4AE) else Color(0xFF78909C)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(availableRingtones, key = { it.uriString }) { ringtone ->
                    val isSelected = selectedUris.contains(ringtone.uriString)
                    val isPlaying = currentlyPlayingUri == ringtone.uriString
                    val isCheckboxEnabled = isSelected || !isLimitReached

                    RingtoneItemRow(
                        ringtone = ringtone,
                        isSelected = isSelected,
                        isPlaying = isPlaying,
                        isCheckboxEnabled = isCheckboxEnabled,
                        isDark = isDark,
                        onToggleSelection = {
                            if (isCheckboxEnabled || isSelected) {
                                onToggleRingtone(ringtone.uriString)
                            }
                        },
                        onTogglePlay = {
                            onToggleAudioPreview(ringtone.uriString)
                        }
                    )
                }
            }
        }

        // Bottom Done Button
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF00897B) else Color(0xFF00796B),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ringtone_picker_done_button")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isArabic) "حفظ واكتمال الاختيار" else "Save & Complete Selection",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RingtoneItemRow(
    ringtone: DeviceRingtone,
    isSelected: Boolean,
    isPlaying: Boolean,
    isCheckboxEnabled: Boolean,
    isDark: Boolean,
    onToggleSelection: () -> Unit,
    onTogglePlay: () -> Unit
) {
    val cardBg = when {
        isSelected -> if (isDark) Color(0xFF132B25) else Color(0xFFE8F5E9)
        isPlaying -> if (isDark) Color(0xFF1C2D37) else Color(0xFFE1F5FE)
        else -> if (isDark) Color(0xFF1B2624) else Color.White
    }

    val borderColor = when {
        isSelected -> if (isDark) Color(0xFF1DE9B6) else Color(0xFF4CAF50)
        isPlaying -> if (isDark) Color(0xFF0288D1) else Color(0xFF03A9F4)
        else -> if (isDark) Color(0xFF263238) else Color(0xFFEEEEEE)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected || isPlaying) 2.dp else 0.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ringtone_item_${ringtone.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) {
                            if (isDark) Color(0xFF00B0FF) else Color(0xFF0288D1)
                        } else {
                            if (isDark) Color(0xFF263238) else Color(0xFFE0F2F1)
                        }
                    )
                    .testTag("play_tone_${ringtone.id}")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Tone" else "Play Tone",
                    tint = if (isPlaying) Color.White else (if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B)),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (isCheckboxEnabled || isSelected) {
                            onToggleSelection()
                        }
                    }
            ) {
                Text(
                    text = ringtone.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isDark) Color.White else Color(0xFF1A2A28),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isPlaying) {
                    Text(
                        text = "Playing preview...",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF80D8FF) else Color(0xFF0288D1)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .alpha(if (isCheckboxEnabled) 1f else 0.38f)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    enabled = isCheckboxEnabled,
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (isDark) Color(0xFF1DE9B6) else Color(0xFF00796B),
                        uncheckedColor = if (isDark) Color(0xFF78909C) else Color(0xFF90A4AE),
                        checkmarkColor = if (isDark) Color(0xFF003831) else Color.White
                    ),
                    modifier = Modifier.testTag("ringtone_checkbox_${ringtone.id}")
                )
            }
        }
    }
}
