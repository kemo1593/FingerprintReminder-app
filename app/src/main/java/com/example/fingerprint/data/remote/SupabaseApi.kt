package com.example.fingerprint.data.remote

import com.example.fingerprint.data.remote.model.SupabaseAppUpdateDto
import com.example.fingerprint.data.remote.model.SupabaseAuthResponseDto
import com.example.fingerprint.data.remote.model.SupabaseFeedbackDto
import com.example.fingerprint.data.remote.model.SupabaseProfileDto
import com.example.fingerprint.data.remote.model.SupabaseScheduleDto
import com.example.fingerprint.data.remote.model.SupabaseScheduleEventDto
import com.example.fingerprint.data.remote.model.SupabaseUserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface SupabaseApi {

    // --- AUTH / GOTRUE ENDPOINTS ---

    @Headers("Content-Type: application/json")
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Query("redirect_to") redirectTo: String? = null,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<SupabaseAuthResponseDto>

    @Headers("Content-Type: application/json")
    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(
        @Header("apikey") apiKey: String,
        @Body body: Map<String, String>
    ): Response<SupabaseAuthResponseDto>

    @Headers("Content-Type: application/json")
    @POST("auth/v1/token?grant_type=id_token")
    suspend fun signInWithIdToken(
        @Header("apikey") apiKey: String,
        @Body body: Map<String, String>
    ): Response<SupabaseAuthResponseDto>

    @Headers("Content-Type: application/json")
    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Header("apikey") apiKey: String,
        @Body body: Map<String, String>
    ): Response<SupabaseAuthResponseDto>

    @Headers("Content-Type: application/json")
    @GET("auth/v1/user")
    suspend fun getUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String
    ): Response<SupabaseUserDto>

    @Headers("Content-Type: application/json")
    @POST("auth/v1/recover")
    suspend fun sendPasswordReset(
        @Header("apikey") apiKey: String,
        @Query("redirect_to") redirectTo: String? = null,
        @Body body: Map<String, String>
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @PUT("auth/v1/user")
    suspend fun updateUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<SupabaseUserDto>

    @Headers("Content-Type: application/json")
    @POST("auth/v1/logout")
    suspend fun logout(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String
    ): Response<Unit>

    // --- POSTGREST REST / DATABASE ENDPOINTS ---

    @Headers("Content-Type: application/json")
    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body body: SupabaseProfileDto
    ): Response<Unit>

    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String
    ): Response<List<SupabaseProfileDto>>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/schedules")
    suspend fun upsertSchedules(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Query("on_conflict") onConflict: String = "id",
        @Body body: List<SupabaseScheduleDto>
    ): Response<Unit>

    @GET("rest/v1/schedules")
    suspend fun getSchedules(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("user_id") userIdFilter: String
    ): Response<List<SupabaseScheduleDto>>

    @DELETE("rest/v1/schedules")
    suspend fun deleteSchedules(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("user_id") userIdFilter: String
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/schedule_events")
    suspend fun upsertEvents(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Query("on_conflict") onConflict: String = "id",
        @Body body: List<SupabaseScheduleEventDto>
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/schedule_events")
    suspend fun insertEvents(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body body: List<SupabaseScheduleEventDto>
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @PATCH("rest/v1/schedule_events")
    suspend fun updateEvent(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("id") idFilter: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Unit>

    @GET("rest/v1/schedule_events")
    suspend fun getEvents(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("user_id") userIdFilter: String
    ): Response<List<SupabaseScheduleEventDto>>

    @DELETE("rest/v1/schedule_events")
    suspend fun deleteEvents(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("user_id") userIdFilter: String
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/feedback")
    suspend fun submitFeedback(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Body body: SupabaseFeedbackDto
    ): Response<Unit>

    @Headers("Accept: application/json")
    @GET("rest/v1/app_updates")
    suspend fun getLatestAppUpdate(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String,
        @Query("select") select: String = "*",
        @Query("order", encoded = true) order: String = "latest_version_code.desc",
        @Query("limit") limit: Int = 1
    ): Response<List<SupabaseAppUpdateDto>>
}
