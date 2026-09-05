package com.example.fingerprint.ui.auth

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fingerprint.alarm.AlarmScheduler
import com.example.fingerprint.auth.AuthRepository
import com.example.fingerprint.auth.AuthState
import com.example.fingerprint.data.local.FingerprintDatabase
import com.example.fingerprint.data.remote.SupabaseSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AuthUiState(
    val authState: AuthState = AuthState.Unauthenticated,
    val showAuthDialog: Boolean = false,
    val isSignUpMode: Boolean = false,
    val emailInput: String = "",
    val passwordInput: String = "",
    val nameInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val syncStatusMessage: String? = null,
    val isSyncing: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val db = FingerprintDatabase.getInstance(application)
    private val syncService = SupabaseSyncService(
        context = application,
        dao = db.fingerprintDao()
    )
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            syncInBackground()
        }
    }

    init {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                val isUnauth = state !is AuthState.Authenticated
                _uiState.value = _uiState.value.copy(
                    authState = state,
                    showAuthDialog = if (isUnauth) true else _uiState.value.showAuthDialog
                )
                if (state is AuthState.Authenticated) {
                    performSync(state.userId, state.accessToken)
                }
            }
        }

        // Periodic background check for single-device session enforcement
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(15_000)
                val current = authRepository.authState.value
                if (current is AuthState.Authenticated) {
                    val isValid = authRepository.checkDeviceSessionValidity(current.userId, current.accessToken)
                    if (!isValid) {
                        AlarmScheduler.cancelAllAlarms(getApplication())
                        authRepository.signOut()
                        _uiState.value = _uiState.value.copy(
                            showAuthDialog = true,
                            syncStatusMessage = "تم تسجيل الخروج لأن الحساب مفتوح على جهاز آخر"
                        )
                    }
                }
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Ignore if registration is not permitted
        }
    }

    fun checkActiveDeviceSession() {
        val currentState = authRepository.authState.value
        if (currentState is AuthState.Authenticated) {
            viewModelScope.launch {
                val isSessionValid = authRepository.checkDeviceSessionValidity(currentState.userId, currentState.accessToken)
                if (!isSessionValid) {
                    AlarmScheduler.cancelAllAlarms(getApplication())
                    authRepository.signOut()
                    _uiState.value = _uiState.value.copy(
                        showAuthDialog = true,
                        syncStatusMessage = "تم تسجيل الخروج لأن الحساب مفتوح على جهاز آخر"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun openAuthDialog() {
        _uiState.value = _uiState.value.copy(
            showAuthDialog = true,
            errorMessage = null,
            syncStatusMessage = null
        )
    }

    fun closeAuthDialog() {
        if (_uiState.value.authState is AuthState.Authenticated) {
            _uiState.value = _uiState.value.copy(showAuthDialog = false)
        }
    }

    fun toggleSignUpMode(isSignUp: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = isSignUp,
            errorMessage = null
        )
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(emailInput = email)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(passwordInput = password)
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(nameInput = name)
    }

    fun submitEmailAuth(isArabic: Boolean = true) {
        val email = _uiState.value.emailInput.trim()
        val pass = _uiState.value.passwordInput.trim()
        val name = _uiState.value.nameInput.trim()

        if (email.isEmpty() || pass.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = if (isArabic) "الرجاء إدخال البريد الإلكتروني وكلمة المرور" else "Please enter email and password"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = if (_uiState.value.isSignUpMode) {
                authRepository.signUpWithEmail(email, pass, if (name.isEmpty()) email.substringBefore("@") else name, isArabic)
            } else {
                authRepository.signInWithEmail(email, pass, isArabic)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Authentication failed")
            }.onSuccess {
                _uiState.value = _uiState.value.copy(showAuthDialog = false)
                syncInBackground()
            }
        }
    }

    fun signInWithGoogleIdToken(idToken: String, isArabic: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithGoogleIdToken(idToken, isArabic)
            _uiState.value = _uiState.value.copy(isLoading = false)
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage ?: "Google Sign-In failed")
            }.onSuccess {
                _uiState.value = _uiState.value.copy(showAuthDialog = false)
                syncInBackground()
            }
        }
    }

    fun setErrorMessage(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message, isLoading = false)
    }

    fun syncInBackground() {
        val currentState = authRepository.authState.value
        if (currentState is AuthState.Authenticated) {
            performSync(currentState.userId, currentState.accessToken)
        }
    }

    private fun performSync(userId: String, accessToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncStatusMessage = null)

            // Check if this device is still the authorized active session for this account
            val isSessionValid = authRepository.checkDeviceSessionValidity(userId, accessToken)
            if (!isSessionValid) {
                // Another device logged in: Terminate session on this device and cancel alarms
                AlarmScheduler.cancelAllAlarms(getApplication())
                authRepository.signOut()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    showAuthDialog = true,
                    syncStatusMessage = "تم تسجيل الخروج لأن الحساب مفتوح على جهاز آخر"
                )
                return@launch
            }

            val res = syncService.sync(userId, accessToken)
            val allEvents = withContext(Dispatchers.IO) {
                db.fingerprintDao().getAllEventsDirect()
            }
            if (allEvents.isNotEmpty()) {
                AlarmScheduler.scheduleAllAlarms(getApplication(), allEvents.map { it.toDomain() })
            }

            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = if (res.isSuccess) "تمت المزامنة السحابية بنجاح ✨" else "تم الحفظ محلياً"
            )
        }
    }

    fun changePassword(oldPass: String, newPass: String, isArabic: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.updatePassword(oldPass, newPass, isArabic)
            res.onSuccess {
                onResult(true, if (isArabic) "تم تغيير كلمة المرور بنجاح" else "Password changed successfully")
            }.onFailure { e ->
                onResult(false, e.localizedMessage ?: (if (isArabic) "فشل تغيير كلمة المرور" else "Failed to change password"))
            }
        }
    }

    fun sendPasswordReset(email: String, isArabic: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = authRepository.sendPasswordReset(email, isArabic)
            res.onSuccess {
                onResult(true, if (isArabic) "تم إرسال رابط استعادة كلمة المرور إلى $email" else "Password reset link sent to $email")
            }.onFailure { e ->
                onResult(false, e.localizedMessage ?: (if (isArabic) "تعذر إرسال رابط الاستعادة" else "Failed to send reset link"))
            }
        }
    }

    fun deleteAccount(isArabic: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val currentState = authRepository.authState.value
            if (currentState is AuthState.Authenticated) {
                syncService.sync(currentState.userId, currentState.accessToken, isExplicitDeletion = true)
            }
            val res = authRepository.deleteAccount(isArabic)
            res.onSuccess {
                AlarmScheduler.cancelAllAlarms(getApplication())
                db.fingerprintDao().clearScheduleConfigs()
                db.fingerprintDao().clearAllEvents()
                _uiState.value = _uiState.value.copy(
                    showAuthDialog = true,
                    syncStatusMessage = if (isArabic) "تم حذف الحساب بنجاح" else "Account deleted successfully"
                )
                onResult(true, if (isArabic) "تم حذف الحساب بنجاح" else "Account deleted successfully")
            }.onFailure { e ->
                onResult(false, e.localizedMessage ?: (if (isArabic) "تعذر حذف الحساب" else "Failed to delete account"))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            AlarmScheduler.cancelAllAlarms(getApplication())
            // Preserve local schedule and events in local phone memory so that
            // when logging into an account with no remote schedule, it syncs to the account.
            authRepository.signOut()
            _uiState.value = _uiState.value.copy(
                showAuthDialog = true,
                syncStatusMessage = "تم تسجيل الخروج"
            )
        }
    }
}
