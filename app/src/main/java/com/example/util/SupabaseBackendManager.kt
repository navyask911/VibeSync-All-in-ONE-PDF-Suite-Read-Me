package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.RegisteredAccountEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Production Supabase & Cloudflare R2 Backend Client Manager
 * Provides zero-cost backend scalability with Postgrest, Realtime WebSockets,
 * Cloudflare R2 pre-signed S3 uploads, and Edge Function multi-agent message routing.
 */
object SupabaseBackendManager {
    private const val TAG = "SupabaseBackend"

    // Default Supabase Configuration (Overridden via Secrets / BuildConfig in Production)
    var SUPABASE_URL = "https://ysij5gfggpt2akxlk7vm3b.supabase.co"
    var SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InliaW5nIiwicm9sZSI6ImFub24ifQ"
    var SUPABASE_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InliaW5nIiwicm9sZSI6InNlcnZpY2Vfcm9sZSJ9"
    var CLOUDFLARE_R2_ENDPOINT = "https://media.vibesync.app"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)

    // Realtime Connection & Status Flow
    private val _realtimeConnected = MutableStateFlow(false)
    val realtimeConnected: StateFlow<Boolean> = _realtimeConnected.asStateFlow()

    private val _agentQueueCount = MutableStateFlow(0)
    val agentQueueCount: StateFlow<Int> = _agentQueueCount.asStateFlow()

    private var realtimeWebSocket: WebSocket? = null

    fun initialize(context: Context, url: String? = null, anonKey: String? = null) {
        if (!url.isNullOrBlank()) SUPABASE_URL = url
        if (!anonKey.isNullOrBlank()) SUPABASE_ANON_KEY = anonKey
        Log.i(TAG, "🚀 Initialized Supabase Engine at $SUPABASE_URL")
    }

    /**
     * Connects to Supabase Realtime WebSocket for multi-agent message broadcasts
     */
    fun connectRealtimeWebsocket(onMessageReceived: (JSONObject) -> Unit) {
        try {
            val wsUrl = SUPABASE_URL.replace("https://", "wss://").replace("http://", "ws://") +
                    "/realtime/v1/websocket?apikey=$SUPABASE_ANON_KEY&v=1.0.0"

            val request = Request.Builder().url(wsUrl).build()
            realtimeWebSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                    _realtimeConnected.value = true
                    Log.i(TAG, "⚡ Supabase Realtime WebSocket Connected!")

                    // Join public presence & broadcast channel
                    val joinMsg = JSONObject().apply {
                        put("topic", "realtime:public:messages")
                        put("event", "phx_join")
                        put("payload", JSONObject())
                        put("ref", "1")
                    }
                    webSocket.send(joinMsg.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val event = json.optString("event")
                        if (event == "INSERT" || event == "broadcast") {
                            val payload = json.optJSONObject("payload") ?: JSONObject()
                            onMessageReceived(payload)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing Supabase realtime frame: ${e.message}")
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _realtimeConnected.value = false
                    Log.w(TAG, "Supabase Realtime disconnected: $reason")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    _realtimeConnected.value = false
                    Log.e(TAG, "Supabase Realtime WebSocket failure: ${t.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Supabase WebSocket: ${e.message}")
        }
    }

    /**
     * Sync user profile to Supabase Postgrest Database
     */
    suspend fun syncProfileToSupabase(profile: ProfileEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = profile.phoneNumber.filter { it.isDigit() }
            val docId = if (cleanPhone.isNotBlank()) cleanPhone else profile.id

            val json = JSONObject().apply {
                put("id", docId)
                put("name", profile.name)
                put("age", profile.age)
                put("occupation", profile.occupation)
                put("city", profile.city)
                put("country", profile.country)
                put("bio", profile.bio)
                put("interests", profile.interests)
                put("phone_number", profile.phoneNumber)
                put("clean_phone", cleanPhone)
                put("email", profile.email)
                put("is_verified", profile.isVerified)
                put("is_deleted", profile.isDeleted)
                put("account_status", profile.accountStatus)
                put("updated_at", System.currentTimeMillis())
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/profiles")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                Log.d(TAG, "Postgrest profile sync success: $success (${response.code})")
                return@withContext success
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase Postgrest sync exception: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Calls Supabase Edge Function /v1/send-message for Multi-Agent routing
     */
    suspend fun sendMultiAgentMessage(
        senderId: String,
        receiverId: String,
        messageText: String,
        apiKey: String = "vbs_live_agent_9981"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("sender_id", senderId)
                put("receiver_id", receiverId)
                put("message", messageText)
                put("timestamp", System.currentTimeMillis())
            }

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$SUPABASE_URL/functions/v1/send-message")
                .header("x-api-key", apiKey)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .header("Content-Type", "application/json")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val success = response.isSuccessful
                Log.i(TAG, "⚡ Edge Function /v1/send-message response: ${response.code}")
                return@withContext success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Edge Function invocation failed: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Generates a pre-signed Cloudflare R2 upload URL for zero-cost media asset transfers
     */
    suspend fun generateCloudflareR2UploadUrl(fileName: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/functions/v1/get-r2-upload-url?file=$fileName")
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    return@withContext json.optString("upload_url", "")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch Cloudflare R2 upload URL: ${e.message}")
        }
        return@withContext "$CLOUDFLARE_R2_ENDPOINT/uploads/$fileName"
    }
}
