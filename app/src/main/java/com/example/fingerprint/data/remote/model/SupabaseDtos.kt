package com.example.fingerprint.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- SUPABASE AUTH DTOS ---

@JsonClass(generateAdapter = true)
data class SupabaseUserDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any>? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponseDto(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "user") val user: SupabaseUserDto? = null
)

// --- SUPABASE PROFILE DTO ---

@JsonClass(generateAdapter = true)
data class SupabaseProfileDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "email") val email: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "trial_start_timestamp") val trialStartTimestamp: Long? = null,
    @Json(name = "subscription_status") val subscriptionStatus: String? = "trial",
    @Json(name = "current_device_id") val currentDeviceId: String? = null,
    @Json(name = "last_active_at") val lastActiveAt: String? = null
)

// --- SUPABASE SCHEDULE & EVENT DTOS ---

@JsonClass(generateAdapter = true)
data class SupabaseScheduleDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "system_type") val systemType: String = "",
    @Json(name = "first_entry_date") val firstEntryDate: String = "",
    @Json(name = "first_entry_time") val firstEntryTime: String = "",
    @Json(name = "last_entry_date") val lastEntryDate: String = "",
    @Json(name = "last_entry_time") val lastEntryTime: String = "",
    @Json(name = "created_at") val createdAt: Any? = null,
    @Json(name = "is_active") val isActive: Boolean? = true,
    @Json(name = "sync_timestamp") val syncTimestamp: Any? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseScheduleEventDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "schedule_id") val scheduleId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "scheduled_date_time") val scheduledDateTime: String = "",
    @Json(name = "event_type") val eventType: String = "",
    @Json(name = "status") val status: String = "",
    @Json(name = "completed_at") val completedAt: String? = null,
    @Json(name = "snoozed_until") val snoozedUntil: String? = null,
    @Json(name = "sync_timestamp") val syncTimestamp: Any? = null
)

// --- SUPABASE FEEDBACK DTO ---

@JsonClass(generateAdapter = true)
data class SupabaseFeedbackDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "user_email") val userEmail: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "rating") val rating: Int = 5,
    @Json(name = "message") val message: String = ""
)

// --- SUPABASE APP UPDATE DTO ---

@JsonClass(generateAdapter = true)
data class SupabaseAppUpdateDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "latest_version_code") val latestVersionCode: Int = 1,
    @Json(name = "latest_version_name") val latestVersionName: String = "1.0.0",
    @Json(name = "download_url") val downloadUrl: String = "",
    @Json(name = "release_notes_ar") val releaseNotesAr: String? = null,
    @Json(name = "release_notes_en") val releaseNotesEn: String? = null,
    @Json(name = "is_mandatory") val isMandatory: Boolean = false
)
