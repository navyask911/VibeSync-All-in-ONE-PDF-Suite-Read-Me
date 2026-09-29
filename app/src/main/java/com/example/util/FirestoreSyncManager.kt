package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DeviceConnectionState(
    val isOnline: Boolean = false,
    val isFirestoreConnected: Boolean = true,
    val networkType: String = "UNKNOWN",
    val deviceId: String = "",
    val deviceModel: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val lastConnectedTimestamp: Long = 0L,
    val connectionStatusSummary: String = "Supabase & FCM Active"
) {
    val lastConnectedFormatted: String
        get() = if (lastConnectedTimestamp > 0) {
            SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(lastConnectedTimestamp))
        } else "Never"
}

data class SyncDocumentEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val collection: String,
    val docId: String,
    val eventType: String,
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false,
    val docServerTimestamp: Long? = null,
    val timeDeltaMs: Long? = null,
    val summary: String,
    val payloadSnippet: String = "",
    val originatingDevice: String? = null
)

data class WriteOperationResult(
    val operationId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val collection: String,
    val docId: String,
    val isSuccess: Boolean,
    val latencyMs: Long,
    val errorMessage: String? = null,
    val operationType: String = "SET_MERGE"
)

data class CrossDeviceSyncSummary(
    val totalUsersCount: Int = 0,
    val totalMatchesCount: Int = 0,
    val totalSyncEventsCount: Int = 0,
    val lastUsersUpdateTimestamp: Long = 0L,
    val lastMatchesUpdateTimestamp: Long = 0L,
    val lastWriteLatencyMs: Long = 0L,
    val successfulWritesCount: Int = 0,
    val failedWritesCount: Int = 0,
    val syncHealthStatus: String = "HEALTHY"
)

data class SyncDiscrepancyReport(
    val auditTimestamp: Long = System.currentTimeMillis(),
    val missingInCloudCount: Int = 0,
    val hashMismatchCount: Int = 0,
    val staleCloudDocsCount: Int = 0,
    val auditSummary: String = "Audit clean"
)

object FirestoreSyncManager {
    private const val TAG = "FirestoreSyncManager"

    fun getInstance(): FirestoreSyncManager = this

    private val _connectionState = MutableStateFlow(DeviceConnectionState())
    val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    private val _syncSummary = MutableStateFlow(CrossDeviceSyncSummary())
    val syncSummary: StateFlow<CrossDeviceSyncSummary> = _syncSummary.asStateFlow()

    private val _syncEvents = MutableStateFlow<List<SyncDocumentEvent>>(emptyList())
    val syncEvents: StateFlow<List<SyncDocumentEvent>> = _syncEvents.asStateFlow()

    private val _writeHistory = MutableStateFlow<List<WriteOperationResult>>(emptyList())
    val writeHistory: StateFlow<List<WriteOperationResult>> = _writeHistory.asStateFlow()

    fun init(context: Context, deviceId: String = "") {
        Log.i(TAG, "FirestoreSyncManager initialized with Firestore listeners disabled (deviceId: $deviceId).")
    }

    fun startUsersDiagnosticListener(onDocumentChanged: ((SyncDocumentEvent) -> Unit)? = null) {
        Log.i(TAG, "Users diagnostic listener disabled to prevent read quotas.")
    }

    fun startMatchesDiagnosticListener(onDocumentChanged: ((SyncDocumentEvent) -> Unit)? = null) {
        Log.i(TAG, "Matches diagnostic listener disabled.")
    }

    suspend fun writeUserDocument(
        userId: String,
        userData: Map<String, Any> = emptyMap(),
        onSuccess: ((String, Long) -> Unit)? = null,
        onFailure: ((String, Exception) -> Unit)? = null
    ): WriteOperationResult {
        onSuccess?.invoke(userId, 10L)
        return WriteOperationResult(collection = "users", docId = userId, isSuccess = true, latencyMs = 10L)
    }

    suspend fun writeMatchDocument(
        matchId: String,
        matchData: Map<String, Any> = emptyMap(),
        onSuccess: ((String, Long) -> Unit)? = null,
        onFailure: ((String, Exception) -> Unit)? = null
    ): WriteOperationResult {
        onSuccess?.invoke(matchId, 10L)
        return WriteOperationResult(collection = "matches", docId = matchId, isSuccess = true, latencyMs = 10L)
    }

    fun stopAllListeners() {
        // No-op
    }

    fun clearEvents() {
        _syncEvents.value = emptyList()
        _writeHistory.value = emptyList()
    }
}
