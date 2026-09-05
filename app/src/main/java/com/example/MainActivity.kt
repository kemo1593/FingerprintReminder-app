package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fingerprint.auth.AuthState
import com.example.fingerprint.ui.auth.AuthDialog
import com.example.fingerprint.ui.auth.AuthViewModel
import com.example.fingerprint.ui.feedback.FeedbackAndUpdateViewModel
import com.example.fingerprint.ui.feedback.FeedbackDialog
import com.example.fingerprint.ui.schedule.ScheduleCreationScreen
import com.example.fingerprint.ui.schedule.ScheduleTimelineScreen
import com.example.fingerprint.ui.schedule.ScheduleViewModel
import com.example.fingerprint.ui.settings.SettingsDialog
import com.example.fingerprint.ui.settings.SettingsViewModel
import com.example.fingerprint.ui.update.AppUpdateDialog
import com.example.ui.theme.FingerprintReminderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val scheduleViewModel: ScheduleViewModel = viewModel()
            val uiState by scheduleViewModel.uiState.collectAsState()

            val authViewModel: AuthViewModel = viewModel()
            val authUiState by authViewModel.uiState.collectAsState()

            val feedbackViewModel: FeedbackAndUpdateViewModel = viewModel()
            val feedbackUiState by feedbackViewModel.uiState.collectAsState()

            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settingsState.collectAsState()
            val isSettingsOpen by settingsViewModel.isSettingsOpen.collectAsState()
            val isRingtonePickerOpen by settingsViewModel.isRingtonePickerOpen.collectAsState()
            val availableRingtones by settingsViewModel.availableRingtones.collectAsState()
            val currentlyPlayingUri by settingsViewModel.currentlyPlayingUri.collectAsState()

            val layoutDirection = if (uiState.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        authViewModel.checkActiveDeviceSession()
                        authViewModel.syncInBackground()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                FingerprintReminderTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val config = uiState.activeConfig
                        val isAuthenticated = authUiState.authState is AuthState.Authenticated

                        if (!isAuthenticated) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (uiState.isArabic) "تسجيل الدخول مطلوب" else "Sign In Required",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (uiState.isArabic)
                                            "يجب تسجيل الدخول بحساب للوصول إلى الجداول وخصائص التطبيق"
                                        else
                                            "Please sign in with an account to access scheduling and app settings",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { authViewModel.openAuthDialog() },
                                        modifier = Modifier.testTag("open_login_button")
                                    ) {
                                        Text(if (uiState.isArabic) "تسجيل الدخول" else "Sign In")
                                    }
                                }
                            }
                        } else if (config != null && uiState.events.isNotEmpty()) {
                            ScheduleTimelineScreen(
                                config = config,
                                events = uiState.events,
                                nextEvent = uiState.nextEvent,
                                isArabic = uiState.isArabic,
                                onLanguageToggle = { scheduleViewModel.setLanguage(!uiState.isArabic) },
                                onOpenAuthDialog = { authViewModel.openAuthDialog() },
                                onOpenFeedbackDialog = { feedbackViewModel.openFeedbackDialog() },
                                onOpenSettings = { settingsViewModel.openSettings() },
                                onClearSchedule = { scheduleViewModel.clearSchedule() },
                                onDoneEvent = { eventId -> scheduleViewModel.markEventDone(eventId) },
                                onSnoozeEvent = { eventId, eventType ->
                                    scheduleViewModel.snoozeEvent(eventId, eventType, settings.snoozeDurationMinutes)
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        } else {
                            ScheduleCreationScreen(
                                isArabic = uiState.isArabic,
                                onLanguageToggle = { scheduleViewModel.setLanguage(!uiState.isArabic) },
                                onOpenAuthDialog = { authViewModel.openAuthDialog() },
                                onOpenFeedbackDialog = { feedbackViewModel.openFeedbackDialog() },
                                onOpenSettings = { settingsViewModel.openSettings() },
                                onGenerateSchedule = { systemType, firstEntry, lastEntry, workDays, customOffset ->
                                    scheduleViewModel.generateAndSaveSchedule(
                                        systemType = systemType,
                                        firstEntryLdt = firstEntry,
                                        lastEntryLdt = lastEntry,
                                        workDays = workDays,
                                        customLastOutHoursOffset = customOffset
                                    )
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        // Settings & Alarm Tones Modal Dialog (Only accessible when authenticated)
                        if (isAuthenticated) {
                            SettingsDialog(
                                isOpen = isSettingsOpen,
                                isRingtonePickerOpen = isRingtonePickerOpen,
                                settings = settings,
                                availableRingtones = availableRingtones,
                                currentlyPlayingUri = currentlyPlayingUri,
                                isArabic = uiState.isArabic,
                                authState = authUiState.authState,
                                onDismiss = { settingsViewModel.closeSettings() },
                                onOpenRingtonePicker = { settingsViewModel.openRingtonePicker() },
                                onCloseRingtonePicker = { settingsViewModel.closeRingtonePicker() },
                                onSetMixCount = { count -> settingsViewModel.setAlarmMixCount(count) },
                                onToggleRingtone = { uri -> settingsViewModel.toggleRingtoneSelection(uri) },
                                onSetSnoozeDuration = { mins -> settingsViewModel.setSnoozeDurationMinutes(mins) },
                                onSetGraceIn = { mins -> settingsViewModel.setGraceMinutesIn(mins) },
                                onSetGraceOutIn = { mins -> settingsViewModel.setGraceMinutesOutIn(mins) },
                                onSetGraceOut = { mins -> settingsViewModel.setGraceMinutesOut(mins) },
                                onToggleAudioPreview = { uri -> settingsViewModel.toggleAudioPreview(uri) },
                                onOpenAuthDialog = { authViewModel.openAuthDialog() },
                                onChangePassword = { oldPass, newPass, onResult ->
                                    authViewModel.changePassword(oldPass, newPass, uiState.isArabic, onResult)
                                },
                                onDeleteAccount = { onResult ->
                                    authViewModel.deleteAccount(uiState.isArabic, onResult)
                                },
                                onSignOut = { authViewModel.signOut() }
                            )
                        }

                        // Check for updates on startup & on login
                        androidx.compose.runtime.LaunchedEffect(authUiState.authState) {
                            feedbackViewModel.checkForRemoteUpdates()
                        }

                        // Auth Modal Dialog
                        AuthDialog(
                            uiState = authUiState,
                            isArabic = uiState.isArabic,
                            onDismiss = { authViewModel.closeAuthDialog() },
                            onToggleSignUp = { isSignUp -> authViewModel.toggleSignUpMode(isSignUp) },
                            onEmailChanged = { email -> authViewModel.onEmailChanged(email) },
                            onPasswordChanged = { pass -> authViewModel.onPasswordChanged(pass) },
                            onNameChanged = { name -> authViewModel.onNameChanged(name) },
                            onSubmitEmailAuth = { authViewModel.submitEmailAuth(uiState.isArabic) },
                            onGoogleSignIn = { idToken -> authViewModel.signInWithGoogleIdToken(idToken, uiState.isArabic) },
                            onError = { err -> authViewModel.setErrorMessage(err) },
                            onSendPasswordReset = { email ->
                                authViewModel.sendPasswordReset(email, uiState.isArabic) { _, _ -> }
                            },
                            onSignOut = { authViewModel.signOut() }
                        )

                        // Feedback & Suggestion Modal Dialog
                        val authState = authUiState.authState
                        val initialName = if (authState is AuthState.Authenticated) authState.displayName else ""
                        val initialEmail = if (authState is AuthState.Authenticated) authState.email else ""
                        val currentUserId = if (authState is AuthState.Authenticated) authState.userId else null

                        FeedbackDialog(
                            isOpen = feedbackUiState.isFeedbackDialogOpen,
                            isArabic = uiState.isArabic,
                            initialName = initialName,
                            initialEmail = initialEmail,
                            isLoading = feedbackUiState.isSubmittingFeedback,
                            statusMessage = feedbackUiState.feedbackStatusMessage,
                            onDismiss = { feedbackViewModel.closeFeedbackDialog() },
                            onSubmitFeedback = { name, email, type, rating, msg ->
                                feedbackViewModel.submitFeedback(
                                    name = name,
                                    email = email,
                                    type = type,
                                    rating = rating,
                                    message = msg,
                                    userId = currentUserId,
                                    isArabic = uiState.isArabic
                                )
                            }
                        )

                        // Remote App Update & Announcement Dialog
                        AppUpdateDialog(
                            updateDto = feedbackUiState.availableUpdate,
                            isArabic = uiState.isArabic,
                            onDismiss = { feedbackViewModel.dismissUpdateDialog() }
                        )
                    }
                }
            }
        }
    }
}
