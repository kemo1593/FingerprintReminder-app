package com.example.fingerprint.ui.settings

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fingerprint.settings.AppSettings
import com.example.fingerprint.settings.AppSettingsManager
import com.example.fingerprint.settings.DeviceRingtone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = AppSettingsManager.getInstance(application)
    val settingsState: StateFlow<AppSettings> = settingsManager.settingsState

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isRingtonePickerOpen = MutableStateFlow(false)
    val isRingtonePickerOpen: StateFlow<Boolean> = _isRingtonePickerOpen.asStateFlow()

    private val _availableRingtones = MutableStateFlow<List<DeviceRingtone>>(emptyList())
    val availableRingtones: StateFlow<List<DeviceRingtone>> = _availableRingtones.asStateFlow()

    private val _currentlyPlayingUri = MutableStateFlow<String?>(null)
    val currentlyPlayingUri: StateFlow<String?> = _currentlyPlayingUri.asStateFlow()

    private var previewMediaPlayer: MediaPlayer? = null
    private var previewRingtone: Ringtone? = null

    init {
        loadDeviceRingtones()
    }

    fun loadDeviceRingtones() {
        viewModelScope.launch(Dispatchers.IO) {
            val ringtones = AppSettingsManager.getAvailableDeviceRingtones(getApplication())
            _availableRingtones.value = ringtones
        }
    }

    fun openSettings() {
        _isSettingsOpen.value = true
        loadDeviceRingtones()
    }

    fun closeSettings() {
        stopPreview()
        _isRingtonePickerOpen.value = false
        _isSettingsOpen.value = false
    }

    fun openRingtonePicker() {
        _isRingtonePickerOpen.value = true
    }

    fun closeRingtonePicker() {
        stopPreview()
        _isRingtonePickerOpen.value = false
    }

    fun setAlarmMixCount(count: Int) {
        settingsManager.setAlarmMixCount(count)
    }

    fun toggleRingtoneSelection(uriString: String) {
        settingsManager.toggleRingtoneSelection(uriString)
    }

    fun setSnoozeDurationMinutes(mins: Int) {
        settingsManager.setSnoozeDurationMinutes(mins)
    }

    fun setGraceMinutesIn(mins: Int) {
        settingsManager.setGraceMinutesIn(mins)
    }

    fun setGraceMinutesOutIn(mins: Int) {
        settingsManager.setGraceMinutesOutIn(mins)
    }

    fun setGraceMinutesOut(mins: Int) {
        settingsManager.setGraceMinutesOut(mins)
    }

    fun toggleAudioPreview(uriString: String) {
        if (_currentlyPlayingUri.value == uriString) {
            stopPreview()
        } else {
            playPreview(uriString)
        }
    }

    fun playPreview(uriString: String) {
        stopPreview()
        val context: Context = getApplication()
        try {
            val uri = Uri.parse(uriString)
            previewMediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener {
                    _currentlyPlayingUri.value = null
                }
                prepare()
                start()
            }
            _currentlyPlayingUri.value = uriString
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val uri = Uri.parse(uriString)
                previewRingtone = RingtoneManager.getRingtone(context, uri)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = false
                    }
                    play()
                }
                _currentlyPlayingUri.value = uriString
            } catch (ex: Exception) {
                ex.printStackTrace()
                _currentlyPlayingUri.value = null
            }
        }
    }

    fun stopPreview() {
        try {
            previewMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
            previewMediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            previewRingtone?.stop()
            previewRingtone = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _currentlyPlayingUri.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }
}
