package com.example.fingerprint.settings

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceRingtone(
    val id: String,
    val title: String,
    val uriString: String
)

data class AppSettings(
    val alarmMixCount: Int = 5,
    val selectedRingtoneUris: Set<String> = emptySet(),
    val snoozeDurationMinutes: Int = 5,
    val graceMinutesIn: Int = 0,
    val graceMinutesOutIn: Int = 15,
    val graceMinutesOut: Int = 30
)

class AppSettingsManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fingerprint_app_settings", Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(loadSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    private fun loadSettings(): AppSettings {
        val mixCount = prefs.getInt("alarm_mix_count", 5)
        val selectedUris = prefs.getStringSet("selected_ringtone_uris", emptySet()) ?: emptySet()
        val snoozeMins = prefs.getInt("snooze_duration_minutes", 5)
        val graceIn = prefs.getInt("grace_minutes_in", 0)
        val graceOutIn = prefs.getInt("grace_minutes_out_in", 15)
        val graceOut = prefs.getInt("grace_minutes_out", 30)

        return AppSettings(
            alarmMixCount = mixCount,
            selectedRingtoneUris = selectedUris,
            snoozeDurationMinutes = snoozeMins,
            graceMinutesIn = graceIn,
            graceMinutesOutIn = graceOutIn,
            graceMinutesOut = graceOut
        )
    }

    fun setAlarmMixCount(count: Int) {
        val validCount = count.coerceIn(1, 20)
        // If current selection is more than the new limit, trim to validCount
        val currentUris = _settingsState.value.selectedRingtoneUris
        val trimmedUris = if (currentUris.size > validCount) {
            currentUris.take(validCount).toSet()
        } else {
            currentUris
        }

        prefs.edit()
            .putInt("alarm_mix_count", validCount)
            .putStringSet("selected_ringtone_uris", trimmedUris)
            .apply()

        _settingsState.value = _settingsState.value.copy(
            alarmMixCount = validCount,
            selectedRingtoneUris = trimmedUris
        )
    }

    fun toggleRingtoneSelection(uriString: String) {
        val current = _settingsState.value.selectedRingtoneUris.toMutableSet()
        val limit = _settingsState.value.alarmMixCount

        if (current.contains(uriString)) {
            current.remove(uriString)
        } else {
            if (current.size < limit) {
                current.add(uriString)
            }
        }

        prefs.edit().putStringSet("selected_ringtone_uris", current).apply()
        _settingsState.value = _settingsState.value.copy(selectedRingtoneUris = current)
    }

    fun setSnoozeDurationMinutes(minutes: Int) {
        val valid = if (minutes == 3) 3 else 5
        prefs.edit().putInt("snooze_duration_minutes", valid).apply()
        _settingsState.value = _settingsState.value.copy(snoozeDurationMinutes = valid)
    }

    fun setGraceMinutesIn(minutes: Int) {
        val valid = minutes.coerceAtLeast(0)
        prefs.edit().putInt("grace_minutes_in", valid).apply()
        _settingsState.value = _settingsState.value.copy(graceMinutesIn = valid)
    }

    fun setGraceMinutesOutIn(minutes: Int) {
        val valid = minutes.coerceAtLeast(0)
        prefs.edit().putInt("grace_minutes_out_in", valid).apply()
        _settingsState.value = _settingsState.value.copy(graceMinutesOutIn = valid)
    }

    fun setGraceMinutesOut(minutes: Int) {
        val valid = minutes.coerceAtLeast(0)
        prefs.edit().putInt("grace_minutes_out", valid).apply()
        _settingsState.value = _settingsState.value.copy(graceMinutesOut = valid)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppSettingsManager? = null

        fun getInstance(context: Context): AppSettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Queries the Android system ringtones and alarm sounds.
         */
        fun getAvailableDeviceRingtones(context: Context): List<DeviceRingtone> {
            val list = mutableListOf<DeviceRingtone>()
            val seenUris = mutableSetOf<String>()

            // 1. Query Phone Ringtones first
            try {
                val ringtoneManager = RingtoneManager(context).apply {
                    setType(RingtoneManager.TYPE_RINGTONE)
                }
                val cursor = ringtoneManager.cursor
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val pos = cursor.position
                    val uri = ringtoneManager.getRingtoneUri(pos)
                    if (uri != null && seenUris.add(uri.toString())) {
                        list.add(
                            DeviceRingtone(
                                id = "ringtone_$pos",
                                title = title ?: "Ringtone #${pos + 1}",
                                uriString = uri.toString()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Query Alarm Sounds
            try {
                val alarmManager = RingtoneManager(context).apply {
                    setType(RingtoneManager.TYPE_ALARM)
                }
                val cursor = alarmManager.cursor
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val pos = cursor.position
                    val uri = alarmManager.getRingtoneUri(pos)
                    if (uri != null && seenUris.add(uri.toString())) {
                        list.add(
                            DeviceRingtone(
                                id = "alarm_$pos",
                                title = title ?: "Alarm Sound #${pos + 1}",
                                uriString = uri.toString()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fallback default if system query returned empty
            if (list.isEmpty()) {
                val defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val defaultAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val defaultNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                defaultRingtone?.let {
                    list.add(DeviceRingtone("default_ringtone", "Default Phone Ringtone", it.toString()))
                }
                defaultAlarm?.let {
                    list.add(DeviceRingtone("default_alarm", "Default Alarm Sound", it.toString()))
                }
                defaultNotification?.let {
                    list.add(DeviceRingtone("default_notification", "Default Notification Tone", it.toString()))
                }
            }

            return list
        }

        /**
         * Selects a random alarm URI based on user settings selection or system pool.
         */
        fun getRandomAlarmSoundUri(context: Context): Uri {
            val prefs = context.getSharedPreferences("fingerprint_app_settings", Context.MODE_PRIVATE)
            val selectedUris = prefs.getStringSet("selected_ringtone_uris", emptySet()) ?: emptySet()

            if (selectedUris.isNotEmpty()) {
                val chosenUriString = selectedUris.random()
                try {
                    return Uri.parse(chosenUriString)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback: pick randomly from available device ringtones
            val available = getAvailableDeviceRingtones(context)
            if (available.isNotEmpty()) {
                val mixCount = prefs.getInt("alarm_mix_count", 5)
                val pool = available.take(mixCount)
                val picked = pool.random()
                return Uri.parse(picked.uriString)
            }

            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }
}
