package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Presence Status Enum representing online, recently active, background suspended, or offline states.
 */
enum class PresenceStatus {
    ONLINE,
    RECENTLY_ACTIVE,
    BACKGROUND_SUSPENDED,
    OFFLINE_NETWORK_ISSUE,
    OFFLINE
}

/**
 * Root Cause diagnosis explaining why a sync delay or offline state exists.
 */
enum class PresenceDelayReason {
    NONE_ONLINE,
    BACKGROUND_PROCESS_SUSPENSION,
    NETWORK_CONNECTIVITY_ISSUE,
    CLIENT_OFFLINE,
    APP_TERMINATED_OR_IDLE,
    CLOCK_SKEW_DETECTED
}

/**
 * User Presence Model exposing UI-ready status and diagnostic details.
 */
data class UserPresenceInfo(
    val userId: String = "",
    val userName: String = "",
    val status: PresenceStatus = PresenceStatus.OFFLINE,
    val lastActiveTimestamp: Long = 0L,
    val isExplicitlyOnline: Boolean = false,
    val isAppInForeground: Boolean = true,
    val originatingDeviceId: String = "",
    val delayReason: PresenceDelayReason = PresenceDelayReason.APP_TERMINATED_OR_IDLE,
    val diagnosticMessage: String = "No presence heartbeat recorded",
    val latencySeconds: Long = 0L
) {
    val isOnline: Boolean get() = status == PresenceStatus.ONLINE || status == PresenceStatus.RECENTLY_ACTIVE

    val formattedLastActive: String
        get() = if (lastActiveTimestamp > 0) {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastActiveTimestamp))
        } else "Never"

    val statusLabel: String
        get() = when (status) {
            PresenceStatus.ONLINE -> "Online 🟢"
            PresenceStatus.RECENTLY_ACTIVE -> "Active Recently 🟡"
            PresenceStatus.BACKGROUND_SUSPENDED -> "App Minimized (Background) 🟠"
            PresenceStatus.OFFLINE_NETWORK_ISSUE -> "Network Disconnected 🔴"
            PresenceStatus.OFFLINE -> "Offline ⚫"
        }
}

/**
 * UserPresenceManager
 *
 * Kotlin singleton that monitors Firestore 'users' collection document 'lastActive' / 'lastActiveTimestamp'
 * fields in real-time. Provides Online/Offline status to the UI and helps identify if sync delays
 * are due to Android background process suspension vs. device network connectivity issues.
 */
class UserPresenceManager private constructor() {

    companion object {
        private const val TAG = "UserPresenceManager"
        private const val TAG_DIAGNOSTIC = "UserPresenceDiag"
        private const val COLLECTION_USERS = "users"

        // Thresholds in milliseconds
        const val ONLINE_THRESHOLD_MS = 60_000L // 60s -> Active Online
        const val RECENTLY_ACTIVE_THRESHOLD_MS = 180_000L // 3 min -> Recently Active
        const val BACKGROUND_SUSPENSION_THRESHOLD_MS = 300_000L // 5 min -> Background Suspended
        const val HEARTBEAT_INTERVAL_MS = 25_000L // Send heartbeat every 25s

        @Volatile
        private var instance: UserPresenceManager? = null

        fun getInstance(): UserPresenceManager {
            return instance ?: synchronized(this) {
                instance ?: UserPresenceManager().also { instance = it }
            }
        }
    }

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // State flows for UI consumption
    private val _userPresenceMap = MutableStateFlow<Map<String, UserPresenceInfo>>(emptyMap())
    val userPresenceMap: StateFlow<Map<String, UserPresenceInfo>> = _userPresenceMap.asStateFlow()

    private val _isDeviceOnline = MutableStateFlow(true)
    val isDeviceOnline: StateFlow<Boolean> = _isDeviceOnline.asStateFlow()

    private val _lastDiagnosticLog = MutableStateFlow("Presence monitoring idle.")
    val lastDiagnosticLog: StateFlow<String> = _lastDiagnosticLog.asStateFlow()

    private var usersListenerRegistration: ListenerRegistration? = null
    private var heartbeatJob: Job? = null
    private var periodicEvaluatorJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var connectivityManager: ConnectivityManager? = null

    private var currentUserId: String = ""
    private var isAppInForeground: Boolean = true
    private val deviceId = "${Build.MODEL}_${Build.SERIAL.take(4).ifBlank { UUID.randomUUID().toString().take(6) }}"

    private var connectedListener: ValueEventListener? = null
    private val rtdb: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    /**
     * Starts monitoring user presence across the Firestore 'users' collection & Firebase Realtime Database.
     */
    fun startMonitoring(context: Context, localUserId: String) {
        currentUserId = localUserId.trim()
        Log.i(TAG, "🚀 [START] Starting UserPresenceManager for localUserId: '$currentUserId' (Device: $deviceId)")

        setupNetworkMonitoring(context)
        setupFirestoreUsersPresenceListener()
        setupRealtimeDatabaseConnectionPresence(currentUserId)
        startPeriodicHeartbeat(currentUserId)
        startPeriodicEvaluator()
    }

    /**
     * Firebase Realtime Database connection state listener (.info/connected).
     * Sets status/'online' on connect and schedules status/'offline' via onDisconnect.
     */
    private fun setupRealtimeDatabaseConnectionPresence(userId: String) {
        val cleanId = userId.trim()
        if (cleanId.isBlank()) return

        try {
            val connectedRef = rtdb.getReference(".info/connected")
            val userStatusRef = rtdb.getReference("status/$cleanId")

            connectedListener?.let { connectedRef.removeEventListener(it) }

            connectedListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        // Configure onDisconnect for automatic offline state update on server
                        val offlineMap = mapOf<String, Any>(
                            "state" to "offline",
                            "last_changed" to ServerValue.TIMESTAMP,
                            "isTyping" to false
                        )
                        userStatusRef.onDisconnect().setValue(offlineMap)

                        val onlineMap = mapOf<String, Any>(
                            "state" to "online",
                            "last_changed" to ServerValue.TIMESTAMP,
                            "isTyping" to false,
                            "deviceId" to deviceId
                        )
                        userStatusRef.setValue(onlineMap)
                        Log.i(TAG, "⚡ [RTDB PRESENCE] User '$cleanId' connected -> set state=online with onDisconnect handler.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "⚠️ Realtime DB connection listener cancelled: ${error.message}")
                }
            }

            connectedRef.addValueEventListener(connectedListener!!)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Firebase Realtime Database presence: ${e.message}")
        }
    }

    /**
     * Updates typing status in Firebase Realtime Database for active keyboard usage.
     */
    fun setUserTypingState(userId: String, targetPartnerId: String, isTyping: Boolean) {
        val cleanId = userId.trim()
        if (cleanId.isBlank()) return

        try {
            val userStatusRef = rtdb.getReference("status/$cleanId")
            val updates = mapOf<String, Any>(
                "isTyping" to isTyping,
                "typingTarget" to targetPartnerId.trim(),
                "last_changed" to ServerValue.TIMESTAMP
            )
            userStatusRef.updateChildren(updates)
            Log.d(TAG, "✍️ Typing state updated for '$cleanId' -> isTyping=$isTyping (Target: $targetPartnerId)")
        } catch (e: Exception) {
            Log.w(TAG, "Error setting typing state: ${e.message}")
        }
    }

    /**
     * Checks if push notification for a user should be suppressed.
     * Returns true if user has an active connection/session (is app in foreground or online state).
     */
    fun shouldSuppressPushNotification(targetUserId: String = ""): Boolean {
        if (isAppInForeground) return true
        if (targetUserId.isNotBlank()) {
            val info = getUserPresence(targetUserId)
            if (info.isOnline) return true
        }
        return false
    }

    /**
     * Sets whether the local application is currently in the foreground or background.
     */
    fun setAppForegroundState(isInForeground: Boolean) {
        isAppInForeground = isInForeground
        Log.d(TAG, "📱 App foreground state changed: isInForeground=$isInForeground")
        if (currentUserId.isNotBlank()) {
            managerScope.launch {
                updateLocalHeartbeat(currentUserId, isAppInForeground)
            }
        }
    }

    /**
     * Updates the local user's heartbeat in Firestore 'users' collection.
     */
    suspend fun updateLocalHeartbeat(userId: String, inForeground: Boolean = true) = withContext(Dispatchers.IO) {
        val cleanId = userId.trim()
        if (cleanId.isBlank()) return@withContext

        val now = System.currentTimeMillis()
        val heartbeatData = mapOf(
            "lastActive" to now,
            "lastActiveTimestamp" to now,
            "isOnline" to true,
            "isForeground" to inForeground,
            "originatingDeviceId" to deviceId,
            "updatedAt" to now
        )

        try {
            firestore.collection(COLLECTION_USERS)
                .document(cleanId)
                .set(heartbeatData, SetOptions.merge())
                .await()
            Log.d(TAG, "💓 [HEARTBEAT] Local heartbeat sent for '$cleanId' (Foreground: $inForeground)")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [HEARTBEAT ERROR] Failed to send heartbeat for '$cleanId': ${e.message}")
        }
    }

    /**
     * Marks the local user as explicitly offline upon logout or graceful exit.
     */
    suspend fun markUserOffline(userId: String) = withContext(Dispatchers.IO) {
        val cleanId = userId.trim()
        if (cleanId.isBlank()) return@withContext

        val now = System.currentTimeMillis()
        val offlineData = mapOf(
            "lastActive" to now,
            "lastActiveTimestamp" to now,
            "isOnline" to false,
            "isForeground" to false,
            "originatingDeviceId" to deviceId,
            "updatedAt" to now
        )

        try {
            firestore.collection(COLLECTION_USERS)
                .document(cleanId)
                .set(offlineData, SetOptions.merge())
                .await()
            Log.i(TAG, "🔌 [USER OFFLINE] User '$cleanId' marked offline in Firestore.")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to mark '$cleanId' offline: ${e.message}")
        }
    }

    /**
     * Gets current presence info for a specific user ID.
     */
    fun getUserPresence(userId: String): UserPresenceInfo {
        return _userPresenceMap.value[userId.trim()] ?: UserPresenceInfo(
            userId = userId,
            status = PresenceStatus.OFFLINE,
            diagnosticMessage = "User not found in presence cache"
        )
    }

    /**
     * Returns a Flow observing presence for a specific user ID.
     */
    fun observeUserPresence(userId: String) = userPresenceMap.map { map ->
        map[userId.trim()] ?: UserPresenceInfo(
            userId = userId,
            status = PresenceStatus.OFFLINE,
            diagnosticMessage = "User not found in presence cache"
        )
    }

    /**
     * Diagnoses why a presence or synchronization delay exists for a given user.
     * Evaluates lastActive timestamp, device connectivity, and foreground status.
     */
    fun diagnosePresenceDelay(targetUserId: String): UserPresenceInfo {
        val info = getUserPresence(targetUserId)
        val now = System.currentTimeMillis()
        val deltaMs = if (info.lastActiveTimestamp > 0) now - info.lastActiveTimestamp else Long.MAX_VALUE
        val deltaSec = if (deltaMs != Long.MAX_VALUE) deltaMs / 1000 else -1

        val delayReason: PresenceDelayReason
        val status: PresenceStatus
        val diagnosticMsg: String

        if (!_isDeviceOnline.value) {
            status = PresenceStatus.OFFLINE_NETWORK_ISSUE
            delayReason = PresenceDelayReason.NETWORK_CONNECTIVITY_ISSUE
            diagnosticMsg = "Local device has no internet connectivity. Sync updates are held in Firestore offline cache."
        } else if (info.lastActiveTimestamp == 0L) {
            status = PresenceStatus.OFFLINE
            delayReason = PresenceDelayReason.APP_TERMINATED_OR_IDLE
            diagnosticMsg = "No active timestamp recorded for user '$targetUserId'."
        } else if (deltaMs <= ONLINE_THRESHOLD_MS && info.isExplicitlyOnline) {
            status = PresenceStatus.ONLINE
            delayReason = PresenceDelayReason.NONE_ONLINE
            diagnosticMsg = "User is actively Online (Heartbeat received ${deltaSec}s ago)."
        } else if (deltaMs <= RECENTLY_ACTIVE_THRESHOLD_MS) {
            status = PresenceStatus.RECENTLY_ACTIVE
            delayReason = PresenceDelayReason.NONE_ONLINE
            diagnosticMsg = "User was active recently (${deltaSec}s ago)."
        } else if (!info.isAppInForeground && deltaMs <= BACKGROUND_SUSPENSION_THRESHOLD_MS) {
            status = PresenceStatus.BACKGROUND_SUSPENDED
            delayReason = PresenceDelayReason.BACKGROUND_PROCESS_SUSPENSION
            diagnosticMsg = "App is suspended in background by Android OS (${deltaSec}s since last foreground active heartbeat). Push notifications or app foregrounding required to resume instant sync."
        } else {
            status = PresenceStatus.OFFLINE
            delayReason = PresenceDelayReason.APP_TERMINATED_OR_IDLE
            diagnosticMsg = "User is Offline (Last active ${deltaSec}s ago from device '${info.originatingDeviceId}')."
        }

        val enriched = info.copy(
            status = status,
            delayReason = delayReason,
            diagnosticMessage = diagnosticMsg,
            latencySeconds = deltaSec
        )

        Log.i(TAG_DIAGNOSTIC, "🔬 [PRESENCE DIAGNOSIS] User '${info.userId}' -> Status: ${status.name} | DelayReason: ${delayReason.name} | Latency: ${deltaSec}s | Note: $diagnosticMsg")
        return enriched
    }

    /**
     * Initializes Firestore real-time snapshot listener on the 'users' collection.
     */
    private fun setupFirestoreUsersPresenceListener() {
        usersListenerRegistration?.remove()
        try {
            usersListenerRegistration = firestore.collection(COLLECTION_USERS)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "❌ [SNAPSHOT ERROR] Error listening to 'users' presence: ${error.message}", error)
                        _lastDiagnosticLog.value = "Firestore presence listener error: ${error.message}"
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val currentMap = _userPresenceMap.value.toMutableMap()
                        val now = System.currentTimeMillis()

                        for (doc in snapshot.documents) {
                            val uid = doc.id
                            val name = doc.getString("name") ?: doc.getString("userName") ?: uid
                            val lastActive = doc.getLong("lastActive")
                                ?: doc.getLong("lastActiveTimestamp")
                                ?: doc.getLong("updatedAt")
                                ?: 0L
                            val isOnlineExplicit = doc.getBoolean("isOnline") ?: false
                            val isForeground = doc.getBoolean("isForeground") ?: true
                            val originDevice = doc.getString("originatingDeviceId") ?: doc.getString("deviceId") ?: "unknown"

                            val deltaMs = if (lastActive > 0) now - lastActive else Long.MAX_VALUE
                            val deltaSec = if (deltaMs != Long.MAX_VALUE) deltaMs / 1000 else -1

                            val (status, reason, msg) = evaluateStatus(
                                isDeviceOnline = _isDeviceOnline.value,
                                lastActive = lastActive,
                                isOnlineExplicit = isOnlineExplicit,
                                isForeground = isForeground,
                                deltaMs = deltaMs,
                                deltaSec = deltaSec,
                                originDevice = originDevice
                            )

                            currentMap[uid] = UserPresenceInfo(
                                userId = uid,
                                userName = name,
                                status = status,
                                lastActiveTimestamp = lastActive,
                                isExplicitlyOnline = isOnlineExplicit,
                                isAppInForeground = isForeground,
                                originatingDeviceId = originDevice,
                                delayReason = reason,
                                diagnosticMessage = msg,
                                latencySeconds = deltaSec
                            )
                        }

                        _userPresenceMap.value = currentMap
                        val onlineCount = currentMap.values.count { it.isOnline }
                        val log = "Updated presence for ${currentMap.size} users ($onlineCount Online/Active)."
                        _lastDiagnosticLog.value = log
                        Log.d(TAG, "👥 [PRESENCE UPDATE] $log")
                    }
                }
            Log.i(TAG, "✅ [LISTENER ATTACHED] Firestore 'users' presence snapshot listener active.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach users presence listener: ${e.message}", e)
        }
    }

    private fun evaluateStatus(
        isDeviceOnline: Boolean,
        lastActive: Long,
        isOnlineExplicit: Boolean,
        isForeground: Boolean,
        deltaMs: Long,
        deltaSec: Long,
        originDevice: String
    ): Triple<PresenceStatus, PresenceDelayReason, String> {
        return when {
            !isDeviceOnline -> Triple(
                PresenceStatus.OFFLINE_NETWORK_ISSUE,
                PresenceDelayReason.NETWORK_CONNECTIVITY_ISSUE,
                "Device offline: Local network disconnected."
            )
            lastActive == 0L -> Triple(
                PresenceStatus.OFFLINE,
                PresenceDelayReason.APP_TERMINATED_OR_IDLE,
                "No heartbeat timestamp recorded."
            )
            deltaMs <= ONLINE_THRESHOLD_MS && isOnlineExplicit -> Triple(
                PresenceStatus.ONLINE,
                PresenceDelayReason.NONE_ONLINE,
                "Active Online (${deltaSec}s ago)."
            )
            deltaMs <= RECENTLY_ACTIVE_THRESHOLD_MS -> Triple(
                PresenceStatus.RECENTLY_ACTIVE,
                PresenceDelayReason.NONE_ONLINE,
                "Active recently (${deltaSec}s ago)."
            )
            !isForeground && deltaMs <= BACKGROUND_SUSPENSION_THRESHOLD_MS -> Triple(
                PresenceStatus.BACKGROUND_SUSPENDED,
                PresenceDelayReason.BACKGROUND_PROCESS_SUSPENSION,
                "Background process suspended by OS (${deltaSec}s ago)."
            )
            else -> Triple(
                PresenceStatus.OFFLINE,
                PresenceDelayReason.APP_TERMINATED_OR_IDLE,
                "Offline (${deltaSec}s ago from '$originDevice')."
            )
        }
    }

    /**
     * Starts recurring heartbeat coroutine to keep local user presence fresh in Firestore.
     */
    private fun startPeriodicHeartbeat(userId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = managerScope.launch {
            while (isActive) {
                if (currentUserId.isNotBlank() && _isDeviceOnline.value) {
                    updateLocalHeartbeat(currentUserId, isAppInForeground)
                }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    /**
     * Periodically recalculates presence status of cached users as time progresses.
     */
    private fun startPeriodicEvaluator() {
        periodicEvaluatorJob?.cancel()
        periodicEvaluatorJob = managerScope.launch {
            while (isActive) {
                delay(15_000L) // Re-evaluate every 15s
                val current = _userPresenceMap.value
                if (current.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val updated = current.mapValues { (_, info) ->
                        val deltaMs = if (info.lastActiveTimestamp > 0) now - info.lastActiveTimestamp else Long.MAX_VALUE
                        val deltaSec = if (deltaMs != Long.MAX_VALUE) deltaMs / 1000 else -1
                        val (status, reason, msg) = evaluateStatus(
                            isDeviceOnline = _isDeviceOnline.value,
                            lastActive = info.lastActiveTimestamp,
                            isOnlineExplicit = info.isExplicitlyOnline,
                            isForeground = info.isAppInForeground,
                            deltaMs = deltaMs,
                            deltaSec = deltaSec,
                            originDevice = info.originatingDeviceId
                        )
                        info.copy(
                            status = status,
                            delayReason = reason,
                            diagnosticMessage = msg,
                            latencySeconds = deltaSec
                        )
                    }
                    _userPresenceMap.value = updated
                }
            }
        }
    }

    /**
     * Registers network connectivity callback.
     */
    private fun setupNetworkMonitoring(context: Context) {
        try {
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.i(TAG, "🌐 Network AVAILABLE for presence sync.")
                    _isDeviceOnline.value = true
                    if (currentUserId.isNotBlank()) {
                        managerScope.launch { updateLocalHeartbeat(currentUserId, isAppInForeground) }
                    }
                }

                override fun onLost(network: Network) {
                    Log.w(TAG, "🔌 Network LOST. Presence switching to OFFLINE_NETWORK_ISSUE.")
                    _isDeviceOnline.value = false
                }
            }

            networkCallback?.let { connectivityManager?.registerNetworkCallback(request, it) }
        } catch (e: Exception) {
            Log.w(TAG, "Could not register network callback for presence: ${e.message}")
        }
    }

    /**
     * Stops all listeners, timers, and callbacks.
     */
    fun stopMonitoring() {
        usersListenerRegistration?.remove()
        usersListenerRegistration = null

        heartbeatJob?.cancel()
        heartbeatJob = null

        periodicEvaluatorJob?.cancel()
        periodicEvaluatorJob = null

        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            networkCallback = null
        } catch (_: Exception) {}

        Log.i(TAG, "🛑 UserPresenceManager monitoring stopped.")
    }
}
