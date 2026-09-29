package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * CrudOperationType - Categorizes CRUD actions executed on Firestore collections.
 */
enum class CrudOperationType {
    CREATE,
    READ,
    UPDATE,
    DELETE
}

private val sharedDiagTimeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
private fun formatDiagTime(): String = try {
    synchronized(sharedDiagTimeFormat) { sharedDiagTimeFormat.format(Date()) }
} catch (_: Throwable) {
    ""
}

/**
 * FirestoreCrudLogEntry - Data model for each logged CRUD action.
 */
data class FirestoreCrudLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = formatDiagTime(),
    val operation: CrudOperationType,
    val collection: String, // "users", "matches", etc.
    val docId: String,
    val isSuccess: Boolean,
    val latencyMs: Long = 0L,
    val details: String = "",
    val errorMessage: String? = null,
    val payloadPreview: String = "",
    val originatingDeviceId: String = ""
)

/**
 * FirestoreEventListenerEntry - Data model for real-time listener events.
 */
data class FirestoreEventListenerEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = formatDiagTime(),
    val collection: String,
    val eventType: String, // "ATTACHED", "SNAPSHOT", "ADDED", "MODIFIED", "REMOVED", "ERROR"
    val docId: String,
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false,
    val serverTimestamp: Long? = null,
    val clockSkewMs: Long? = null,
    val summary: String,
    val mirrorStatus: String = "SYNCED" // "SYNCED", "PENDING_LOCAL", "EXTERNAL_UPDATE", "MIRROR_MISMATCH"
)

/**
 * MirroringHealthSummary - Summary of cross-device synchronization state.
 */
data class MirroringHealthSummary(
    val isListening: Boolean = false,
    val totalUsersObserved: Int = 0,
    val totalMatchesObserved: Int = 0,
    val totalCrudOpsLogged: Int = 0,
    val totalSuccessCrud: Int = 0,
    val totalFailedCrud: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncTimeFormatted: String = "Never",
    val primaryDeviceId: String = "",
    val mirroringDiagnosis: String = "Initializing listener diagnostics..."
)

/**
 * FirestoreDiagnosticManager
 *
 * Singleton that:
 * 1. Logs all CRUD operations (CREATE, READ, UPDATE, DELETE) on 'users' and 'matches'.
 * 2. Implements timestamped real-time event snapshot listeners with detailed Logcat logging.
 * 3. Inspects document schema, timestamps, and origin metadata to diagnose why data is not mirroring between devices.
 */
object FirestoreDiagnosticManager {

    private const val TAG = "FirestoreDiag"
    private const val TAG_CRUD = "FirestoreDiag_CRUD"
    private const val TAG_USERS = "FirestoreDiag_Users"
    private const val TAG_MATCHES = "FirestoreDiag_Matches"
    private const val TAG_MIRROR = "FirestoreDiag_Mirroring"

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Observable StateFlows for UI / Admin Diagnostics
    private val _crudLogs = MutableStateFlow<List<FirestoreCrudLogEntry>>(emptyList())
    val crudLogs: StateFlow<List<FirestoreCrudLogEntry>> = _crudLogs.asStateFlow()

    private val _listenerEvents = MutableStateFlow<List<FirestoreEventListenerEntry>>(emptyList())
    val listenerEvents: StateFlow<List<FirestoreEventListenerEntry>> = _listenerEvents.asStateFlow()

    private val _mirroringHealth = MutableStateFlow(MirroringHealthSummary())
    val mirroringHealth: StateFlow<MirroringHealthSummary> = _mirroringHealth.asStateFlow()

    private var usersListener: ListenerRegistration? = null
    private var matchesListener: ListenerRegistration? = null
    private var currentDeviceId: String = "dev_${Build.MODEL.replace(" ", "_").lowercase()}_${UUID.randomUUID().toString().take(6)}"

    private var isInitialized = false

    /**
     * Initializes the diagnostic manager with optional device identification.
     */
    fun init(context: Context? = null, deviceId: String = "") {
        if (deviceId.isNotBlank()) {
            currentDeviceId = deviceId
        }

        _mirroringHealth.value = _mirroringHealth.value.copy(
            primaryDeviceId = currentDeviceId,
            mirroringDiagnosis = "Diagnostics active for device '$currentDeviceId'"
        )

        Log.i(TAG, "=================================================================")
        Log.i(TAG, "🚀 [INIT] FirestoreDiagnosticManager initialized for Device: $currentDeviceId")
        Log.i(TAG, "📱 Model: ${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.i(TAG, "=================================================================")

        startDiagnosticListeners()
        isInitialized = true
    }

    // ============================================================================================
    // REAL-TIME TIMESTAMPED EVENT LISTENERS (USERS & MATCHES)
    // ============================================================================================

    /**
     * Starts timestamped snapshot listeners on 'users' and 'matches' collections, printing debug info to Logcat.
     */
    fun startDiagnosticListeners() {
        startUsersCollectionListener()
        startMatchesCollectionListener()
    }

    private fun startUsersCollectionListener() {
        usersListener?.remove()

        Log.i(TAG_USERS, "🔗 [ATTACH] Attaching timestamped listener to 'users' collection with MetadataChanges.INCLUDE...")

        recordListenerEvent(
            collection = "users",
            eventType = "ATTACHED",
            docId = "ALL",
            summary = "Timestamped snapshot listener attached to 'users' collection"
        )

        usersListener = firestore.collection("users")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                val now = System.currentTimeMillis()
                val nowFormatted = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(now))

                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(TAG_USERS, "ℹ️ [USERS LISTENER] Awaiting auth permissions: ${error.message}")
                    } else {
                        val errMsg = "❌ [USERS LISTENER ERROR][$nowFormatted] code=${error.code}, message=${error.message}"
                        Log.w(TAG_USERS, errMsg)
                        recordListenerEvent(
                            collection = "users",
                            eventType = "ERROR",
                            docId = "ALL",
                            summary = errMsg,
                            mirrorStatus = "MIRROR_MISMATCH"
                        )
                    }
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val size = snapshot.size()
                val fromCache = snapshot.metadata.isFromCache
                val hasPendingWrites = snapshot.metadata.hasPendingWrites()
                val docChanges = snapshot.documentChanges

                Log.i(
                    TAG_USERS,
                    "📦 [USERS SNAPSHOT][$nowFormatted] Total: $size docs | Changes: ${docChanges.size} | Source: ${if (fromCache) "CACHE" else "SERVER"} | PendingWrites: $hasPendingWrites"
                )

                _mirroringHealth.value = _mirroringHealth.value.copy(
                    isListening = true,
                    totalUsersObserved = size,
                    lastSyncTimestamp = now,
                    lastSyncTimeFormatted = nowFormatted
                )

                for (change in docChanges) {
                    val doc = change.document
                    val docId = doc.id
                    val changeType = change.type.name
                    val name = doc.getString("name") ?: doc.getString("userName") ?: "Unknown"
                    val phone = doc.getString("phoneNumber") ?: doc.getString("mobileNumber") ?: ""
                    val originatingDev = doc.getString("originatingDeviceId") ?: "unknown"

                    val serverTime = doc.getLong("lastActiveTimestamp")
                        ?: doc.getLong("updatedAt")
                        ?: doc.getLong("timestamp")

                    val skew = if (serverTime != null && serverTime > 0) now - serverTime else null

                    val mirrorStatus = when {
                        hasPendingWrites -> "PENDING_LOCAL"
                        originatingDev == currentDeviceId -> "SYNCED_LOCAL_ORIGIN"
                        else -> "EXTERNAL_UPDATE"
                    }

                    val summary = "[$changeType] User '$name' ($phone) [Doc: $docId, Origin: $originatingDev, Skew: ${skew ?: 0}ms]"

                    recordListenerEvent(
                        collection = "users",
                        eventType = changeType,
                        docId = docId,
                        isFromCache = fromCache,
                        hasPendingWrites = hasPendingWrites,
                        serverTimestamp = serverTime,
                        clockSkewMs = skew,
                        summary = summary,
                        mirrorStatus = mirrorStatus
                    )

                    val typeEmoji = when (change.type) {
                        DocumentChange.Type.ADDED -> "➕"
                        DocumentChange.Type.MODIFIED -> "✏️"
                        DocumentChange.Type.REMOVED -> "🗑️"
                    }

                    Log.d(
                        TAG_USERS,
                        "$typeEmoji [$changeType][$nowFormatted] Doc: $docId | Name: '$name' | Phone: '$phone' | Origin: $originatingDev | Skew: ${skew}ms | Source: ${if (fromCache) "CACHE" else "SERVER"}"
                    )

                    // Mirroring diagnostic check: Check for missing essential fields
                    if (name.isBlank() || (phone.isBlank() && doc.getString("email").isNullOrBlank())) {
                        Log.w(
                            TAG_MIRROR,
                            "⚠️ [MIRROR WARNING] User document '$docId' is missing critical fields (name='$name', phone='$phone'). May fail to render on secondary device."
                        )
                    }
                }
            }
    }

    private fun startMatchesCollectionListener() {
        matchesListener?.remove()

        Log.i(TAG_MATCHES, "🔗 [ATTACH] Attaching timestamped listener to 'matches' collection with MetadataChanges.INCLUDE...")

        recordListenerEvent(
            collection = "matches",
            eventType = "ATTACHED",
            docId = "ALL",
            summary = "Timestamped snapshot listener attached to 'matches' collection"
        )

        matchesListener = firestore.collection("matches")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                val now = System.currentTimeMillis()
                val nowFormatted = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(now))

                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(TAG_MATCHES, "ℹ️ [MATCHES LISTENER] Awaiting auth permissions: ${error.message}")
                    } else {
                        val errMsg = "❌ [MATCHES LISTENER ERROR][$nowFormatted] code=${error.code}, message=${error.message}"
                        Log.w(TAG_MATCHES, errMsg)
                        recordListenerEvent(
                            collection = "matches",
                            eventType = "ERROR",
                            docId = "ALL",
                            summary = errMsg,
                            mirrorStatus = "MIRROR_MISMATCH"
                        )
                    }
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val size = snapshot.size()
                val fromCache = snapshot.metadata.isFromCache
                val hasPendingWrites = snapshot.metadata.hasPendingWrites()
                val docChanges = snapshot.documentChanges

                Log.i(
                    TAG_MATCHES,
                    "🔥 [MATCHES SNAPSHOT][$nowFormatted] Total: $size matches | Changes: ${docChanges.size} | Source: ${if (fromCache) "CACHE" else "SERVER"}"
                )

                _mirroringHealth.value = _mirroringHealth.value.copy(
                    totalMatchesObserved = size,
                    lastSyncTimestamp = now,
                    lastSyncTimeFormatted = nowFormatted
                )

                for (change in docChanges) {
                    val doc = change.document
                    val docId = doc.id
                    val changeType = change.type.name
                    val userA = doc.getString("userA") ?: ""
                    val userB = doc.getString("userB") ?: ""
                    val isMutual = doc.getBoolean("isMutual") ?: true
                    val matchTime = doc.getLong("timestamp") ?: doc.getLong("createdAt")
                    val originatingDev = doc.getString("originatingDeviceId") ?: "unknown"

                    val skew = if (matchTime != null && matchTime > 0) now - matchTime else null

                    val summary = "[$changeType] Match '$userA' <-> '$userB' (Mutual: $isMutual) [Doc: $docId, Origin: $originatingDev]"

                    recordListenerEvent(
                        collection = "matches",
                        eventType = changeType,
                        docId = docId,
                        isFromCache = fromCache,
                        hasPendingWrites = hasPendingWrites,
                        serverTimestamp = matchTime,
                        clockSkewMs = skew,
                        summary = summary,
                        mirrorStatus = if (isMutual) "MUTUAL_MATCH_CONFIRMED" else "PENDING_MUTUAL"
                    )

                    Log.d(
                        TAG_MATCHES,
                        "❤️ [$changeType][$nowFormatted] Doc: $docId | UserA: '$userA' <---> UserB: '$userB' | Mutual: $isMutual | Origin: $originatingDev | Skew: ${skew}ms"
                    )

                    // Mirroring diagnostic check: Check participant validity
                    if (userA.isBlank() || userB.isBlank()) {
                        Log.w(
                            TAG_MIRROR,
                            "⚠️ [MIRROR WARNING] Match document '$docId' has blank user participant (userA='$userA', userB='$userB'). Will not mirror to chats properly."
                        )
                    }
                }
            }
    }

    // ============================================================================================
    // LOGGED CRUD OPERATIONS (USERS & MATCHES)
    // ============================================================================================

    /**
     * Executes and logs a CREATE operation on the 'users' collection.
     */
    fun createUser(
        userId: String,
        userData: Map<String, Any>,
        onComplete: ((isSuccess: Boolean, latencyMs: Long, error: Exception?) -> Unit)? = null
    ) {
        val startTime = System.currentTimeMillis()
        val cleanUserId = userId.trim()
        val payload = userData.toMutableMap().apply {
            put("originatingDeviceId", currentDeviceId)
            put("createdAt", System.currentTimeMillis())
            put("lastActiveTimestamp", System.currentTimeMillis())
            put("serverTimestamp", FieldValue.serverTimestamp())
        }

        logCrud(
            operation = CrudOperationType.CREATE,
            collection = "users",
            docId = cleanUserId,
            isSuccess = true,
            details = "Initiating CREATE for user '$cleanUserId'",
            payloadPreview = payload.entries.take(4).joinToString { "${it.key}=${it.value}" }
        )

        scope.launch {
            try {
                firestore.collection("users").document(cleanUserId)
                    .set(payload, SetOptions.merge())
                    .await()

                val latency = System.currentTimeMillis() - startTime
                logCrud(
                    operation = CrudOperationType.CREATE,
                    collection = "users",
                    docId = cleanUserId,
                    isSuccess = true,
                    latencyMs = latency,
                    details = "✅ CREATE succeeded in ${latency}ms for user '$cleanUserId'"
                )

                Log.i(TAG_CRUD, "🟢 [CRUD CREATE][SUCCESS] user '$cleanUserId' written in ${latency}ms")
                withContext(Dispatchers.Main) { onComplete?.invoke(true, latency, null) }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                logCrud(
                    operation = CrudOperationType.CREATE,
                    collection = "users",
                    docId = cleanUserId,
                    isSuccess = false,
                    latencyMs = latency,
                    details = "❌ CREATE failed for user '$cleanUserId'",
                    errorMessage = e.message
                )

                Log.e(TAG_CRUD, "🔴 [CRUD CREATE][FAILED] user '$cleanUserId': ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(false, latency, e) }
            }
        }
    }

    /**
     * Executes and logs a READ operation on the 'users' collection.
     */
    fun readUser(
        userId: String,
        onComplete: (exists: Boolean, data: Map<String, Any>?, latencyMs: Long, error: Exception?) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        val cleanUserId = userId.trim()

        scope.launch {
            try {
                val doc = firestore.collection("users").document(cleanUserId).get().await()
                val latency = System.currentTimeMillis() - startTime
                val exists = doc.exists()

                logCrud(
                    operation = CrudOperationType.READ,
                    collection = "users",
                    docId = cleanUserId,
                    isSuccess = true,
                    latencyMs = latency,
                    details = if (exists) "✅ READ succeeded: user exists" else "⚠️ READ succeeded: user does NOT exist in cloud"
                )

                Log.i(TAG_CRUD, "🔍 [CRUD READ][SUCCESS] user '$cleanUserId' exists=$exists (took ${latency}ms)")
                withContext(Dispatchers.Main) { onComplete(exists, doc.data, latency, null) }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                logCrud(
                    operation = CrudOperationType.READ,
                    collection = "users",
                    docId = cleanUserId,
                    isSuccess = false,
                    latencyMs = latency,
                    details = "❌ READ failed for user '$cleanUserId'",
                    errorMessage = e.message
                )

                Log.e(TAG_CRUD, "🔴 [CRUD READ][FAILED] user '$cleanUserId': ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete(false, null, latency, e) }
            }
        }
    }

    /**
     * Executes and logs an UPDATE operation on the 'users' collection.
     */
    fun updateUser(
        userId: String,
        updatedFields: Map<String, Any>,
        onComplete: ((isSuccess: Boolean, latencyMs: Long, error: Exception?) -> Unit)? = null
    ) {
        val startTime = System.currentTimeMillis()
        val cleanUserId = userId.trim()
        val payload = updatedFields.toMutableMap().apply {
            put("originatingDeviceId", currentDeviceId)
            put("updatedAt", System.currentTimeMillis())
            put("serverTimestamp", FieldValue.serverTimestamp())
        }

        scope.launch {
            try {
                firestore.collection("users").document(cleanUserId)
                    .set(payload, SetOptions.merge())
                    .await()

                val latency = System.currentTimeMillis() - startTime
                logCrud(
                    operation = CrudOperationType.UPDATE,
                    collection = "users",
                    docId = cleanUserId,
                    isSuccess = true,
                    latencyMs = latency,
                    details = "✅ UPDATE succeeded in ${latency}ms for user '$cleanUserId'",
                    payloadPreview = updatedFields.keys.joinToString()
                )

                Log.i(TAG_CRUD, "✏️ [CRUD UPDATE][SUCCESS] user '$cleanUserId' updated in ${latency}ms")
                withContext(Dispatchers.Main) { onComplete?.invoke(true, latency, null) }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                logCrud(
                    operation = CrudOperationType.UPDATE,
                    collection = "users",
                    docId = cleanUserId,
                    isSuccess = false,
                    latencyMs = latency,
                    details = "❌ UPDATE failed for user '$cleanUserId'",
                    errorMessage = e.message
                )

                Log.e(TAG_CRUD, "🔴 [CRUD UPDATE][FAILED] user '$cleanUserId': ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(false, latency, e) }
            }
        }
    }

    /**
     * Executes and logs a DELETE operation on the 'users' collection.
     */
    fun deleteUser(
        userId: String,
        onComplete: ((isSuccess: Boolean, latencyMs: Long, error: Exception?) -> Unit)? = null
    ) {
        val startTime = System.currentTimeMillis()
        val cleanUserId = userId.trim()

        scope.launch {
            try {
                firestore.collection("users").document(cleanUserId).delete().await()
                val latency = System.currentTimeMillis() - startTime

                logCrud(
                    operation = CrudOperationType.DELETE,
                    collection = "users",
                    docId = cleanUserId,
                    isSuccess = true,
                    latencyMs = latency,
                    details = "✅ DELETE succeeded in ${latency}ms for user '$cleanUserId'"
                )

                Log.i(TAG_CRUD, "🗑️ [CRUD DELETE][SUCCESS] user '$cleanUserId' deleted in ${latency}ms")
                withContext(Dispatchers.Main) { onComplete?.invoke(true, latency, null) }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                logCrud(
                    operation = CrudOperationType.DELETE,
                    collection = "users",
                    docId = cleanUserId,
                    isSuccess = false,
                    latencyMs = latency,
                    details = "❌ DELETE failed for user '$cleanUserId'",
                    errorMessage = e.message
                )

                Log.e(TAG_CRUD, "🔴 [CRUD DELETE][FAILED] user '$cleanUserId': ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(false, latency, e) }
            }
        }
    }

    /**
     * Executes and logs a CREATE operation on the 'matches' collection.
     */
    fun createMatch(
        matchId: String,
        matchData: Map<String, Any>,
        onComplete: ((isSuccess: Boolean, latencyMs: Long, error: Exception?) -> Unit)? = null
    ) {
        val startTime = System.currentTimeMillis()
        val cleanMatchId = matchId.trim()
        val payload = matchData.toMutableMap().apply {
            put("originatingDeviceId", currentDeviceId)
            put("timestamp", System.currentTimeMillis())
            put("serverTimestamp", FieldValue.serverTimestamp())
        }

        scope.launch {
            try {
                firestore.collection("matches").document(cleanMatchId)
                    .set(payload, SetOptions.merge())
                    .await()

                val latency = System.currentTimeMillis() - startTime
                logCrud(
                    operation = CrudOperationType.CREATE,
                    collection = "matches",
                    docId = cleanMatchId,
                    isSuccess = true,
                    latencyMs = latency,
                    details = "✅ CREATE match '$cleanMatchId' succeeded in ${latency}ms"
                )

                Log.i(TAG_CRUD, "❤️ [CRUD CREATE][SUCCESS] match '$cleanMatchId' created in ${latency}ms")
                withContext(Dispatchers.Main) { onComplete?.invoke(true, latency, null) }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                logCrud(
                    operation = CrudOperationType.CREATE,
                    collection = "matches",
                    docId = cleanMatchId,
                    isSuccess = false,
                    latencyMs = latency,
                    details = "❌ CREATE match failed",
                    errorMessage = e.message
                )

                Log.e(TAG_CRUD, "🔴 [CRUD CREATE][FAILED] match '$cleanMatchId': ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(false, latency, e) }
            }
        }
    }

    /**
     * Executes and logs a DELETE operation on the 'matches' collection.
     */
    fun deleteMatch(
        matchId: String,
        onComplete: ((isSuccess: Boolean, latencyMs: Long, error: Exception?) -> Unit)? = null
    ) {
        val startTime = System.currentTimeMillis()
        val cleanMatchId = matchId.trim()

        scope.launch {
            try {
                firestore.collection("matches").document(cleanMatchId).delete().await()
                val latency = System.currentTimeMillis() - startTime

                logCrud(
                    operation = CrudOperationType.DELETE,
                    collection = "matches",
                    docId = cleanMatchId,
                    isSuccess = true,
                    latencyMs = latency,
                    details = "✅ DELETE match '$cleanMatchId' succeeded in ${latency}ms"
                )

                Log.i(TAG_CRUD, "🗑️ [CRUD DELETE][SUCCESS] match '$cleanMatchId' deleted in ${latency}ms")
                withContext(Dispatchers.Main) { onComplete?.invoke(true, latency, null) }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                logCrud(
                    operation = CrudOperationType.DELETE,
                    collection = "matches",
                    docId = cleanMatchId,
                    isSuccess = false,
                    latencyMs = latency,
                    details = "❌ DELETE match failed",
                    errorMessage = e.message
                )

                Log.e(TAG_CRUD, "🔴 [CRUD DELETE][FAILED] match '$cleanMatchId': ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(false, latency, e) }
            }
        }
    }

    // ============================================================================================
    // INTERNAL LOG RECORDING & DIAGNOSTICS
    // ============================================================================================

    private fun logCrud(
        operation: CrudOperationType,
        collection: String,
        docId: String,
        isSuccess: Boolean,
        latencyMs: Long = 0L,
        details: String = "",
        errorMessage: String? = null,
        payloadPreview: String = ""
    ) {
        val entry = FirestoreCrudLogEntry(
            operation = operation,
            collection = collection,
            docId = docId,
            isSuccess = isSuccess,
            latencyMs = latencyMs,
            details = details,
            errorMessage = errorMessage,
            payloadPreview = payloadPreview,
            originatingDeviceId = currentDeviceId
        )

        val list = _crudLogs.value.toMutableList()
        if (list.size >= 300) {
            list.removeAt(list.size - 1)
        }
        list.add(0, entry)
        _crudLogs.value = list

        val total = _mirroringHealth.value.totalCrudOpsLogged + 1
        val successes = if (isSuccess) _mirroringHealth.value.totalSuccessCrud + 1 else _mirroringHealth.value.totalSuccessCrud
        val failures = if (!isSuccess) _mirroringHealth.value.totalFailedCrud + 1 else _mirroringHealth.value.totalFailedCrud

        _mirroringHealth.value = _mirroringHealth.value.copy(
            totalCrudOpsLogged = total,
            totalSuccessCrud = successes,
            totalFailedCrud = failures
        )
    }

    private fun recordListenerEvent(
        collection: String,
        eventType: String,
        docId: String,
        isFromCache: Boolean = false,
        hasPendingWrites: Boolean = false,
        serverTimestamp: Long? = null,
        clockSkewMs: Long? = null,
        summary: String = "",
        mirrorStatus: String = "SYNCED"
    ) {
        val entry = FirestoreEventListenerEntry(
            collection = collection,
            eventType = eventType,
            docId = docId,
            isFromCache = isFromCache,
            hasPendingWrites = hasPendingWrites,
            serverTimestamp = serverTimestamp,
            clockSkewMs = clockSkewMs,
            summary = summary,
            mirrorStatus = mirrorStatus
        )

        val list = _listenerEvents.value.toMutableList()
        if (list.size >= 300) {
            list.removeAt(list.size - 1)
        }
        list.add(0, entry)
        _listenerEvents.value = list
    }

    /**
     * Clears all collected in-memory logs.
     */
    fun clearLogs() {
        _crudLogs.value = emptyList()
        _listenerEvents.value = emptyList()
    }

    /**
     * Stops and tears down active snapshot listeners.
     */
    fun stopListeners() {
        usersListener?.remove()
        usersListener = null

        matchesListener?.remove()
        matchesListener = null

        _mirroringHealth.value = _mirroringHealth.value.copy(isListening = false)
        Log.i(TAG, "🛑 [STOPPED] FirestoreDiagnosticManager listeners detached.")
    }
}
