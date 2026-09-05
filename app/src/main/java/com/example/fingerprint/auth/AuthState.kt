package com.example.fingerprint.auth

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data object Loading : AuthState
    data class Authenticated(
        val userId: String,
        val email: String,
        val displayName: String,
        val photoUrl: String? = null,
        val subscriptionStatus: String = "TRIAL",
        val accessToken: String
    ) : AuthState
    data class Error(val message: String) : AuthState
}
