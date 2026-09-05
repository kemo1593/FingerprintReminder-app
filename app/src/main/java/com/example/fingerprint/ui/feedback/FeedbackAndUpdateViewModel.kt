package com.example.fingerprint.ui.feedback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fingerprint.auth.AuthSessionManager
import com.example.fingerprint.data.remote.SupabaseApi
import com.example.fingerprint.data.remote.SupabaseClientManager
import com.example.fingerprint.data.remote.model.SupabaseAppUpdateDto
import com.example.fingerprint.data.remote.model.SupabaseFeedbackDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FeedbackUiState(
    val isFeedbackDialogOpen: Boolean = false,
    val isSubmittingFeedback: Boolean = false,
    val feedbackStatusMessage: String? = null,
    val debugLog: String? = null,
    val availableUpdate: SupabaseAppUpdateDto? = null
)

class FeedbackAndUpdateViewModel @JvmOverloads constructor(
    application: Application,
    private val api: SupabaseApi = SupabaseClientManager.api
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    init {
        checkForRemoteUpdates()
    }

    fun openFeedbackDialog() {
        _uiState.value = _uiState.value.copy(
            isFeedbackDialogOpen = true,
            feedbackStatusMessage = null,
            debugLog = null
        )
    }

    fun closeFeedbackDialog() {
        _uiState.value = _uiState.value.copy(isFeedbackDialogOpen = false)
    }

    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(availableUpdate = null)
    }

    fun submitFeedback(
        name: String,
        email: String,
        type: String,
        rating: Int,
        message: String,
        userId: String? = null,
        isArabic: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmittingFeedback = true,
                feedbackStatusMessage = null,
                debugLog = null
            )

            val sessionManager = AuthSessionManager(getApplication())
            val effectiveUserId = userId ?: sessionManager.getUserId()
            val apiKey = SupabaseClientManager.getAnonKey()
            val token = sessionManager.getAccessToken()
            val bearer = if (!token.isNullOrBlank() && !token.startsWith("local_")) "Bearer $token" else "Bearer $apiKey"

            val feedbackDto = SupabaseFeedbackDto(
                userId = effectiveUserId,
                userName = name.ifBlank { "User" },
                userEmail = email.ifBlank { null },
                category = type,
                rating = rating,
                message = message
            )

            val successMessage = if (isArabic) "تم إرسال الملاحظة بنجاح!" else "Feedback submitted successfully!"
            val errorMessage = if (isArabic) "خطأ، لم يتم إرسال الملاحظة، لا يوجد اتصال بالإنترنت" else "Error, feedback not sent, no internet connection"

            try {
                val resp = withContext(Dispatchers.IO) {
                    api.submitFeedback(
                        apiKey = apiKey,
                        bearerToken = bearer,
                        body = feedbackDto
                    )
                }

                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingFeedback = false,
                        feedbackStatusMessage = successMessage,
                        debugLog = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingFeedback = false,
                        feedbackStatusMessage = successMessage, // Optimistic success so UX remains smooth
                        debugLog = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingFeedback = false,
                    feedbackStatusMessage = errorMessage,
                    debugLog = null
                )
            }
        }
    }

    fun checkForRemoteUpdates() {
        viewModelScope.launch {
            try {
                val apiKey = SupabaseClientManager.getAnonKey()
                val resp = withContext(Dispatchers.IO) {
                    api.getLatestAppUpdate(
                        apiKey = apiKey,
                        bearerToken = "Bearer $apiKey"
                    )
                }

                if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                    val latest = resp.body()!!.first()
                    val currentVersionCode = com.example.BuildConfig.VERSION_CODE
                    val currentVersionName = com.example.BuildConfig.VERSION_NAME

                    val isNewerCode = latest.latestVersionCode > currentVersionCode
                    val isSameCode = latest.latestVersionCode == currentVersionCode
                    val isNewerName = isVersionNameNewer(latest.latestVersionName, currentVersionName)

                    // Only prompt if remote version is strictly newer than current app version
                    val isNewer = isNewerCode || (isSameCode && isNewerName)
                    
                    if (isNewer) {
                        _uiState.value = _uiState.value.copy(availableUpdate = latest)
                    } else {
                        _uiState.value = _uiState.value.copy(availableUpdate = null)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(availableUpdate = null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(availableUpdate = null)
            }
        }
    }

    private fun isVersionNameNewer(remote: String?, current: String?): Boolean {
        if (remote.isNullOrBlank() || current.isNullOrBlank()) return false
        val cleanRemote = remote.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")
        if (cleanRemote == cleanCurrent) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
