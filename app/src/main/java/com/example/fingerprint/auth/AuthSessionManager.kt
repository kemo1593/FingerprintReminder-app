package com.example.fingerprint.auth

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthSessionManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fingerprint_auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(getInitialState())
    val authState: StateFlow<AuthState> = _authState

    fun getDeviceId(): String {
        var deviceId = prefs.getString("device_unique_id", null)
        if (deviceId.isNullOrEmpty()) {
            deviceId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                null
            }
            if (deviceId.isNullOrEmpty() || deviceId == "9774d56d682e549c") {
                deviceId = UUID.randomUUID().toString()
            }
            prefs.edit().putString("device_unique_id", deviceId).apply()
        }
        return deviceId
    }

    fun isLoggedIn(): Boolean {
        val token = getAccessToken()
        val userId = getUserId()
        return !token.isNullOrEmpty() && !userId.isNullOrEmpty()
    }

    private fun getInitialState(): AuthState {
        val accessToken = prefs.getString("access_token", null)
        val userId = prefs.getString("user_id", null)
        val email = prefs.getString("email", null)
        val displayName = prefs.getString("display_name", null)
        val photoUrl = prefs.getString("photo_url", null)
        val subStatus = prefs.getString("sub_status", "TRIAL") ?: "TRIAL"

        return if (!accessToken.isNullOrEmpty() && !userId.isNullOrEmpty()) {
            AuthState.Authenticated(
                userId = userId,
                email = email ?: "user@example.com",
                displayName = displayName ?: (email?.substringBefore("@") ?: "User"),
                photoUrl = photoUrl,
                subscriptionStatus = subStatus,
                accessToken = accessToken
            )
        } else {
            AuthState.Unauthenticated
        }
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        email: String,
        displayName: String,
        photoUrl: String? = null,
        subscriptionStatus: String = "TRIAL"
    ) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putString("user_id", userId)
            .putString("email", email)
            .putString("display_name", displayName)
            .putString("photo_url", photoUrl)
            .putString("sub_status", subscriptionStatus)
            .apply()

        _authState.value = AuthState.Authenticated(
            userId = userId,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            subscriptionStatus = subscriptionStatus,
            accessToken = accessToken
        )
    }

    fun updateTokens(accessToken: String, refreshToken: String? = null) {
        val editor = prefs.edit().putString("access_token", accessToken)
        if (!refreshToken.isNullOrBlank()) {
            editor.putString("refresh_token", refreshToken)
        }
        editor.apply()
        val current = _authState.value
        if (current is AuthState.Authenticated) {
            _authState.value = current.copy(accessToken = accessToken)
        }
    }

    fun updateSubscriptionStatus(status: String) {
        prefs.edit().putString("sub_status", status).apply()
        val current = _authState.value
        if (current is AuthState.Authenticated) {
            _authState.value = current.copy(subscriptionStatus = status)
        }
    }

    fun clearSession() {
        val deviceId = getDeviceId()
        val lastSynced = getLastSyncedUserId()
        prefs.edit().clear().apply()
        prefs.edit().putString("device_unique_id", deviceId).apply()
        if (lastSynced != null) {
            prefs.edit().putString("last_synced_user_id", lastSynced).apply()
        }
        _authState.value = AuthState.Unauthenticated
    }

    fun getLastSyncedUserId(): String? = prefs.getString("last_synced_user_id", null)

    fun setLastSyncedUserId(userId: String?) {
        if (userId != null) {
            prefs.edit().putString("last_synced_user_id", userId).apply()
        } else {
            prefs.edit().remove("last_synced_user_id").apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun getUserId(): String? = prefs.getString("user_id", null)
}
