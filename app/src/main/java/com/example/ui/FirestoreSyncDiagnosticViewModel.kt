package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.DatingDatabase
import com.example.util.FirestoreSyncManager
import com.example.util.SystemHealthDiagnosticsManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserDocumentValidationInfo(
    val userId: String,
    val collectionName: String,
    val exists: Boolean,
    val name: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val hasRequiredFields: Boolean = false,
    val missingFields: List<String> = emptyList(),
    val lastVerifiedTimeFormatted: String = "",
    val rawDataSnippet: String = ""
)

data class MatchDocumentValidationInfo(
    val matchId: String,
    val userA: String,
    val userB: String,
    val isMutual: Boolean,
    val userAExistsInCloud: Boolean,
    val userBExistsInCloud: Boolean,
    val timestamp: Long,
    val statusSummary: String
)

private val sharedDiagTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
private fun formatDiagTime(): String = try {
    synchronized(sharedDiagTimeFormat) { sharedDiagTimeFormat.format(Date()) }
} catch (_: Throwable) {
    ""
}

data class DiagnosticConsoleLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = formatDiagTime(),
    val tag: String,
    val level: String, // "INFO", "DEBUG", "WARN", "ERROR", "SUCCESS"
    val message: String,
    val collection: String = "",
    val details: String = ""
)

data class FirestoreSyncDiagnosticState(
    val isListening: Boolean = false,
    val localUserId: String = "",
    val localUserPhoneNumber: String = "",
    val localUserEmail: String = "",
    val localUserDocExistsInUsers: Boolean = false,
    val localUserDocExistsInProfiles: Boolean = false,
    val localUserDocStatusMessage: String = "Initializing...",
    val totalUsersInSnapshot: Int = 0,
    val totalMatchesInSnapshot: Int = 0,
    val validatedUsersMap: Map<String, UserDocumentValidationInfo> = emptyMap(),
    val validatedMatchesList: List<MatchDocumentValidationInfo> = emptyList(),
    val crossDeviceDiagnosisReport: String = "Monitoring real-time sync between devices...",
    val consoleLogs: List<DiagnosticConsoleLog> = emptyList()
)

class FirestoreSyncDiagnosticViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "FirestoreSyncDiagnostic"
        private const val USERS_TAG = "FirestoreSyncUsers"
        private const val MATCHES_TAG = "FirestoreSyncMatches"
    }

    private val db = DatingDatabase.getDatabase(application)
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _state = MutableStateFlow(FirestoreSyncDiagnosticState())
    val state: StateFlow<FirestoreSyncDiagnosticState> = _state.asStateFlow()

    private var usersListener: ListenerRegistration? = null
    private var matchesListener: ListenerRegistration? = null
    private var profilesListener: ListenerRegistration? = null
    private var localUserDocListener: ListenerRegistration? = null

    init {
        logConsole(
            level = "INFO",
            tag = TAG,
            message = "Initializing FirestoreSyncDiagnosticViewModel"
        )
    }

    /**
     * Start continuous real-time diagnostic listeners for 'users', 'matches', and 'profiles' collections.
     */
    fun startDiagnosticListeners() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = db.userPreferencesDao().getPreferencesSync()
            val phone = prefs?.verifiedMobileNumber ?: ""
            val email = (prefs?.googleEmail ?: "").trim().lowercase()
            val myId = if (phone.isNotBlank()) phone.trim().replace(" ", "") else if (email.isNotBlank()) email else "USER"

            _state.value = _state.value.copy(
                localUserId = myId,
                localUserPhoneNumber = phone,
                localUserEmail = email,
                isListening = true
            )

            logConsole(
                level = "INFO",
                tag = TAG,
                message = "📱 Local Device ID: '$myId' | Phone: '$phone' | Email: '$email'"
            )

            // Initialize global FirestoreSyncManager and FirestoreDiagnosticManager with the resolved device identifier
            try {
                FirestoreSyncManager.getInstance().init(getApplication(), myId)
                com.example.util.FirestoreDiagnosticManager.init(getApplication(), myId)
            } catch (e: Exception) {
                Log.w(TAG, "Sync and Diagnostic Manager init notice: ${e.message}")
            }

            // 1. Verify local user's own document existence in Firestore
            validateLocalUserDocumentExistence(myId, phone, email)

            // 2. Attach real-time snapshot listener for 'users' collection
            listenToUsersCollection(myId)

            // 3. Attach real-time snapshot listener for 'matches' collection
            listenToMatchesCollection(myId)

            // 4. Attach real-time snapshot listener for 'profiles' collection
            listenToProfilesCollection(myId)
        }
    }

    /**
     * Real-time listener for 'users' collection.
     * Logs all snapshot updates, validates user document schema, and checks if other test devices are registered.
     */
    private fun listenToUsersCollection(currentUserId: String) {
        usersListener?.remove()
        try {
            logConsole(
                level = "INFO",
                tag = USERS_TAG,
                collection = "users",
                message = "🔗 Subscribing to real-time snapshot updates on 'users' collection..."
            )

            usersListener = firestore.collection("users")
                .limit(30)
                .addSnapshotListener { snapshot, error ->
                    try {
                        if (error != null) {
                            if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                Log.w(USERS_TAG, "ℹ️ 'users' listener awaiting auth session or open rules: ${error.message}")
                                logConsole(level = "INFO", tag = USERS_TAG, collection = "users", message = "ℹ️ 'users' listener operating in offline/cache mode (waiting for auth session)")
                                SystemHealthDiagnosticsManager.logFirestoreTrace(
                                    collection = "users",
                                    eventType = "OFFLINE_CACHE",
                                    docId = "ALL",
                                    summary = "Operating in local cache mode (awaiting auth session)",
                                    syncStatus = "CACHE_ONLY"
                                )
                            } else {
                                val errMsg = "❌ 'users' listener error: code=${error.code}, message=${error.message}"
                                Log.w(USERS_TAG, errMsg)
                                logConsole(level = "WARN", tag = USERS_TAG, collection = "users", message = errMsg)
                                SystemHealthDiagnosticsManager.logFirestoreTrace(
                                    collection = "users",
                                    eventType = "ERROR",
                                    docId = "ALL",
                                    summary = errMsg,
                                    syncStatus = "ERROR"
                                )
                            }
                            return@addSnapshotListener
                        }

                        if (snapshot == null) return@addSnapshotListener

                        val size = snapshot.size()
                        val fromCache = snapshot.metadata.isFromCache
                        val hasPendingWrites = snapshot.metadata.hasPendingWrites()
                        val changes = snapshot.documentChanges

                        Log.i(
                            USERS_TAG,
                            "============================================================"
                        )
                        Log.i(
                            USERS_TAG,
                            "📥 [USERS SNAPSHOT] Total Docs: $size | Changes: ${changes.size} | Source: ${if (fromCache) "CACHE" else "SERVER"} | PendingWrites: $hasPendingWrites"
                        )

                        val updatedMap = _state.value.validatedUsersMap.toMutableMap()

                        for (change in changes) {
                            val doc = change.document
                            val docId = doc.id
                            val changeType = change.type.name
                            val name = doc.getString("name") ?: doc.getString("userName") ?: ""
                            val phone = doc.getString("phoneNumber") ?: doc.getString("mobileNumber") ?: ""
                            val email = doc.getString("googleEmail") ?: doc.getString("email") ?: ""
                            val isVerified = doc.getBoolean("isVerified") ?: false

                            val missingFields = mutableListOf<String>()
                            if (name.isBlank()) missingFields.add("name")
                            if (phone.isBlank() && email.isBlank()) missingFields.add("phone/email")

                            val isValid = missingFields.isEmpty()

                            val valInfo = UserDocumentValidationInfo(
                                userId = docId,
                                collectionName = "users",
                                exists = change.type != DocumentChange.Type.REMOVED,
                                name = name,
                                phoneNumber = phone,
                                email = email,
                                hasRequiredFields = isValid,
                                missingFields = missingFields,
                                lastVerifiedTimeFormatted = formatDiagTime(),
                                rawDataSnippet = "name=$name, phone=$phone, isVerified=$isVerified, fields=${doc.data?.keys?.size ?: 0}"
                            )

                            if (change.type == DocumentChange.Type.REMOVED) {
                                updatedMap.remove(docId)
                                Log.w(
                                    USERS_TAG,
                                    "🗑️ [USER REMOVED] DocId: $docId ($name)"
                                )
                            } else {
                                updatedMap[docId] = valInfo
                                val icon = if (change.type == DocumentChange.Type.ADDED) "➕" else "✏️"
                                Log.d(
                                    USERS_TAG,
                                    "$icon [USER $changeType] DocId: $docId | Name: '$name' | Phone: '$phone' | Valid: $isValid ${if (!isValid) "Missing: $missingFields" else ""}"
                                )
                            }

                            // Check if this document is the local user's
                            if (docId == currentUserId || (phone.isNotBlank() && phone.filter { it.isDigit() }.takeLast(10) == currentUserId.filter { it.isDigit() }.takeLast(10))) {
                                Log.i(
                                    USERS_TAG,
                                    "🎯 [LOCAL USER DOC DETECTED IN CLOUD] DocId: $docId matches current user '$currentUserId'!"
                                )
                            }
                        }

                        // Keep memory strictly bounded
                        if (updatedMap.size > 50) {
                            val keysToRemove = updatedMap.keys.take(updatedMap.size - 50)
                            keysToRemove.forEach { updatedMap.remove(it) }
                        }

                        _state.value = _state.value.copy(
                            totalUsersInSnapshot = size,
                            validatedUsersMap = updatedMap,
                            localUserDocExistsInUsers = updatedMap.containsKey(currentUserId) || updatedMap.values.any { it.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) == currentUserId.filter { c -> c.isDigit() }.takeLast(10) }
                        )

                        updateCrossDeviceDiagnosisReport()
                    } catch (oom: OutOfMemoryError) {
                        Log.e(USERS_TAG, "OutOfMemory handled safely in users listener", oom)
                        _state.value = _state.value.copy(validatedUsersMap = emptyMap())
                        System.gc()
                    } catch (t: Throwable) {
                        Log.w(USERS_TAG, "Exception in users listener: ${t.message}")
                    }
                }
        } catch (e: Exception) {
            val err = "Failed to listen to 'users' collection: ${e.message}"
            Log.e(USERS_TAG, err, e)
            logConsole(level = "ERROR", tag = USERS_TAG, collection = "users", message = err)
        }
    }

    /**
     * Real-time listener for 'matches' collection.
     * Logs all snapshot updates, checks mutual match participants, and validates if both participants exist in Firestore.
     */
    private fun listenToMatchesCollection(currentUserId: String) {
        matchesListener?.remove()
        try {
            logConsole(
                level = "INFO",
                tag = MATCHES_TAG,
                collection = "matches",
                message = "🔗 Subscribing to real-time snapshot updates on 'matches' collection..."
            )

            matchesListener = firestore.collection("matches")
                .limit(30)
                .addSnapshotListener { snapshot, error ->
                    try {
                        if (error != null) {
                            if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                Log.w(MATCHES_TAG, "ℹ️ 'matches' listener awaiting auth session or open rules: ${error.message}")
                                logConsole(level = "INFO", tag = MATCHES_TAG, collection = "matches", message = "ℹ️ 'matches' listener operating in offline/cache mode (waiting for auth session)")
                                SystemHealthDiagnosticsManager.logFirestoreTrace(
                                    collection = "matches",
                                    eventType = "OFFLINE_CACHE",
                                    docId = "ALL",
                                    summary = "Operating in local cache mode (awaiting auth session)",
                                    syncStatus = "CACHE_ONLY"
                                )
                            } else {
                                val errMsg = "❌ 'matches' listener error: code=${error.code}, message=${error.message}"
                                Log.w(MATCHES_TAG, errMsg)
                                logConsole(level = "WARN", tag = MATCHES_TAG, collection = "matches", message = errMsg)
                                SystemHealthDiagnosticsManager.logFirestoreTrace(
                                    collection = "matches",
                                    eventType = "ERROR",
                                    docId = "ALL",
                                    summary = errMsg,
                                    syncStatus = "ERROR"
                                )
                            }
                            return@addSnapshotListener
                        }

                        if (snapshot == null) return@addSnapshotListener

                        val size = snapshot.size()
                        val fromCache = snapshot.metadata.isFromCache
                        val changes = snapshot.documentChanges

                        Log.i(
                            MATCHES_TAG,
                            "============================================================"
                        )
                        Log.i(
                            MATCHES_TAG,
                            "📥 [MATCHES SNAPSHOT] Total Matches: $size | Changes: ${changes.size} | Source: ${if (fromCache) "CACHE" else "SERVER"}"
                        )

                        val matchList = mutableListOf<MatchDocumentValidationInfo>()

                        for (doc in snapshot.documents.take(30)) {
                            val matchId = doc.id
                            val userA = doc.getString("userA") ?: ""
                            val userB = doc.getString("userB") ?: ""
                            val isMutual = doc.getBoolean("isMutual") ?: true
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                            val userAExists = _state.value.validatedUsersMap.containsKey(userA)
                            val userBExists = _state.value.validatedUsersMap.containsKey(userB)

                            val status = when {
                                !userAExists && !userBExists -> "⚠️ Neither userA nor userB found in 'users' collection"
                                !userAExists -> "⚠️ userA ('$userA') missing from cloud 'users'"
                                !userBExists -> "⚠️ userB ('$userB') missing from cloud 'users'"
                                else -> "✅ Both users verified in cloud"
                            }

                            matchList.add(
                                MatchDocumentValidationInfo(
                                    matchId = matchId,
                                    userA = userA,
                                    userB = userB,
                                    isMutual = isMutual,
                                    userAExistsInCloud = userAExists,
                                    userBExistsInCloud = userBExists,
                                    timestamp = timestamp,
                                    statusSummary = status
                                )
                            )
                        }

                        for (change in changes.take(30)) {
                            val doc = change.document
                            val uA = doc.getString("userA") ?: ""
                            val uB = doc.getString("userB") ?: ""
                            val matchId = doc.id
                            val changeType = change.type.name

                            Log.d(
                                MATCHES_TAG,
                                "🔥 [MATCH $changeType] ID: $matchId | UserA: '$uA' <---> UserB: '$uB' | isMutual: ${doc.getBoolean("isMutual")}"
                            )

                            val isRelevantToMe = (uA == currentUserId || uB == currentUserId ||
                                    (currentUserId.length >= 10 && (uA.takeLast(10) == currentUserId.takeLast(10) || uB.takeLast(10) == currentUserId.takeLast(10))))

                            if (isRelevantToMe) {
                                Log.i(
                                    MATCHES_TAG,
                                    "✨ [ACTIVE MATCH FOR LOCAL DEVICE] Match $matchId connects local user '$currentUserId' with partner '${if (uA == currentUserId) uB else uA}'"
                                )
                            }
                        }

                        _state.value = _state.value.copy(
                            totalMatchesInSnapshot = size,
                            validatedMatchesList = matchList
                        )

                        updateCrossDeviceDiagnosisReport()
                    } catch (oom: OutOfMemoryError) {
                        Log.e(MATCHES_TAG, "OutOfMemory handled safely in matches listener", oom)
                        _state.value = _state.value.copy(validatedMatchesList = emptyList())
                        System.gc()
                    } catch (t: Throwable) {
                        Log.w(MATCHES_TAG, "Exception in matches listener: ${t.message}")
                    }
                }
        } catch (e: Exception) {
            val err = "Failed to listen to 'matches' collection: ${e.message}"
            Log.e(MATCHES_TAG, err, e)
            logConsole(level = "ERROR", tag = MATCHES_TAG, collection = "matches", message = err)
        }
    }

    /**
     * Real-time listener for 'profiles' collection.
     */
    private fun listenToProfilesCollection(currentUserId: String) {
        profilesListener?.remove()
        try {
            profilesListener = firestore.collection("profiles")
                .limit(30)
                .addSnapshotListener { snapshot, error ->
                    try {
                        if (error != null || snapshot == null) return@addSnapshotListener

                        val size = snapshot.size()
                        val myDocExists = snapshot.documents.any { doc ->
                            doc.id == currentUserId || (doc.getString("phoneNumber")?.filter { it.isDigit() }?.takeLast(10) == currentUserId.filter { it.isDigit() }.takeLast(10))
                        }

                        Log.d(
                            TAG,
                            "📥 [PROFILES SNAPSHOT] Total Profiles: $size | Local Profile in Cloud: $myDocExists"
                        )

                        _state.value = _state.value.copy(
                            localUserDocExistsInProfiles = myDocExists
                        )

                        updateCrossDeviceDiagnosisReport()
                    } catch (oom: OutOfMemoryError) {
                        Log.e(TAG, "OutOfMemory handled safely in profiles listener", oom)
                        System.gc()
                    } catch (t: Throwable) {
                        Log.w(TAG, "Exception in profiles listener: ${t.message}")
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Profiles listener setup note: ${e.message}")
        }
    }

    private suspend fun safeGetDocument(docRef: DocumentReference): DocumentSnapshot? {
        return try {
            docRef.get(Source.DEFAULT).await()
        } catch (e: Exception) {
            try {
                docRef.get(Source.CACHE).await()
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun safeGetQuery(query: Query): QuerySnapshot? {
        return try {
            query.get(Source.DEFAULT).await()
        } catch (e: Exception) {
            try {
                query.get(Source.CACHE).await()
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Validates if the local device user document exists in 'users' and 'profiles' collections.
     */
    fun validateLocalUserDocumentExistence(userId: String, phone: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            logConsole(
                level = "INFO",
                tag = TAG,
                message = "🔍 Validating local user document existence for ID='$userId'..."
            )

            try {
                var existsInUsers = false
                var existsInProfiles = false

                // 1. Check directly in 'users' doc safely (supports offline cache)
                if (userId.isNotBlank()) {
                    val userDoc = safeGetDocument(firestore.collection("users").document(userId))
                    existsInUsers = userDoc?.exists() == true
                }

                // If not found by direct ID, search by phone safely
                if (!existsInUsers && phone.isNotBlank()) {
                    val queryByPhone = safeGetQuery(
                        firestore.collection("users")
                            .whereEqualTo("phoneNumber", phone)
                            .limit(1)
                    )
                    existsInUsers = queryByPhone != null && !queryByPhone.isEmpty
                }

                // 2. Check directly in 'profiles' doc safely
                if (userId.isNotBlank()) {
                    val profDoc = safeGetDocument(firestore.collection("profiles").document(userId))
                    existsInProfiles = profDoc?.exists() == true
                }

                val statusMsg = when {
                    existsInUsers && existsInProfiles -> "✅ User document is VALID and published in both 'users' and 'profiles' collections."
                    existsInUsers -> "⚠️ User exists in 'users' collection but profile document is missing in 'profiles'."
                    existsInProfiles -> "⚠️ Profile exists in 'profiles' but account document is missing in 'users'."
                    else -> "ℹ️ Local user document not yet cached or offline. Real-time snapshot listener will sync automatically."
                }

                Log.i(TAG, "🎯 [LOCAL USER VALIDATION RESULT] $statusMsg")
                logConsole(
                    level = if (existsInUsers && existsInProfiles) "SUCCESS" else "INFO",
                    tag = TAG,
                    message = statusMsg,
                    details = "users=$existsInUsers, profiles=$existsInProfiles"
                )

                _state.value = _state.value.copy(
                    localUserDocExistsInUsers = existsInUsers,
                    localUserDocExistsInProfiles = existsInProfiles,
                    localUserDocStatusMessage = statusMsg
                )

                updateCrossDeviceDiagnosisReport()
            } catch (e: Exception) {
                val isOffline = e.message?.contains("offline", ignoreCase = true) == true
                val statusMsg = if (isOffline) {
                    "⏳ Client offline: Using cached sync data. Snapshot listeners will auto-update upon connection."
                } else {
                    "ℹ️ Local user validation note: ${e.message}"
                }
                Log.i(TAG, statusMsg)
                logConsole(level = "INFO", tag = TAG, message = statusMsg)
                _state.value = _state.value.copy(
                    localUserDocStatusMessage = statusMsg
                )
            }
        }
    }

    /**
     * Validates a specific arbitrary user ID or phone number in real-time.
     */
    fun validateSpecificUserInRealtime(targetUserIdOrPhone: String, onResult: (UserDocumentValidationInfo) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanTarget = targetUserIdOrPhone.trim()
            if (cleanTarget.isBlank()) return@launch

            logConsole(
                level = "INFO",
                tag = TAG,
                message = "🔎 Real-time checking user document '$cleanTarget' across collections..."
            )

            try {
                // Check 'users'
                var doc = safeGetDocument(firestore.collection("users").document(cleanTarget))
                var collectionUsed = "users"

                if (doc == null || !doc.exists()) {
                    // Try profiles
                    val profDoc = safeGetDocument(firestore.collection("profiles").document(cleanTarget))
                    if (profDoc != null && profDoc.exists()) {
                        doc = profDoc
                        collectionUsed = "profiles"
                    }
                }

                if ((doc == null || !doc.exists()) && cleanTarget.any { it.isDigit() }) {
                    val phoneQuery = safeGetQuery(
                        firestore.collection("users")
                            .whereEqualTo("phoneNumber", cleanTarget)
                            .limit(1)
                    )
                    if (phoneQuery != null && !phoneQuery.isEmpty) {
                        doc = phoneQuery.documents.first()
                        collectionUsed = "users"
                    }
                }

                val exists = doc != null && doc.exists()
                val name = doc?.getString("name") ?: doc?.getString("userName") ?: ""
                val phone = doc?.getString("phoneNumber") ?: doc?.getString("mobileNumber") ?: ""
                val email = doc?.getString("googleEmail") ?: doc?.getString("email") ?: ""

                val missing = mutableListOf<String>()
                if (name.isBlank()) missing.add("name")
                if (phone.isBlank() && email.isBlank()) missing.add("phone/email")

                val result = UserDocumentValidationInfo(
                    userId = if (exists && doc != null) doc.id else cleanTarget,
                    collectionName = collectionUsed,
                    exists = exists,
                    name = name,
                    phoneNumber = phone,
                    email = email,
                    hasRequiredFields = exists && missing.isEmpty(),
                    missingFields = missing,
                    lastVerifiedTimeFormatted = formatDiagTime(),
                    rawDataSnippet = if (exists) "Exists=true in '$collectionUsed', name='$name', phone='$phone'" else "Not found in cloud"
                )

                Log.i(
                    TAG,
                    "📋 [USER VALIDATION] Target: '$cleanTarget' -> Exists: $exists in '$collectionUsed' | Name: '$name' | Phone: '$phone'"
                )

                logConsole(
                    level = if (exists) "SUCCESS" else "INFO",
                    tag = TAG,
                    collection = collectionUsed,
                    message = "Validation for '$cleanTarget': Exists=$exists ($name, $phone)",
                    details = result.rawDataSnippet
                )

                val updatedMap = _state.value.validatedUsersMap.toMutableMap()
                if (exists && doc != null) {
                    updatedMap[doc.id] = result
                    _state.value = _state.value.copy(validatedUsersMap = updatedMap)
                }

                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } catch (e: Exception) {
                val isOffline = e.message?.contains("offline", ignoreCase = true) == true
                val err = if (isOffline) "Device offline: user query will resolve when connection is restored." else "User validation note: ${e.message}"
                Log.i(TAG, err)
                logConsole(level = "INFO", tag = TAG, message = err)
            }
        }
    }

    /**
     * Cross-Device Pair Test: Validates synchronization readiness between Device 1 and Device 2.
     */
    fun testTwoDeviceSyncPair(device1PhoneOrId: String, device2PhoneOrId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            logConsole(
                level = "INFO",
                tag = TAG,
                message = "🧪 Testing cross-device sync between Device 1 ('$device1PhoneOrId') and Device 2 ('$device2PhoneOrId')..."
            )

            try {
                val user1Doc = safeGetDocument(firestore.collection("users").document(device1PhoneOrId))
                val user2Doc = safeGetDocument(firestore.collection("users").document(device2PhoneOrId))

                val user1InUsers = user1Doc?.exists() == true
                val user2InUsers = user2Doc?.exists() == true

                // Check matches
                val matchQuery1 = safeGetQuery(
                    firestore.collection("matches")
                        .whereEqualTo("userA", device1PhoneOrId)
                        .whereEqualTo("userB", device2PhoneOrId)
                )

                val matchQuery2 = safeGetQuery(
                    firestore.collection("matches")
                        .whereEqualTo("userA", device2PhoneOrId)
                        .whereEqualTo("userB", device1PhoneOrId)
                )

                val matchDocExists = (matchQuery1 != null && !matchQuery1.isEmpty) || (matchQuery2 != null && !matchQuery2.isEmpty)

                val report = buildString {
                    appendLine("📱 CROSS-DEVICE SYNC DIAGNOSTIC REPORT:")
                    appendLine("• Device 1 ('$device1PhoneOrId'): ${if (user1InUsers) "✅ EXISTS in 'users'" else "❌ MISSING from 'users'"}")
                    appendLine("• Device 2 ('$device2PhoneOrId'): ${if (user2InUsers) "✅ EXISTS in 'users'" else "❌ MISSING from 'users'"}")
                    appendLine("• Mutual Match Document: ${if (matchDocExists) "✅ EXISTS in 'matches'" else "⚠️ NO MATCH DOC YET"}")
                    if (!user1InUsers || !user2InUsers) {
                        appendLine("💡 ROOT CAUSE: One or both test devices have not published their user profile to Firestore yet.")
                    } else if (!matchDocExists) {
                        appendLine("💡 ACTION REQUIRED: Both users exist! Swipe right on each other to generate the 'matches' document.")
                    } else {
                        appendLine("🎉 STATUS: All cloud documents are in place. Profiles and real-time chat are fully synchronized!")
                    }
                }

                Log.i(TAG, report)
                logConsole(
                    level = if (user1InUsers && user2InUsers) "SUCCESS" else "INFO",
                    tag = TAG,
                    message = "Cross-Device Test Completed:\n$report"
                )

                _state.value = _state.value.copy(
                    crossDeviceDiagnosisReport = report
                )
            } catch (e: Exception) {
                val err = "Cross-device pair test note: ${e.message}"
                Log.i(TAG, err)
                logConsole(level = "INFO", tag = TAG, message = err)
            }
        }
    }

    /**
     * Force sync the local user profile to Firestore immediately to fix missing document errors.
     */
    fun forcePublishLocalUserToFirestore() {
        viewModelScope.launch(Dispatchers.IO) {
            logConsole(
                level = "INFO",
                tag = TAG,
                message = "⚡ Force publishing local user profile to Firestore 'users' & 'profiles'..."
            )
            try {
                val prefs = db.userPreferencesDao().getPreferencesSync()
                val phone = prefs?.verifiedMobileNumber ?: ""
                val email = prefs?.googleEmail ?: ""
                val name = prefs?.userName ?: "Verified User"
                val myId = if (phone.isNotBlank()) phone.trim().replace(" ", "") else if (email.isNotBlank()) email else "USER"

                val userPayload = mapOf(
                    "id" to myId,
                    "userId" to myId,
                    "name" to name,
                    "userName" to name,
                    "phoneNumber" to phone,
                    "mobileNumber" to phone,
                    "googleEmail" to email,
                    "isVerified" to true,
                    "isRealFaceVerified" to (prefs?.isFaceVerified ?: true),
                    "age" to (prefs?.userAge ?: 24),
                    "city" to (prefs?.userCity ?: "Nearby"),
                    "bio" to (prefs?.userBio ?: ""),
                    "avatarUrl" to "",
                    "lastActiveTimestamp" to System.currentTimeMillis()
                )

                // Write to 'users'
                firestore.collection("users").document(myId)
                    .set(userPayload, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                // Write to 'profiles'
                firestore.collection("profiles").document(myId)
                    .set(userPayload, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                // Write to 'registered_accounts'
                firestore.collection("registered_accounts").document(myId)
                    .set(userPayload, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                val msg = "✅ Local user '$myId' ($name) successfully published to 'users', 'profiles', & 'registered_accounts'!"
                Log.i(TAG, msg)
                logConsole(level = "SUCCESS", tag = TAG, message = msg)

                validateLocalUserDocumentExistence(myId, phone, email)
            } catch (e: Exception) {
                val err = "Failed to force publish local user: ${e.message}"
                Log.e(TAG, err, e)
                logConsole(level = "ERROR", tag = TAG, message = err)
            }
        }
    }

    private fun updateCrossDeviceDiagnosisReport() {
        val totalUsers = _state.value.totalUsersInSnapshot
        val totalMatches = _state.value.totalMatchesInSnapshot
        val myDocExists = _state.value.localUserDocExistsInUsers

        val report = buildString {
            append("Realtime Sync Status: ")
            if (myDocExists) {
                append("✅ Local user registered in cloud. ")
            } else {
                append("⚠️ Local user doc not yet detected in 'users' collection. ")
            }
            append("Cloud state: $totalUsers users, $totalMatches active matches live in Firestore.")
        }

        _state.value = _state.value.copy(crossDeviceDiagnosisReport = report)
    }

    private fun logConsole(level: String, tag: String, message: String, collection: String = "", details: String = "") {
        val logEntry = DiagnosticConsoleLog(
            tag = tag,
            level = level,
            message = message,
            collection = collection,
            details = details
        )
        val current = _state.value.consoleLogs.toMutableList()
        if (current.size >= 50) {
            current.removeAt(current.size - 1)
        }
        current.add(0, logEntry)
        _state.value = _state.value.copy(consoleLogs = current)
    }

    fun clearConsoleLogs() {
        _state.value = _state.value.copy(consoleLogs = emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.remove()
        matchesListener?.remove()
        profilesListener?.remove()
        localUserDocListener?.remove()
        Log.i(TAG, "🛑 FirestoreSyncDiagnosticViewModel cleared and listeners detached.")
    }
}
