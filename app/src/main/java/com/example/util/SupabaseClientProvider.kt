package com.example.util

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * SupabaseClientProvider Singleton
 * Centrally manages Supabase project credentials and provides standard REST & PostgREST configuration
 * to migrate data operations away from Firestore.
 */
object SupabaseClientProvider {

    private const val TAG = "SupabaseClientProvider"

    const val SUPABASE_URL = "https://imhcbgpvjwersbnlgwzq.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_U1jQSTm-S9YNQxx7RHPl-Q_w_Up-RBe"

    val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun buildPostgrestRequest(
        tableOrEndpoint: String,
        method: String,
        jsonBody: String? = null,
        preferHeader: String? = null
    ): Request {
        val url = if (tableOrEndpoint.startsWith("http")) {
            tableOrEndpoint
        } else {
            "$SUPABASE_URL/rest/v1/$tableOrEndpoint"
        }

        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")

        if (preferHeader != null) {
            builder.addHeader("Prefer", preferHeader)
        }

        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post((jsonBody ?: "{}").toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch((jsonBody ?: "{}").toRequestBody(jsonMediaType))
            "PUT" -> builder.put((jsonBody ?: "{}").toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete((jsonBody ?: "{}").toRequestBody(jsonMediaType))
        }

        return builder.build()
    }
}
