package com.example.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CrossDeviceDiagnosticManager
 * Real-time diagnostic snapshot listener for Firestore and Supabase collections (disabled for Firestore 'users').
 */
object CrossDeviceDiagnosticManager {

    private const val TAG = "CrossDeviceDiagnostic"

    data class DiagnosticSnapshotSummary(
        val firestoreUsersCount: Int = 0,
        val firestoreProfilesCount: Int = 0,
        val firestoreIsFromCache: Boolean = false,
        val firestoreHasPendingWrites: Boolean = false,
        val supabaseProfilesCount: Int = 0,
        val supabaseIsConnected: Boolean = false,
        val lastSnapshotTimestamp: Long = System.currentTimeMillis(),
        val activeDeviceDocIds: List<String> = emptyList(),
        val diagnosticLogText: String = "Supabase & FCM Active (Firestore Listeners Disabled)"
    )

    private val _snapshotSummary = MutableStateFlow(DiagnosticSnapshotSummary())
    val snapshotSummary: StateFlow<DiagnosticSnapshotSummary> = _snapshotSummary.asStateFlow()

    fun startDiagnosticListeners() {
        Log.i(TAG, "CrossDeviceDiagnosticManager Firestore listeners disabled to prevent read quotas.")
    }

    fun stopDiagnosticListeners() {
        // No-op
    }
}
