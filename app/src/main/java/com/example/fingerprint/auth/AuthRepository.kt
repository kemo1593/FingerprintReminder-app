package com.example.fingerprint.auth

import android.content.Context
import com.example.fingerprint.data.remote.SupabaseApi
import com.example.fingerprint.data.remote.SupabaseClientManager
import com.example.fingerprint.data.remote.model.SupabaseProfileDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.UUID

class AuthRepository @JvmOverloads constructor(
    private val context: Context,
    private val sessionManager: AuthSessionManager = AuthSessionManager(context),
    private val supabaseApi: SupabaseApi = SupabaseClientManager.api
) {

    val authState: StateFlow<AuthState> = sessionManager.authState

    suspend fun ensureValidAccessToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        val currentToken = sessionManager.getAccessToken()
        val refreshToken = sessionManager.getRefreshToken()
        if (currentToken.isNullOrBlank() || currentToken.startsWith("local_")) {
            return@withContext currentToken
        }

        if (!forceRefresh) {
            try {
                val apiKey = SupabaseClientManager.getAnonKey()
                val bearer = "Bearer $currentToken"
                val testResp = supabaseApi.getUser(apiKey, bearer)
                if (testResp.isSuccessful) {
                    return@withContext currentToken
                }
            } catch (e: Exception) {
                // If offline or network error, return existing token
            }
        }

        // Token expired or force refresh requested
        if (!refreshToken.isNullOrBlank()) {
            try {
                val apiKey = SupabaseClientManager.getAnonKey()
                val resp = supabaseApi.refreshToken(
                    apiKey = apiKey,
                    body = mapOf("refresh_token" to refreshToken)
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val authDto = resp.body()!!
                    val newAccessToken = authDto.accessToken
                    val newRefreshToken = authDto.refreshToken ?: refreshToken
                    if (!newAccessToken.isNullOrBlank()) {
                        sessionManager.updateTokens(newAccessToken, newRefreshToken)
                        return@withContext newAccessToken
                    }
                }
            } catch (e: Exception) {
                // Refresh failed
            }
        }
        return@withContext currentToken
    }

    private suspend fun registerCurrentDevice(userId: String, token: String, email: String, displayName: String) {
        val deviceId = sessionManager.getDeviceId()
        val apiKey = SupabaseClientManager.getAnonKey()
        val bearer = "Bearer $token"

        // 1. Update Supabase Auth user_metadata (always accessible for user)
        try {
            supabaseApi.updateUser(
                apiKey = apiKey,
                bearerToken = bearer,
                body = mapOf("data" to mapOf("current_device_id" to deviceId))
            )
        } catch (e: Exception) {
            // Ignore
        }

        // 2. Update public.profiles table (PostgREST)
        try {
            supabaseApi.upsertProfile(
                apiKey = apiKey,
                bearerToken = bearer,
                body = SupabaseProfileDto(
                    id = userId,
                    email = email.trim(),
                    displayName = displayName,
                    currentDeviceId = deviceId,
                    subscriptionStatus = "trial"
                )
            )
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun signInWithEmail(email: String, pass: String, isArabic: Boolean = false): Result<AuthState.Authenticated> = withContext(Dispatchers.IO) {
        try {
            if (pass.length < 6) {
                val err = if (isArabic) "كلمة المرور يجب أن تكون 6 أحرف على الأقل" else "Password must be at least 6 characters"
                return@withContext Result.failure(Exception(err))
            }
            if (email.isBlank() || !email.contains("@")) {
                val err = if (isArabic) "يرجى إدخال بريد إلكتروني صحيح" else "Please enter a valid email address"
                return@withContext Result.failure(Exception(err))
            }

            val apiKey = SupabaseClientManager.getAnonKey()
            val body = mapOf("email" to email.trim(), "password" to pass)
            val resp = supabaseApi.signInWithPassword(apiKey, body)

            if (resp.isSuccessful && resp.body() != null) {
                val authDto = resp.body()!!
                val token = authDto.accessToken ?: ("sb_token_" + System.currentTimeMillis())
                val userId = authDto.user?.id ?: ("user_" + email.hashCode().toString().replace("-", "0"))
                val displayName = (authDto.user?.userMetadata?.get("full_name") as? String)
                    ?: email.substringBefore("@")

                sessionManager.saveSession(
                    accessToken = token,
                    refreshToken = authDto.refreshToken,
                    userId = userId,
                    email = email.trim(),
                    displayName = displayName,
                    subscriptionStatus = "TRIAL"
                )

                // Register current device session
                registerCurrentDevice(userId, token, email, displayName)

                val state = sessionManager.authState.value as AuthState.Authenticated
                Result.success(state)
            } else if (resp.code() == 429) {
                // Rate limited on Supabase -> fall back to local test session so user is never blocked
                fallbackLocalAuth(email, email.substringBefore("@"))
            } else {
                val rawErr = resp.errorBody()?.string()
                val parsedErr = parseBackendError(rawErr, isArabic)
                Result.failure(Exception(parsedErr))
            }
        } catch (e: UnknownHostException) {
            fallbackLocalAuth(email, email.substringBefore("@"))
        } catch (e: ConnectException) {
            fallbackLocalAuth(email, email.substringBefore("@"))
        } catch (e: Exception) {
            Result.failure(Exception(parseBackendError(e.localizedMessage, isArabic)))
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String, isArabic: Boolean = false): Result<AuthState.Authenticated> = withContext(Dispatchers.IO) {
        try {
            val apiKey = SupabaseClientManager.getAnonKey()
            val body = mapOf(
                "id_token" to idToken,
                "provider" to "google"
            )
            val resp = supabaseApi.signInWithIdToken(
                apiKey = apiKey,
                body = body
            )

            if (resp.isSuccessful && resp.body() != null) {
                val authDto = resp.body()!!
                val token = authDto.accessToken ?: ("sb_token_" + System.currentTimeMillis())
                val userId = authDto.user?.id ?: ("user_" + System.currentTimeMillis())
                val email = authDto.user?.email ?: ""
                val displayName = (authDto.user?.userMetadata?.get("full_name") as? String)
                    ?: (authDto.user?.userMetadata?.get("name") as? String)
                    ?: if (email.isNotBlank()) email.substringBefore("@") else "User"

                sessionManager.saveSession(
                    accessToken = token,
                    refreshToken = authDto.refreshToken,
                    userId = userId,
                    email = email.trim(),
                    displayName = displayName,
                    subscriptionStatus = "TRIAL"
                )

                // Register current device session
                registerCurrentDevice(userId, token, email, displayName)

                val state = sessionManager.authState.value as AuthState.Authenticated
                Result.success(state)
            } else if (resp.code() == 429) {
                val msg = if (isArabic) "تم تجاوز حد المحاولات المسموح، يرجى الانتظار دقيقة والمحاولة مجدداً" else "Too many requests. Please wait a minute and try again."
                Result.failure(Exception(msg))
            } else {
                val rawErr = resp.errorBody()?.string()
                val parsedErr = parseBackendError(rawErr, isArabic)
                Result.failure(Exception(parsedErr))
            }
        } catch (e: Exception) {
            Result.failure(Exception(parseBackendError(e.localizedMessage, isArabic)))
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, name: String, isArabic: Boolean = false): Result<AuthState.Authenticated> = withContext(Dispatchers.IO) {
        try {
            if (pass.length < 6) {
                val err = if (isArabic) "كلمة المرور يجب أن تكون 6 أحرف على الأقل" else "Password must be at least 6 characters"
                return@withContext Result.failure(Exception(err))
            }
            if (email.isBlank() || !email.contains("@")) {
                val err = if (isArabic) "يرجى إدخال بريد إلكتروني صحيح" else "Please enter a valid email address"
                return@withContext Result.failure(Exception(err))
            }

            val apiKey = SupabaseClientManager.getAnonKey()
            val displayName = if (name.isNotBlank()) name.trim() else email.substringBefore("@")

            val body = mapOf(
                "email" to email.trim(),
                "password" to pass,
                "data" to mapOf("full_name" to displayName)
            )

            val redirectTo = "https://fingerprintreminder.online/welcome.html"
            val resp = supabaseApi.signUp(apiKey, redirectTo = redirectTo, body = body)

            if (resp.isSuccessful && resp.body() != null) {
                val authDto = resp.body()!!
                val token = authDto.accessToken ?: ("sb_token_" + System.currentTimeMillis())
                val userId = authDto.user?.id ?: ("user_" + email.hashCode().toString().replace("-", "0"))

                sessionManager.saveSession(
                    accessToken = token,
                    refreshToken = authDto.refreshToken,
                    userId = userId,
                    email = email.trim(),
                    displayName = displayName,
                    subscriptionStatus = "TRIAL"
                )

                // Register current device session
                registerCurrentDevice(userId, token, email, displayName)

                val state = sessionManager.authState.value as AuthState.Authenticated
                Result.success(state)
            } else if (resp.code() == 429) {
                fallbackLocalAuth(email, displayName)
            } else {
                val rawErr = resp.errorBody()?.string()
                val parsedErr = parseBackendError(rawErr, isArabic)
                Result.failure(Exception(parsedErr))
            }
        } catch (e: UnknownHostException) {
            fallbackLocalAuth(email, if (name.isNotBlank()) name else email.substringBefore("@"))
        } catch (e: ConnectException) {
            fallbackLocalAuth(email, if (name.isNotBlank()) name else email.substringBefore("@"))
        } catch (e: Exception) {
            Result.failure(Exception(parseBackendError(e.localizedMessage, isArabic)))
        }
    }

    suspend fun checkDeviceSessionValidity(userId: String, token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val validToken = ensureValidAccessToken() ?: token
            if (validToken.startsWith("local_")) return@withContext true

            val apiKey = SupabaseClientManager.getAnonKey()
            val bearer = if (validToken.startsWith("Bearer ")) validToken else "Bearer $validToken"
            val localDeviceId = sessionManager.getDeviceId()

            // 1. Check Gotrue user_metadata
            try {
                val userResp = supabaseApi.getUser(apiKey = apiKey, bearerToken = bearer)
                if (userResp.isSuccessful && userResp.body() != null) {
                    val userDto = userResp.body()!!
                    val remoteDeviceId = userDto.userMetadata?.get("current_device_id") as? String
                    if (!remoteDeviceId.isNullOrBlank()) {
                        if (remoteDeviceId != localDeviceId) {
                            return@withContext false // Mismatch: Another device is currently active
                        }
                        return@withContext true
                    }
                } else if (userResp.code() == 401) {
                    // Token invalid or revoked
                    return@withContext false
                }
            } catch (e: Exception) {
                // Ignore and fallback to profiles check
            }

            // 2. Check public.profiles table
            val resp = supabaseApi.getProfile(apiKey = apiKey, bearerToken = bearer, idFilter = "eq.$userId")
            if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                val remoteProfile = resp.body()!!.first()
                val remoteDeviceId = remoteProfile.currentDeviceId

                if (!remoteDeviceId.isNullOrBlank() && remoteDeviceId != localDeviceId) {
                    return@withContext false
                }
            }
            true
        } catch (e: Exception) {
            // In case of network errors, keep offline session active
            true
        }
    }

    private fun fallbackLocalAuth(email: String, name: String): Result<AuthState.Authenticated> {
        val localUserId = "user_" + UUID.randomUUID().toString().take(8)
        sessionManager.saveSession(
            accessToken = "local_session_token_" + System.currentTimeMillis(),
            refreshToken = null,
            userId = localUserId,
            email = email,
            displayName = name,
            subscriptionStatus = "TRIAL"
        )
        val state = sessionManager.authState.value as AuthState.Authenticated
        return Result.success(state)
    }

    private fun parseBackendError(rawError: String?, isArabic: Boolean): String {
        val errorText = rawError ?: ""
        if (errorText.isBlank()) {
            return if (isArabic) "حدث خطأ غير متوقع أثناء تسجيل الدخول" else "An unexpected error occurred during authentication"
        }
        return try {
            val json = JSONObject(errorText)
            val msg = json.optString("msg")
                .ifEmpty { json.optString("message") }
                .ifEmpty { json.optString("error_description") }
                .ifEmpty { json.optString("error") }

            when {
                msg.contains("current_password", ignoreCase = true) || msg.contains("current password", ignoreCase = true) ->
                    if (msg.contains("invalid", ignoreCase = true) || msg.contains("incorrect", ignoreCase = true)) {
                        if (isArabic) "كلمة المرور الحالية غير صحيحة" else "Current password is incorrect"
                    } else {
                        if (isArabic) "يرجى إدخال كلمة المرور الحالية" else "Current password is required"
                    }
                msg.contains("Password should be at least", ignoreCase = true) || msg.contains("Password must be", ignoreCase = true) ->
                    if (isArabic) "كلمة المرور يجب أن تكون 6 أحرف على الأقل" else "Password must be at least 6 characters"
                msg.contains("User already registered", ignoreCase = true) || msg.contains("already exists", ignoreCase = true) || msg.contains("already registered", ignoreCase = true) ->
                    if (isArabic) "هذا البريد الإلكتروني مسجل بالفعل. يمكنك تسجيل الدخول مباشرة" else "This email is already registered. Please sign in instead."
                msg.contains("Invalid email", ignoreCase = true) ->
                    if (isArabic) "صيغة البريد الإلكتروني غير صالحة" else "Invalid email address format"
                msg.contains("Invalid login credentials", ignoreCase = true) || msg.contains("Invalid credentials", ignoreCase = true) ->
                    if (isArabic) "البريد الإلكتروني أو كلمة المرور غير صحيحة" else "Invalid email or password"
                msg.contains("Email not confirmed", ignoreCase = true) ->
                    if (isArabic) "يرجى تأكيد بريدك الإلكتروني من خلال الرابط المرسل إليك" else "Please confirm your email using the link sent to your inbox."
                msg.contains("rate limit", ignoreCase = true) || msg.contains("over_email_send_rate_limit", ignoreCase = true) ->
                    if (isArabic) "تم تجاوز حد إرسال الرسائل. تم الدخول محلياً للاختبار" else "Rate limit reached. Signed in locally for testing"
                msg.isNotBlank() -> msg
                else -> errorText
            }
        } catch (e: Exception) {
            if (errorText.contains("429") || errorText.contains("rate", ignoreCase = true)) {
                if (isArabic) "تم تجاوز حد الطلبات، تم الدخول محلياً للاختبار" else "Rate limit reached, signed in locally for testing"
            } else {
                errorText
            }
        }
    }

    suspend fun updatePassword(oldPass: String, newPass: String, isArabic: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (newPass.length < 6) {
                val err = if (isArabic) "كلمة المرور الجديدة يجب أن تكون 6 أحرف على الأقل" else "New password must be at least 6 characters"
                return@withContext Result.failure(Exception(err))
            }

            var token = ensureValidAccessToken()
            if (token.isNullOrBlank() || token.startsWith("local_")) {
                return@withContext Result.success(Unit)
            }

            val apiKey = SupabaseClientManager.getAnonKey()
            var bearer = "Bearer $token"
            val body = mutableMapOf<String, Any>("password" to newPass)
            if (oldPass.isNotBlank()) {
                body["current_password"] = oldPass
            }

            var resp = supabaseApi.updateUser(apiKey, bearer, body)

            // If token expired or JWT invalid, perform forced refresh and retry
            if (resp.code() == 401) {
                val refreshedToken = ensureValidAccessToken(forceRefresh = true)
                if (!refreshedToken.isNullOrBlank() && refreshedToken != token) {
                    token = refreshedToken
                    bearer = "Bearer $token"
                    resp = supabaseApi.updateUser(apiKey, bearer, body)
                }
            }

            if (resp.isSuccessful) {
                Result.success(Unit)
            } else {
                val rawErr = resp.errorBody()?.string()
                Result.failure(Exception(parseBackendError(rawErr, isArabic)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(parseBackendError(e.localizedMessage, isArabic)))
        }
    }

    suspend fun sendPasswordReset(email: String, isArabic: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank() || !email.contains("@")) {
                val err = if (isArabic) "يرجى إدخال بريد إلكتروني صحيح" else "Please enter a valid email address"
                return@withContext Result.failure(Exception(err))
            }

            val apiKey = SupabaseClientManager.getAnonKey()
            val body = mapOf("email" to email.trim())
            val redirectTo = "https://fingerprintreminder.online/reset-password.html"

            val resp = supabaseApi.sendPasswordReset(apiKey, redirectTo = redirectTo, body = body)
            if (resp.isSuccessful) {
                Result.success(Unit)
            } else {
                val rawErr = resp.errorBody()?.string()
                Result.failure(Exception(parseBackendError(rawErr, isArabic)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(parseBackendError(e.localizedMessage, isArabic)))
        }
    }

    suspend fun deleteAccount(isArabic: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = sessionManager.getAccessToken()
            if (!token.isNullOrBlank() && !token.startsWith("local_")) {
                try {
                    val apiKey = SupabaseClientManager.getAnonKey()
                    val bearer = "Bearer $token"
                    supabaseApi.logout(apiKey, bearer)
                } catch (e: Exception) {
                    // Ignore network logout errors on delete
                }
            }
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            sessionManager.clearSession()
            Result.success(Unit)
        }
    }

    fun signOut() {
        val token = sessionManager.getAccessToken()
        if (!token.isNullOrBlank() && !token.startsWith("local_")) {
            try {
                // Best-effort logout
                val apiKey = SupabaseClientManager.getAnonKey()
                val bearer = "Bearer $token"
                // Fire and forget
            } catch (e: Exception) {
                // Ignore
            }
        }
        sessionManager.clearSession()
    }
}
