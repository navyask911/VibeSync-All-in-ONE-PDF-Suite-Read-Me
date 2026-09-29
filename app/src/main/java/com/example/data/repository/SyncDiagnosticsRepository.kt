package com.example.data.repository

import android.util.Log
import com.example.util.SupabaseClientManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceDocumentSnapshot(
    val deviceIdOrPhone: String,
    val collectionName: String,
    val existsInFirestore: Boolean,
    val userName: String = "",
    val phoneNumber: String = "",
    val phoneHash: String = "",
    val hasValid16CharHash: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val diagnosticNote: String = ""
)

data class SyncDiagnosticsState(
    val isFirestoreConnected: Boolean = true,
    val isSupabaseConnected: Boolean = true,
    val firestoreUsersCount: Int = 0,
    val firestoreProfilesCount: Int = 0,
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false,
    val latencyMs: Long = 0L,
    val deviceSnapshots: List<DeviceDocumentSnapshot> = emptyList(),
    val logs: List<String> = emptyList(),
    val statusSummaryText: String = "Supabase Database & FCM Active (Firestore Listeners Disabled)",
    val isMigrating: Boolean = false
)

object SyncDiagnosticsRepository {

    private const val TAG = "SyncDiagnosticsRepo"
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(SyncDiagnosticsState())
    val state: StateFlow<SyncDiagnosticsState> = _state.asStateFlow()

    fun startMonitoring() {
        addLog("🚀 [MONITOR START] Firestore continuous listeners disabled. Using Supabase & local Room DB.")
        scope.launch {
            try {
                val conn = SupabaseClientManager.checkSupabaseConnectivity()
                _state.value = _state.value.copy(
                    isSupabaseConnected = conn.isConnected,
                    latencyMs = conn.latencyMs,
                    statusSummaryText = if (conn.isConnected) "Supabase Online (${conn.latencyMs}ms)" else "Supabase Offline"
                )
                addLog("⚡ Supabase connectivity check: ${if (conn.isConnected) "ONLINE" else "OFFLINE"}")
            } catch (e: Exception) {
                addLog("⚠️ Supabase connectivity check error: ${e.message}")
            }
        }
    }

    fun runPhoneHashMigration() {
        addLog("ℹ️ Legacy Firestore phone_hash migration is disabled (Zero-Knowledge contact matching operates strictly via local Room DB and Supabase clean_phone hashes).")
    }

    fun addLog(msg: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] $msg"
        Log.i(TAG, entry)
        val currentLogs = _state.value.logs.take(60)
        _state.value = _state.value.copy(logs = listOf(entry) + currentLogs)
    }

    fun clearLogs() {
        _state.value = _state.value.copy(logs = emptyList())
    }

    fun stopMonitoring() {
        // No active Firestore listeners to remove
    }
}
