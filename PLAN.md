# Fingerprint Time Reminder App — Architecture & Implementation Plan

## 1. Project Architecture & Package Structure
The app follows Clean Architecture principles using Jetpack Compose, ViewModel, Kotlin Coroutines/Flow, Room for local caching, and Supabase for cloud backend, authentication, user data, and subscription synchronization.

Package root: `com.example.fingerprint`

```
com.example/
  ├── fingerprint/
  │   ├── alarm/               # AlarmManager, BroadcastReceivers, Notification & Fullscreen Alarm logic
  │   │   ├── AlarmReceiver.kt
  │   │   ├── AlarmScheduler.kt
  │   │   └── BootReceiver.kt
  │   ├── auth/                # Supabase Auth state & sign-in helper
  │   │   ├── AuthRepository.kt
  │   │   └── AuthState.kt
  │   ├── billing/             # Subscription repository interface & Play Billing / Paymob implementations
  │   │   ├── SubscriptionRepository.kt
  │   │   ├── PlayBillingRepositoryImpl.kt
  │   │   └── PaymobRepositoryImpl.kt
  │   ├── data/                # Data layer: Room DB, Entities, DAOs, Supabase Sync
  │   │   ├── local/
  │   │   │   ├── FingerprintDatabase.kt
  │   │   │   ├── FingerprintDao.kt
  │   │   │   └── FingerprintEventEntity.kt
  │   │   ├── remote/
  │   │   │   └── SupabaseSyncService.kt
  │   │   └── repository/
  │   │       └── ScheduleRepositoryImpl.kt
  │   ├── domain/              # Pure domain logic & models
  │   │   ├── engine/          # Pure Kotlin Fingerprint Schedule Engine
  │   │   │   ├── ScheduleEngine.kt
  │   │   │   └── ScheduleCalculator.kt
  │   │   ├── model/           # Core Kotlin Data Classes
  │   │   │   ├── UserProfile.kt
  │   │   │   ├── ScheduleConfig.kt
  │   │   │   ├── FingerprintEvent.kt
  │   │   │   └── SubscriptionState.kt
  │   │   └── repository/
  │   │       └── ScheduleRepository.kt
  │   └── ui/                  # UI Layer (M3 Jetpack Compose)
  │       ├── theme/           # Customized Material 3 Theme (Teal/Emerald palette + Cairo/Tajawal Typography)
  │       ├── navigation/      # Type-safe Navigation
  │       ├── schedule/        # Schedule creation & display views
  │       ├── alarm/           # Full-screen lockable alarm UI
  │       ├── profile/         # Auth & Profile screen
  │       ├── billing/         # Subscription management screen
  │       └── settings/        # Settings & Localization (Arabic/English, work days, snooze)
```

---

## 2. Core Data Models (Kotlin Data Classes)

### `UserProfile`
```kotlin
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

### `ScheduleConfig`
```kotlin
enum class SystemType {
    SYSTEM_8_2,
    SYSTEM_8_3,
    SYSTEM_9_3,
    SYSTEM_10_5,
    SYSTEM_18_6,
    SYSTEM_24_6
}

data class ScheduleConfig(
    val id: String = UUID.randomUUID().toString(),
    val systemType: SystemType,
    val firstEntryTimeEpochMillis: Long,
    val lastEntryTimeEpochMillis: Long,
    val workDays: Set<Int> = setOf(1, 2, 3, 4, 7), // Calendar.SUNDAY to THURSDAY
    val customLastOutHoursOffset: Int? = null // Allow overriding last event offset
)
```

### `FingerprintEvent`
```kotlin
enum class EventType {
    IN,      // دخول
    OUT_IN,  // خروج / دخول
    OUT      // خروج
}

enum class EventStatus {
    PENDING,
    DONE,    // بصمت
    SNOOZED  // غفوة
}

data class FingerprintEvent(
    val id: String = UUID.randomUUID().toString(),
    val scheduleId: String,
    val eventType: EventType,
    val scheduledTimeEpochMillis: Long,
    val cycleIndex: Int = 0,
    val status: EventStatus = EventStatus.PENDING,
    val snoozedUntilEpochMillis: Long? = null
)
```

### `SubscriptionState`
```kotlin
enum class EntitlementStatus {
    TRIAL,
    ACTIVE,
    GRACE_PERIOD,
    EXPIRED
}

data class SubscriptionState(
    val status: EntitlementStatus = EntitlementStatus.TRIAL,
    val trialEndEpochMillis: Long = 0L,
    val renewalEpochMillis: Long = 0L,
    val planId: String = "sub_8_day",
    val isAutoRenewing: Boolean = true
)
```

---

## 3. Supabase Database Schema (PostgreSQL Tables & RLS)

### `users` table
- `id`: `uuid` PRIMARY KEY (references `auth.users.id` ON DELETE CASCADE)
- `email`: `text` NOT NULL
- `display_name`: `text`
- `photo_url`: `text`
- `subscription_status`: `text` DEFAULT `'TRIAL'` ("TRIAL" | "ACTIVE" | "GRACE_PERIOD" | "EXPIRED")
- `trial_end_epoch_millis`: `bigint`
- `renewal_epoch_millis`: `bigint`
- `plan_id`: `text` DEFAULT `'sub_8_day'`
- `is_auto_renewing`: `boolean` DEFAULT `true`
- `created_at`: `timestamptz` DEFAULT `now()`

### `schedules` table
- `id`: `uuid` PRIMARY KEY DEFAULT `gen_random_uuid()`
- `user_id`: `uuid` NOT NULL REFERENCES `users(id)` ON DELETE CASCADE
- `system_type`: `text` NOT NULL
- `first_entry_time_epoch_millis`: `bigint` NOT NULL
- `last_entry_time_epoch_millis`: `bigint` NOT NULL
- `work_days`: `integer[]` NOT NULL
- `created_at`: `timestamptz` DEFAULT `now()`

### `events` table
- `id`: `uuid` PRIMARY KEY DEFAULT `gen_random_uuid()`
- `user_id`: `uuid` NOT NULL REFERENCES `users(id)` ON DELETE CASCADE
- `schedule_id`: `uuid` NOT NULL REFERENCES `schedules(id)` ON DELETE CASCADE
- `event_type`: `text` NOT NULL ("IN" | "OUT_IN" | "OUT")
- `scheduled_time_epoch_millis`: `bigint` NOT NULL
- `status`: `text` NOT NULL DEFAULT `'PENDING'` ("PENDING" | "DONE" | "SNOOZED")
- `snoozed_until_epoch_millis`: `bigint`
- `updated_at`: `timestamptz` DEFAULT `now()`

### `feedbacks` table
- `id`: `uuid` PRIMARY KEY DEFAULT `gen_random_uuid()`
- `user_id`: `uuid` REFERENCES `users(id)` ON DELETE SET NULL
- `user_name`: `text` NOT NULL
- `user_email`: `text` NOT NULL
- `feedback_type`: `text` NOT NULL DEFAULT `'SUGGESTION'` ("SUGGESTION" | "BUG_REPORT" | "OTHER")
- `rating`: `integer` DEFAULT 5
- `message`: `text` NOT NULL
- `app_version`: `text`
- `created_at`: `timestamptz` DEFAULT `now()`

### `app_updates` table (Remote Version Checks & Direct APK Updates)
- `id`: `uuid` PRIMARY KEY DEFAULT `gen_random_uuid()`
- `latest_version_code`: `integer` NOT NULL
- `latest_version_name`: `text` NOT NULL
- `apk_download_url`: `text` NOT NULL
- `is_mandatory`: `boolean` DEFAULT `false`
- `title_ar`: `text`
- `title_en`: `text`
- `release_notes_ar`: `text`
- `release_notes_en`: `text`
- `created_at`: `timestamptz` DEFAULT `now()`

### Row Level Security (RLS) Policies
```sql
-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
ALTER TABLE feedbacks ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_updates ENABLE ROW LEVEL SECURITY;

-- Users can read/update their own profile
CREATE POLICY "Allow users to view/edit own profile" ON users
  FOR ALL USING (auth.uid() = id);

-- Users can manage their own schedules
CREATE POLICY "Allow users to manage own schedules" ON schedules
  FOR ALL USING (auth.uid() = user_id);

-- Users can manage their own schedule events
CREATE POLICY "Allow users to manage own events" ON events
  FOR ALL USING (auth.uid() = user_id);

-- Anyone authenticated or anonymous can submit feedback
CREATE POLICY "Allow users to insert feedback" ON feedbacks
  FOR INSERT WITH CHECK (true);

-- Anyone can read active app updates
CREATE POLICY "Allow public read on app updates" ON app_updates
  FOR SELECT USING (true);
```

---

## 4. Phased Build Order & Current Status

- [x] **Phase 0: Project Setup & Custom Design System**
  - [x] Configure unique `applicationId`, `app_name`, and `metadata.json`
  - [x] Add Room, AlarmManager permissions, and Arabic localization support
  - [x] Implement M3 Teal/Emerald theme with Cairo/Tajawal font support & RTL support
  - [x] Create adaptive launcher icon

- [x] **Phase 1: Core Scheduling Engine (Pure Kotlin & Unit Tests)**
  - [x] Implement `ScheduleEngine` with support for daily systems (8-2, 8-3, 9-3, 10-5)
  - [x] Implement 18/6 system (3 fixed clock time events per day)
  - [x] Implement 24/6 system (30-hour cycle loop with rotating +6h start time)
  - [x] Write unit tests for all 6 systems, validating day/month boundary calculations and 24/6 shift cycles

- [x] **Phase 2: Core UI & Schedule Flow**
  - [x] First Entry Date & Time Picker (displaying Arabic/English weekday name)
  - [x] Fingerprint System Selector (8-2, 8-3, 9-3, 10-5, 18-6, 24-6)
  - [x] Last Entry Date & Time Picker with custom final Out confirmation
  - [x] Schedule Timeline Display with "Next Fingerprint" highlighted popping card

- [x] **Phase 3: Alarm System & Background Manager**
  - [x] `AlarmManager` integration with `setExactAndAllowWhileIdle` and permission request UI
  - [x] Full-Screen Lock-Screen Alarm activity (launches audio/vibration full screen when alarm fires on lockscreen)
  - [x] Unlocked Device Alarm Stream & Notification banner with direct "بصمت" (Done) and "غفوة 5د" (Snooze 5m) action buttons
  - [x] Compartmentalized `AlarmSoundManager` singleton for leak-free alarm audio playback and vibration control across all triggers
  - [x] Strict Timing Window Logic in `EventTimingHelper` for Next Fingerprint hero card buttons:
    - **IN (دخول)**: Buttons activate 15 minutes before scheduled time
    - **OUT/IN (خروج/دخول)**: Buttons activate at exact scheduled time
    - **OUT (خروج)**: Buttons activate at exact scheduled time
  - [x] Room persistence for event completion and snooze states
  - [x] Synced dismissals: Tapping Done/Snooze in notification or full-screen stops alarm sound and clears notifications immediately
  - [x] Streamlined Notification Banner: Displays heads-up notification banner with responsive "Done" and "Snooze" action buttons across all states (app open, foreground, background, or closed) without obstructive full-screen takeovers on unlocked devices.
  - [x] Single-Tone Clean Alarm Audio (`FLAG_INSISTENT`): Eliminated secondary MediaPlayer conflicts while preserving high-priority notification channel audio, persistent looping alarm chime, full heads-up banner on unlocked screens, and lock-screen fullScreenIntent wakeup.
  - [x] Isolated Lock-Screen Task Dismissal: `AlarmActivity` runs in an isolated task (`taskAffinity="com.example.fingerprint.alarmtask"`, `launchMode="singleInstance"`, `noHistory="true"`). Tapping "Done" or "Snooze" on the full-screen alarm executes the action and dismisses itself via `finishAndRemoveTask()` without launching or bringing the main app to the foreground.

## 4. Phased Build Order & Current Status

- [x] **Phase 0: Project Setup & Custom Design System**
  - [x] Configure unique `applicationId`, `app_name`, and `metadata.json`
  - [x] Add Room, AlarmManager permissions, and Arabic localization support
  - [x] Implement M3 Teal/Emerald theme with Cairo/Tajawal font support & RTL support
  - [x] Create adaptive launcher icon

- [x] **Phase 1: Core Scheduling Engine (Pure Kotlin & Unit Tests)**
  - [x] Implement `ScheduleEngine` with support for daily systems (8-2, 8-3, 9-3, 10-5)
  - [x] Implement 18/6 system (3 fixed clock time events per day)
  - [x] Implement 24/6 system (30-hour cycle loop with rotating +6h start time)
  - [x] Write unit tests for all 6 systems, validating day/month boundary calculations and 24/6 shift cycles

- [x] **Phase 2: Core UI & Schedule Flow**
  - [x] First Entry Date & Time Picker (displaying Arabic/English weekday name)
  - [x] Fingerprint System Selector (8-2, 8-3, 9-3, 10-5, 18-6, 24-6)
  - [x] Last Entry Date & Time Picker with custom final Out confirmation
  - [x] Schedule Timeline Display with "Next Fingerprint" highlighted popping card

- [x] **Phase 3: Alarm System & Background Manager**
  - [x] `AlarmManager` integration with `setExactAndAllowWhileIdle` and permission request UI
  - [x] Full-Screen Lock-Screen Alarm activity (launches audio/vibration full screen when alarm fires on lockscreen)
  - [x] Unlocked Device Alarm Stream & Notification banner with direct "بصمت" (Done) and "غفوة 5د" (Snooze 5m) action buttons
  - [x] Compartmentalized `AlarmSoundManager` singleton for leak-free alarm audio playback and vibration control across all triggers
  - [x] Strict Timing Window Logic in `EventTimingHelper` for Next Fingerprint hero card buttons:
    - **IN (دخول)**: Buttons activate 15 minutes before scheduled time
    - **OUT/IN (خروج/دخول)**: Buttons activate at exact scheduled time
    - **OUT (خروج)**: Buttons activate at exact scheduled time
  - [x] Room persistence for event completion and snooze states
  - [x] Synced dismissals: Tapping Done/Snooze in notification or full-screen stops alarm sound and clears notifications immediately
  - [x] Streamlined Notification Banner: Displays heads-up notification banner with responsive "Done" and "Snooze" action buttons across all states (app open, foreground, background, or closed) without obstructive full-screen takeovers on unlocked devices.
  - [x] Single-Tone Clean Alarm Audio (`FLAG_INSISTENT`): Eliminated secondary MediaPlayer conflicts while preserving high-priority notification channel audio, persistent looping alarm chime, full heads-up banner on unlocked screens, and lock-screen fullScreenIntent wakeup.
  - [x] Isolated Lock-Screen Task Dismissal: `AlarmActivity` runs in an isolated task (`taskAffinity="com.example.fingerprint.alarmtask"`, `launchMode="singleInstance"`, `noHistory="true"`). Tapping "Done" or "Snooze" on the full-screen alarm executes the action and dismisses itself via `finishAndRemoveTask()` without launching or bringing the main app to the foreground.

- [x] **Phase 4: Authentication, Cloud Sync & Account Security**
  - [x] **Auth State & Session Manager**: Implemented `AuthSessionManager` with secure local credential and session persistence, plus reactive state flows (`AuthState`).
  - [x] **Automatic JWT Token Refresh**: Implemented automatic detection and refreshing of expired Supabase access tokens (`grant_type=refresh_token`), resolving "invalid JWT: token has invalid claims: token is expired" on password changes and authenticated operations.
  - [x] **Email & Password Authentication**: Full Login and Sign-Up dialogs with bilingual validation, input fields, and smooth tab switching.
  - [x] **Forgot Password**: Password reset request flow accessible directly from the login view.
  - [x] **Account & Security Tab in Settings**:
    - User email & verified account badge display
    - In-app password change form (with auto-token refresh, new password requirements, and matching checks)
    - Permanent account deletion danger zone with confirmation alert dialog
    - Direct sign-out controls
  - [x] **Automated Background Cloud Sync**: Seamless automatic sync triggering when network becomes available, on login, and upon any schedule modifications without requiring manual interaction.
  - [x] **Clean Schedule Deletion Handling**: Fixed cloud sync logic so that deleting a schedule clears both local storage and remote cloud entries, preventing old schedules from resurrecting.
  - [x] **Single-Device Session Enforcement**: User accounts are tied to an active device identifier in the cloud (`profiles.current_device_id` & `user_metadata.current_device_id`). Device A monitors session validity via periodic background loops (15s) and `onResume` lifecycle events; if an account is opened on Device B, Device A immediately terminates the session, clears alarms, and shows a prompt.
  - [x] **Logged-Out Alarm Suppression**: Alarms, notifications, and ringtones are strictly disabled when the user is logged out. The app requires an authenticated session (even in offline cached mode) to schedule and fire alarms.
  - [x] **White-Labeled UI**: Completely free of any vendor names or internal infrastructure references in user-facing strings and dialogs.

- [ ] **Phase 4.1: Free Web Landing Pages & Custom Domain Integration (100% Free on GitHub Pages / Vercel)**
  - [ ] **Custom Hostinger Domain Connection**: Point your domain/subdomain (e.g., `app.yourdomain.com` or `yourdomain.com`) to GitHub Pages or Vercel (100% free forever, free SSL certificate).
  - [ ] **Welcome & App Download Page (`/` or `/welcome`)**:
    - Beautiful, branded landing page for users who sign up via Google or email.
    - App download buttons (Direct APK download & Google Play link).
  - [ ] **Email Confirmation Success Page (`/confirm-email`)**:
    - Clean landing page welcoming the user after clicking their email confirmation link.
    - Button to open the installed Android app directly or download the latest update.
  - [ ] **Web-Based & Direct App Password Reset Page (`/reset-password`)**:
    - Dual-action page: allows users to reset their password directly on the web form OR tap a button to launch the Android app and reset in-app.
  - [ ] **Android App Links (`.well-known/assetlinks.json`)**:
    - Host SHA-256 fingerprint certificate on the domain so Android opens the app instantly with zero browser redirects.

- [x] **Phase 5: Subscriptions & Entitlements**
  - [x] **Subscription Tab in Settings**:
    - Active entitlement banner displaying the free trial and days remaining counter
    - Tier showcase cards: Pro Monthly ($4.99/mo), VIP Annual ($39.99/yr - Save 33%), and Lifetime License ($99.99)
    - Feature breakdowns, upgrade action buttons, and restore purchase affordances
  - [ ] **Upcoming: 3-Day Trial Test Mode & Feature Lock (Post-Payment Integration)**:
    - Configure temporary 3-day trial duration for live testing
    - Lock schedule generation, editing, and alarms upon trial expiration with mandatory subscription prompt
  - [ ] **Upcoming: Device Hardware Trial Anti-Abuse System**:
    - Track hardware device identifiers via `device_trials` table in Supabase to prevent trial resets across new emails on the same device.

- [x] **Phase 6: Feedback, Remote Version Updates & Advanced Settings**
  - [x] **Feedback & Suggestion System**: Modal dialog to submit user feedback, ratings, and feature requests directly to Supabase `feedback` table.
  - [x] **Feedback UX Polish**: Clean bilingual messages ("تم إرسال الملاحظة بنجاح!" / "خطأ، لم يتم إرسال الملاحظة، لا يوجد اتصال بالإنترنت"), removed debug logs from UI.
  - [x] **Feedback Card Design Harmonization**: Expanded dialog width (`DialogProperties(usePlatformDefaultWidth = false)`), single-line chips with no text wrapping, and identical deep teal palette (`#00695C`, `#004D40`, `#E0F2F1`) matching the main screen.
  - [x] **Automatic Dark / Light Mode Contrast**: Dynamic semantic tokens and `isSystemInDarkTheme()` color handling across Hero Notification card, timeline event cards, feedback card, and update modal ensuring high contrast and legibility in both modes.
  - [x] **Remote App Update Check & Announcements**: Query Supabase `app_updates` table on app startup and login with strict version comparison (`latest_version_code > currentVersionCode` or newer semantic `versionName`) to notify users only when a newer release is published.
  - [x] Arabic (default RTL) / English language toggle with persistent preference
  - [x] Custom Work Days config (Default Sat-Thu, Friday dimmed)
  - [x] **Alarm Tones Mixer**: Multi-tone randomizer (1, 2, or 3 tones), audio preview player, and device ringtone picker.
  - [x] **Snooze Duration Settings**: Quick select chips for 3, 5, 10, or 15 minutes.
  - [x] **Grace Period / Late Calculation**: Configurable grace periods for IN (default 15m), OUT/IN (default 15m), and OUT (default 30m) fingerprint events.
  - [x] Battery Optimization whitelist helper & exact alarm permission status card

---

## 5. Summary of Production Readiness & Next Steps

### Current Completed Milestones:
1. **Supabase Integration**: Connected directly to Supabase with real GoTrue Authentication, custom Resend SMTP relay, Postgrest 2-way data synchronization, user feedback, and remote app updates.
2. **Core Business Logic**: All 6 fingerprint schedule engines (8-2, 8-3, 9-3, 10-5, 18-6, 24-6) fully implemented, verified, and tested.
3. **Offline-First Storage**: Local Room database handles schedules, events, event completion (`DONE`), and snooze timestamps (`SNOOZED`).
4. **Alarm & Reminder System**: `AlarmManager` exact alarm scheduling with full-screen lock screen activity, heads-up notifications, and clean audio handling.
5. **Authentication & Account Security**: Email/password authentication, password recovery (Forgot Password), in-app password changes, and permanent account deletion.
6. **Subscription & Entitlements UI**: Active 60-day trial status with days countdown, plus tier selection cards for monthly, annual, and lifetime licenses.
7. **Settings & Customization**: Multi-tone alarm mixer, device ringtone picker, customizable snooze intervals (3, 5, 10, 15m), and grace period calculators for IN/OUT events.
8. **Feedback & Remote Updates**: In-app feedback system and automatic remote APK update checking with Supabase.

