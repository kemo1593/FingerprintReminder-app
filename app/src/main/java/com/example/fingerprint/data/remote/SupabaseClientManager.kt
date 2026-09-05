package com.example.fingerprint.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClientManager {

    private const val FALLBACK_URL = "https://hnwrsxowdrwmoosyjbro.supabase.co/"
    private const val FALLBACK_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhud3JzeG93ZHJ3bW9vc3lqYnJvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU5NDM3ODgsImV4cCI6MjEwMTUxOTc4OH0.aSf2oslOt8QKBjX7G7wbUONKiVa3ro4dZSV2h3n_o64"

    fun getSupabaseUrl(): String {
        return try {
            val url = BuildConfig.SUPABASE_URL
            if (url.isNullOrBlank() || url.contains("your-supabase")) {
                FALLBACK_URL
            } else {
                if (url.endsWith("/")) url else "$url/"
            }
        } catch (e: Exception) {
            FALLBACK_URL
        }
    }

    fun getAnonKey(): String {
        return try {
            val key = BuildConfig.SUPABASE_ANON_KEY
            if (key.isNullOrBlank() || key.contains("your-supabase")) {
                FALLBACK_ANON_KEY
            } else {
                key
            }
        } catch (e: Exception) {
            FALLBACK_ANON_KEY
        }
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val api: SupabaseApi by lazy {
        val baseUrl = getSupabaseUrl()
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApi::class.java)
    }
}
