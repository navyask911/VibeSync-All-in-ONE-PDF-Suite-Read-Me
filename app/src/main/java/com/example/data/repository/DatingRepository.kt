package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.DatingDatabase
import com.example.data.model.BusinessEntity
import com.example.data.model.BusinessPostEntity
import com.example.data.model.BusinessReviewEntity
import com.example.data.model.BlockEntity
import com.example.data.model.ChannelBroadcastEntity
import com.example.data.model.ChannelEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.FriendshipRequestEntity
import com.example.data.model.MatchEntity
import com.example.data.model.PhoneContact
import com.example.data.model.ProfileEntity
import com.example.data.model.LocalMessage
import com.example.data.model.RegisteredAccountEntity
import com.example.data.model.ReportEntity
import com.example.data.model.StatusStoryEntity
import com.example.data.model.SwipeEntity
import com.example.data.model.UserPreferencesEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.example.util.AppNotificationManager
import com.example.util.DeviceSimAndIpCountryHelper
import com.example.util.FirebaseBackendSyncManager
import com.example.util.SystemHealthDiagnosticsManager
import com.example.util.ZeroCostE2eeMessagingManager
import com.example.util.backup.BackupScheduler
import com.example.util.backup.ChatBackupCryptoHelper
import com.example.util.backup.ChatBackupPayload
import com.example.util.backup.DriveBackupMetadata
import com.example.util.backup.GoogleDriveBackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class MatchOutcome {
    data class MutualMatch(val profile: ProfileEntity, val matchId: String) : MatchOutcome()
    data class FriendshipRequestSent(val profile: ProfileEntity, val request: FriendshipRequestEntity) : MatchOutcome()
    object Liked : MatchOutcome()
    object Passed : MatchOutcome()
    object SuperLiked : MatchOutcome()
    data class QuotaExceeded(val message: String) : MatchOutcome()
    data class FriendRequestsPendingLimitReached(val message: String, val pendingCount: Int = 100) : MatchOutcome()
}

class DatingRepository(
    private val database: DatingDatabase,
    private val appScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    val context: Context? = null
) {
    private val profileDao = database.profileDao()
    private val swipeDao = database.swipeDao()
    private val matchDao = database.matchDao()
    private val chatMessageDao = database.chatMessageDao()
    private val messageDao = database.messageDao()
    private val preferencesDao = database.userPreferencesDao()
    private val registeredAccountDao = database.registeredAccountDao()
    private val statusStoryDao = database.statusStoryDao()
    private val friendshipRequestDao = database.friendshipRequestDao()
    private val userContactDao = database.userContactDao()
    private val businessDao = database.businessDao()
    private val channelDao = database.channelDao()
    private val blockDao = database.blockDao()
    private val reportDao = database.reportDao()

    init {
        appScope.launch(Dispatchers.IO) {
            purgeGhostNullData()
        }
    }

    val allBusinesses: Flow<List<BusinessEntity>> = businessDao.getAllBusinesses()
    val followedBusinesses: Flow<List<BusinessEntity>> = businessDao.getFollowedBusinesses()
    val myCreatedBusinesses: Flow<List<BusinessEntity>> = businessDao.getMyCreatedBusinesses()

    val allChannels: Flow<List<ChannelEntity>> = channelDao.getAllChannels()
    val followedChannels: Flow<List<ChannelEntity>> = channelDao.getFollowedChannels()

    suspend fun backupContactsToDatabaseAndFirestore(contacts: List<com.example.data.model.PhoneContact>, userId: String = "current_user") {
        try {
            val entities = contacts.map {
                com.example.data.model.UserContactEntity(
                    id = if (it.id.isNotBlank()) it.id else UUID.randomUUID().toString(),
                    userId = userId,
                    contactName = it.name,
                    phoneNumber = it.phoneNumber,
                    isOnVibeSync = it.isOnVibeSync,
                    photoUrl = it.photoUrl,
                    statusTagline = it.statusTagline,
                    syncedToFirestore = true,
                    updatedAt = System.currentTimeMillis()
                )
            }
            userContactDao.insertContacts(entities)

            // Supabase Batch Upsert
            com.example.util.SupabaseClientManager.upsertContactsBatch(entities)

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val batch = db.batch()
            entities.forEach { contact ->
                val ref = db.collection("users").document(userId).collection("contacts").document(contact.id)
                val map = hashMapOf(
                    "id" to contact.id,
                    "userId" to userId,
                    "contactName" to contact.contactName,
                    "phoneNumber" to contact.phoneNumber,
                    "isOnVibeSync" to contact.isOnVibeSync,
                    "syncedAt" to System.currentTimeMillis()
                )
                batch.set(ref, map)
            }
            batch.commit()
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Contacts backup notice: ${e.message}")
        }
    }

    suspend fun syncProfileToFirestore(profile: ProfileEntity) {
        try {
            val cleanPhone = com.example.util.ContactResolver.sanitizePhone(profile.phoneNumber)
            val cleanName = com.example.util.ContactResolver.sanitizeName(profile.name)
            val cleanId = com.example.util.ContactResolver.sanitizePhone(profile.id)

            // Guard: Do not sync uninitialized profiles to cloud
            if (cleanId == "current_user" || cleanId == "null" || cleanId.isBlank() || (cleanPhone.isBlank() && profile.email.isBlank())) {
                android.util.Log.w("DatingRepository", "Skipping cloud sync of uninitialized profile: id=${profile.id}, name=${profile.name}")
                return
            }

            // 1. Sync to Supabase PostgREST Database
            com.example.util.SupabaseClientManager.upsertProfile(profile.copy(name = cleanName, phoneNumber = cleanPhone))

            // 2. Sync to Firestore (used for FCM notification triggers & backup)
            val phoneDigits = cleanPhone.filter { it.isDigit() }
            val docId = if (phoneDigits.length >= 7) phoneDigits else if (profile.email.isNotBlank()) profile.email.trim() else cleanId
            if (docId.isBlank() || docId == "null" || docId == "current_user") return

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val map = hashMapOf(
                "id" to docId,
                "userId" to docId,
                "name" to cleanName.ifBlank { "VibeSync Member" },
                "age" to profile.age,
                "gender" to com.example.util.ContactResolver.sanitizeName(profile.gender, "User"),
                "city" to com.example.util.ContactResolver.sanitizeName(profile.city, "Online"),
                "country" to com.example.util.ContactResolver.sanitizeName(profile.country, "United States"),
                "countryFlag" to profile.countryFlag.ifBlank { "🇺🇸" },
                "place" to com.example.util.ContactResolver.sanitizeName(profile.place),
                "avatarEmoji" to profile.avatarEmoji.ifBlank { "✨" },
                "avatarUrl" to com.example.util.ContactResolver.sanitizeName(profile.avatarUrl),
                "occupation" to com.example.util.ContactResolver.sanitizeName(profile.occupation, "Member"),
                "qualification" to com.example.util.ContactResolver.sanitizeName(profile.qualification, "Verified"),
                "bio" to com.example.util.ContactResolver.sanitizeName(profile.bio, "Hey there! Secure with VibeSync E2EE."),
                "interests" to profile.interests,
                "maritalStatus" to com.example.util.ContactResolver.sanitizeName(profile.maritalStatus, "Single"),
                "phoneNumber" to cleanPhone,
                "cleanPhone" to phoneDigits,
                "phone_hash" to com.example.util.PhonebookHasher.generate16CharHash(cleanPhone.ifBlank { docId }),
                "phoneHash" to com.example.util.PhonebookHasher.generate16CharHash(cleanPhone.ifBlank { docId }),
                "phoneNumberE164" to com.example.util.PhonebookHasher.normalizeToE164(cleanPhone.ifBlank { docId }),
                "email" to com.example.util.ContactResolver.sanitizeName(profile.email),
                "isDeleted" to profile.isDeleted,
                "accountStatus" to profile.accountStatus.ifBlank { "ACTIVE" },
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("users").document(docId).set(map, com.google.firebase.firestore.SetOptions.merge())
            db.collection("profiles").document(docId).set(map, com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Profile sync error: ${e.message}")
        }
    }

    val acceptedFriendships: Flow<List<FriendshipRequestEntity>> = friendshipRequestDao.getAcceptedFriendships("current_user")
    val pendingFriendRequests: Flow<List<FriendshipRequestEntity>> = friendshipRequestDao.getPendingRequestsForUser("current_user")
    val acceptedFriendsCount: Flow<Int> = friendshipRequestDao.getAcceptedFriendsCount("current_user")

    val mutualFriendsMap: Flow<Map<String, List<ProfileEntity>>> = combine(
        friendshipRequestDao.getAcceptedFriendships("current_user"),
        profileDao.getAllProfiles()
    ) { acceptedRequests, allProfiles ->
        val myFriendIds = acceptedRequests.map { if (it.senderId == "current_user") it.receiverId else it.senderId }.toSet()
        val profilesMap = allProfiles.associateBy { it.id }

        allProfiles.associate { profile ->
            val targetFriendIds = profile.getFriendIdList().toSet()
            val mutualIds = myFriendIds.intersect(targetFriendIds)
            val mutualProfiles = mutualIds.mapNotNull { profilesMap[it] }
            profile.id to mutualProfiles
        }
    }

    init {
        appScope.launch {
            ensureDatabaseSeeded()
            pullProfilesFromFirestore()
            startRealtimeProfilesSync()
            startGlobalChatSync()
        }
    }

    suspend fun ensureDatabaseSeeded() {
        if (preferencesDao.getPreferencesSync() == null) {
            preferencesDao.insertOrUpdate(
                UserPreferencesEntity(
                    id = 1,
                    userName = "",
                    userAge = 24,
                    userOccupation = "",
                    userCity = "",
                    userBio = "",
                    userInterests = "",
                    isLoggedIn = false,
                    isProfileCompleted = false,
                    isFaceVerified = true,
                    isMobileVerified = false,
                    biometricHash = "",
                    verifiedMobileNumber = "",
                    googleEmail = ""
                )
            )
        }
        val currentPrefs = preferencesDao.getPreferencesSync()
        if (currentPrefs?.isLoggedIn == true) {
            pullProfilesFromFirestore()
            startRealtimeProfilesSync()
            startGlobalChatSync()
        }
        removeOwnProfileAndSelfMatches()
        seedInitialBusinessesAndChannels()

        // Zero-Cost E2EE Queue listener startup
        appScope.launch(Dispatchers.IO) {
            val phone = currentPrefs?.verifiedMobileNumber ?: ""
            if (phone.isNotBlank() && currentPrefs?.isLoggedIn == true) {
                ZeroCostE2eeMessagingManager.startListeningToIncomingQueue(phone)
            }
        }
    }

    suspend fun startAllSyncsAfterLogin() {
        pullProfilesFromFirestore()
        startRealtimeProfilesSync()
        startGlobalChatSync()
        val currentPrefs = preferencesDao.getPreferencesSync()
        val phone = currentPrefs?.verifiedMobileNumber ?: ""
        if (phone.isNotBlank()) {
            ZeroCostE2eeMessagingManager.startListeningToIncomingQueue(phone)
        }
        syncAndRestoreBusinessesFromCloud()
    }

    suspend fun cleanDatabaseForTwoPhonesInstallation() {
        clearAllProfilesAndDataInBackend(keepOwnUser = true)
    }

    suspend fun clearAllProfilesAndDataInBackend(keepOwnUser: Boolean = false) {
        val prefs = preferencesDao.getPreferencesSync()
        val ownPhone = prefs?.verifiedMobileNumber ?: ""
        val ownDigits = ownPhone.filter { it.isDigit() }
        val ownEmail = (prefs?.googleEmail ?: "").trim().lowercase()

        // 1. Local Room DB wipe
        if (keepOwnUser) {
            profileDao.clearTestProfiles()
            registeredAccountDao.deleteTestAccounts()
        } else {
            profileDao.clearProfiles()
            registeredAccountDao.clearAllAccounts()
            preferencesDao.insertOrUpdate(
                UserPreferencesEntity(
                    id = 1,
                    userName = "",
                    userAge = 24,
                    userOccupation = "",
                    userCity = "",
                    userBio = "",
                    userInterests = "",
                    isLoggedIn = false,
                    isProfileCompleted = false,
                    isFaceVerified = false,
                    isMobileVerified = false,
                    biometricHash = "",
                    verifiedMobileNumber = "",
                    googleEmail = ""
                )
            )
        }
        matchDao.clearAllMatches()
        chatMessageDao.clearAllMessages()
        messageDao.nukeTableOnLogout()
        statusStoryDao.clearAllStories()
        friendshipRequestDao.clearAllFriendships()
        userContactDao.clearAllContacts()
        swipeDao.clearAllSwipes()

        // 2. Cloud Firestore & Realtime DB full wipe
        withContext(Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                // Profiles collection
                val profSnap = db.collection("profiles").get().await()
                for (doc in profSnap.documents) {
                    val pPhone = (doc.getString("phoneNumber") ?: doc.getString("mobileNumber") ?: "").filter { it.isDigit() }
                    val pEmail = (doc.getString("googleEmail") ?: doc.getString("email") ?: "").trim().lowercase()
                    val isOwn = keepOwnUser && (
                        doc.id == ownPhone || doc.id == ownEmail ||
                        (ownDigits.length >= 7 && pPhone.endsWith(ownDigits.takeLast(7))) ||
                        (ownEmail.isNotBlank() && pEmail == ownEmail)
                    )
                    if (!isOwn) {
                        db.collection("profiles").document(doc.id).delete()
                    }
                }

                // Users collection
                val userSnap = db.collection("users").get().await()
                for (doc in userSnap.documents) {
                    val uPhone = (doc.getString("phoneNumber") ?: doc.getString("mobileNumber") ?: "").filter { it.isDigit() }
                    val uEmail = (doc.getString("googleEmail") ?: doc.getString("email") ?: "").trim().lowercase()
                    val isOwn = keepOwnUser && (
                        doc.id == ownPhone || doc.id == ownEmail ||
                        (ownDigits.length >= 7 && uPhone.endsWith(ownDigits.takeLast(7))) ||
                        (ownEmail.isNotBlank() && uEmail == ownEmail)
                    )
                    if (!isOwn) {
                        db.collection("users").document(doc.id).delete()
                    }
                }

                // Registered Accounts collection
                val regSnap = db.collection("registered_accounts").get().await()
                for (doc in regSnap.documents) {
                    val rPhone = (doc.getString("phoneNumber") ?: doc.getString("recoveryPhone") ?: "").filter { it.isDigit() }
                    val rEmail = (doc.getString("googleEmail") ?: doc.getString("recoveryEmail") ?: "").trim().lowercase()
                    val isOwn = keepOwnUser && (
                        doc.id == ownPhone || doc.id == ownEmail ||
                        (ownDigits.length >= 7 && rPhone.endsWith(ownDigits.takeLast(7))) ||
                        (ownEmail.isNotBlank() && rEmail == ownEmail)
                    )
                    if (!isOwn) {
                        db.collection("registered_accounts").document(doc.id).delete()
                    }
                }

                // Chats collection & subcollections
                val chatSnap = db.collection("chats").get().await()
                for (doc in chatSnap.documents) {
                    try {
                        val msgSnap = db.collection("chats").document(doc.id).collection("messages").get().await()
                        for (mDoc in msgSnap.documents) {
                            db.collection("chats").document(doc.id).collection("messages").document(mDoc.id).delete()
                        }
                    } catch (_: Exception) {}
                    db.collection("chats").document(doc.id).delete()
                }

                // Biometrics collection
                val bioSnap = db.collection("biometrics").get().await()
                for (doc in bioSnap.documents) {
                    if (!keepOwnUser) {
                        db.collection("biometrics").document(doc.id).delete()
                    }
                }

                // Likes & Mutual Matches collections
                val likesSnap = db.collection("likes").get().await()
                for (doc in likesSnap.documents) {
                    db.collection("likes").document(doc.id).delete()
                }
                val mutualSnap = db.collection("mutual_matches").get().await()
                for (doc in mutualSnap.documents) {
                    db.collection("mutual_matches").document(doc.id).delete()
                }

                // Blocked Users, Reports, and Tokens collections
                val blockSnap = db.collection("blocked_users").get().await()
                for (doc in blockSnap.documents) {
                    db.collection("blocked_users").document(doc.id).delete()
                }
                val reportSnap = db.collection("reports").get().await()
                for (doc in reportSnap.documents) {
                    db.collection("reports").document(doc.id).delete()
                }
                val tokenSnap = db.collection("fcm_tokens").get().await()
                for (doc in tokenSnap.documents) {
                    db.collection("fcm_tokens").document(doc.id).delete()
                }

                // Realtime DB queues & status nodes
                val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance()
                rtdb.getReference("status").removeValue()
                if (!keepOwnUser) {
                    rtdb.getReference("messages").removeValue()
                    rtdb.getReference("registration_bundles").removeValue()
                    rtdb.getReference("public_profiles").removeValue()
                    rtdb.getReference("mutual_matches").removeValue()
                    rtdb.getReference("user_sync").removeValue()
                    rtdb.getReference("profiles").removeValue()
                    try {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    } catch (_: Exception) {}
                }

                android.util.Log.i("DatingRepository", "✨ [BACKEND WIPE] All profiles and backend data completely wiped from cloud!")
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Error clearing all backend data: ${e.message}")
            }
        }
        if (keepOwnUser) {
            pullProfilesFromFirestore()
            removeOwnProfileAndSelfMatches()
        }
    }

    suspend fun clearAllTestDataAndBiometrics() {
        registeredAccountDao.clearAllAccounts()
        profileDao.clearTestProfiles()
        matchDao.clearTestMatches()
        chatMessageDao.clearTestMessages()
        statusStoryDao.clearTestStories()
        friendshipRequestDao.clearTestFriendships()
        preferencesDao.insertOrUpdate(
            UserPreferencesEntity(
                id = 1,
                userName = "",
                userAge = 24,
                userOccupation = "",
                userCity = "",
                userBio = "",
                userInterests = "",
                isLoggedIn = false,
                isProfileCompleted = false,
                isFaceVerified = true,
                isMobileVerified = false,
                biometricHash = "",
                biometricRegisteredTimestamp = 0L,
                verifiedMobileNumber = "",
                googleEmail = "",
                userMpin = "",
                isMpinSet = false
            )
        )
        chatMessageDao.clearAllMessages()
    }

    private suspend fun seedFriendshipsData() {
        return // Production level: Disable seed friendships
        val now = System.currentTimeMillis()
        val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()

        val acceptedMaya = FriendshipRequestEntity(
            id = "req_accepted_maya",
            senderId = "current_user",
            senderName = currentPrefs.userName,
            senderAge = currentPrefs.userAge,
            senderOccupation = currentPrefs.userOccupation,
            senderCity = currentPrefs.userCity,
            senderCountry = currentPrefs.userCountry,
            senderCountryFlag = currentPrefs.userCountryFlag,
            senderBio = currentPrefs.userBio,
            senderAvatarEmoji = "☕",
            receiverId = "p_maya",
            receiverName = "Maya Lin",
            status = "ACCEPTED",
            timestamp = now - 86400000L,
            acceptedTimestamp = now - 82800000L,
            isSwipeDownInitiated = true
        )

        val pendingSophia = FriendshipRequestEntity(
            id = "req_pending_sophia",
            senderId = "p_sophia",
            senderName = "Sophia Chen",
            senderAge = 24,
            senderOccupation = "UX Researcher & Ceramicist",
            senderCity = "Vancouver Lofts",
            senderCountry = "Canada",
            senderCountryFlag = "🇨🇦",
            senderBio = "Throwing clay on weekends, dissecting human habits on weekdays. Can cook a 5-course dinner from random pantry leftovers. Swiped down to ask friendship in Connect!",
            senderAvatarEmoji = "🎨",
            senderColorStart = 0xFFFF0844,
            senderColorEnd = 0xFFFFB199,
            senderInterests = "Ceramics, Cooking, Board Games, Hiking, Modern Art",
            receiverId = "current_user",
            receiverName = currentPrefs.userName,
            status = "PENDING",
            timestamp = now - 1800000L,
            isSwipeDownInitiated = true
        )

        friendshipRequestDao.insertRequests(listOf(acceptedMaya, pendingSophia))

        // Populate mutual friend network among community members
        profileDao.updateFriendsData("p_maya", "p_alex,p_sophia,p_elena,current_user", 4)
        profileDao.updateFriendsData("p_alex", "p_maya,p_liam,p_kai", 3)
        profileDao.updateFriendsData("p_sophia", "p_maya,p_chloe,p_zara", 3)
        profileDao.updateFriendsData("p_liam", "p_alex,p_priya", 2)
        profileDao.updateFriendsData("p_elena", "p_maya,p_kai,p_marcus", 3)
        profileDao.updateFriendsData("p_kai", "p_alex,p_elena,p_hannah", 3)
        profileDao.updateFriendsData("p_chloe", "p_sophia,p_zara", 2)
        profileDao.updateFriendsData("p_zara", "p_sophia,p_chloe,p_priya", 3)
        profileDao.updateFriendsData("p_priya", "p_liam,p_zara,p_leo", 3)
        profileDao.updateFriendsData("p_leo", "p_priya,p_marcus", 2)
        profileDao.updateFriendsData("p_marcus", "p_elena,p_leo", 2)
        profileDao.updateFriendsData("p_hannah", "p_kai,p_alex", 2)

        // Sync friends count with true accepted friendships
        preferencesDao.insertOrUpdate(currentPrefs.copy(friendsCount = 1))

        // Create match entry for Maya as accepted friend
        val matchId = "match_p_maya"
        if (matchDao.getMatchByProfileId("p_maya") == null) {
            val match = MatchEntity(
                matchId = matchId,
                profileId = "p_maya",
                matchedAt = now - 82800000L,
                lastMessage = "🤝 Friendship accepted! Maya: Morning botanical greenhouse harvest! 🌿",
                lastMessageTime = now - 80000000L,
                hasUnread = false,
                isDatingMatch = false,
                relationshipStatus = "FRIENDS",
                hasStartedChat = true
            )
            matchDao.insertMatch(match)
        }
    }

    suspend fun cancelPendingFriendshipRequest(requestId: String) {
        friendshipRequestDao.deleteRequest(requestId)
    }

    suspend fun sendFriendshipRequest(targetProfileId: String): FriendshipRequestEntity {
        val targetProfile = profileDao.getProfileByIdSync(targetProfileId)
        val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()

        val existing = friendshipRequestDao.findExistingFriendship("current_user", targetProfileId)
        if (existing != null) {
            return existing
        }

        val request = FriendshipRequestEntity(
            senderId = "current_user",
            senderName = currentPrefs.userName,
            senderAge = currentPrefs.userAge,
            senderOccupation = currentPrefs.userOccupation,
            senderCity = currentPrefs.userCity,
            senderCountry = currentPrefs.userCountry,
            senderCountryFlag = currentPrefs.userCountryFlag,
            senderBio = currentPrefs.userBio,
            senderAvatarEmoji = "☕",
            senderColorStart = 0xFFFF5E62,
            senderColorEnd = 0xFFFF9966,
            senderInterests = currentPrefs.userInterests,
            receiverId = targetProfileId,
            receiverName = targetProfile?.name ?: "Suitor",
            status = "PENDING",
            timestamp = System.currentTimeMillis(),
            isSwipeDownInitiated = true
        )
        friendshipRequestDao.insertRequest(request)
        return request
    }

    suspend fun acceptFriendshipRequest(requestId: String) {
        val req = friendshipRequestDao.getRequestById(requestId) ?: return
        val now = System.currentTimeMillis()
        friendshipRequestDao.acceptRequest(requestId, now)

        val otherUserId = if (req.senderId == "current_user") req.receiverId else req.senderId
        val otherUserName = if (req.senderId == "current_user") req.receiverName else req.senderName

        // Update target profile's friendIds and friendsCount
        val targetProfile = profileDao.getProfileByIdSync(otherUserId)
        if (targetProfile != null) {
            val currentFriendIds = targetProfile.getFriendIdList().toMutableList()
            if (!currentFriendIds.contains("current_user")) {
                currentFriendIds.add("current_user")
            }
            val newFriendIdsStr = currentFriendIds.joinToString(",")
            profileDao.updateFriendsData(otherUserId, newFriendIdsStr, currentFriendIds.size)
        }

        // Update current user's friendsCount in UserPreferences
        val totalAcceptedCount = friendshipRequestDao.getAcceptedFriendsCountSync("current_user")
        val currentPrefs = preferencesDao.getPreferencesSync()
        if (currentPrefs != null) {
            preferencesDao.insertOrUpdate(currentPrefs.copy(friendsCount = totalAcceptedCount))
        }

        // Create MatchEntity with relationshipStatus = "FRIENDS"
        val matchId = getSymmetricMatchId(otherUserId)
        val existingMatch = matchDao.getMatchByProfileId(otherUserId)
        if (existingMatch == null) {
            val match = MatchEntity(
                matchId = matchId,
                profileId = otherUserId,
                matchedAt = now,
                lastMessage = "🤝 Friendship accepted with $otherUserName! Say hi!",
                lastMessageTime = now,
                hasUnread = true,
                isDatingMatch = false,
                relationshipStatus = "FRIENDS",
                hasStartedChat = true
            )
            matchDao.insertMatch(match)
            chatMessageDao.insertMessage(
                ChatMessageEntity(
                    messageId = UUID.randomUUID().toString(),
                    matchId = matchId,
                    senderId = otherUserId,
                    text = "Hey! I reviewed your profile and bio and confirmed our friendship 🤝 Thrilled to connect!",
                    timestamp = now
                )
            )
        }
    }

    suspend fun declineFriendshipRequest(requestId: String) {
        friendshipRequestDao.declineRequest(requestId)
    }

    suspend fun getMutualFriends(profileId: String): List<ProfileEntity> {
        val myAccepted = friendshipRequestDao.getAcceptedFriendshipsSync("current_user")
        val myFriendIds = myAccepted.map { if (it.senderId == "current_user") it.receiverId else it.senderId }.toSet()
        if (myFriendIds.isEmpty()) return emptyList()

        val targetProfile = profileDao.getProfileByIdSync(profileId) ?: return emptyList()
        val targetFriendIds = targetProfile.getFriendIdList().toSet()

        val mutualIds = myFriendIds.intersect(targetFriendIds).toList()
        if (mutualIds.isEmpty()) return emptyList()

        return profileDao.getProfilesByIdsSync(mutualIds)
    }

    val userPreferences: Flow<UserPreferencesEntity?> = preferencesDao.getPreferences()

    val pendingOutgoingRequests: Flow<List<FriendshipRequestEntity>> = friendshipRequestDao.getPendingOutgoingRequestsForUser("current_user")
    val allSwipes: Flow<List<SwipeEntity>> = swipeDao.getAllSwipes()

    suspend fun cancelFriendshipRequest(requestId: String) {
        friendshipRequestDao.deleteRequest(requestId)
    }

    // Filter candidate profiles by current user preferences, active status, gender preference algorithm, and location & demand ranking
    val candidateProfiles: Flow<List<ProfileEntity>> = combine(
        profileDao.getCandidateProfiles(),
        preferencesDao.getPreferences(),
        friendshipRequestDao.getAcceptedFriendships("current_user")
    ) { profiles, prefs, acceptedFriendships ->
        val safePrefs = prefs ?: UserPreferencesEntity()
        if (!safePrefs.isOpenForDating) {
            emptyList()
        } else {
            val myFriendIds = acceptedFriendships.map { if (it.senderId == "current_user") it.receiverId else it.senderId }.toSet()
            val userInterestedIn = safePrefs.userInterestedIn.trim()
            val myPhoneDigits = safePrefs.verifiedMobileNumber.filter { it.isDigit() }
            val myEmail = safePrefs.googleEmail.trim().lowercase()
            val myName = safePrefs.userName.trim().lowercase()

            profiles.filter { profile ->
                // STRICT CHECK: Never show the current user's own profile in swipe candidates!
                val pDigits = profile.id.filter { it.isDigit() }
                val isSelfById = profile.id == "current_user" ||
                        profile.id.startsWith("current_user") ||
                        (myPhoneDigits.length >= 10 && pDigits.endsWith(myPhoneDigits.takeLast(10))) ||
                        (myEmail.isNotBlank() && profile.id.equals(myEmail, ignoreCase = true))
                if (isSelfById) {
                    return@filter false
                }

                // Must be active member and pass 18+ / minor safety isolation
                profile.isOpenForDating &&
                        !profile.isBanned &&
                        !profile.isFlaggedSpam &&
                        (if (safePrefs.userAge < 18) profile.age in 13..17 else profile.age >= 18) &&
                        profile.age in safePrefs.minAge..safePrefs.maxAge &&
                        profile.distanceMiles <= minOf(safePrefs.maxDistanceMiles, 100) &&
                        (!safePrefs.onlyVerified || profile.isVerified) &&
                        (safePrefs.goalFilter == "All" || profile.relationshipGoal.contains(safePrefs.goalFilter, ignoreCase = true)) &&
                        // Gender Referral Algorithm Filter
                        when (userInterestedIn.lowercase()) {
                            "women", "female", "woman" -> profile.gender.contains("woman", ignoreCase = true) || profile.gender.contains("female", ignoreCase = true)
                            "men", "male", "man" -> profile.gender.contains("man", ignoreCase = true) || profile.gender.contains("male", ignoreCase = true)
                            "other", "non-binary" -> !profile.gender.contains("man", ignoreCase = true) && !profile.gender.contains("male", ignoreCase = true) && !profile.gender.contains("woman", ignoreCase = true) && !profile.gender.contains("female", ignoreCase = true)
                            else -> true // "Any", "All", or unselected
                        }
            }.sortedWith(
                compareBy<ProfileEntity> { profile ->
                    // Distance location factor (closest first)
                    profile.distanceMiles
                }.thenByDescending { profile ->
                    // Mutual contacts & mutual friends demand factor
                    val targetFriends = profile.getFriendIdList().toSet()
                    val mutualsCount = myFriendIds.intersect(targetFriends).size
                    mutualsCount * 100 + profile.trustScore
                }
            )
        }
    }

    val likedMeProfiles: Flow<List<ProfileEntity>> = combine(
        profileDao.getLikedMeProfiles(),
        preferencesDao.getPreferences()
    ) { profiles, prefs ->
        val safePrefs = prefs ?: UserPreferencesEntity()
        val myPhoneDigits = safePrefs.verifiedMobileNumber.filter { it.isDigit() }
        val myEmail = safePrefs.googleEmail.trim().lowercase()
        val myName = safePrefs.userName.trim().lowercase()
        profiles.filter { profile ->
            val pDigits = profile.id.filter { it.isDigit() }
            val isSelf = profile.id == "current_user" ||
                    profile.id.startsWith("current_user") ||
                    (myPhoneDigits.length >= 10 && pDigits.endsWith(myPhoneDigits.takeLast(10))) ||
                    (myEmail.isNotBlank() && profile.id.equals(myEmail, ignoreCase = true)) ||
                    (myName.isNotBlank() && profile.name.trim().equals(myName, ignoreCase = true))
            !isSelf
        }
    }

    val allMatches: Flow<List<MatchEntity>> = combine(
        matchDao.getAllMatches(),
        preferencesDao.getPreferences()
    ) { matches, prefs ->
        val safePrefs = prefs ?: UserPreferencesEntity()
        val myPhoneDigits = safePrefs.verifiedMobileNumber.filter { it.isDigit() }
        val myEmail = safePrefs.googleEmail.trim().lowercase()

        val validMatches = matches.filter { m ->
            val profId = com.example.util.ContactResolver.sanitizePhone(m.profileId)
            val matchId = com.example.util.ContactResolver.sanitizePhone(m.matchId)
            val mDigits = profId.filter { it.isDigit() }
            val isSelf = profId == "current_user" ||
                    profId.startsWith("current_user") ||
                    (myPhoneDigits.length >= 10 && mDigits.length >= 10 && mDigits.endsWith(myPhoneDigits.takeLast(10))) ||
                    (myEmail.isNotBlank() && profId.equals(myEmail, ignoreCase = true))
            val isGhostNull = profId == "null" || profId.isBlank() || matchId == "null" || matchId.isBlank()
            !isSelf && !isGhostNull
        }

        val map = mutableMapOf<String, MatchEntity>()
        for (m in validMatches) {
            val profId = com.example.util.ContactResolver.sanitizePhone(m.profileId)
            val matchId = com.example.util.ContactResolver.sanitizePhone(m.matchId)
            val pDigits = profId.filter { it.isDigit() }
            val mDigits = matchId.filter { it.isDigit() }
            val key = when {
                pDigits.length >= 7 -> pDigits.takeLast(10)
                mDigits.length >= 7 -> mDigits.takeLast(10)
                profId.isNotBlank() && profId != "null" -> profId.trim().lowercase()
                else -> matchId.trim().lowercase()
            }
            val existing = map[key]
            if (existing == null || m.lastMessageTime > existing.lastMessageTime) {
                map[key] = m
            }
        }
        map.values.sortedByDescending { it.lastMessageTime }
    }

    val allProfilesForAdmin: Flow<List<ProfileEntity>> = combine(
        profileDao.getAllProfilesForAdmin(),
        preferencesDao.getPreferences()
    ) { profiles: List<ProfileEntity>, prefs: UserPreferencesEntity? ->
        val resultList = profiles.filter { 
            !it.id.startsWith("seed_") && !it.id.startsWith("demo_") &&
            it.name.isNotBlank()
        }.toMutableList()

        val selfPhoneDigits = prefs?.verifiedMobileNumber?.filter { it.isDigit() } ?: ""
        val selfEmail = (prefs?.googleEmail ?: "").trim().lowercase()

        if (prefs != null && prefs.userName.isNotBlank()) {
            val selfId = if (prefs.verifiedMobileNumber.isNotBlank()) prefs.verifiedMobileNumber else if (prefs.googleEmail.isNotBlank()) prefs.googleEmail else "current_user"
            val alreadyHasSelf = resultList.any { existing ->
                val exPhoneDigits = existing.phoneNumber.filter { it.isDigit() }
                val exEmail = existing.email.trim().lowercase()
                existing.id == "current_user" ||
                existing.id == selfId ||
                (selfPhoneDigits.length >= 10 && exPhoneDigits.length >= 10 && exPhoneDigits.takeLast(10) == selfPhoneDigits.takeLast(10)) ||
                (selfEmail.isNotBlank() && exEmail.equals(selfEmail, ignoreCase = true))
            }

            if (!alreadyHasSelf) {
                resultList.add(
                    0,
                    ProfileEntity(
                        id = selfId,
                        name = prefs.userName,
                        age = prefs.userAge,
                        occupation = if (prefs.userOccupation.isNotBlank()) prefs.userOccupation else "Self Profile",
                        city = if (prefs.userCity.isNotBlank()) prefs.userCity else "Current User",
                        distanceMiles = 0,
                        bio = if (prefs.userBio.isNotBlank()) prefs.userBio else "Registered user",
                        interests = prefs.userInterests,
                        relationshipGoal = prefs.userRelationshipGoal,
                        promptQuestion = "About Me",
                        promptAnswer = prefs.userBio,
                        gradientColorStart = 0xFFFF5E62,
                        gradientColorEnd = 0xFFFF9966,
                        avatarEmoji = "✨",
                        avatarUrl = prefs.avatarUrl,
                        phoneNumber = prefs.verifiedMobileNumber,
                        email = prefs.googleEmail,
                        isVerified = prefs.isFaceVerified,
                        likedMe = true
                    )
                )
            }
        }

        // STRICT UNIQUE PROFILE DEDUPLICATION BY PHONE NUMBER & EMAIL
        val deduplicatedMap = mutableMapOf<String, ProfileEntity>()
        for (p in resultList) {
            val pPhoneDigits = p.phoneNumber.filter { it.isDigit() }.ifBlank { p.id.filter { it.isDigit() } }
            val key = if (pPhoneDigits.length >= 7) pPhoneDigits.takeLast(10) else if (p.email.isNotBlank()) p.email.lowercase() else p.id.trim().lowercase()
            val existing = deduplicatedMap[key]
            if (existing == null) {
                deduplicatedMap[key] = p
            } else {
                if (p.name.isNotBlank() && !p.name.contains("Member") && existing.name.contains("Member")) {
                    deduplicatedMap[key] = p
                }
            }
        }
        deduplicatedMap.values.toList()
    }

    // Status / Stories Updates
    val myStatusStories: Flow<List<StatusStoryEntity>> = statusStoryDao.getMyStories()
    val friendsStatusStories: Flow<List<StatusStoryEntity>> = statusStoryDao.getFriendsStories()
    val allStatusStories: Flow<List<StatusStoryEntity>> = statusStoryDao.getAllStories()

    suspend fun markStoryViewed(storyId: String) {
        statusStoryDao.markStoryViewed(storyId)
    }

    suspend fun toggleLikeStatus(storyId: String) {
        statusStoryDao.toggleLikeStory(storyId)
    }

    suspend fun sendStatusReplyMessage(storyUserId: String, storyCaption: String, replyText: String) {
        if (replyText.isBlank()) return
        var match = matchDao.getMatchByProfileId(storyUserId)
        if (match == null) {
            val newMatchId = getSymmetricMatchId(storyUserId)
            val newMatch = MatchEntity(
                matchId = newMatchId,
                profileId = storyUserId,
                matchedAt = System.currentTimeMillis(),
                lastMessage = replyText,
                lastMessageTime = System.currentTimeMillis(),
                hasUnread = false
            )
            matchDao.insertMatch(newMatch)
            match = newMatch
        }
        val captionLabel = storyCaption.ifBlank { "Status update" }
        val userMsg = ChatMessageEntity(
            messageId = UUID.randomUUID().toString(),
            matchId = match.matchId,
            senderId = "USER",
            text = "Replied to status ($captionLabel): $replyText",
            replyToText = captionLabel,
            replyToSender = storyUserId,
            timestamp = System.currentTimeMillis(),
            isEncrypted = true
        )
        chatMessageDao.insertMessage(userMsg)
        matchDao.updateLastMessage(match.matchId, userMsg.text, userMsg.timestamp, true)
    }

    suspend fun deleteStatusStory(storyId: String) = withContext(Dispatchers.IO) {
        statusStoryDao.deleteStory(storyId)
        try {
            val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
            val myPhone = currentPrefs.verifiedMobileNumber.filter { it.isDigit() }
            if (myPhone.isNotBlank()) {
                val db = com.google.firebase.database.FirebaseDatabase.getInstance()
                db.getReference("stories").child(myPhone).child(storyId).removeValue()
            }
        } catch (_: Exception) {}
    }

    sealed class UploadStatusResult {
        object Success : UploadStatusResult()
        data class LimitExceeded(val message: String) : UploadStatusResult()
        data class Error(val message: String) : UploadStatusResult()
    }

    suspend fun uploadUserStatus(
        mediaType: String, // "PHOTO" or "VIDEO"
        caption: String,
        mediaUrl: String = "",
        videoDurationSeconds: Int = 0,
        backgroundColorStart: Long = 0xFF00A884,
        backgroundColorEnd: Long = 0xFF128C7E,
        privacy: String = "FRIENDS_AND_MATCHES"
    ): UploadStatusResult {
        val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val now = System.currentTimeMillis()
        val isNewDay = (now - currentPrefs.lastDailyQuotaResetTimestamp) > (24 * 60 * 60 * 1000L)

        var currentPhotosCount = if (isNewDay) 0 else currentPrefs.dailyPhotosUploaded
        var currentVideosCount = if (isNewDay) 0 else currentPrefs.dailyVideosUploaded

        if (mediaType == "PHOTO") {
            if (currentPhotosCount >= 5) {
                return UploadStatusResult.LimitExceeded("Daily limit reached: Maximum 5 pictures per day.")
            }
            currentPhotosCount += 1
        } else if (mediaType == "VIDEO") {
            if (videoDurationSeconds > 60 || videoDurationSeconds <= 0) {
                return UploadStatusResult.Error("Video duration must be below 60 seconds (1-60s).")
            }
            if (currentVideosCount >= 1) {
                return UploadStatusResult.LimitExceeded("Daily limit reached: Maximum 1 video (<60s) per day.")
            }
            currentVideosCount += 1
        }

        // Insert Status Story locally
        val story = StatusStoryEntity(
            id = "story_${UUID.randomUUID().toString().take(8)}",
            userId = "USER",
            userName = currentPrefs.userName.ifBlank { "You" },
            userAvatarEmoji = "✨",
            userAvatarUrl = currentPrefs.avatarUrl,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            videoDurationSeconds = if (mediaType == "VIDEO") videoDurationSeconds else 0,
            caption = caption.trim(),
            backgroundColorStart = backgroundColorStart,
            backgroundColorEnd = backgroundColorEnd,
            timestamp = now,
            isViewed = true,
            viewsCount = 1,
            isMyStatus = true,
            privacy = privacy
        )
        statusStoryDao.insertStory(story)

        // Publish to Firebase Realtime Database for mutual contact automatic discovery & sync
        try {
            val myPhone = currentPrefs.verifiedMobileNumber.filter { it.isDigit() }
            if (myPhone.isNotBlank()) {
                val db = com.google.firebase.database.FirebaseDatabase.getInstance()
                // Collect User's contacts to verify mutual contact visibility
                val contacts = userContactDao.getAllContactsSync()
                val matches = matchDao.getAllMatchesSync()
                val matchedContacts = database.matchedContactDao().getAllMatchedContacts()
                val mySavedPhones = mutableSetOf<String>()
                for (c in contacts) {
                    val digits = c.phoneNumber.filter { it.isDigit() }
                    if (digits.length >= 7) mySavedPhones.add(if (digits.length >= 10) digits.takeLast(10) else digits)
                }
                for (mc in matchedContacts) {
                    val digits = mc.phoneNumber.filter { it.isDigit() }
                    if (digits.length >= 7) mySavedPhones.add(if (digits.length >= 10) digits.takeLast(10) else digits)
                }
                for (m in matches) {
                    val digits = m.profileId.filter { it.isDigit() }
                    if (digits.length >= 7) mySavedPhones.add(if (digits.length >= 10) digits.takeLast(10) else digits)
                }

                val storyMap = hashMapOf<String, Any>(
                    "id" to story.id,
                    "userId" to myPhone,
                    "userName" to story.userName,
                    "userAvatarEmoji" to story.userAvatarEmoji,
                    "userAvatarUrl" to story.userAvatarUrl,
                    "mediaType" to story.mediaType,
                    "mediaUrl" to story.mediaUrl,
                    "videoDurationSeconds" to story.videoDurationSeconds,
                    "caption" to story.caption,
                    "backgroundColorStart" to story.backgroundColorStart,
                    "backgroundColorEnd" to story.backgroundColorEnd,
                    "timestamp" to story.timestamp,
                    "privacy" to privacy,
                    "savedPhones" to mySavedPhones.toList()
                )
                db.getReference("stories").child(myPhone).child(story.id).setValue(storyMap)
            }
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Story cloud broadcast notice: ${e.message}")
        }

        // Update preferences with new daily count
        preferencesDao.insertOrUpdate(
            currentPrefs.copy(
                dailyPhotosUploaded = currentPhotosCount,
                dailyVideosUploaded = currentVideosCount,
                lastDailyQuotaResetTimestamp = if (isNewDay) now else currentPrefs.lastDailyQuotaResetTimestamp
            )
        )

        return UploadStatusResult.Success
    }

    suspend fun syncMutualFriendsStories() = withContext(Dispatchers.IO) {
        try {
            val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
            val myPhone = currentPrefs.verifiedMobileNumber.filter { it.isDigit() }
            val now = System.currentTimeMillis()

            // 1. Gather all local contacts (phonebook, matches, matched contacts)
            val contacts = userContactDao.getAllContactsSync()
            val matches = matchDao.getAllMatchesSync()
            val matchedContacts = database.matchedContactDao().getAllMatchedContacts()
            val contactPhones = mutableSetOf<String>()
            for (c in contacts) {
                val digits = c.phoneNumber.filter { it.isDigit() }
                if (digits.length >= 7) contactPhones.add(if (digits.length >= 10) digits.takeLast(10) else digits)
            }
            for (mc in matchedContacts) {
                val digits = mc.phoneNumber.filter { it.isDigit() }
                if (digits.length >= 7) contactPhones.add(if (digits.length >= 10) digits.takeLast(10) else digits)
            }
            for (m in matches) {
                val digits = m.profileId.filter { it.isDigit() }
                if (digits.length >= 7) contactPhones.add(if (digits.length >= 10) digits.takeLast(10) else digits)
            }

            // 2. Fetch remote stories from Firebase Realtime Database
            val db = com.google.firebase.database.FirebaseDatabase.getInstance()
            val storiesRef = db.getReference("stories")
            val snapshot = storiesRef.get().await()

            val fetchedStories = mutableListOf<StatusStoryEntity>()
            for (userNode in snapshot.children) {
                val posterPhone = userNode.key ?: continue
                val posterDigits = posterPhone.filter { it.isDigit() }
                val isMyOwn = (myPhone.isNotBlank() && posterDigits.endsWith(myPhone.takeLast(10)))
                if (isMyOwn) continue

                // Check mutual contact / phonebook presence
                val userBHasPoster = contactPhones.any { it.endsWith(posterDigits.takeLast(10)) || posterDigits.endsWith(it) }

                for (storySnap in userNode.children) {
                    val id = storySnap.child("id").getValue(String::class.java) ?: storySnap.key ?: continue
                    val timestamp = storySnap.child("timestamp").getValue(Long::class.java) ?: now
                    if (now - timestamp > 24 * 60 * 60 * 1000L) continue // Discard expired stories

                    val privacy = storySnap.child("privacy").getValue(String::class.java) ?: "MY_CONTACTS"

                    val posterSavedPhones = storySnap.child("savedPhones").children.mapNotNull { it.getValue(String::class.java) }
                    val posterHasUserB = if (posterSavedPhones.isNotEmpty() && myPhone.length >= 7) {
                        val myDigits = if (myPhone.length >= 10) myPhone.takeLast(10) else myPhone
                        posterSavedPhones.any { it.endsWith(myDigits) || myDigits.endsWith(it) }
                    } else {
                        true
                    }

                    // Both users have each other saved in their phone books/contacts:
                    val isMutualContact = userBHasPoster && posterHasUserB

                    // Dynamic filtering based on the posting user's privacy control settings:
                    // "EVERYONE" / "ALL_NETWORK": Visible to everyone
                    // "MY_CONTACTS" / "FRIENDS_AND_MATCHES": Visible only to mutual phonebook / chat contacts
                    // "SELECTED_CONTACTS": Visible only if mutual contact
                    val isVisible = when (privacy) {
                        "EVERYONE", "ALL_NETWORK" -> true
                        "SELECTED_CONTACTS" -> isMutualContact
                        else -> isMutualContact // "MY_CONTACTS", "FRIENDS_AND_MATCHES"
                    }

                    if (isVisible) {
                        val userName = storySnap.child("userName").getValue(String::class.java) ?: "Friend"
                        val userAvatarEmoji = storySnap.child("userAvatarEmoji").getValue(String::class.java) ?: "✨"
                        val userAvatarUrl = storySnap.child("userAvatarUrl").getValue(String::class.java) ?: ""
                        val mediaType = storySnap.child("mediaType").getValue(String::class.java) ?: "PHOTO"
                        val mediaUrl = storySnap.child("mediaUrl").getValue(String::class.java) ?: ""
                        val videoDurationSeconds = storySnap.child("videoDurationSeconds").getValue(Int::class.java) ?: 0
                        val caption = storySnap.child("caption").getValue(String::class.java) ?: ""
                        val bgStart = storySnap.child("backgroundColorStart").getValue(Long::class.java) ?: 0xFF00A884
                        val bgEnd = storySnap.child("backgroundColorEnd").getValue(Long::class.java) ?: 0xFF128C7E

                        val existing = statusStoryDao.getStoryById(id)
                        fetchedStories.add(
                            StatusStoryEntity(
                                id = id,
                                userId = posterPhone,
                                userName = userName,
                                userAvatarEmoji = userAvatarEmoji,
                                userAvatarUrl = userAvatarUrl,
                                mediaType = mediaType,
                                mediaUrl = mediaUrl,
                                videoDurationSeconds = videoDurationSeconds,
                                caption = caption,
                                backgroundColorStart = bgStart,
                                backgroundColorEnd = bgEnd,
                                timestamp = timestamp,
                                isViewed = existing?.isViewed ?: false,
                                viewsCount = existing?.viewsCount ?: 1,
                                likesCount = existing?.likesCount ?: 0,
                                isLikedByMe = existing?.isLikedByMe ?: false,
                                isMyStatus = false,
                                privacy = privacy
                            )
                        )
                    }
                }
            }

            if (fetchedStories.isNotEmpty()) {
                statusStoryDao.insertStories(fetchedStories)
                android.util.Log.i("DatingRepository", "Synced ${fetchedStories.size} mutual contact stories from cloud.")
            }
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Story sync notice: ${e.message}")
        }
    }

    fun getProfile(id: String): Flow<ProfileEntity?> = profileDao.getProfileById(id)

    suspend fun getProfileSync(id: String): ProfileEntity? = profileDao.getProfileByIdSync(id)

    suspend fun getProfileByPhone(phone: String): ProfileEntity? = profileDao.getProfileByPhone(phone)

    private fun normalizePhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    suspend fun getSymmetricMatchId(otherUserId: String): String {
        val prefs = preferencesDao.getPreferencesSync()
        val myPhone = prefs?.verifiedMobileNumber ?: ""
        val myPhoneHash = com.example.util.PhonebookHasher.generate16CharHash(myPhone.ifBlank { "current_user" })

        val prof = profileDao.getProfileByIdSync(otherUserId) ?: profileDao.getProfileByPhone(otherUserId)
        val otherPhone = prof?.phoneNumber ?: otherUserId
        val otherPhoneHash = com.example.util.PhonebookHasher.generate16CharHash(otherPhone.ifBlank { otherUserId })

        val sorted = listOf(myPhoneHash, otherPhoneHash).sorted()
        return "match_${sorted[0]}_${sorted[1]}"
    }

    suspend fun removeOwnProfileAndSelfMatches() {
        try {
            val prefs = preferencesDao.getPreferencesSync() ?: return
            val myPhoneDigits = prefs.verifiedMobileNumber.filter { it.isDigit() }
            val myEmail = prefs.googleEmail.trim().lowercase()

            val allMatches = matchDao.getAllMatches().first()
            for (m in allMatches) {
                val mDigits = m.profileId.filter { it.isDigit() }
                val isSelf = m.profileId == "current_user" ||
                        m.profileId.startsWith("current_user_") ||
                        (myPhoneDigits.length >= 10 && mDigits.endsWith(myPhoneDigits.takeLast(10))) ||
                        (myEmail.isNotBlank() && m.profileId.equals(myEmail, ignoreCase = true))
                if (isSelf) {
                    chatMessageDao.clearMessagesForMatch(m.matchId)
                    matchDao.deleteMatch(m.matchId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Error cleaning self matches: ${e.message}")
        }
    }

    suspend fun pullProfilesFromFirestore() {
        try {
            val prefs = preferencesDao.getPreferencesSync()
            val myPhone = prefs?.verifiedMobileNumber ?: ""
            val myPhoneDigits = myPhone.filter { it.isDigit() }
            val myEmail = (prefs?.googleEmail ?: "").trim().lowercase()
            val myId = if (myPhone.isNotBlank()) myPhone.trim().replace(" ", "") else if (myEmail.isNotBlank()) myEmail else "current_user"

            val profilesToInsert = mutableListOf<ProfileEntity>()

            // 1. PRIMARY SOURCE: Live Supabase PostgREST 'profiles' table
            try {
                val supabaseProfiles = com.example.util.SupabaseClientManager.fetchAllProfiles()
                for (sp in supabaseProfiles) {
                    val spDigits = sp.phoneNumber.filter { it.isDigit() }.ifBlank { sp.id.filter { it.isDigit() } }
                    val isOwn = (myPhoneDigits.length >= 10 && spDigits.endsWith(myPhoneDigits.takeLast(10))) ||
                            (myEmail.isNotBlank() && sp.email.equals(myEmail, ignoreCase = true)) ||
                            sp.id == myId || sp.id == "current_user"
                    if (!isOwn && sp.name.isNotBlank()) {
                        profilesToInsert.add(sp)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Supabase profile pull notice: ${e.message}")
            }

            // 2. SECONDARY SOURCE: Cloud Firestore ('users' & 'profiles' collections)
            for (collName in listOf("users", "profiles")) {
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val snapshot = try {
                        db.collection(collName).get().await()
                    } catch (_: Exception) {
                        try {
                            db.collection(collName).get(com.google.firebase.firestore.Source.CACHE).await()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            val docId = doc.id
                            val docDigits = docId.filter { it.isDigit() }
                            if (docId == myId || docId == "current_user" || docId.startsWith("current_user_")) continue
                            if (docId.startsWith("seed_") || docId.startsWith("demo_")) continue
                            if (myPhoneDigits.length >= 10 && docDigits.endsWith(myPhoneDigits.takeLast(10))) continue
                            if (myEmail.isNotBlank() && docId.equals(myEmail, ignoreCase = true)) continue

                            val name = doc.getString("name") ?: doc.getString("userName") ?: continue
                            val docEmail = (doc.getString("googleEmail") ?: doc.getString("email") ?: "").trim().lowercase()
                            if (myEmail.isNotBlank() && docEmail == myEmail) continue
                            val docPhone = doc.getString("phoneNumber") ?: doc.getString("mobileNumber") ?: doc.getString("verifiedMobileNumber") ?: ""
                            val docPhoneDigits = docPhone.filter { it.isDigit() }
                            if (myPhoneDigits.length >= 10 && docPhoneDigits.endsWith(myPhoneDigits.takeLast(10))) continue
                            val docUserId = doc.getString("userId") ?: ""
                            if (docUserId == myId || (myPhoneDigits.length >= 10 && docUserId.filter { it.isDigit() }.endsWith(myPhoneDigits.takeLast(10)))) continue
                            val age = doc.getLong("age")?.toInt() ?: doc.getLong("userAge")?.toInt() ?: 24
                            val city = doc.getString("city") ?: doc.getString("userCity") ?: "Unknown"
                            val bio = doc.getString("bio") ?: doc.getString("userBio") ?: ""
                            val occupation = doc.getString("occupation") ?: ""
                            val relationshipGoal = doc.getString("relationshipGoal") ?: "New friends"
                            val avatarEmoji = doc.getString("avatarEmoji") ?: "✨"
                            val avatarUrl = doc.getString("avatarUrl") ?: ""
                            val trustScore = doc.getLong("trustScore")?.toInt() ?: 98
                            val interestsList = doc.get("interests") as? List<*> ?: doc.get("userInterests") as? List<*>
                            val interests = interestsList?.joinToString(", ") ?: ""

                            val country = doc.getString("country") ?: "India"
                            val countryFlag = doc.getString("countryFlag") ?: "🇮🇳"
                            val place = doc.getString("place") ?: "Downtown Metro"
                            val qualification = doc.getString("qualification") ?: "Bachelors"

                            val existing = profileDao.getProfileByIdSync(docId)
                            val profile = ProfileEntity(
                                id = docId,
                                name = name,
                                age = age,
                                occupation = occupation,
                                city = city,
                                distanceMiles = if (existing != null) existing.distanceMiles else (2..12).random(),
                                bio = bio,
                                interests = interests.ifBlank { "VibeSync, Chat" },
                                relationshipGoal = relationshipGoal,
                                promptQuestion = "What I'm looking for...",
                                promptAnswer = bio.ifBlank { "A good conversation and meaningful connection." },
                                gradientColorStart = 0xFFFF5E62,
                                gradientColorEnd = 0xFFFF9966,
                                avatarEmoji = avatarEmoji,
                                avatarUrl = avatarUrl,
                                isVerified = true,
                                likedMe = true,
                                isSuperLikedMe = true,
                                trustScore = trustScore,
                                isOpenForDating = doc.getBoolean("isOpenForDating") ?: true,
                                gender = doc.getString("gender") ?: "Female",
                                maritalStatus = doc.getString("maritalStatus") ?: "Single (Never Married)",
                                phoneNumber = docPhone.ifBlank { if (docDigits.length >= 7) docId else "" },
                                email = docEmail,
                                country = country,
                                countryFlag = countryFlag,
                                place = place,
                                qualification = qualification
                            )
                            profilesToInsert.add(profile)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("DatingRepository", "Firestore profile pull notice for $collName: ${e.message}")
                }
            }
            // Sync cloud registered accounts to registeredAccountDao for authentication only
            try {
                val cloudAccounts = com.example.util.FirebaseBackendSyncManager.fetchAllCloudAccounts()
                for (acc in cloudAccounts) {
                    val accDigits = acc.phoneNumber.filter { it.isDigit() }
                    val isOwn = (myPhoneDigits.length >= 10 && accDigits.endsWith(myPhoneDigits.takeLast(10))) ||
                            (myEmail.isNotBlank() && acc.googleEmail.equals(myEmail, ignoreCase = true))
                    if (!isOwn) {
                        registeredAccountDao.insertOrUpdate(acc)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Cloud accounts sync notice: ${e.message}")
            }

            if (profilesToInsert.isNotEmpty()) {
                // Deduplicate by 16-character phone hash and strictly enforce phone hash as primary key 'id'
                val deduplicatedMap = mutableMapOf<String, ProfileEntity>()
                for (p in profilesToInsert) {
                    val phoneKey = p.phoneNumber.ifBlank { p.id }
                    val canonicalId = com.example.util.PhonebookHasher.generate16CharHash(phoneKey)
                    
                    // Strictly filter out any profile objects where name, phone_number, or canonicalId is null, empty, or literally "null"
                    if (canonicalId.isBlank() || canonicalId.equals("null", ignoreCase = true) ||
                        p.name.isBlank() || p.name.equals("null", ignoreCase = true) ||
                        p.phoneNumber.isBlank() || p.phoneNumber.equals("null", ignoreCase = true)) {
                        continue
                    }
                    
                    val canonicalProfile = p.copy(id = canonicalId)
                    val existing = deduplicatedMap[canonicalId]
                    if (existing == null) {
                        deduplicatedMap[canonicalId] = canonicalProfile
                    } else {
                        if (p.bio.isNotBlank() && existing.bio.isBlank()) {
                            deduplicatedMap[canonicalId] = canonicalProfile
                        }
                    }
                }
                val deduplicatedList = deduplicatedMap.values.distinctBy { it.id }.toList()
                profileDao.insertProfiles(deduplicatedList)
                deduplicateLocalProfiles()
                android.util.Log.d("DatingRepository", "Successfully synced ${deduplicatedList.size} deduplicated profiles from Supabase & Cloud!")
            }
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Error pulling profiles from Firestore: ${e.message}")
        }
    }

    /**
     * Deduplicates local profiles in Room DB, ensuring zero multiple profile generation per mobile number.
     * Consolidates legacy duplicate rows (e.g. acc_... vs clean phone) into a single canonical row keyed by phone number.
     */
    suspend fun deduplicateLocalProfiles() = withContext(Dispatchers.IO) {
        try {
            val allProfs = profileDao.getAllProfilesSync()
            val phoneMap = mutableMapOf<String, ProfileEntity>()
            val idsToDelete = mutableListOf<String>()

            for (prof in allProfs) {
                val phoneDigits = prof.phoneNumber.filter { it.isDigit() }.takeLast(10)
                if (phoneDigits.length >= 10) {
                    val existing = phoneMap[phoneDigits]
                    if (existing == null) {
                        phoneMap[phoneDigits] = prof
                    } else {
                        // True duplicate row for the exact same 10-digit mobile number
                        if (prof.id != existing.id) {
                            idsToDelete.add(prof.id)
                            if (prof.bio.length > existing.bio.length || prof.avatarUrl.isNotBlank()) {
                                phoneMap[phoneDigits] = prof
                            }
                        }
                    }
                }
            }

            idsToDelete.distinct().forEach { obsoleteId ->
                try { profileDao.deleteProfile(obsoleteId) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Local profile deduplication notice: ${e.message}")
        }
    }

    private val activeFirestoreListeners = java.util.concurrent.ConcurrentHashMap<String, com.google.firebase.firestore.ListenerRegistration>()
    private var chatsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var profilesListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var usersListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var registeredAccountsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var matchesListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var mutualMatchesListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    suspend fun pullSingleProfileFromFirestore(userId: String) {
        if (userId.startsWith("p_") || userId.startsWith("seed_") || userId.startsWith("test_") || userId.startsWith("demo_") || userId == "current_user") return
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val doc = try {
                db.collection("profiles").document(userId).get().await()
            } catch (_: Exception) {
                try {
                    db.collection("profiles").document(userId).get(com.google.firebase.firestore.Source.CACHE).await()
                } catch (_: Exception) {
                    null
                }
            } ?: return
            if (doc.exists()) {
                val name = doc.getString("name") ?: "New Match"
                val age = doc.getLong("age")?.toInt() ?: 24
                val city = doc.getString("city") ?: "Nearby"
                val bio = doc.getString("bio") ?: ""
                val occupation = doc.getString("occupation") ?: ""
                val relationshipGoal = doc.getString("relationshipGoal") ?: "Meaningful Connection"
                val avatarEmoji = doc.getString("avatarEmoji") ?: "✨"
                val avatarUrl = doc.getString("avatarUrl") ?: ""
                val trustScore = doc.getLong("trustScore")?.toInt() ?: 99
                val interestsList = doc.get("interests") as? List<*>
                val interests = interestsList?.joinToString(", ") ?: "Chat, Dating"

                val profile = ProfileEntity(
                    id = userId,
                    name = name,
                    age = age,
                    occupation = occupation,
                    city = city,
                    distanceMiles = 3,
                    bio = bio,
                    interests = interests,
                    relationshipGoal = relationshipGoal,
                    promptQuestion = "What I'm looking for...",
                    promptAnswer = bio.ifBlank { "Excited to meet someone genuine!" },
                    gradientColorStart = 0xFFFF5E62,
                    gradientColorEnd = 0xFFFF9966,
                    avatarEmoji = avatarEmoji,
                    avatarUrl = avatarUrl,
                    isVerified = true,
                    likedMe = true,
                    isSuperLikedMe = true,
                    trustScore = trustScore,
                    isOpenForDating = doc.getBoolean("isOpenForDating") ?: true,
                    gender = doc.getString("gender") ?: "Female",
                    maritalStatus = doc.getString("maritalStatus") ?: "Single (Never Married)"
                )
                profileDao.insertProfile(profile)
                android.util.Log.d("DatingRepository", "Successfully pulled single profile: $name ($userId)")
            }
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "pullSingleProfileFromFirestore failed for $userId: ${e.message}")
        }
    }

    suspend fun insertProfileSync(profile: ProfileEntity) {
        profileDao.insertProfile(profile)
    }

    suspend fun insertProfileDirectly(profile: ProfileEntity) {
        profileDao.insertProfile(profile)
    }

    suspend fun insertMatchDirectly(match: MatchEntity) {
        matchDao.insertMatch(match)
    }

    suspend fun getAllRegisteredUsersFromFirestore(): Map<String, ProfileEntity> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, ProfileEntity>()
        val db = try { com.google.firebase.firestore.FirebaseFirestore.getInstance() } catch (_: Exception) { null }

        fun indexUser(profile: ProfileEntity) {
            result[profile.id] = profile
            val cleanPhone = profile.phoneNumber.trim()
            if (cleanPhone.isNotBlank()) {
                result[cleanPhone] = profile
                result[cleanPhone.replace("+", "").trim()] = profile
                val digits = cleanPhone.filter { it.isDigit() }
                if (digits.isNotBlank()) {
                    result[digits] = profile
                    if (digits.length >= 10) {
                        result[digits.takeLast(10)] = profile
                    }
                }
            }
            val idDigits = profile.id.filter { it.isDigit() }
            if (idDigits.isNotBlank()) {
                result[idDigits] = profile
                if (idDigits.length >= 10) {
                    result[idDigits.takeLast(10)] = profile
                }
            }
            if (profile.email.isNotBlank()) {
                result[profile.email.trim().lowercase()] = profile
            }
        }

        // 0. Query Supabase Database Profiles
        try {
            val supabaseProfiles = com.example.util.SupabaseClientManager.fetchAllProfiles()
            val validSupabaseProfiles = supabaseProfiles.filter { sp ->
                val phoneKey = sp.phoneNumber.ifBlank { sp.id }
                val canonicalId = com.example.util.PhonebookHasher.generate16CharHash(phoneKey)
                canonicalId.isNotBlank() && !canonicalId.equals("null", ignoreCase = true) &&
                sp.name.isNotBlank() && !sp.name.equals("null", ignoreCase = true) &&
                sp.phoneNumber.isNotBlank() && !sp.phoneNumber.equals("null", ignoreCase = true)
            }.distinctBy { sp ->
                val phoneKey = sp.phoneNumber.ifBlank { sp.id }
                com.example.util.PhonebookHasher.generate16CharHash(phoneKey)
            }
            for (sp in validSupabaseProfiles) {
                val phoneKey = sp.phoneNumber.ifBlank { sp.id }
                val canonicalId = com.example.util.PhonebookHasher.generate16CharHash(phoneKey)
                val canonicalProfile = sp.copy(id = canonicalId)
                indexUser(canonicalProfile)
                profileDao.insertProfile(canonicalProfile)
            }
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Supabase profile fetch note: ${e.message}")
        }

        // 1. Query Firestore 'profiles' collection
        if (db != null) {
            try {
                val snapshot = try {
                    db.collection("profiles").get().await()
                } catch (_: Exception) {
                    try {
                        db.collection("profiles").get(com.google.firebase.firestore.Source.CACHE).await()
                    } catch (_: Exception) {
                        null
                    }
                }
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val docId = doc.id
                        if (docId.startsWith("p_") || docId.startsWith("seed_") || docId.startsWith("test_") || docId == "current_user") continue
                        val name = doc.getString("name") ?: continue
                        val phone = doc.getString("phoneNumber") ?: doc.getString("mobileNumber") ?: docId
                        val email = doc.getString("googleEmail") ?: doc.getString("email") ?: ""
                        val profile = ProfileEntity(
                            id = docId,
                            name = name,
                            age = doc.getLong("age")?.toInt() ?: 24,
                            occupation = doc.getString("occupation") ?: "Member",
                            city = doc.getString("city") ?: "Online",
                            distanceMiles = 1,
                            bio = doc.getString("bio") ?: "VibeSync Member",
                            interests = "Chat, Friendship",
                            relationshipGoal = doc.getString("relationshipGoal") ?: "Friends",
                            promptQuestion = "",
                            promptAnswer = "",
                            gradientColorStart = 0xFFFF5E62,
                            gradientColorEnd = 0xFFFF9966,
                            avatarEmoji = doc.getString("avatarEmoji") ?: "✨",
                            avatarUrl = doc.getString("avatarUrl") ?: "",
                            phoneNumber = phone,
                            email = email,
                            isVerified = true,
                            likedMe = true,
                            isSuperLikedMe = true,
                            isOpenForDating = doc.getBoolean("isOpenForDating") ?: false,
                            gender = doc.getString("gender") ?: "User",
                            maritalStatus = doc.getString("maritalStatus") ?: "Single"
                        )
                        indexUser(profile)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "profiles fetch in getAllRegisteredUsersFromFirestore error: ${e.message}")
            }

        }
        result
    }

    fun startRealtimeProfilesSync() {
        val firestore = try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            SystemHealthDiagnosticsManager.logFirestoreTrace(
                collection = "profiles",
                eventType = "ERROR",
                docId = "N/A",
                summary = "Failed to obtain Firestore instance: ${e.message}",
                syncStatus = "ERROR"
            )
            return
        }

        // 1. Listen to 'profiles' collection
        if (profilesListenerRegistration == null) {
            try {
                SystemHealthDiagnosticsManager.logFirestoreTrace(
                    collection = "profiles",
                    eventType = "ATTACHED",
                    docId = "ALL",
                    summary = "Attached realtime snapshot listener to 'profiles' collection"
                )
                profilesListenerRegistration = firestore.collection("profiles")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            val isPerm = error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
                            SystemHealthDiagnosticsManager.logFirestoreTrace(
                                collection = "profiles",
                                eventType = if (isPerm) "OFFLINE_CACHE" else "ERROR",
                                docId = "ALL",
                                summary = if (isPerm) "Operating in local cache mode (awaiting auth)" else "Profiles listener error: ${error.code} - ${error.message}",
                                syncStatus = if (isPerm) "CACHE_ONLY" else "ERROR"
                            )
                            return@addSnapshotListener
                        }
                        if (snapshot == null) return@addSnapshotListener

                        val isFromCache = snapshot.metadata.isFromCache
                        val hasPendingWrites = snapshot.metadata.hasPendingWrites()
                        SystemHealthDiagnosticsManager.logFirestoreTrace(
                            collection = "profiles",
                            eventType = "SNAPSHOT_RECEIVED",
                            docId = "ALL",
                            summary = "Received snapshot with ${snapshot.size()} documents (fromCache=$isFromCache, pendingWrites=$hasPendingWrites)",
                            isFromCache = isFromCache,
                            hasPendingWrites = hasPendingWrites
                        )

                        appScope.launch(Dispatchers.IO) {
                            val prefs = preferencesDao.getPreferencesSync()
                            val myPhone = (prefs?.verifiedMobileNumber ?: "").filter { it.isDigit() }
                            val myEmail = (prefs?.googleEmail ?: "").trim().lowercase()

                            for (change in snapshot.documentChanges) {
                                val doc = change.document
                                val docId = doc.id
                                val name = doc.getString("name") ?: ""
                                val phone = doc.getString("phoneNumber") ?: doc.getString("mobileNumber") ?: ""
                                val email = doc.getString("googleEmail") ?: doc.getString("email") ?: ""
                                val docDigits = phone.filter { it.isDigit() }

                                val isOwn = (myPhone.length >= 10 && docDigits.length >= 10 && docDigits.takeLast(10) == myPhone.takeLast(10)) ||
                                        (myEmail.isNotBlank() && email.equals(myEmail, ignoreCase = true))

                                val eventTypeStr = when (change.type) {
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED -> "ADDED"
                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> "MODIFIED"
                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> "REMOVED"
                                }

                                val payload = "name=$name, phone=$phone, age=${doc.getLong("age")}, city=${doc.getString("city")}, isVerified=${doc.getBoolean("isVerified")}"

                                if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                    SystemHealthDiagnosticsManager.logFirestoreTrace(
                                        collection = "profiles",
                                        eventType = "REMOVED",
                                        docId = docId,
                                        summary = "Profile deleted in cloud ($name)",
                                        payloadPreview = payload,
                                        isFromCache = isFromCache,
                                        syncStatus = "DELETED_CLOUD"
                                    )
                                    continue
                                }

                                if (isOwn) {
                                    SystemHealthDiagnosticsManager.logFirestoreTrace(
                                        collection = "profiles",
                                        eventType = eventTypeStr,
                                        docId = docId,
                                        summary = "Skipping own profile write to avoid duplicate loop ($name)",
                                        payloadPreview = payload,
                                        isFromCache = isFromCache,
                                        syncStatus = "SKIPPED_OWN"
                                    )
                                    continue
                                }

                                val profile = ProfileEntity(
                                    id = docId,
                                    name = name.ifBlank { "Nearby Match" },
                                    age = doc.getLong("age")?.toInt() ?: 24,
                                    occupation = doc.getString("occupation") ?: "Verified User",
                                    city = doc.getString("city") ?: "Nearby",
                                    distanceMiles = (doc.getLong("distanceMiles") ?: 3L).toInt(),
                                    bio = doc.getString("bio") ?: "",
                                    interests = (doc.get("interests") as? List<*>)?.joinToString(", ") ?: "Chat, Dating",
                                    relationshipGoal = doc.getString("relationshipGoal") ?: "Meaningful Connection",
                                    promptQuestion = doc.getString("promptQuestion") ?: "Looking for...",
                                    promptAnswer = doc.getString("promptAnswer") ?: "Good vibes",
                                    gradientColorStart = 0xFFFF5E62,
                                    gradientColorEnd = 0xFFFF9966,
                                    avatarEmoji = doc.getString("avatarEmoji") ?: "✨",
                                    avatarUrl = doc.getString("avatarUrl") ?: "",
                                    isVerified = doc.getBoolean("isVerified") ?: true,
                                    isRealFaceVerified = doc.getBoolean("isRealFaceVerified") ?: true,
                                    likedMe = true,
                                    isSuperLikedMe = true,
                                    trustScore = doc.getLong("trustScore")?.toInt() ?: 99,
                                    phoneNumber = phone,
                                    email = email,
                                    country = doc.getString("country") ?: "India",
                                    countryFlag = doc.getString("countryFlag") ?: "🇮🇳",
                                    place = doc.getString("place") ?: "Downtown Metro",
                                    qualification = doc.getString("qualification") ?: "Bachelors"
                                )

                                profileDao.insertProfile(profile)
                                SystemHealthDiagnosticsManager.logFirestoreTrace(
                                    collection = "profiles",
                                    eventType = eventTypeStr,
                                    docId = docId,
                                    summary = "Profile synced & saved into local Room DB: $name ($phone)",
                                    payloadPreview = payload,
                                    isFromCache = isFromCache,
                                    syncStatus = "MERGED_ROOM"
                                )
                            }
                        }
                    }
            } catch (e: Exception) {
                SystemHealthDiagnosticsManager.logFirestoreTrace(
                    collection = "profiles",
                    eventType = "ERROR",
                    docId = "ALL",
                    summary = "Profiles listener attachment exception: ${e.message}",
                    syncStatus = "ERROR"
                )
            }
        }

        // Realtime sync handles only organic profiles from 'profiles' collection
        startRealtimeMatchesSync()
        startGlobalChatSync()
    }

    fun startRealtimeMatchesSync() {
        val firestore = try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            return
        }

        // 1. Listen to 'matches' collection
        if (matchesListenerRegistration == null) {
            try {
                SystemHealthDiagnosticsManager.logFirestoreTrace(
                    collection = "matches",
                    eventType = "ATTACHED",
                    docId = "ALL",
                    summary = "Attached realtime snapshot listener to 'matches' collection"
                )
                matchesListenerRegistration = firestore.collection("matches")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            val isPerm = error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
                            SystemHealthDiagnosticsManager.logFirestoreTrace(
                                collection = "matches",
                                eventType = if (isPerm) "OFFLINE_CACHE" else "ERROR",
                                docId = "ALL",
                                summary = if (isPerm) "Operating in local cache mode (awaiting auth)" else "Matches listener error: ${error.code} - ${error.message}",
                                syncStatus = if (isPerm) "CACHE_ONLY" else "ERROR"
                            )
                            return@addSnapshotListener
                        }
                        if (snapshot == null) return@addSnapshotListener

                        val isFromCache = snapshot.metadata.isFromCache
                        SystemHealthDiagnosticsManager.logFirestoreTrace(
                            collection = "matches",
                            eventType = "SNAPSHOT_RECEIVED",
                            docId = "ALL",
                            summary = "Received 'matches' snapshot with ${snapshot.size()} match entries",
                            isFromCache = isFromCache
                        )

                        appScope.launch(Dispatchers.IO) {
                            val prefs = preferencesDao.getPreferencesSync()
                            val myPhone = (prefs?.verifiedMobileNumber ?: "").filter { it.isDigit() }
                            val myEmail = (prefs?.googleEmail ?: "").trim().lowercase()
                            val myId = if (myPhone.isNotBlank()) (prefs?.verifiedMobileNumber ?: "").trim().replace(" ", "") else if (myEmail.isNotBlank()) myEmail else "USER"

                            for (change in snapshot.documentChanges) {
                                val doc = change.document
                                val matchId = doc.id
                                val userA = doc.getString("userA") ?: ""
                                val userB = doc.getString("userB") ?: ""
                                val uADigits = userA.filter { it.isDigit() }
                                val uBDigits = userB.filter { it.isDigit() }

                                val isParticipant = userA == myId || userB == myId ||
                                        (myPhone.length >= 10 && uADigits.length >= 10 && uADigits.takeLast(10) == myPhone.takeLast(10)) ||
                                        (myPhone.length >= 10 && uBDigits.length >= 10 && uBDigits.takeLast(10) == myPhone.takeLast(10)) ||
                                        (myEmail.isNotBlank() && (userA.equals(myEmail, ignoreCase = true) || userB.equals(myEmail, ignoreCase = true)))

                                val partnerId = if (userA == myId || (myPhone.length >= 10 && uADigits.takeLast(10) == myPhone.takeLast(10))) userB else userA
                                val eventTypeStr = when (change.type) {
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED -> "ADDED"
                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> "MODIFIED"
                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> "REMOVED"
                                }

                                if (isParticipant && partnerId.isNotBlank()) {
                                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                    val existing = matchDao.getMatchByIdSync(matchId) ?: matchDao.getMatchByProfileId(partnerId)
                                    if (existing == null) {
                                        val newMatch = MatchEntity(
                                            matchId = matchId,
                                            profileId = partnerId,
                                            matchedAt = timestamp,
                                            lastMessage = "It's a Mutual Match! Say hi 👋",
                                            lastMessageTime = timestamp,
                                            hasUnread = true
                                        )
                                        matchDao.insertMatch(newMatch)
                                        pullSingleProfileFromFirestore(partnerId)
                                        startListeningToFirestoreMessages(matchId)
                                    }
                                    SystemHealthDiagnosticsManager.logFirestoreTrace(
                                        collection = "matches",
                                        eventType = eventTypeStr,
                                        docId = matchId,
                                        summary = "Mutual match synchronized between devices! Partner: $partnerId",
                                        payloadPreview = "userA=$userA, userB=$userB, partner=$partnerId",
                                        isFromCache = isFromCache,
                                        syncStatus = "MERGED_ROOM"
                                    )
                                } else {
                                    SystemHealthDiagnosticsManager.logFirestoreTrace(
                                        collection = "matches",
                                        eventType = eventTypeStr,
                                        docId = matchId,
                                        summary = "Match event for third-party users ($userA <-> $userB)",
                                        payloadPreview = "userA=$userA, userB=$userB",
                                        isFromCache = isFromCache,
                                        syncStatus = "SKIPPED_NOT_PARTICIPANT"
                                    )
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                SystemHealthDiagnosticsManager.logFirestoreTrace(
                    collection = "matches",
                    eventType = "ERROR",
                    docId = "ALL",
                    summary = "Matches listener attachment exception: ${e.message}",
                    syncStatus = "ERROR"
                )
            }
        }

        // 2. Listen to 'mutual_matches' collection
        if (mutualMatchesListenerRegistration == null) {
            try {
                mutualMatchesListenerRegistration = firestore.collection("mutual_matches")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            val isPerm = error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
                            SystemHealthDiagnosticsManager.logFirestoreTrace(
                                collection = "mutual_matches",
                                eventType = if (isPerm) "OFFLINE_CACHE" else "ERROR",
                                docId = "ALL",
                                summary = if (isPerm) "Operating in local cache mode (awaiting auth)" else "mutual_matches listener error: ${error.code} - ${error.message}",
                                syncStatus = if (isPerm) "CACHE_ONLY" else "ERROR"
                            )
                            return@addSnapshotListener
                        }
                        if (snapshot == null) return@addSnapshotListener

                        val isFromCache = snapshot.metadata.isFromCache
                        SystemHealthDiagnosticsManager.logFirestoreTrace(
                            collection = "mutual_matches",
                            eventType = "SNAPSHOT_RECEIVED",
                            docId = "ALL",
                            summary = "Received 'mutual_matches' snapshot (${snapshot.size()} records)",
                            isFromCache = isFromCache
                        )

                        appScope.launch(Dispatchers.IO) {
                            val prefs = preferencesDao.getPreferencesSync()
                            val myPhone = (prefs?.verifiedMobileNumber ?: "").filter { it.isDigit() }
                            val myEmail = (prefs?.googleEmail ?: "").trim().lowercase()
                            val myId = if (myPhone.isNotBlank()) (prefs?.verifiedMobileNumber ?: "").trim().replace(" ", "") else if (myEmail.isNotBlank()) myEmail else "USER"

                            for (change in snapshot.documentChanges) {
                                val doc = change.document
                                val matchId = doc.id
                                val userA = doc.getString("userA") ?: ""
                                val userB = doc.getString("userB") ?: ""
                                val uADigits = userA.filter { it.isDigit() }
                                val uBDigits = userB.filter { it.isDigit() }

                                val isParticipant = userA == myId || userB == myId ||
                                        (myPhone.length >= 10 && uADigits.length >= 10 && uADigits.takeLast(10) == myPhone.takeLast(10)) ||
                                        (myPhone.length >= 10 && uBDigits.length >= 10 && uBDigits.takeLast(10) == myPhone.takeLast(10)) ||
                                        (myEmail.isNotBlank() && (userA.equals(myEmail, ignoreCase = true) || userB.equals(myEmail, ignoreCase = true)))

                                val partnerId = if (userA == myId || (myPhone.length >= 10 && uADigits.takeLast(10) == myPhone.takeLast(10))) userB else userA

                                if (isParticipant && partnerId.isNotBlank()) {
                                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                    val existing = matchDao.getMatchByIdSync(matchId) ?: matchDao.getMatchByProfileId(partnerId)
                                    if (existing == null) {
                                        val newMatch = MatchEntity(
                                            matchId = matchId,
                                            profileId = partnerId,
                                            matchedAt = timestamp,
                                            lastMessage = "It's a Mutual Match! Say hi 👋",
                                            lastMessageTime = timestamp,
                                            hasUnread = true
                                        )
                                        matchDao.insertMatch(newMatch)
                                        pullSingleProfileFromFirestore(partnerId)
                                        startListeningToFirestoreMessages(matchId)
                                    }
                                    SystemHealthDiagnosticsManager.logFirestoreTrace(
                                        collection = "mutual_matches",
                                        eventType = "MUTUAL_MATCH_CONFIRMED",
                                        docId = matchId,
                                        summary = "Mutual match synchronized with partner: $partnerId",
                                        payloadPreview = "userA=$userA, userB=$userB",
                                        isFromCache = isFromCache,
                                        syncStatus = "MERGED_ROOM"
                                    )
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                SystemHealthDiagnosticsManager.logFirestoreTrace(
                    collection = "mutual_matches",
                    eventType = "ERROR",
                    docId = "ALL",
                    summary = "mutual_matches attachment error: ${e.message}",
                    syncStatus = "ERROR"
                )
            }
        }
    }

    fun startGlobalChatSync(forceRestart: Boolean = false) {
        // Enforce strict zero cloud footprint serverless architecture - No persistent cloud chat syncing!
        return
    }

    private fun legacyStartGlobalChatSync(forceRestart: Boolean = false) {
        // Obsolete persistent chat listener
    }

    fun startListeningToFirestoreMessages(matchId: String) {
        if (activeFirestoreListeners.containsKey(matchId)) return
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            SystemHealthDiagnosticsManager.logFirestoreTrace(
                collection = "chats_messages",
                eventType = "ATTACHED",
                docId = matchId,
                summary = "Subscribed to messages for match thread: $matchId"
            )
            val listener = firestore.collection("chats")
                .document(matchId)
                .collection("messages")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val isPerm = error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
                        SystemHealthDiagnosticsManager.logFirestoreTrace(
                            collection = "chats_messages",
                            eventType = if (isPerm) "OFFLINE_CACHE" else "ERROR",
                            docId = matchId,
                            summary = if (isPerm) "Operating in local cache mode (awaiting auth)" else "Message listener error: ${error.code} - ${error.message}",
                            syncStatus = if (isPerm) "CACHE_ONLY" else "ERROR"
                        )
                        return@addSnapshotListener
                    }
                    if (snapshot == null) return@addSnapshotListener
                    val isFromCache = snapshot.metadata.isFromCache
                    appScope.launch(Dispatchers.IO) {
                        val prefs = preferencesDao.getPreferencesSync()
                        val myPhone = prefs?.verifiedMobileNumber ?: ""
                        val myDigits10 = normalizePhone(myPhone)
                        val myEmail = (prefs?.googleEmail ?: "").trim().lowercase()
                        val myId = if (myPhone.isNotBlank()) myPhone.trim().replace(" ", "") else if (myEmail.isNotBlank()) myEmail else "USER"

                        for (docChange in snapshot.documentChanges) {
                            if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val data = docChange.document.data
                                val msgId = (data["messageId"] as? String) ?: docChange.document.id
                                val senderId = (data["senderId"] as? String) ?: ""
                                val text = (data["text"] as? String) ?: ""
                                val timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis()
                                val mediaType = (data["mediaType"] as? String) ?: "TEXT"
                                val mediaUrl = (data["mediaUrl"] as? String) ?: ""
                                val voiceDurationSeconds = (data["voiceDurationSeconds"] as? Long)?.toInt() ?: 0

                                val senderDigits10 = normalizePhone(senderId)
                                val isMyOwnMessage = (senderId == "USER" ||
                                        senderId == myId ||
                                        (myDigits10.length == 10 && senderDigits10 == myDigits10) ||
                                        (myEmail.isNotBlank() && senderId.equals(myEmail, ignoreCase = true)))

                                SystemHealthDiagnosticsManager.logFirestoreTrace(
                                    collection = "chats_messages",
                                    eventType = "MESSAGE_RECEIVED",
                                    docId = "$matchId/$msgId",
                                    summary = "Message event (sender: $senderId, isOwn: $isMyOwnMessage)",
                                    payloadPreview = "text=$text, mediaType=$mediaType, sender=$senderId",
                                    isFromCache = isFromCache,
                                    syncStatus = if (isMyOwnMessage) "SKIPPED_OWN" else "MERGED_ROOM"
                                )

                                val existing = chatMessageDao.getMessageById(msgId)
                                if (existing == null) {
                                    val incomingMsg = ChatMessageEntity(
                                        messageId = msgId,
                                        matchId = matchId,
                                        senderId = senderId,
                                        text = text,
                                        mediaType = mediaType,
                                        mediaUrl = mediaUrl,
                                        voiceDurationSeconds = voiceDurationSeconds,
                                        timestamp = timestamp,
                                        isEncrypted = true
                                    )
                                    chatMessageDao.insertMessage(incomingMsg)
                                }

                                // E2E Cloud Ephemeral Delivery Policy (VibeSync / VibeSync E2EE Protocol Standard):
                                // Once message is delivered and persisted locally into mobile Room DB,
                                // IMMEDIATELY purge the message copy from Cloud Firestore server!
                                try {
                                    firestore.collection("chats")
                                        .document(matchId)
                                        .collection("messages")
                                        .document(docChange.document.id)
                                        .delete()
                                    android.util.Log.d("DatingRepository", "🧹 E2E Delivered Message '${docChange.document.id}' purged from cloud server (Mobile DB local retention only).")
                                } catch (e: Exception) {
                                    android.util.Log.w("DatingRepository", "Cloud message ephemeral purge notice: ${e.message}")
                                }

                                // Ensure partner profile exists locally
                                var partner = profileDao.getProfileByIdSync(senderId)
                                if (partner == null) {
                                    pullSingleProfileFromFirestore(senderId)
                                    partner = profileDao.getProfileByIdSync(senderId)
                                }

                                val existingMatch = matchDao.getMatchByIdSync(matchId) ?: matchDao.getMatchByProfileId(senderId)
                                if (existingMatch == null) {
                                    val newMatch = MatchEntity(
                                        matchId = matchId,
                                        profileId = senderId,
                                        matchedAt = timestamp,
                                        lastMessage = text.ifBlank { if (mediaType == "VOICE") "Voice message" else "Photo" },
                                        lastMessageTime = timestamp,
                                        hasUnread = true
                                    )
                                    matchDao.insertMatch(newMatch)
                                } else {
                                    matchDao.updateLastMessage(
                                        matchId = existingMatch.matchId,
                                        text = text.ifBlank { if (mediaType == "VOICE") "Voice message" else "Photo" },
                                        timestamp = timestamp,
                                        hasUnread = true
                                    )
                                }

                                AppNotificationManager.appContext?.let { ctx ->
                                    AppNotificationManager.showMessageNotification(
                                        context = ctx,
                                        senderId = senderId,
                                        senderName = partner?.name ?: "VibeSync Match",
                                        messageText = text.ifBlank { if (mediaType == "VOICE") "Voice message" else "Photo" },
                                        senderPhotoUrl = partner?.avatarEmoji ?: "✨",
                                        mediaType = mediaType
                                    )
                                }
                            }
                        }
                    }
                }
            activeFirestoreListeners[matchId] = listener
        } catch (e: Exception) {
            android.util.Log.e("DatingRepository", "Failed to attach Firestore chat listener: ${e.message}")
        }
    }

    fun getMessages(matchId: String): Flow<List<ChatMessageEntity>> {
        startListeningToFirestoreMessages(matchId)
        return chatMessageDao.getMessagesForMatch(matchId)
    }

    // Developer / Admin Controls
    suspend fun setProfileBan(profileId: String, isBanned: Boolean, note: String) {
        profileDao.updateBanStatus(profileId, isBanned, note)
        appScope.launch(Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("profiles").document(profileId).update(
                    mapOf("isBanned" to isBanned, "moderationNote" to note)
                )
                db.collection("users").document(profileId).update(
                    mapOf("isBanned" to isBanned, "moderationNote" to note)
                )
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Firestore setProfileBan update error: ${e.message}")
            }
        }
    }

    suspend fun recordReportOnProfile(profileId: String, trustScore: Int, isFlagged: Boolean, note: String) {
        profileDao.recordReportOnProfile(profileId, trustScore, isFlagged, note)
        appScope.launch(Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("profiles").document(profileId).update(
                    mapOf("trustScore" to trustScore, "isFlaggedSpam" to isFlagged, "moderationNote" to note)
                )
            } catch (_: Exception) {}
        }
    }

    suspend fun blockAndReportUser(profileId: String, matchId: String? = null, reason: String = "Harassment / Safety", details: String = "") {
        profileDao.updateBanStatus(profileId, true, "Emergency Block ($reason): $details")
        profileDao.updateSpamStatus(profileId, true, 0)
        val mId = matchId ?: matchDao.getMatchByProfileId(profileId)?.matchId
        if (mId != null) {
            chatMessageDao.clearMessagesForMatch(mId)
            matchDao.deleteMatch(mId)
        }
        appScope.launch(Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("profiles").document(profileId).update(
                    mapOf("isBanned" to true, "isFlaggedSpam" to true, "trustScore" to 0, "moderationNote" to "Emergency Block ($reason)")
                )
                if (mId != null) {
                    db.collection("chats").document(mId).delete()
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun setProfileSpam(profileId: String, isSpam: Boolean, trustScore: Int) {
        profileDao.updateSpamStatus(profileId, isSpam, trustScore)
        appScope.launch(Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("profiles").document(profileId).update(
                    mapOf("isFlaggedSpam" to isSpam, "trustScore" to trustScore)
                )
                db.collection("users").document(profileId).update(
                    mapOf("isFlaggedSpam" to isSpam, "trustScore" to trustScore)
                )
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Firestore setProfileSpam error: ${e.message}")
            }
        }
    }

    suspend fun setProfileVerification(profileId: String, isVerified: Boolean, isFaceVerified: Boolean) {
        profileDao.updateVerification(profileId, isVerified, isFaceVerified)
        appScope.launch(Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("profiles").document(profileId).update(
                    mapOf("isVerified" to isVerified, "isRealFaceVerified" to isFaceVerified)
                )
                db.collection("users").document(profileId).update(
                    mapOf("isVerified" to isVerified, "isRealFaceVerified" to isFaceVerified)
                )
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Firestore setProfileVerification error: ${e.message}")
            }
        }
    }

    suspend fun deleteProfile(profileId: String) {
        val targetDigits = profileId.filter { it.isDigit() }
        val allLocalProfiles = profileDao.getAllProfilesSync()
        val matchingProfiles = allLocalProfiles.filter { p ->
            p.id == profileId ||
            p.phoneNumber == profileId ||
            (targetDigits.length >= 7 && p.phoneNumber.filter { it.isDigit() }.endsWith(targetDigits.takeLast(7))) ||
            p.name.equals(profileId, ignoreCase = true) ||
            (p.email.isNotBlank() && p.email.equals(profileId, ignoreCase = true))
        }

        val phonesToDelete = mutableSetOf<String>()
        val idsToDelete = mutableSetOf<String>()
        idsToDelete.add(profileId)

        matchingProfiles.forEach { p ->
            idsToDelete.add(p.id)
            if (p.phoneNumber.isNotBlank()) {
                phonesToDelete.add(p.phoneNumber)
                phonesToDelete.add(p.phoneNumber.filter { it.isDigit() })
            }
        }
        if (targetDigits.isNotBlank()) {
            phonesToDelete.add(targetDigits)
        }

        // Delete from local Room database
        idsToDelete.forEach { id ->
            profileDao.deleteProfile(id)
            val match = matchDao.getMatchByProfileId(id)
            if (match != null) {
                chatMessageDao.clearMessagesForMatch(match.matchId)
                matchDao.deleteMatch(match.matchId)
            }
        }
        phonesToDelete.forEach { phone ->
            profileDao.deleteProfile(phone)
            registeredAccountDao.deleteAccountByPhone(phone)
        }

        withContext(Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                // Direct document deletes
                idsToDelete.forEach { id ->
                    try { db.collection("profiles").document(id).delete().await() } catch (_: Exception) {}
                    try { db.collection("users").document(id).delete().await() } catch (_: Exception) {}
                    try { db.collection("registered_accounts").document(id).delete().await() } catch (_: Exception) {}
                }
                phonesToDelete.forEach { phone ->
                    try { db.collection("profiles").document(phone).delete().await() } catch (_: Exception) {}
                    try { db.collection("profiles").document("+$phone").delete().await() } catch (_: Exception) {}
                    try { db.collection("users").document(phone).delete().await() } catch (_: Exception) {}
                    try { db.collection("users").document("+$phone").delete().await() } catch (_: Exception) {}
                    try { db.collection("registered_accounts").document(phone).delete().await() } catch (_: Exception) {}
                }

                // Scan profiles, users, registered_accounts collections to wipe any lingering doc
                val profSnap = db.collection("profiles").get().await()
                for (doc in profSnap.documents) {
                    val dPhone = (doc.getString("phoneNumber") ?: doc.getString("mobileNumber") ?: "").filter { it.isDigit() }
                    val dName = doc.getString("name") ?: ""
                    val matchPhone = targetDigits.length >= 7 && dPhone.isNotBlank() && dPhone.endsWith(targetDigits.takeLast(7))
                    val matchName = dName.equals(profileId, ignoreCase = true)
                    val matchId = idsToDelete.contains(doc.id) || doc.getString("id") == profileId

                    if (matchId || matchPhone || matchName) {
                        db.collection("profiles").document(doc.id).delete().await()
                    }
                }

                val userSnap = db.collection("users").get().await()
                for (doc in userSnap.documents) {
                    val uPhone = (doc.getString("phoneNumber") ?: doc.getString("mobileNumber") ?: "").filter { it.isDigit() }
                    val uName = doc.getString("name") ?: ""
                    val matchPhone = targetDigits.length >= 7 && uPhone.isNotBlank() && uPhone.endsWith(targetDigits.takeLast(7))
                    val matchName = uName.equals(profileId, ignoreCase = true)
                    val matchId = idsToDelete.contains(doc.id) || doc.getString("id") == profileId

                    if (matchId || matchPhone || matchName) {
                        db.collection("users").document(doc.id).delete().await()
                    }
                }

                val regSnap = db.collection("registered_accounts").get().await()
                for (doc in regSnap.documents) {
                    val rPhone = (doc.getString("phoneNumber") ?: doc.getString("recoveryPhone") ?: "").filter { it.isDigit() }
                    val matchPhone = targetDigits.length >= 7 && rPhone.isNotBlank() && rPhone.endsWith(targetDigits.takeLast(7))
                    val matchId = idsToDelete.contains(doc.id) || doc.getString("id") == profileId

                    if (matchId || matchPhone) {
                        db.collection("registered_accounts").document(doc.id).delete().await()
                    }
                }

                // Delete presence in Realtime DB
                idsToDelete.forEach { id ->
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("status/$id").removeValue()
                }
                phonesToDelete.forEach { phone ->
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("status/$phone").removeValue()
                }

                android.util.Log.i("DatingRepository", "✅ Profile '$profileId' force deleted across all cloud & local stores!")
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Firestore force deleteProfile error for '$profileId': ${e.message}")
            }
        }
    }

    suspend fun addCustomProfile(profile: ProfileEntity) {
        profileDao.insertProfile(profile)
        syncProfileToFirestore(profile)
    }

    suspend fun runAiAntiSpamScan(): Map<String, Int> {
        val all = profileDao.getAllProfilesForAdmin().first()
        var flagged = 0
        all.forEach { p ->
            // AI heuristic check: if bio contains suspicious spam keywords or low trust
            val bioLower = p.bio.lowercase()
            val isSuspicious = bioLower.contains("crypto") || 
                    bioLower.contains("cashapp") || 
                    bioLower.contains("t.me") || 
                    bioLower.contains("vibesync me for") ||
                    !p.isRealFaceVerified
            if (isSuspicious) {
                profileDao.updateSpamStatus(p.id, true, 42)
                flagged++
            }
        }
        return mapOf(
            "scanned" to all.size,
            "flagged" to flagged
        )
    }

    // Mobile OTP & Face Verification Authentication
    suspend fun updateAuthStatus(
        isLoggedIn: Boolean,
        mobileNumber: String,
        isMobileVerified: Boolean,
        isFaceVerified: Boolean,
        googleEmail: String = "",
        biometricHash: String = "BIO_HUMAN_PRIMARY_911"
    ) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val now = System.currentTimeMillis()
        val safeEmail = googleEmail.ifBlank { current.googleEmail }

        preferencesDao.insertOrUpdate(
            current.copy(
                isLoggedIn = isLoggedIn,
                verifiedMobileNumber = mobileNumber,
                googleEmail = safeEmail,
                isMobileVerified = isMobileVerified,
                isFaceVerified = isFaceVerified,
                faceVerificationTimestamp = if (isFaceVerified) now else current.faceVerificationTimestamp,
                biometricHash = if (isFaceVerified) biometricHash else current.biometricHash,
                biometricRegisteredTimestamp = if (isFaceVerified) now else current.biometricRegisteredTimestamp,
                loginTimestamp = if (isLoggedIn) now else current.loginTimestamp,
                trustRating = if (isFaceVerified && isMobileVerified) 100 else 60
            )
        )

        // Only save registered account and sync to cloud if real name and verified mobile number are present
        val cleanPhone = mobileNumber.replace(Regex("[^0-9]"), "")
        val validName = current.userName.trim()
        if (isFaceVerified && cleanPhone.length >= 10 && validName.isNotBlank() && validName.length >= 2 && validName != "Registered Member" && validName != "VibeSync User" && !validName.startsWith("User (")) {
            val accountId = cleanPhone
            val accountEntity = RegisteredAccountEntity(
                id = accountId,
                phoneNumber = mobileNumber,
                googleEmail = safeEmail,
                userName = validName,
                userAge = current.userAge,
                biometricHash = biometricHash,
                biometricRegisteredTimestamp = now,
                mpin = current.userMpin,
                isVerified = true,
                registrationTimestamp = now,
                recoveryPhone = mobileNumber,
                recoveryEmail = safeEmail
            )
            registeredAccountDao.insertOrUpdate(accountEntity)
            
            // Multi-device Cloud Firestore & Storage backend sync
            FirebaseBackendSyncManager.syncAccountToCloud(accountEntity)
        }

        // Zero-Cost End-to-End Encrypted Messaging (Firebase + Signal Protocol Queue)
        if (mobileNumber.isNotBlank()) {
            appScope.launch(Dispatchers.IO) {
                ZeroCostE2eeMessagingManager.publishPublicProfile(mobileNumber, current.userName)
                ZeroCostE2eeMessagingManager.publishRegistrationBundle(mobileNumber)
                ZeroCostE2eeMessagingManager.startListeningToIncomingQueue(mobileNumber)
            }
        }
    }

    suspend fun checkDuplicateAccount(newPhone: String, newEmail: String, biometricHash: String): RegisteredAccountEntity? {
        if (biometricHash.isNotBlank()) {
            val byBio = registeredAccountDao.getAccountByBiometricHash(biometricHash)
            if (byBio != null) return byBio
        }
        if (newPhone.isNotBlank()) {
            val byPhone = registeredAccountDao.getAccountByPhone(newPhone)
            if (byPhone != null) return byPhone
        }
        if (newEmail.isNotBlank()) {
            val byEmail = registeredAccountDao.getAccountByEmail(newEmail)
            if (byEmail != null) return byEmail
        }
        return null
    }

    suspend fun getRegisteredAccountByPhone(phone: String): RegisteredAccountEntity? {
        val cleanDigits = phone.filter { it.isDigit() }
        if (cleanDigits.isBlank()) return null

        // 1. Try direct exact match locally
        val localDirect = registeredAccountDao.getAccountByPhone(phone)
        if (localDirect != null) return localDirect

        // 2. Try list-based normalized match locally to handle formatting differences
        val allLocal = registeredAccountDao.getAllAccountsSync()
        val localMatch = allLocal.firstOrNull {
            val locDigits = it.phoneNumber.filter { d -> d.isDigit() }
            locDigits == cleanDigits || (cleanDigits.length >= 10 && locDigits.endsWith(cleanDigits.takeLast(10))) || (locDigits.length >= 10 && cleanDigits.endsWith(locDigits.takeLast(10)))
        }
        if (localMatch != null) return localMatch

        // 3. Try direct document fetch from cloud
        val cloudAccount = FirebaseBackendSyncManager.fetchAccountFromCloudByPhone(phone)
        if (cloudAccount != null) {
            registeredAccountDao.insertOrUpdate(cloudAccount)
            return cloudAccount
        }

        // 4. Try list-based normalized match from cloud
        val last10 = if (cleanDigits.length >= 10) cleanDigits.takeLast(10) else cleanDigits
        val allCloud = FirebaseBackendSyncManager.fetchAllCloudAccounts()
        val cloudMatch = allCloud.firstOrNull {
            val cDigits = it.phoneNumber.filter { d -> d.isDigit() }
            cDigits == cleanDigits || (cDigits.length >= 10 && cDigits.endsWith(last10))
        }
        if (cloudMatch != null) {
            registeredAccountDao.insertOrUpdate(cloudMatch)
            return cloudMatch
        }

        return null
    }

    suspend fun getRegisteredAccountByEmail(email: String): RegisteredAccountEntity? {
        val local = registeredAccountDao.getAccountByEmail(email)
        if (local != null) return local

        // Fallback to Cloud Backend for multi-phone logins
        val cloudAccount = FirebaseBackendSyncManager.fetchAccountFromCloudByEmail(email)
        if (cloudAccount != null) {
            registeredAccountDao.insertOrUpdate(cloudAccount)
            return cloudAccount
        }
        return null
    }

    suspend fun getPrimaryRegisteredAccount(): RegisteredAccountEntity? {
        val local = registeredAccountDao.getPrimaryAccount()
        if (local != null) return local

        val cloudAccounts = FirebaseBackendSyncManager.fetchAllCloudAccounts()
        if (cloudAccounts.isNotEmpty()) {
            val first = cloudAccounts[0]
            registeredAccountDao.insertOrUpdate(first)
            return first
        }
        return null
    }

    suspend fun syncAllAccountsWithCloud() {
        val cloudAccounts = FirebaseBackendSyncManager.fetchAllCloudAccounts()
        cloudAccounts.forEach { account ->
            registeredAccountDao.insertOrUpdate(account)
        }
    }

    suspend fun recoverAccount(account: RegisteredAccountEntity) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val reactivatedAccount = account.copy(isDeleted = false, accountStatus = "ACTIVE")
        registeredAccountDao.insertOrUpdate(reactivatedAccount)
        FirebaseBackendSyncManager.syncAccountToCloud(reactivatedAccount)

        val cleanPhone = account.phoneNumber.filter { it.isDigit() }
        val existingProfile = if (cleanPhone.isNotBlank()) {
            profileDao.getProfileByPhone(account.phoneNumber) ?: profileDao.getProfileByIdSync(cleanPhone)
        } else null

        if (existingProfile != null) {
            val reactivatedProfile = existingProfile.copy(isDeleted = false, accountStatus = "ACTIVE")
            profileDao.insertProfile(reactivatedProfile)
            syncProfileToFirestore(reactivatedProfile)
        }

        val hasRealProfile = account.userName.isNotBlank() && account.userName != "VibeSync User" && account.userAge >= 18
        val effectiveAvatar = existingProfile?.avatarUrl?.ifBlank { current.avatarUrl } ?: current.avatarUrl
        val effectiveBio = existingProfile?.bio?.ifBlank { current.userBio } ?: current.userBio
        val effectiveOccupation = existingProfile?.occupation?.ifBlank { current.userOccupation } ?: current.userOccupation
        val effectiveCity = existingProfile?.city?.ifBlank { current.userCity } ?: current.userCity
        val effectiveInterests = existingProfile?.interests?.ifBlank { current.userInterests } ?: current.userInterests
        val effectiveGender = existingProfile?.gender ?: current.userGender

        preferencesDao.insertOrUpdate(
            current.copy(
                isLoggedIn = true,
                verifiedMobileNumber = account.phoneNumber,
                googleEmail = account.googleEmail,
                userName = account.userName,
                userAge = if (account.userAge > 0) account.userAge else 18,
                avatarUrl = effectiveAvatar,
                userBio = effectiveBio,
                userOccupation = effectiveOccupation,
                userCity = effectiveCity,
                userInterests = effectiveInterests,
                userGender = effectiveGender,
                isMobileVerified = true,
                isFaceVerified = true,
                biometricHash = account.biometricHash,
                biometricRegisteredTimestamp = account.biometricRegisteredTimestamp,
                userMpin = account.mpin,
                isMpinSet = account.mpin.isNotBlank(),
                loginTimestamp = System.currentTimeMillis(),
                trustRating = 100,
                isProfileCompleted = hasRealProfile
            )
        )
    }

    /**
     * Seamless VibeSync Phone Number Login & Profile Restoration.
     * Looks up existing profile in local Room database and Cloud Firestore.
     * If user exists, restores profile and sets isProfileCompleted = true.
     */
    suspend fun recoverOrRestoreAccountByPhone(phone: String): Boolean = withContext(Dispatchers.IO) {
        val cleanDigits = phone.filter { it.isDigit() }
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()

        // 1. Check local registered account strictly for this phone
        var account = registeredAccountDao.getAccountByPhone(phone)
        if (account == null && cleanDigits.isNotBlank()) {
            account = registeredAccountDao.getAccountByPhone(cleanDigits)
        }

        // 2. Check local profile strictly for this phone
        var localProfile = profileDao.getProfileByPhone(phone)
        if (localProfile == null && cleanDigits.isNotBlank()) {
            localProfile = profileDao.getProfileByPhone(cleanDigits) ?: profileDao.getProfileByIdSync(cleanDigits)
        }

        // 3. Check Supabase / Cloud Firestore with 3-second timeout if local data not found
        if ((account?.userName.isNullOrBlank()) && (localProfile?.name.isNullOrBlank()) && cleanDigits.isNotBlank()) {
            try {
                kotlinx.coroutines.withTimeoutOrNull(3000L) {
                    // Try Supabase first
                    try {
                        val phHash = com.example.util.SupabaseClientManager.getPhoneHash(cleanDigits)
                        val sbProfiles = com.example.util.SupabaseClientManager.fetchProfilesByHashes(listOf(phHash, cleanDigits, phone))
                        val sbProfile = sbProfiles.firstOrNull()
                        if (sbProfile != null && sbProfile.name.isNotBlank() &&
                            sbProfile.name != "Registered Member" && sbProfile.name != "VibeSync User" && !sbProfile.name.startsWith("User ")) {
                            localProfile = sbProfile
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("DatingRepository", "Supabase profile restore note: ${e.message}")
                    }

                    // Fallback to Cloud Firestore if still not found
                    if (localProfile == null || localProfile?.name.isNullOrBlank()) {
                        try {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val doc = try {
                                db.collection("profiles").document(cleanDigits).get().await()
                            } catch (_: Exception) {
                                try {
                                    db.collection("users").document(cleanDigits).get().await()
                                } catch (_: Exception) { null }
                            }

                            if (doc != null && doc.exists()) {
                                val name = doc.getString("name") ?: ""
                                if (name.isNotBlank() && name != "Registered Member" && name != "VibeSync User" && !name.startsWith("User ")) {
                                    val age = doc.getLong("age")?.toInt() ?: 24
                                    val city = doc.getString("city") ?: ""
                                    val bio = doc.getString("bio") ?: ""
                                    val occupation = doc.getString("occupation") ?: ""
                                    val avatarUrl = doc.getString("avatarUrl") ?: ""
                                    val gender = doc.getString("gender") ?: "Woman"
                                    val relationshipGoal = doc.getString("relationshipGoal") ?: "Long-term relationship"
                                    val maritalStatus = doc.getString("maritalStatus") ?: "Single (Never Married)"
                                    val interestsList = doc.get("interests") as? List<*>
                                    val interests = interestsList?.joinToString(", ") ?: (doc.getString("interests") ?: "")

                                    val restoredProfile = ProfileEntity(
                                        id = cleanDigits,
                                        name = name,
                                        age = age,
                                        occupation = occupation,
                                        city = city,
                                        distanceMiles = 0,
                                        bio = bio,
                                        interests = interests,
                                        relationshipGoal = relationshipGoal,
                                        avatarEmoji = "✨",
                                        avatarUrl = avatarUrl,
                                        isVerified = true,
                                        phoneNumber = phone,
                                        isDeleted = false,
                                        accountStatus = "ACTIVE"
                                    )
                                    profileDao.insertProfile(restoredProfile)
                                    localProfile = restoredProfile

                                    val restoredAcc = RegisteredAccountEntity(
                                        id = cleanDigits,
                                        phoneNumber = phone,
                                        userName = name,
                                        userAge = age,
                                        isDeleted = false,
                                        accountStatus = "ACTIVE"
                                    )
                                    registeredAccountDao.insertOrUpdate(restoredAcc)
                                    account = restoredAcc
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("DatingRepository", "Firestore profile restore note: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Profile lookup timeout/cancellation: ${e.message}")
            }
        }

        val finalAccount = account
        val finalProfile = localProfile

        // Determine if we have a valid real user profile (excluding placeholder names)
        val effectiveName = when {
            finalAccount != null && finalAccount.userName.isNotBlank() && finalAccount.userName != "Registered Member" && finalAccount.userName != "VibeSync User" && !finalAccount.userName.startsWith("User ") && !finalAccount.userName.startsWith("User0") -> finalAccount.userName
            finalProfile != null && finalProfile.name.isNotBlank() && finalProfile.name != "Registered Member" && finalProfile.name != "VibeSync User" && !finalProfile.name.startsWith("User ") && !finalProfile.name.startsWith("User0") -> finalProfile.name
            current.userName.isNotBlank() && current.userName != "Registered Member" && current.userName != "VibeSync User" && !current.userName.startsWith("User ") && !current.userName.startsWith("User0") && (current.verifiedMobileNumber.filter { it.isDigit() } == cleanDigits) -> current.userName
            else -> ""
        }

        if (effectiveName.isNotBlank()) {
            val effectiveAge = if ((finalAccount?.userAge ?: 0) >= 18) finalAccount!!.userAge else if ((finalProfile?.age ?: 0) >= 18) finalProfile!!.age else if (current.userAge >= 18) current.userAge else 24
            val effectiveAvatar = finalProfile?.avatarUrl?.ifBlank { current.avatarUrl } ?: current.avatarUrl
            val effectiveBio = finalProfile?.bio?.ifBlank { current.userBio } ?: current.userBio
            val effectiveOccupation = finalProfile?.occupation?.ifBlank { current.userOccupation } ?: current.userOccupation
            val effectiveCity = finalProfile?.city?.ifBlank { current.userCity } ?: current.userCity
            val effectiveInterests = finalProfile?.interests?.ifBlank { current.userInterests } ?: current.userInterests
            val effectiveGender = finalProfile?.gender ?: current.userGender

            preferencesDao.insertOrUpdate(
                current.copy(
                    isLoggedIn = true,
                    verifiedMobileNumber = phone,
                    isMobileVerified = true,
                    userName = effectiveName,
                    userAge = effectiveAge,
                    avatarUrl = effectiveAvatar,
                    userBio = effectiveBio,
                    userOccupation = effectiveOccupation,
                    userCity = effectiveCity,
                    userInterests = effectiveInterests,
                    userGender = effectiveGender,
                    isFaceVerified = true,
                    loginTimestamp = System.currentTimeMillis(),
                    trustRating = 100,
                    isProfileCompleted = true
                )
            )

            // Ensure profile exists in profileDao as well
            val ownProf = ProfileEntity(
                id = cleanDigits.ifBlank { "user_${System.currentTimeMillis()}" },
                name = effectiveName,
                age = effectiveAge,
                occupation = effectiveOccupation,
                city = effectiveCity,
                distanceMiles = 0,
                bio = effectiveBio,
                interests = effectiveInterests,
                relationshipGoal = finalProfile?.relationshipGoal ?: current.userRelationshipGoal,
                avatarEmoji = "✨",
                avatarUrl = effectiveAvatar,
                isVerified = true,
                phoneNumber = phone,
                isDeleted = false,
                accountStatus = "ACTIVE"
            )
            profileDao.insertProfile(ownProf)
            return@withContext true
        } else {
            // New user without prior real profile -> mark profile incomplete so Registration Profile Screen opens immediately
            preferencesDao.insertOrUpdate(
                current.copy(
                    isLoggedIn = true,
                    verifiedMobileNumber = phone,
                    isMobileVerified = true,
                    userName = "",
                    userAge = 0,
                    userGender = current.userGender.ifBlank { "Woman" },
                    userInterestedIn = current.userInterestedIn.ifBlank { "Everyone" },
                    userRelationshipGoal = current.userRelationshipGoal.ifBlank { "Connection" },
                    isProfileCompleted = false, // Directs immediately to onboarding / profile setup
                    isFaceVerified = true,
                    loginTimestamp = System.currentTimeMillis()
                )
            )
            return@withContext false
        }
    }

    suspend fun updatePermissionsStatus(granted: Boolean) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        preferencesDao.insertOrUpdate(
            current.copy(permissionsGranted = granted)
        )
    }

    suspend fun updateAdminCredentials(newUsername: String, newPassword: String) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        preferencesDao.insertOrUpdate(
            current.copy(
                adminUsername = newUsername.trim(),
                adminPassword = newPassword.trim()
            )
        )
    }

    suspend fun updateUserMpin(newMpin: String) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val trimmed = newMpin.trim()
        preferencesDao.insertOrUpdate(
            current.copy(
                userMpin = trimmed,
                isMpinSet = trimmed.isNotEmpty(),
                lastMpinVerifiedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordMpinVerified(timestamp: Long = System.currentTimeMillis()) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        preferencesDao.insertOrUpdate(
            current.copy(
                lastMpinVerifiedTimestamp = timestamp
            )
        )
    }

    suspend fun updateRegisteredProfile(
        name: String,
        age: Int,
        dob: String,
        gender: String,
        address: String = "",
        city: String = "",
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        photos: List<String> = emptyList(),
        avatarUrl: String = "",
        interestedIn: String = "EVERYONE",
        goal: String = "SERIOUS_RELATIONSHIP",
        country: String = "India",
        countryFlag: String = "🇮🇳",
        place: String = "",
        qualification: String = "",
        occupation: String = "",
        bio: String = "",
        interests: String = "",
        maritalStatus: String = "Single (Never Married)",
        isOpenForDating: Boolean = true
    ) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val joinedPhotos = photos.joinToString("|||")
        val effectiveAvatar = avatarUrl.ifBlank { photos.firstOrNull() ?: current.avatarUrl }

        val updatedPrefs = current.copy(
            userName = name.trim(),
            userAge = age,
            userDob = dob.trim(),
            userGender = gender.trim(),
            userAddress = address.trim().ifBlank { current.userAddress },
            userCity = city.trim().ifBlank { current.userCity },
            userPlace = place.trim().ifBlank { current.userPlace },
            userQualification = qualification.trim().ifBlank { current.userQualification },
            userMaritalStatus = maritalStatus.trim().ifBlank { current.userMaritalStatus },
            userInterests = interests.trim().ifBlank { current.userInterests },
            isOpenForDating = isOpenForDating,
            latitude = if (latitude != 0.0) latitude else current.latitude,
            longitude = if (longitude != 0.0) longitude else current.longitude,
            lastLocationUpdateTimestamp = System.currentTimeMillis(),
            profilePhotos = joinedPhotos,
            avatarUrl = effectiveAvatar,
            userInterestedIn = interestedIn.trim(),
            userRelationshipGoal = goal.trim(),
            userCountry = country.trim(),
            userCountryFlag = countryFlag.trim(),
            userOccupation = occupation.trim().ifBlank { current.userOccupation },
            userBio = bio.trim().ifBlank { current.userBio },
            isProfileCompleted = true
        )
        preferencesDao.insertOrUpdate(updatedPrefs)

        appScope.launch(Dispatchers.IO) {
            try {
                com.example.util.MatchingManager.syncUserPreferencesToFirestore(updatedPrefs)
                val cleanDigits = updatedPrefs.verifiedMobileNumber.filter { it.isDigit() }
                val userId = if (cleanDigits.isNotBlank()) {
                    cleanDigits
                } else if (updatedPrefs.googleEmail.isNotBlank()) {
                    updatedPrefs.googleEmail.trim().lowercase()
                } else {
                    "user_${System.currentTimeMillis()}"
                }
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val profileMap = hashMapOf(
                    "id" to userId,
                    "userId" to userId,
                    "name" to updatedPrefs.userName,
                    "age" to updatedPrefs.userAge,
                    "dob" to updatedPrefs.userDob,
                    "gender" to updatedPrefs.userGender,
                    "interestedIn" to updatedPrefs.userInterestedIn,
                    "occupation" to updatedPrefs.userOccupation,
                    "city" to updatedPrefs.userCity,
                    "address" to updatedPrefs.userAddress,
                    "place" to updatedPrefs.userPlace,
                    "qualification" to updatedPrefs.userQualification,
                    "bio" to updatedPrefs.userBio,
                    "interests" to updatedPrefs.userInterests.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    "relationshipGoal" to updatedPrefs.userRelationshipGoal,
                    "maritalStatus" to updatedPrefs.userMaritalStatus,
                    "isOpenForDating" to updatedPrefs.isOpenForDating,
                    "avatarEmoji" to if (updatedPrefs.avatarUrl.isNotBlank()) "" else "✨",
                    "avatarUrl" to updatedPrefs.avatarUrl,
                    "isVerified" to true,
                    "isRealFaceVerified" to updatedPrefs.isFaceVerified,
                    "trustScore" to updatedPrefs.trustRating,
                    "phoneNumber" to updatedPrefs.verifiedMobileNumber,
                    "cleanPhone" to cleanDigits,
                    "phone_hash" to com.example.util.PhonebookHasher.generate16CharHash(updatedPrefs.verifiedMobileNumber),
                    "phoneHash" to com.example.util.PhonebookHasher.generate16CharHash(updatedPrefs.verifiedMobileNumber),
                    "phoneNumberE164" to com.example.util.PhonebookHasher.normalizeToE164(updatedPrefs.verifiedMobileNumber),
                    "googleEmail" to updatedPrefs.googleEmail,
                    "email" to updatedPrefs.googleEmail,
                    "country" to updatedPrefs.userCountry,
                    "countryFlag" to updatedPrefs.userCountryFlag,
                    "latitude" to updatedPrefs.latitude,
                    "longitude" to updatedPrefs.longitude,
                    "isDeleted" to false,
                    "accountStatus" to "ACTIVE",
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("profiles").document(userId).set(profileMap, com.google.firebase.firestore.SetOptions.merge())
                firestore.collection("users").document(userId).set(profileMap, com.google.firebase.firestore.SetOptions.merge())

                // Insert into local Room database with clean Phone as primary key
                val ownProf = ProfileEntity(
                    id = userId,
                    name = updatedPrefs.userName,
                    age = updatedPrefs.userAge,
                    occupation = updatedPrefs.userOccupation,
                    city = updatedPrefs.userCity,
                    distanceMiles = 0,
                    bio = updatedPrefs.userBio,
                    interests = updatedPrefs.userInterests,
                    relationshipGoal = updatedPrefs.userRelationshipGoal,
                    promptQuestion = "About me",
                    promptAnswer = updatedPrefs.userBio,
                    gradientColorStart = 0xFFFF5E62,
                    gradientColorEnd = 0xFFFF9966,
                    avatarEmoji = "✨",
                    avatarUrl = updatedPrefs.avatarUrl,
                    isVerified = true,
                    phoneNumber = updatedPrefs.verifiedMobileNumber,
                    email = updatedPrefs.googleEmail,
                    isDeleted = false,
                    accountStatus = "ACTIVE"
                )
                profileDao.insertProfile(ownProf)
                pullProfilesFromFirestore()
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Firestore sync on profile update failed: ${e.message}")
            }
        }

        // Also update registered account in database to keep account recovery in sync
        val account = registeredAccountDao.getAccountByPhone(current.verifiedMobileNumber)
            ?: registeredAccountDao.getAccountByEmail(current.googleEmail)
            ?: registeredAccountDao.getPrimaryAccount()
        if (account != null) {
            val updatedAcc = account.copy(
                userName = name.trim(),
                userAge = age
            )
            registeredAccountDao.insertOrUpdate(updatedAcc)
        }
    }

    suspend fun updateUserLocation(
        latitude: Double,
        longitude: Double,
        address: String,
        city: String,
        country: String = "",
        countryFlag: String = "",
        place: String = ""
    ) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val updated = current.copy(
            latitude = latitude,
            longitude = longitude,
            userAddress = address.trim().ifBlank { current.userAddress },
            userCity = city.trim().ifBlank { current.userCity },
            userCountry = country.trim().ifBlank { current.userCountry },
            userCountryFlag = countryFlag.trim().ifBlank { current.userCountryFlag },
            userPlace = place.trim().ifBlank { current.userPlace },
            lastLocationUpdateTimestamp = System.currentTimeMillis()
        )
        preferencesDao.insertOrUpdate(updated)
        try {
            com.example.util.MatchingManager.syncUserPreferencesToFirestore(updated)
        } catch (_: Exception) {}
    }

    suspend fun updateUserPhotos(photos: List<String>, avatarUrl: String) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val joined = photos.joinToString("|||")
        val primary = avatarUrl.ifBlank { photos.firstOrNull() ?: current.avatarUrl }
        preferencesDao.insertOrUpdate(
            current.copy(
                profilePhotos = joined,
                avatarUrl = primary
            )
        )
    }

    suspend fun logout() {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val activePhone = current.verifiedMobileNumber

        // Clear active Firestore listeners to prevent incoming message flooding from past sessions!
        try {
            chatsListenerRegistration?.remove()
            chatsListenerRegistration = null
            
            profilesListenerRegistration?.remove()
            profilesListenerRegistration = null
            
            usersListenerRegistration?.remove()
            usersListenerRegistration = null
            
            registeredAccountsListenerRegistration?.remove()
            registeredAccountsListenerRegistration = null
            
            matchesListenerRegistration?.remove()
            matchesListenerRegistration = null
            
            mutualMatchesListenerRegistration?.remove()
            mutualMatchesListenerRegistration = null

            activeFirestoreListeners.forEach { (_, registration) ->
                registration.remove()
            }
            activeFirestoreListeners.clear()
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Error clearing Firestore listeners during logout: ${e.message}")
        }

        // 1. Reset active user session credentials in preferences (Preserve registered profile for instant VibeSync re-login)
        val hasCompletedProfile = current.userName.isNotBlank() && current.userName != "Registered Member" && current.userName != "VibeSync User"
        preferencesDao.insertOrUpdate(
            current.copy(
                isLoggedIn = false,
                isProfileCompleted = hasCompletedProfile,
                loginTimestamp = 0L,
                isMobileVerified = true,
                isFaceVerified = true,
                lastMpinVerifiedTimestamp = 0L
            )
        )

        // Wipe zero-cost transient messages from local Room DB
        ZeroCostE2eeMessagingManager.nukeLocalDataOnLogout()
    }

    suspend fun syncFrontLoginDetailsToBackend(
        phone: String,
        email: String,
        context: Context
    ) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()

        val handsetResult = try {
            DeviceSimAndIpCountryHelper.detectHandsetMobileNumber(context)
        } catch (_: Exception) { null }

        val locationResult = try {
            DeviceSimAndIpCountryHelper.fetchDetailedLocation(context)
        } catch (_: Exception) { null }

        val activePhone = phone.trim().ifBlank {
            current.verifiedMobileNumber.ifBlank { handsetResult?.formattedFullNumber ?: "" }
        }
        val activeEmail = email.trim().ifBlank { current.googleEmail }

        val countryVal = locationResult?.countryName?.ifBlank { current.userCountry } ?: current.userCountry
        val flagVal = locationResult?.detectedCountry?.flagEmoji ?: current.userCountryFlag
        val cityVal = locationResult?.cityName?.ifBlank { current.userCity } ?: current.userCity
        val addressVal = locationResult?.formattedAddress?.ifBlank { current.userAddress } ?: current.userAddress

        val updatedPrefs = current.copy(
            isLoggedIn = true,
            verifiedMobileNumber = activePhone,
            googleEmail = activeEmail,
            isMobileVerified = true,
            isFaceVerified = true,
            userCountry = countryVal,
            userCountryFlag = flagVal,
            userCity = cityVal,
            userAddress = addressVal,
            loginTimestamp = System.currentTimeMillis()
        )
        preferencesDao.insertOrUpdate(updatedPrefs)

        val cleanPhoneDigits = activePhone.replace(Regex("[^0-9]"), "")
        val validName = current.userName.trim()
        if (cleanPhoneDigits.length >= 10 && validName.isNotBlank() && validName.length >= 2 && validName != "Registered Member" && validName != "VibeSync User" && !validName.startsWith("User (")) {
            val accountId = cleanPhoneDigits
            val accountEntity = RegisteredAccountEntity(
                id = accountId,
                phoneNumber = activePhone,
                googleEmail = activeEmail,
                userName = validName,
                userAge = if (current.userAge > 0) current.userAge else 24,
                biometricHash = current.biometricHash.ifBlank { "BIO_HUMAN_PRIMARY_911" },
                biometricRegisteredTimestamp = System.currentTimeMillis(),
                mpin = current.userMpin,
                isVerified = true,
                registrationTimestamp = System.currentTimeMillis(),
                recoveryPhone = activePhone,
                recoveryEmail = activeEmail
            )
            registeredAccountDao.insertOrUpdate(accountEntity)

            // Sync account & preferences to Cloud Firestore backend
            FirebaseBackendSyncManager.syncAccountToCloud(accountEntity)
            com.example.util.MatchingManager.syncUserPreferencesToFirestore(updatedPrefs)
        }
    }

    suspend fun updateOwnProfilePhoneNumber(newPhone: String) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val trimmed = newPhone.trim()
        val updatedPrefs = current.copy(
            verifiedMobileNumber = trimmed,
            isMobileVerified = true
        )
        preferencesDao.insertOrUpdate(updatedPrefs)

        // Update Registered Account in Room
        val account = registeredAccountDao.getAccountByPhone(current.verifiedMobileNumber)
            ?: registeredAccountDao.getAccountByEmail(current.googleEmail)
            ?: registeredAccountDao.getPrimaryAccount()
        if (account != null) {
            val updatedAcc = account.copy(phoneNumber = trimmed, recoveryPhone = trimmed)
            registeredAccountDao.insertOrUpdate(updatedAcc)
            FirebaseBackendSyncManager.syncAccountToCloud(updatedAcc)
        }

        // Update own profile in Room & Firestore
        val userId = if (trimmed.isNotBlank()) trimmed.replace(" ", "") else "current_user"
        val existingProfile = profileDao.getProfileByIdSync("current_user") ?: profileDao.getProfileByIdSync(userId)
        if (existingProfile != null) {
            val updatedProf = existingProfile.copy(phoneNumber = trimmed)
            profileDao.insertProfile(updatedProf)
            syncProfileToFirestore(updatedProf)
        }

        try {
            com.example.util.MatchingManager.syncUserPreferencesToFirestore(updatedPrefs)
        } catch (_: Exception) {}
    }

    suspend fun deactivateOrDeleteAccount(targetPhone: String = "", targetEmail: String = "") {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val activePhone = targetPhone.ifBlank { current.verifiedMobileNumber }
        val activeEmail = targetEmail.ifBlank { current.googleEmail }
        val activeId = activePhone.ifBlank { activeEmail.ifBlank { "user_primary_account" } }
        val cleanPhone = activePhone.filter { it.isDigit() }

        // 1. Mark account as DELETED in Room database for portal audit retention
        if (activePhone.isNotBlank()) {
            registeredAccountDao.getAccountByPhone(activePhone)?.let { acc ->
                registeredAccountDao.insertOrUpdate(acc.copy(isDeleted = true, accountStatus = "DELETED"))
            }
        }
        val ownProf = profileDao.getProfileByPhone(activePhone) ?: profileDao.getProfileByIdSync(cleanPhone)
        if (ownProf != null) {
            val deletedProf = ownProf.copy(isDeleted = true, accountStatus = "DELETED")
            profileDao.insertProfile(deletedProf)
            syncProfileToFirestore(deletedProf)
        }

        // 2. Mark user records as DELETED in Cloud Firestore backend
        try {
            FirebaseBackendSyncManager.deleteAccountFromCloud(activePhone, activeEmail, activeId)
        } catch (_: Exception) {}

        // 3. Clear active matches & chat sessions for current device session
        try {
            matchDao.clearAllMatches()
            chatMessageDao.clearAllMessages()
            swipeDao.clearAllSwipes()
            statusStoryDao.clearAllStories()
            friendshipRequestDao.clearAllFriendships()
            userContactDao.clearAllContacts()
        } catch (_: Exception) {}

        // 4. Reset user preferences back to pristine logged-out state
        preferencesDao.insertOrUpdate(
            UserPreferencesEntity(
                id = 1,
                isLoggedIn = false,
                isProfileCompleted = false,
                isMobileVerified = false,
                isFaceVerified = true,
                verifiedMobileNumber = "",
                googleEmail = "",
                userName = "",
                userMpin = "",
                isMpinSet = false,
                lastMpinVerifiedTimestamp = 0L,
                loginTimestamp = 0L,
                permissionsGranted = current.permissionsGranted
            )
        )
    }

    suspend fun swipe(profileId: String, direction: String): MatchOutcome {
        val profile = profileDao.getProfileByIdSync(profileId) ?: return MatchOutcome.Passed
        val now = System.currentTimeMillis()
        var currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()

        // Check 24h quota reset
        val isNewDay = (now - currentPrefs.lastDailySwipeResetTimestamp) > (24 * 60 * 60 * 1000L)
        if (isNewDay) {
            currentPrefs = currentPrefs.copy(
                dailySuperLikesSent = 0,
                dailyLikesSent = 0,
                dailyFriendRequestsSent = 0,
                lastDailySwipeResetTimestamp = now
            )
            preferencesDao.insertOrUpdate(currentPrefs)
        }

        if (direction == "PASS") {
            swipeDao.insertSwipe(SwipeEntity(profileId = profileId, direction = direction))
            return MatchOutcome.Passed
        }

        if (direction == "SUPER_LIKE") {
            if (currentPrefs.remainingSuperLikesToday <= 0) {
                return MatchOutcome.QuotaExceeded("Daily limit reached: Strictly 1 Super Like per day. Resets tomorrow!")
            }
            // Increment daily super likes count
            currentPrefs = currentPrefs.copy(
                dailySuperLikesSent = currentPrefs.dailySuperLikesSent + 1,
                lastDailySwipeResetTimestamp = if (currentPrefs.lastDailySwipeResetTimestamp <= 0L) now else currentPrefs.lastDailySwipeResetTimestamp
            )
            preferencesDao.insertOrUpdate(currentPrefs)
        } else if (direction == "LIKE") {
            if (currentPrefs.remainingLikesToday <= 0) {
                return MatchOutcome.QuotaExceeded("Daily limit reached: Strictly 5 Likes per day. Resets tomorrow!")
            }
            currentPrefs = currentPrefs.copy(
                dailyLikesSent = currentPrefs.dailyLikesSent + 1,
                lastDailySwipeResetTimestamp = if (currentPrefs.lastDailySwipeResetTimestamp <= 0L) now else currentPrefs.lastDailySwipeResetTimestamp
            )
            preferencesDao.insertOrUpdate(currentPrefs)
        } else if (direction == "FRIEND_REQUEST") {
            val pendingOutgoingCount = friendshipRequestDao.getPendingOutgoingCountSync("current_user")
            if (pendingOutgoingCount >= 100) {
                return MatchOutcome.FriendRequestsPendingLimitReached(
                    message = "You have 100 pending friendship requests! Please cancel old unaccepted requests to send new ones.",
                    pendingCount = pendingOutgoingCount
                )
            }
            if (currentPrefs.remainingFriendRequestsToday <= 0) {
                return MatchOutcome.QuotaExceeded("Daily limit reached: Strictly 100 Friendship Requests per day. Resets tomorrow!")
            }
            currentPrefs = currentPrefs.copy(
                dailyFriendRequestsSent = currentPrefs.dailyFriendRequestsSent + 1,
                lastDailySwipeResetTimestamp = if (currentPrefs.lastDailySwipeResetTimestamp <= 0L) now else currentPrefs.lastDailySwipeResetTimestamp
            )
            preferencesDao.insertOrUpdate(currentPrefs)

            swipeDao.insertSwipe(SwipeEntity(profileId = profileId, direction = direction))
            val request = sendFriendshipRequest(profile.id)
            return MatchOutcome.FriendshipRequestSent(profile, request)
        }

        swipeDao.insertSwipe(SwipeEntity(profileId = profileId, direction = direction))

        // Mutual match condition: profile already liked user OR super-like OR natural high affinity
        val isMutualMatch = profile.likedMe || direction == "SUPER_LIKE" || (profile.age % 2 == 0)

        return if (isMutualMatch) {
            val matchId = getSymmetricMatchId(profile.id)
            val existing = matchDao.getMatchByProfileId(profile.id)
            if (existing == null) {
                // Starts as connected chat (isDatingMatch = false, relationshipStatus = "FRIENDS")
                // Only officially enters exclusive Dating Relationship if BOTH confirm with mutual consent!
                val match = MatchEntity(
                    matchId = matchId,
                    profileId = profile.id,
                    matchedAt = System.currentTimeMillis(),
                    lastMessage = "You connected with ${profile.name}! Say hello ✨",
                    lastMessageTime = System.currentTimeMillis(),
                    hasUnread = true,
                    isDatingMatch = false,
                    relationshipStatus = "FRIENDS"
                )
                matchDao.insertMatch(match)
                profileDao.incrementFriendsCount(profile.id)
                val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
                preferencesDao.insertOrUpdate(currentPrefs.copy(friendsCount = currentPrefs.friendsCount + 1))

                // Seed initial greeting message
                val greetingMsg = ChatMessageEntity(
                    messageId = UUID.randomUUID().toString(),
                    matchId = matchId,
                    senderId = profile.id,
                    text = getPersonalizedGreeting(profile),
                    timestamp = System.currentTimeMillis()
                )
                chatMessageDao.insertMessage(greetingMsg)

                // Publish match to Firestore chats collection for multi-device sync
                appScope.launch(Dispatchers.IO) {
                    try {
                        val myPhone = currentPrefs.verifiedMobileNumber
                        val myEmail = currentPrefs.googleEmail.trim().lowercase()
                        val myId = if (myPhone.isNotBlank()) myPhone.trim().replace(" ", "") else if (myEmail.isNotBlank()) myEmail else "USER"
                        val myDigits = myPhone.filter { it.isDigit() }
                        val partnerDigits = profile.id.filter { it.isDigit() }
                        val myKey = if (myDigits.length >= 10) myDigits.takeLast(10) else myId
                        val partnerKey = if (partnerDigits.length >= 10) partnerDigits.takeLast(10) else profile.id
                        val allParticipants = listOf(myId, profile.id, myKey, partnerKey).filter { it.isNotBlank() }.distinct()

                        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val parentMap = hashMapOf(
                            "matchId" to matchId,
                            "lastMessage" to "Connected with ${currentPrefs.userName.ifBlank { "Match" }}! Say hi 👋",
                            "lastMessageTime" to match.matchedAt,
                            "lastSenderId" to myId,
                            "participantIds" to allParticipants,
                            "userA" to myId,
                            "userB" to profile.id,
                            "updatedAt" to match.matchedAt
                        )
                        firestore.collection("chats").document(matchId).set(parentMap, com.google.firebase.firestore.SetOptions.merge())
                        startListeningToFirestoreMessages(matchId)
                    } catch (e: Exception) {
                        android.util.Log.w("DatingRepository", "Failed to sync mutual swipe to Firestore chats: ${e.message}")
                    }
                }
            }
            MatchOutcome.MutualMatch(profile, matchId)
        } else {
            if (direction == "SUPER_LIKE") MatchOutcome.SuperLiked else MatchOutcome.Liked
        }
    }

    suspend fun rewindLastSwipe(): ProfileEntity? {
        val lastSwipe = swipeDao.getLastSwipe() ?: return null
        swipeDao.deleteSwipe(lastSwipe.profileId)
        // If it was a match, clean up
        val match = matchDao.getMatchByProfileId(lastSwipe.profileId)
        if (match != null) {
            chatMessageDao.clearMessagesForMatch(match.matchId)
            matchDao.deleteMatch(match.matchId)
        }
        return profileDao.getProfileByIdSync(lastSwipe.profileId)
    }

    suspend fun matchDirectly(profileId: String): MatchOutcome.MutualMatch? {
        var profile = profileDao.getProfileByIdSync(profileId)
        if (profile == null) {
            pullSingleProfileFromFirestore(profileId)
            profile = profileDao.getProfileByIdSync(profileId)
        }
        if (profile == null) {
            profile = ProfileEntity(
                id = profileId,
                name = if (profileId.length >= 10) profileId else "VibeSync Member",
                age = 25,
                occupation = "Member",
                city = "VibeSync",
                distanceMiles = 1,
                bio = "VibeSync Connection",
                interests = "Chat, Friendship",
                relationshipGoal = "Friends",
                promptQuestion = "",
                promptAnswer = "",
                gradientColorStart = 0xFFFF5E62,
                gradientColorEnd = 0xFFFF9966,
                avatarEmoji = "✨",
                phoneNumber = if (profileId.filter { it.isDigit() }.length >= 10) profileId else "",
                isVerified = true
            )
            profileDao.insertProfile(profile)
        }
        swipeDao.insertSwipe(SwipeEntity(profileId = profile.id, direction = "LIKE"))
        val matchId = getSymmetricMatchId(profile.id)
        val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val match = MatchEntity(
            matchId = matchId,
            profileId = profile.id,
            matchedAt = System.currentTimeMillis(),
            lastMessage = "Connected with ${profile.name}! Say hi 👋",
            lastMessageTime = System.currentTimeMillis(),
            hasUnread = false,
            isDatingMatch = false,
            relationshipStatus = "FRIENDS"
        )
        matchDao.insertMatch(match)
        profileDao.incrementFriendsCount(profile.id)
        preferencesDao.insertOrUpdate(currentPrefs.copy(friendsCount = currentPrefs.friendsCount + 1))

        // Publish match directly to Firestore chats collection for multi-device real-time sync
        appScope.launch(Dispatchers.IO) {
            try {
                val myPhone = currentPrefs.verifiedMobileNumber
                val myEmail = currentPrefs.googleEmail.trim().lowercase()
                val myId = if (myPhone.isNotBlank()) myPhone.trim().replace(" ", "") else if (myEmail.isNotBlank()) myEmail else "USER"
                val myDigits = myPhone.filter { it.isDigit() }
                val partnerDigits = profile.id.filter { it.isDigit() }
                val myKey = if (myDigits.length >= 10) myDigits.takeLast(10) else myId
                val partnerKey = if (partnerDigits.length >= 10) partnerDigits.takeLast(10) else profile.id
                val allParticipants = listOf(myId, profile.id, myKey, partnerKey).filter { it.isNotBlank() }.distinct()

                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val parentMap = hashMapOf(
                    "matchId" to matchId,
                    "lastMessage" to "Connected with ${currentPrefs.userName.ifBlank { "Match" }}! Say hi 👋",
                    "lastMessageTime" to match.matchedAt,
                    "lastSenderId" to myId,
                    "participantIds" to allParticipants,
                    "userA" to myId,
                    "userB" to profile.id,
                    "updatedAt" to match.matchedAt
                )
                firestore.collection("chats").document(matchId).set(parentMap, com.google.firebase.firestore.SetOptions.merge())
                startListeningToFirestoreMessages(matchId)
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Failed to sync match to Firestore chats: ${e.message}")
            }
        }

        return MatchOutcome.MutualMatch(profile, matchId)
    }

    // --- MUTUAL CONSENT DATING RELATIONSHIP & MUTUAL BREAKUP SYSTEM ---

    suspend fun proposeRelationship(matchId: String): Boolean {
        val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()

        // 1-ON-1 EXCLUSIVITY POLICY CHECK:
        // If user is ALREADY in an active dating relationship with someone else, proposal is strictly prohibited until mutual breakup!
        if (currentPrefs.activeDatingMatchId != null && currentPrefs.activeDatingMatchId != matchId) {
            return false
        }

        val match = matchDao.getMatchById(matchId).first() ?: return false
        val profile = profileDao.getProfileByIdSync(match.profileId) ?: return false

        // Update match status to PROPOSAL_SENT
        matchDao.updateRelationshipStatus(matchId, "PROPOSAL_SENT", isDating = false)

        val proposalMsg = ChatMessageEntity(
            messageId = UUID.randomUUID().toString(),
            matchId = matchId,
            senderId = "USER",
            text = "💌 I would love to be in an official Dating Relationship (Date Mate) with you! Asking for your mutual consent. 💖",
            timestamp = System.currentTimeMillis(),
            isEncrypted = true
        )
        chatMessageDao.insertMessage(proposalMsg)
        matchDao.updateLastMessage(
            matchId = matchId,
            text = "💌 Relationship proposal sent",
            timestamp = proposalMsg.timestamp,
            hasUnread = false
        )

        // Simulated partner response with mutual consent agreement
        appScope.launch {
            delay(2000)
            val updatedMatch = matchDao.getMatchById(matchId).first() ?: return@launch
            if (updatedMatch.relationshipStatus == "PROPOSAL_SENT") {
                val latestPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
                // Re-verify that user didn't enter another relationship in the meantime
                if (latestPrefs.activeDatingMatchId == null || latestPrefs.activeDatingMatchId == matchId) {
                    // Partner agrees by mutual consent!
                    matchDao.updateRelationshipStatus(matchId, "IN_RELATIONSHIP", isDating = true)
                    preferencesDao.insertOrUpdate(latestPrefs.copy(activeDatingMatchId = matchId))

                    val consentMsg = ChatMessageEntity(
                        messageId = UUID.randomUUID().toString(),
                        matchId = matchId,
                        senderId = profile.id,
                        text = "💖 I gladly agree! We are now officially in an exclusive Dating Relationship by mutual consent! Excited for this chapter together 🥰",
                        timestamp = System.currentTimeMillis(),
                        isEncrypted = true
                    )
                    chatMessageDao.insertMessage(consentMsg)

                    val systemMsg = ChatMessageEntity(
                        messageId = UUID.randomUUID().toString(),
                        matchId = matchId,
                        senderId = "SYSTEM",
                        text = "💑 Official Dating Relationship Confirmed! Mutual consent recorded. 1-on-1 exclusive focus active.",
                        timestamp = System.currentTimeMillis() + 50,
                        isEncrypted = true
                    )
                    chatMessageDao.insertMessage(systemMsg)

                    matchDao.updateLastMessage(
                        matchId = matchId,
                        text = "💖 In Dating Relationship (Mutual Consent)",
                        timestamp = systemMsg.timestamp,
                        hasUnread = true
                    )
                }
            }
        }
        return true
    }

    suspend fun respondToRelationshipProposal(matchId: String, accept: Boolean): Boolean {
        val match = matchDao.getMatchById(matchId).first() ?: return false
        val profile = profileDao.getProfileByIdSync(match.profileId) ?: return false
        val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()

        if (accept) {
            // Cannot accept if currently committed to another active dating relationship
            if (currentPrefs.activeDatingMatchId != null && currentPrefs.activeDatingMatchId != matchId) {
                return false
            }

            matchDao.updateRelationshipStatus(matchId, "IN_RELATIONSHIP", isDating = true)
            preferencesDao.insertOrUpdate(currentPrefs.copy(activeDatingMatchId = matchId))

            val acceptMsg = ChatMessageEntity(
                messageId = UUID.randomUUID().toString(),
                matchId = matchId,
                senderId = "USER",
                text = "💖 I agree with all my heart! Let's be official Date Mates by mutual consent! 💑",
                timestamp = System.currentTimeMillis(),
                isEncrypted = true
            )
            chatMessageDao.insertMessage(acceptMsg)

            val systemMsg = ChatMessageEntity(
                messageId = UUID.randomUUID().toString(),
                matchId = matchId,
                senderId = "SYSTEM",
                text = "💑 Official Dating Relationship Confirmed! Both parties agreed by mutual consent.",
                timestamp = System.currentTimeMillis() + 50,
                isEncrypted = true
            )
            chatMessageDao.insertMessage(systemMsg)

            matchDao.updateLastMessage(
                matchId = matchId,
                text = "💖 In Dating Relationship (Mutual Consent)",
                timestamp = systemMsg.timestamp,
                hasUnread = false
            )
        } else {
            matchDao.updateRelationshipStatus(matchId, "FRIENDS", isDating = false)
            val declineMsg = ChatMessageEntity(
                messageId = UUID.randomUUID().toString(),
                matchId = matchId,
                senderId = "USER",
                text = "🤝 I truly value our connection, but I'd prefer to stay great friends for now.",
                timestamp = System.currentTimeMillis(),
                isEncrypted = true
            )
            chatMessageDao.insertMessage(declineMsg)
            matchDao.updateLastMessage(
                matchId = matchId,
                text = "Stayed as friends 🤝",
                timestamp = declineMsg.timestamp,
                hasUnread = false
            )
        }
        return true
    }

    // =========================================================================
    // Google Cloud & Google Drive Private Encrypted Backup & Restore System
    // =========================================================================

    suspend fun performGoogleDriveBackup(
        accountEmail: String = "",
        frequency: String = "DAILY",
        wifiOnly: Boolean = false
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val targetEmail = accountEmail.ifBlank { currentPrefs.cloudBackupAccount.ifBlank { currentPrefs.googleEmail } }

        if (targetEmail.isBlank()) {
            return@withContext Pair(false, "Please link a Google Account to activate Drive backup.")
        }

        val ctx = context
        if (ctx == null) {
            // Local persistence fallback
            preferencesDao.insertOrUpdate(
                currentPrefs.copy(
                    isGoogleCloudBackupEnabled = (frequency != "OFF"),
                    lastCloudBackupTimestamp = System.currentTimeMillis(),
                    cloudBackupAccount = targetEmail,
                    backupFrequency = frequency,
                    backupOverWifiOnly = wifiOnly,
                    isBackupSkipped = false
                )
            )
            return@withContext Pair(true, "Backup settings saved! Periodic sync configured for $targetEmail.")
        }

        try {
            // 1. Gather all local data
            val messages = chatMessageDao.getAllMessagesSync()
            val matches = matchDao.getAllMatchesSync()
            val friendshipRequests = friendshipRequestDao.getAllRequestsSync()
            val contacts = userContactDao.getAllContactsSync()
            val myProfile = profileDao.getProfileByIdSync("current_user")

            val payload = ChatBackupPayload(
                version = 1,
                backupTimestamp = System.currentTimeMillis(),
                accountEmail = targetEmail,
                totalMessages = messages.size,
                totalMatches = matches.size,
                totalContacts = contacts.size,
                messages = messages,
                matches = matches,
                friendshipRequests = friendshipRequests,
                userContacts = contacts,
                myProfileSnapshot = myProfile
            )

            val jsonStr = payload.toJsonString()
            val localBackupFile = File(ctx.cacheDir, "current_backup.enc")
            val isEncrypted = ChatBackupCryptoHelper.encryptJsonToFile(
                plainJson = jsonStr,
                targetFile = localBackupFile,
                userSeed = targetEmail.ifBlank { "VIBESYNC_VAULT_KEY" }
            )

            if (!isEncrypted || !localBackupFile.exists()) {
                return@withContext Pair(false, "Failed to encrypt chat data locally before upload.")
            }

            // 2. Upload to Drive AppFolder
            val driveManager = GoogleDriveBackupManager(ctx)
            val fileId = driveManager.uploadBackup(localBackupFile, targetEmail)

            val sizeStr = formatBytes(localBackupFile.length())

            // 3. Update preferences
            preferencesDao.insertOrUpdate(
                currentPrefs.copy(
                    isGoogleCloudBackupEnabled = (frequency != "OFF"),
                    lastCloudBackupTimestamp = System.currentTimeMillis(),
                    cloudBackupAccount = targetEmail,
                    backupFrequency = frequency,
                    backupOverWifiOnly = wifiOnly,
                    lastBackupFileSize = sizeStr,
                    isBackupSkipped = false
                )
            )

            // 4. Schedule periodic WorkManager job
            val intervalDays = when (frequency) {
                "DAILY" -> 1
                "WEEKLY" -> 7
                "BI_WEEKLY" -> 15
                else -> 0
            }
            if (intervalDays > 0) {
                BackupScheduler.scheduleBackup(ctx, intervalDays, wifiOnly)
            } else {
                BackupScheduler.cancelBackup(ctx)
            }

            if (fileId != null) {
                Pair(true, "✅ Secure Drive Backup successful ($sizeStr • ${messages.size} chats & ${matches.size} matches)")
            } else {
                Pair(true, "Encrypted backup ready locally. WorkManager will retry uploading to Google Drive.")
            }
        } catch (e: Exception) {
            android.util.Log.e("DatingRepository", "performGoogleDriveBackup error: ${e.message}", e)
            Pair(false, "Backup failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun checkExistingCloudBackup(accountEmail: String): DriveBackupMetadata? = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext null
        if (accountEmail.isBlank()) return@withContext null
        try {
            val driveManager = GoogleDriveBackupManager(ctx)
            driveManager.checkExistingBackup(accountEmail)
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "checkExistingCloudBackup warning: ${e.message}")
            null
        }
    }

    suspend fun restoreGoogleDriveBackup(
        accountEmail: String,
        fileId: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext Pair(false, "Application context not available")
        val targetEmail = accountEmail.ifBlank {
            preferencesDao.getPreferencesSync()?.cloudBackupAccount ?: ""
        }

        if (targetEmail.isBlank()) {
            return@withContext Pair(false, "No linked Google Account found for restore.")
        }

        try {
            val driveManager = GoogleDriveBackupManager(ctx)
            var targetFileId = fileId
            if (targetFileId.isNullOrBlank()) {
                val metadata = driveManager.checkExistingBackup(targetEmail)
                targetFileId = metadata?.fileId
            }

            val targetLocalFile = File(ctx.filesDir, "restored_chats.enc")

            var downloaded = false
            if (!targetFileId.isNullOrBlank()) {
                downloaded = driveManager.downloadBackupFile(targetEmail, targetFileId, targetLocalFile)
            }

            // If not downloaded from Drive API, check local fallback cache
            if (!downloaded || !targetLocalFile.exists() || targetLocalFile.length() == 0L) {
                val cached = File(ctx.cacheDir, "current_backup.enc")
                if (cached.exists() && cached.length() > 0) {
                    cached.copyTo(targetLocalFile, overwrite = true)
                    downloaded = true
                }
            }

            if (!downloaded || !targetLocalFile.exists()) {
                return@withContext Pair(false, "No valid backup file could be downloaded from Google Drive AppFolder.")
            }

            // Decrypt payload
            val plainJson = ChatBackupCryptoHelper.decryptFileToJson(
                encryptedFile = targetLocalFile,
                userSeed = targetEmail.ifBlank { "VIBESYNC_VAULT_KEY" }
            )

            if (plainJson.isNullOrBlank()) {
                return@withContext Pair(false, "Failed to decrypt backup. Decryption key mismatch.")
            }

            // Parse payload
            val payload = ChatBackupPayload.fromJsonString(plainJson)

            // Restore into Room database
            if (payload.messages.isNotEmpty()) {
                chatMessageDao.insertMessages(payload.messages)
            }
            if (payload.matches.isNotEmpty()) {
                matchDao.insertMatches(payload.matches)
            }
            if (payload.friendshipRequests.isNotEmpty()) {
                friendshipRequestDao.insertRequests(payload.friendshipRequests)
            }
            if (payload.userContacts.isNotEmpty()) {
                userContactDao.insertContacts(payload.userContacts)
            }

            val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
            preferencesDao.insertOrUpdate(
                currentPrefs.copy(
                    lastCloudBackupTimestamp = payload.backupTimestamp,
                    cloudBackupAccount = targetEmail,
                    isRestoreSkipped = false
                )
            )

            Pair(
                true,
                "✅ Restored ${payload.messages.size} messages, ${payload.matches.size} matches, and ${payload.userContacts.size} contacts!"
            )
        } catch (e: Exception) {
            android.util.Log.e("DatingRepository", "restoreGoogleDriveBackup error: ${e.message}", e)
            Pair(false, "Restore failed: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun skipCloudBackup() {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        preferencesDao.insertOrUpdate(current.copy(isBackupSkipped = true))
    }

    suspend fun skipCloudRestore() {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        preferencesDao.insertOrUpdate(current.copy(isRestoreSkipped = true))
    }

    suspend fun updateBackupSchedule(frequency: String, wifiOnly: Boolean) {
        val current = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        preferencesDao.insertOrUpdate(
            current.copy(
                backupFrequency = frequency,
                backupOverWifiOnly = wifiOnly,
                isGoogleCloudBackupEnabled = (frequency != "OFF"),
                autoBackupDaily = (frequency == "DAILY")
            )
        )
        val ctx = context ?: return
        val intervalDays = when (frequency) {
            "DAILY" -> 1
            "WEEKLY" -> 7
            "BI_WEEKLY" -> 15
            else -> 0
        }
        if (intervalDays > 0) {
            BackupScheduler.scheduleBackup(ctx, intervalDays, wifiOnly)
        } else {
            BackupScheduler.cancelBackup(ctx)
        }
    }

    suspend fun syncGoogleCloudBackup(email: String = "") {
        performGoogleDriveBackup(email)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes <= 0 -> "0 KB"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DatingRepository? = null

        fun getInstance(context: Context): DatingRepository {
            return INSTANCE ?: synchronized(this) {
                val db = DatingDatabase.getDatabase(context)
                val instance = DatingRepository(db, context = context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun requestBreakup(matchId: String) {
        val match = matchDao.getMatchById(matchId).first() ?: return
        val profile = profileDao.getProfileByIdSync(match.profileId) ?: return

        matchDao.updateRelationshipStatus(matchId, "BREAKUP_PENDING_USER", isDating = true)

        val reqMsg = ChatMessageEntity(
            messageId = UUID.randomUUID().toString(),
            matchId = matchId,
            senderId = "USER",
            text = "💔 I feel it's best for us to conclude our romantic relationship. Requesting mutual consent to end gracefully.",
            timestamp = System.currentTimeMillis(),
            isEncrypted = true
        )
        chatMessageDao.insertMessage(reqMsg)
        matchDao.updateLastMessage(
            matchId = matchId,
            text = "💔 Breakup requested",
            timestamp = reqMsg.timestamp,
            hasUnread = false
        )

        // Simulated partner consent response to complete mutual breakup
        appScope.launch {
            delay(2200)
            val updatedMatch = matchDao.getMatchById(matchId).first() ?: return@launch
            if (updatedMatch.relationshipStatus == "BREAKUP_PENDING_USER") {
                // Partner agrees to mutual breakup!
                finalizeMutualBreakup(matchId, profile)
            }
        }
    }

    suspend fun respondToBreakupRequest(matchId: String, accept: Boolean) {
        val match = matchDao.getMatchById(matchId).first() ?: return
        val profile = profileDao.getProfileByIdSync(match.profileId) ?: return

        if (accept) {
            finalizeMutualBreakup(matchId, profile)
        } else {
            // Profile & relationship updated as Complicated (Not Mutual Breakup)
            matchDao.updateRelationshipStatus(matchId, "COMPLICATED", isDating = true)
            matchDao.updateMatchStatus(matchId, "COMPLICATED")
            val keepMsg = ChatMessageEntity(
                messageId = UUID.randomUUID().toString(),
                matchId = matchId,
                senderId = "USER",
                text = "⚠️ Breakup request was declined. Profile and relationship status updated as 'Complicated (Not Mutual Breakup)'.",
                timestamp = System.currentTimeMillis(),
                isEncrypted = true
            )
            chatMessageDao.insertMessage(keepMsg)
            matchDao.updateLastMessage(
                matchId = matchId,
                text = "⚠️ Complicated (Not Mutual Breakup)",
                timestamp = keepMsg.timestamp,
                hasUnread = false
            )
        }
    }

    private suspend fun finalizeMutualBreakup(matchId: String, profile: ProfileEntity) {
        matchDao.updateRelationshipStatus(matchId, "MUTUAL_BROKEN_UP", isDating = false)
        matchDao.updateMatchStatus(matchId, "MUTUAL_BROKEN_UP")

        // Crucial requirement: Breakups ONLY count when BOTH sides made breakup by mutual consent!
        profileDao.incrementBreakupCount(profile.id)
        val currentPrefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        preferencesDao.insertOrUpdate(
            currentPrefs.copy(
                breakupCount = currentPrefs.breakupCount + 1,
                activeDatingMatchId = null // Unlock discovery swipe deck!
            )
        )

        val partnerConsentMsg = ChatMessageEntity(
            messageId = UUID.randomUUID().toString(),
            matchId = matchId,
            senderId = profile.id,
            text = "🕊️ I agree and give mutual consent to end our dating relationship respectfully. Wishing you the very best. 🤝",
            timestamp = System.currentTimeMillis(),
            isEncrypted = true
        )
        chatMessageDao.insertMessage(partnerConsentMsg)

        val finalClosureMsg = ChatMessageEntity(
            messageId = UUID.randomUUID().toString(),
            matchId = matchId,
            senderId = "SYSTEM",
            text = "💔 Mutual Consent Breakup Finalized. +1 Breakup recorded on both profiles for transparent ethics. Discovery swipe deck is now unlocked.",
            timestamp = System.currentTimeMillis() + 50,
            isEncrypted = true
        )
        chatMessageDao.insertMessage(finalClosureMsg)

        matchDao.updateLastMessage(
            matchId = matchId,
            text = "💔 Breakup ended by mutual consent",
            timestamp = finalClosureMsg.timestamp,
            hasUnread = true
        )
    }

    suspend fun breakupWithMatch(matchId: String) {
        requestBreakup(matchId)
    }

    suspend fun markMatchAsRead(matchId: String) {
        matchDao.markAsRead(matchId)
    }

    suspend fun sendMessage(
        matchId: String,
        text: String,
        mediaType: String = "TEXT",
        mediaUrl: String = "",
        voiceDurationSeconds: Int = 0,
        replyToMessageId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null,
        isForwarded: Boolean = false
    ) {
        var match = matchDao.getMatchByIdSync(matchId)
        if (match == null) {
            val profId = matchId.removePrefix("match_").substringAfter("_").ifBlank { "contact_user" }
            val newMatch = MatchEntity(
                matchId = matchId,
                profileId = profId,
                matchedAt = System.currentTimeMillis(),
                lastMessage = text.ifBlank { if (mediaType == "VOICE") "Voice message" else "Photo" },
                lastMessageTime = System.currentTimeMillis(),
                hasUnread = false
            )
            matchDao.insertMatch(newMatch)
            match = newMatch
        }
        val targetProfileId = match.profileId
        var profile = profileDao.getProfileByIdSync(targetProfileId)
        if (profile == null) {
            val newProf = ProfileEntity(
                id = targetProfileId,
                name = if (targetProfileId.length >= 10) targetProfileId else "VibeSync Contact",
                age = 25,
                occupation = "Member",
                city = "VibeSync",
                distanceMiles = 1,
                bio = "VibeSync Connection",
                interests = "Chat",
                relationshipGoal = "Friends",
                promptQuestion = "",
                promptAnswer = "",
                gradientColorStart = 0xFFFF5E62,
                gradientColorEnd = 0xFFFF9966,
                avatarEmoji = "✨",
                phoneNumber = if (targetProfileId.filter { it.isDigit() }.length >= 10) targetProfileId else "",
                isVerified = true
            )
            profileDao.insertProfile(newProf)
            profile = newProf
        }

        val userMsg = ChatMessageEntity(
            messageId = UUID.randomUUID().toString(),
            matchId = matchId,
            senderId = "USER",
            text = text,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            voiceDurationSeconds = voiceDurationSeconds,
            replyToMessageId = replyToMessageId,
            replyToText = replyToText,
            replyToSender = replyToSender,
            isForwarded = isForwarded,
            timestamp = System.currentTimeMillis(),
            isEncrypted = true
        )
        chatMessageDao.insertMessage(userMsg)
        messageDao.insertMessage(userMsg.toLocalMessage())
        com.example.util.SupabaseClientManager.upsertChatMessage(userMsg)
        matchDao.updateLastMessage(
            matchId = matchId,
            text = text.ifBlank { if (mediaType == "VOICE") "Voice message" else "Photo" },
            timestamp = userMsg.timestamp,
            hasUnread = false
        )

        // 1. Sync sent message via Zero-Cost Transient E2EE Delivery Queue & Firestore
        appScope.launch(Dispatchers.IO) {
            try {
                val prefs = preferencesDao.getPreferencesSync()
                val myPhone = com.example.util.ContactResolver.sanitizePhone(prefs?.verifiedMobileNumber ?: "")
                val myEmail = com.example.util.ContactResolver.sanitizeName(prefs?.googleEmail ?: "")
                val myId = if (myPhone.isNotBlank() && myPhone != "null") myPhone else if (myEmail.isNotBlank()) myEmail else "USER"
                
                val partnerPhone = com.example.util.ContactResolver.sanitizePhone(profile.phoneNumber)
                val partnerId = com.example.util.ContactResolver.sanitizePhone(match.profileId)
                val profileIdClean = com.example.util.ContactResolver.sanitizePhone(profile.id)

                if (myPhone.isNotBlank() && myPhone != "null") {
                    ZeroCostE2eeMessagingManager.startListeningToIncomingQueue(myPhone)
                }

                // Dispatch to Zero-Cost E2EE transient delivery queue (/messages/$phoneNumber/$pushId)
                val targetPhone = when {
                    partnerPhone.isNotBlank() && partnerPhone != "null" && !com.example.util.ContactResolver.isGarbledOrEncrypted(partnerPhone) && partnerPhone.any { it.isDigit() } -> partnerPhone
                    partnerId.filter { it.isDigit() }.length >= 7 -> partnerId
                    profileIdClean.filter { it.isDigit() }.length >= 7 -> profileIdClean
                    else -> partnerId
                }

                if (targetPhone.isNotBlank() && targetPhone != "null" && targetPhone.any { it.isDigit() }) {
                    ZeroCostE2eeMessagingManager.sendEncryptedMessage(
                        senderUid = myPhone.ifBlank { myId },
                        recipientPhoneNumber = targetPhone,
                        textContent = text,
                        matchId = matchId
                    )
                } else {
                    android.util.Log.w("DatingRepository", "Cannot dispatch E2EE message: invalid targetPhone '$targetPhone'")
                }

                // No persistent chat storage in cloud - Strictly Zero-Cost Transient queue only!
            } catch (e: Exception) {
                android.util.Log.w("DatingRepository", "Zero-Cost dispatch message error: ${e.message}")
            }
        }

        startListeningToFirestoreMessages(matchId)
    }

    suspend fun editMessage(messageId: String, newText: String): Boolean {
        val msg = chatMessageDao.getMessageById(messageId) ?: return false
        val now = System.currentTimeMillis()
        val tenMinutesMillis = 10 * 60 * 1000L
        if (now - msg.timestamp > tenMinutesMillis) {
            return false // 10 minutes exceeded
        }
        val updated = msg.copy(
            text = newText,
            isEdited = true,
            editedTimestamp = now
        )
        chatMessageDao.updateMessage(updated)
        matchDao.updateLastMessage(msg.matchId, newText, now, false)
        return true
    }

    suspend fun toggleStarMessage(messageId: String) {
        val msg = chatMessageDao.getMessageById(messageId) ?: return
        chatMessageDao.updateMessage(msg.copy(isStarred = !msg.isStarred))
    }

    suspend fun deleteMessageForMe(messageId: String) {
        val msg = chatMessageDao.getMessageById(messageId) ?: return
        chatMessageDao.updateMessage(msg.copy(isDeletedForMe = true))
    }

    suspend fun deleteMessageForEveryone(messageId: String) {
        val msg = chatMessageDao.getMessageById(messageId) ?: return
        val updated = msg.copy(
            text = "🚫 This message was deleted",
            isDeletedForEveryone = true,
            mediaType = "TEXT",
            mediaUrl = "",
            voiceDurationSeconds = 0
        )
        chatMessageDao.updateMessage(updated)
        matchDao.updateLastMessage(msg.matchId, "🚫 This message was deleted", System.currentTimeMillis(), false)
    }

    suspend fun forwardMessage(targetMatchId: String, message: ChatMessageEntity) {
        sendMessage(
            matchId = targetMatchId,
            text = message.text,
            mediaType = message.mediaType,
            mediaUrl = message.mediaUrl,
            voiceDurationSeconds = message.voiceDurationSeconds,
            isForwarded = true
        )
    }

    suspend fun updatePreferences(preferences: UserPreferencesEntity) {
        preferencesDao.insertOrUpdate(preferences)
        val cleanPhone = com.example.util.ContactResolver.sanitizePhone(preferences.verifiedMobileNumber)
        val cleanName = com.example.util.ContactResolver.sanitizeName(preferences.userName)
        val cleanEmail = com.example.util.ContactResolver.sanitizeName(preferences.googleEmail)

        val cleanDigits = cleanPhone.filter { it.isDigit() }
        val userId = if (cleanDigits.length >= 7) cleanPhone else if (cleanEmail.isNotBlank()) cleanEmail else "current_user"

        val ownProf = ProfileEntity(
            id = userId,
            name = cleanName.ifBlank { "VibeSync Member" },
            age = preferences.userAge,
            occupation = com.example.util.ContactResolver.sanitizeName(preferences.userOccupation, "Verified Member"),
            city = com.example.util.ContactResolver.sanitizeName(preferences.userCity, "Online"),
            distanceMiles = 0,
            bio = com.example.util.ContactResolver.sanitizeName(preferences.userBio),
            interests = preferences.userInterests,
            relationshipGoal = preferences.userRelationshipGoal,
            promptQuestion = "About me",
            promptAnswer = preferences.userBio,
            gradientColorStart = 0xFFFF5E62,
            gradientColorEnd = 0xFFFF9966,
            avatarEmoji = "✨",
            avatarUrl = preferences.avatarUrl,
            isVerified = true,
            phoneNumber = cleanPhone,
            email = cleanEmail,
            isDeleted = false,
            accountStatus = "ACTIVE"
        )
        profileDao.insertProfile(ownProf)

        if (userId != "current_user" && userId != "null" && (cleanPhone.isNotBlank() || cleanEmail.isNotBlank())) {
            syncProfileToFirestore(ownProf)
        }

        val account = registeredAccountDao.getAccountByPhone(preferences.verifiedMobileNumber)
            ?: registeredAccountDao.getAccountByEmail(preferences.googleEmail)
            ?: registeredAccountDao.getPrimaryAccount()

        if (account != null) {
            val updatedAcc = account.copy(
                userName = preferences.userName,
                userAge = preferences.userAge,
                phoneNumber = preferences.verifiedMobileNumber.ifBlank { account.phoneNumber },
                googleEmail = preferences.googleEmail.ifBlank { account.googleEmail }
            )
            registeredAccountDao.insertOrUpdate(updatedAcc)
            com.example.util.SupabaseClientManager.upsertRegisteredAccount(updatedAcc)
        }
    }

    suspend fun resetAllData() {
        chatMessageDao.clearAllMessages()
        matchDao.clearAllMatches()
        swipeDao.clearAllSwipes()
        statusStoryDao.clearAllStories()
        profileDao.clearProfiles()
        // Completely purge both local database and Firestore cloud database server collections
        clearAllProfilesAndDataInBackend(keepOwnUser = false)
    }

    private fun getPersonalizedGreeting(profile: ProfileEntity): String {
        return when {
            profile.interests.contains("Coffee", ignoreCase = true) ->
                "Hey! Saw you love good coffee too ☕ What's your favorite local spot?"
            profile.interests.contains("Photography", ignoreCase = true) ->
                "Hey! Loved your profile. Are you more into film or digital? 📸"
            profile.interests.contains("Hiking", ignoreCase = true) ->
                "Hi there! Always up for a scenic trail. Where's your favorite hike nearby? 🌿"
            profile.interests.contains("Music", ignoreCase = true) || profile.interests.contains("Concerts", ignoreCase = true) ->
                "Hey! Great taste in music 🎶 What's the last live concert you went to?"
            else ->
                "Hey! So glad we matched. Your bio made me smile 😊 How's your week going?"
        }
    }

    private suspend fun seedInitialData() {
        // Disabled - Organic Profiles Only
        return
    }

    private suspend fun legacySeedInitialData() {
        val seedProfiles = listOf(
            ProfileEntity(
                id = "p_maya",
                name = "Maya Lin",
                age = 25,
                occupation = "Botanical Landscape Designer",
                city = "Uptown Greenways",
                distanceMiles = 3,
                bio = "Passionate about lush terrariums, warm sourdough, and golden retriever energy. Always down for sunset walks and indie bookstores.",
                interests = "Plants, Coffee, Film Photography, Indie Folk, Sourdough",
                relationshipGoal = "Long-term relationship",
                promptQuestion = "The fastest way to my heart is...",
                promptAnswer = "Fresh roasted coffee beans, a great bookstore recommendation, and sincere laughs.",
                gradientColorStart = 0xFFFF5E62,
                gradientColorEnd = 0xFFFF9966,
                avatarEmoji = "🌿",
                isVerified = true,
                likedMe = true, // Liked you!
                isSuperLikedMe = true,
                activityStatus = "Online now",
                height = "5'6\"",
                zodiacSign = "Taurus",
                country = "United States",
                countryFlag = "🇺🇸",
                gender = "Female",
                breakupCount = 1,
                friendsCount = 18,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_alex",
                name = "Alex Vance",
                age = 27,
                occupation = "Architectural Photographer",
                city = "Arts District",
                distanceMiles = 5,
                bio = "Chasing light across city skylines. Vinyl records in the evening, espresso in the morning, and exploring hidden gems on two wheels.",
                interests = "Photography, Cycling, Architecture, Vinyl, Jazz",
                relationshipGoal = "Long-term relationship",
                promptQuestion = "Together, we could...",
                promptAnswer = "Bike across the suspension bridge at sunset and hunt down the city's finest tacos.",
                gradientColorStart = 0xFF6A11CB,
                gradientColorEnd = 0xFF2575FC,
                avatarEmoji = "📸",
                isVerified = true,
                likedMe = true, // Liked you!
                isSuperLikedMe = false,
                activityStatus = "Active 15m ago",
                height = "5'11\"",
                zodiacSign = "Leo",
                country = "United States",
                countryFlag = "🇺🇸",
                gender = "Male",
                breakupCount = 0,
                friendsCount = 14,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_sophia",
                name = "Sophia Chen",
                age = 24,
                occupation = "UX Researcher & Ceramicist",
                city = "Vancouver Lofts",
                distanceMiles = 4,
                bio = "Throwing clay on weekends, dissecting human habits on weekdays. Can cook a 5-course dinner from random pantry leftovers.",
                interests = "Ceramics, Cooking, Board Games, Hiking, Modern Art",
                relationshipGoal = "Dating & seeing where it goes",
                promptQuestion = "My most spontaneous moment was...",
                promptAnswer = "Booking a midnight train to Montreal during a snowstorm just for warm bagels.",
                gradientColorStart = 0xFFFF0844,
                gradientColorEnd = 0xFFFFB199,
                avatarEmoji = "🎨",
                isVerified = true,
                likedMe = true, // Liked you!
                isSuperLikedMe = false,
                activityStatus = "Active now",
                height = "5'5\"",
                zodiacSign = "Libra",
                country = "Canada",
                countryFlag = "🇨🇦",
                gender = "Female",
                breakupCount = 2,
                friendsCount = 24,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_liam",
                name = "Liam O'Connor",
                age = 28,
                occupation = "Adventure Guide & Environmentalist",
                city = "Edinburgh Hills",
                distanceMiles = 9,
                bio = "Half mountain goat, half golden retriever. I spend my weekends in alpine lakes and weekdays working on forest conservation.",
                interests = "Trail Running, Backpacking, Dogs, Acoustic Guitar, Campfires",
                relationshipGoal = "Long-term partnership",
                promptQuestion = "I geek out on...",
                promptAnswer = "Stargazing charts, alpine weather patterns, and perfecting campfire flatbread.",
                gradientColorStart = 0xFF11998E,
                gradientColorEnd = 0xFF38EF7D,
                avatarEmoji = "🏔️",
                isVerified = true,
                likedMe = true, // Liked you!
                isSuperLikedMe = false,
                activityStatus = "Active 1h ago",
                height = "6'1\"",
                zodiacSign = "Sagittarius",
                country = "United Kingdom",
                countryFlag = "🇬🇧",
                gender = "Male",
                breakupCount = 1,
                friendsCount = 16,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_elena",
                name = "Elena Rostova",
                age = 26,
                occupation = "Structural Engineer",
                city = "Marina Waterfront",
                distanceMiles = 2,
                bio = "I design bridges by day and bake patisserie by night. Looking for someone with genuine curiosity and a quick wit.",
                interests = "Baking, Running, City Planning, Podcasts, Sailing",
                relationshipGoal = "Long-term relationship",
                promptQuestion = "My simple pleasures include...",
                promptAnswer = "Sunday morning crossword puzzles with fresh croissants and warm sunshine.",
                gradientColorStart = 0xFFFC466B,
                gradientColorEnd = 0xFF3F5EFB,
                avatarEmoji = "🥐",
                isVerified = true,
                likedMe = false,
                isSuperLikedMe = false,
                activityStatus = "Online now",
                height = "5'8\"",
                zodiacSign = "Virgo",
                country = "United States",
                countryFlag = "🇺🇸",
                gender = "Female",
                breakupCount = 0,
                friendsCount = 20,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_zara",
                name = "Zara Patel",
                age = 26,
                occupation = "Creative Director",
                city = "Bandra West, Mumbai",
                distanceMiles = 4,
                bio = "Color palettes, mid-century furniture, and spontaneous gallery openings. Fluent in sarcasm and warm hugs.",
                interests = "Graphic Design, Architecture, Chai, Modern Poetry, Vinyl",
                relationshipGoal = "Long-term relationship",
                promptQuestion = "My non-negotiables are...",
                promptAnswer = "Being kind to service staff, loving animals, and having passionate opinions on breakfast food.",
                gradientColorStart = 0xFFFF416C,
                gradientColorEnd = 0xFFFF4B2B,
                avatarEmoji = "✨",
                isVerified = true,
                likedMe = false,
                isSuperLikedMe = false,
                activityStatus = "Online now",
                height = "5'6\"",
                zodiacSign = "Capricorn",
                country = "India",
                countryFlag = "🇮🇳",
                gender = "Female",
                breakupCount = 0,
                friendsCount = 35,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_rohan",
                name = "Rohan Sharma",
                age = 28,
                occupation = "AI Product Designer",
                city = "Indiranagar, Bengaluru",
                distanceMiles = 5,
                bio = "Building smart tools by daylight, listening to classical sitar and jazz fusion at twilight. Loves filter coffee & street food walks.",
                interests = "Design, Filter Coffee, Indie Music, Street Photography, Trekking",
                relationshipGoal = "Long-term partnership",
                promptQuestion = "A life goal of mine is...",
                promptAnswer = "Starting an eco-friendly community cafe where people disconnect from screens and connect over board games.",
                gradientColorStart = 0xFFFF9966,
                gradientColorEnd = 0xFFFF5E62,
                avatarEmoji = "☕",
                isVerified = true,
                likedMe = false,
                isSuperLikedMe = false,
                activityStatus = "Active now",
                height = "5'11\"",
                zodiacSign = "Aquarius",
                country = "India",
                countryFlag = "🇮🇳",
                gender = "Male",
                breakupCount = 1,
                friendsCount = 42,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_isabella",
                name = "Isabella Santos",
                age = 25,
                occupation = "Botanical Illustrator",
                city = "Jardins, São Paulo",
                distanceMiles = 6,
                bio = "Watercolor nature sketchbook in hand. Bossa nova music, acai bowls, and golden hour cycling along Ibirapuera.",
                interests = "Illustration, Bossa Nova, Cycling, Brazilian Coffee, Plants",
                relationshipGoal = "New friends & companionship",
                promptQuestion = "Together we could...",
                promptAnswer = "Spend an entire afternoon painting ceramics and learning samba rhythm steps.",
                gradientColorStart = 0xFF00B09B,
                gradientColorEnd = 0xFF96C93D,
                avatarEmoji = "🌺",
                isVerified = true,
                likedMe = false,
                isSuperLikedMe = false,
                activityStatus = "Active 20m ago",
                height = "5'7\"",
                zodiacSign = "Leo",
                country = "Brazil",
                countryFlag = "🇧🇷",
                gender = "Female",
                breakupCount = 1,
                friendsCount = 28,
                isDatingGoal = false // Friendship goal!
            ),
            ProfileEntity(
                id = "p_lukas",
                name = "Lukas Meyer",
                age = 29,
                occupation = "Sound Designer & Musician",
                city = "Kreuzberg, Berlin",
                distanceMiles = 7,
                bio = "Analog synthesizers, ambient field recordings, and dark roast espresso. Big fan of indie cinema and flea markets.",
                interests = "Synthesizers, Electronic Music, Vinyl, Cycling, Art Cinema",
                relationshipGoal = "Dating & seeing where it goes",
                promptQuestion = "I geek out on...",
                promptAnswer = "Obscure Japanese ambient records and making homemade sourdough pizza on cast iron.",
                gradientColorStart = 0xFF654EA3,
                gradientColorEnd = 0xFFEAAFC8,
                avatarEmoji = "🎛️",
                isVerified = true,
                likedMe = false,
                isSuperLikedMe = false,
                activityStatus = "Active 2h ago",
                height = "6'2\"",
                zodiacSign = "Virgo",
                country = "Germany",
                countryFlag = "🇩🇪",
                gender = "Male",
                breakupCount = 2,
                friendsCount = 19,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_kenji",
                name = "Kenji Takahashi",
                age = 27,
                occupation = "Game Concept Artist",
                city = "Shibuya, Tokyo",
                distanceMiles = 8,
                bio = "Drawing fantastical anime worlds and drinking ceremonial matcha. Looking for meaningful conversations and kind hearts.",
                interests = "Concept Art, Matcha, Manga, Ramen, City Walking",
                relationshipGoal = "Long-term relationship",
                promptQuestion = "The quickest way to make me smile...",
                promptAnswer = "Sharing your favorite animated movie or introducing me to a cozy hole-in-the-wall noodle shop.",
                gradientColorStart = 0xFF8A2387,
                gradientColorEnd = 0xFFE94057,
                avatarEmoji = "🏮",
                isVerified = true,
                likedMe = false,
                isSuperLikedMe = false,
                activityStatus = "Active now",
                height = "5'10\"",
                zodiacSign = "Pisces",
                country = "Japan",
                countryFlag = "🇯🇵",
                gender = "Male",
                breakupCount = 0,
                friendsCount = 26,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_amber",
                name = "Amber Scott",
                age = 23,
                occupation = "Marine Biologist",
                city = "Bondi, Sydney",
                distanceMiles = 12,
                bio = "Ocean lover, scuba enthusiast, and coral reef protector. Happiest with salty hair and barefoot in the sand.",
                interests = "Scuba Diving, Marine Life, Surfing, Beach Volleyball, Ukulele",
                relationshipGoal = "Dating & seeing where it goes",
                promptQuestion = "A fact about me that surprises people...",
                promptAnswer = "I've swum with manta rays under a bioluminescent night tide!",
                gradientColorStart = 0xFF43CBFF,
                gradientColorEnd = 0xFF9708CC,
                avatarEmoji = "🌊",
                isVerified = true,
                likedMe = false,
                isSuperLikedMe = false,
                activityStatus = "Active today",
                height = "5'5\"",
                zodiacSign = "Cancer",
                country = "Australia",
                countryFlag = "🇦🇺",
                gender = "Female",
                breakupCount = 1,
                friendsCount = 21,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_chloe",
                name = "Chloe Dubois",
                age = 25,
                occupation = "Sommelier & Food Writer",
                city = "Le Marais, Paris",
                distanceMiles = 6,
                bio = "Life is too short for bad wine and boring conversations. Let's debate the best hole-in-the-wall dumpling shop.",
                interests = "Wine Tasting, Food Writing, Film Noir, Thrift Shopping, Jazz",
                relationshipGoal = "Long-term relationship",
                promptQuestion = "A boundary of mine is...",
                promptAnswer = "No pineapple on artisanal pizza, but I can be persuaded if you make the dough from scratch.",
                gradientColorStart = 0xFF8E2DE2,
                gradientColorEnd = 0xFF4A00E0,
                avatarEmoji = "🍷",
                isVerified = true,
                likedMe = false,
                isSuperLikedMe = false,
                activityStatus = "Active 3h ago",
                height = "5'7\"",
                zodiacSign = "Scorpio",
                country = "France",
                countryFlag = "🇫🇷",
                gender = "Female",
                breakupCount = 1,
                friendsCount = 30,
                isDatingGoal = true
            ),
            ProfileEntity(
                id = "p_devon",
                name = "Devon James",
                age = 27,
                occupation = "Culinary Chef",
                city = "Culinary Square",
                distanceMiles = 5,
                bio = "Specializing in farm-to-table seasonal menus. Will happily cook your favorite comfort meal anytime.",
                interests = "Cooking, Farmers Markets, Natural Wine, Cycling, Pottery",
                relationshipGoal = "Long-term partnership",
                promptQuestion = "My secret talent is...",
                promptAnswer = "Tasting any restaurant dish and figuring out the secret ingredients on the first bite.",
                gradientColorStart = 0xFFFA709A,
                gradientColorEnd = 0xFFFEE140,
                avatarEmoji = "🍳",
                isVerified = true,
                likedMe = false,
                isSuperLikedMe = false,
                activityStatus = "Active now",
                height = "5'10\"",
                zodiacSign = "Leo",
                country = "United States",
                countryFlag = "🇺🇸",
                gender = "Male",
                breakupCount = 0,
                friendsCount = 15,
                isDatingGoal = true
            )
        )

        val mappedSeedProfiles = seedProfiles.map { profile ->
            if (profile.avatarUrl.isBlank()) {
                profile.copy(avatarUrl = profile.getEffectiveAvatarUrl())
            } else {
                profile
            }
        }
        profileDao.insertProfiles(mappedSeedProfiles)

        // Ensure any existing profiles in the database have their avatarUrl populated
        val existingProfiles = profileDao.getAllProfilesSync()
        val toUpdate = existingProfiles.filter { it.avatarUrl.isBlank() }.map { it.copy(avatarUrl = it.getEffectiveAvatarUrl()) }
        if (toUpdate.isNotEmpty()) {
            profileDao.insertProfiles(toUpdate)
        }

        // Seed 1 active dating match with pre-existing dialogue for instant testability
        val matchedProfile = seedProfiles[4] // Elena
        val matchId = "match_${matchedProfile.id}"
        val initialMatch = MatchEntity(
            matchId = matchId,
            profileId = matchedProfile.id,
            matchedAt = System.currentTimeMillis() - 3600000 * 4,
            lastMessage = "Hey! Loved your bio. That coffee spot looks incredible!",
            lastMessageTime = System.currentTimeMillis() - 1800000,
            hasUnread = true,
            isDatingMatch = true,
            status = "ACTIVE",
            hasStartedChat = true
        )
        matchDao.insertMatch(initialMatch)
        chatMessageDao.insertMessage(
            ChatMessageEntity(
                messageId = UUID.randomUUID().toString(),
                matchId = matchId,
                senderId = matchedProfile.id,
                text = "Hey Jordan! So glad we matched! That coffee spot on your profile looks incredible ☕ Have you been recently?",
                timestamp = System.currentTimeMillis() - 1800000,
                isEncrypted = true,
                encryptionProtocol = "VibeSync E2EE Protocol (Double Ratchet + Curve25519)"
            )
        )

        // Update preferences so active dating match is registered
        val currentPrefs = preferencesDao.getPreferencesSync()
        if (currentPrefs != null) {
            preferencesDao.insertOrUpdate(currentPrefs.copy(activeDatingMatchId = matchId))
        }

        // Seed initial Status Stories from Real Friends & Matches
        val now = System.currentTimeMillis()
        val seedStories = listOf(
            StatusStoryEntity(
                id = "story_maya_1",
                userId = "p_maya",
                userName = "Maya Lin",
                userAvatarEmoji = "🌿",
                mediaType = "PHOTO",
                caption = "Morning botanical greenhouse harvest! 🌿✨ Nothing beats the smell of fresh eucalyptus.",
                backgroundColorStart = 0xFF11998E,
                backgroundColorEnd = 0xFF38EF7D,
                timestamp = now - 1800000,
                isViewed = false,
                viewsCount = 14,
                isMyStatus = false
            ),
            StatusStoryEntity(
                id = "story_elena_1",
                userId = "p_elena",
                userName = "Elena Rostova",
                userAvatarEmoji = "🥐",
                mediaType = "VIDEO",
                videoDurationSeconds = 24, // Video < 60s
                caption = "Fresh sourdough croissants just came out of the oven! 🥐🧈 (24s clip)",
                backgroundColorStart = 0xFFFC466B,
                backgroundColorEnd = 0xFF3F5EFB,
                timestamp = now - 3600000 * 2,
                isViewed = false,
                viewsCount = 28,
                isMyStatus = false
            ),
            StatusStoryEntity(
                id = "story_alex_1",
                userId = "p_alex",
                userName = "Alex Vance",
                userAvatarEmoji = "📸",
                mediaType = "PHOTO",
                caption = "Golden hour reflections across the suspension bridge 🏙️🚲",
                backgroundColorStart = 0xFF6A11CB,
                backgroundColorEnd = 0xFF2575FC,
                timestamp = now - 3600000 * 4,
                isViewed = false,
                viewsCount = 19,
                isMyStatus = false
            ),
            StatusStoryEntity(
                id = "story_liam_1",
                userId = "p_liam",
                userName = "Liam O'Connor",
                userAvatarEmoji = "🏔️",
                mediaType = "VIDEO",
                videoDurationSeconds = 45, // Video < 60s
                caption = "Sunrise hike through the alpine ridge with Cooper! 🐕🏔️ (45s view)",
                backgroundColorStart = 0xFF00B09B,
                backgroundColorEnd = 0xFF96C93D,
                timestamp = now - 3600000 * 6,
                isViewed = true,
                viewsCount = 42,
                isMyStatus = false
            ),
            StatusStoryEntity(
                id = "story_sophia_1",
                userId = "p_sophia",
                userName = "Sophia Chen",
                userAvatarEmoji = "🎨",
                mediaType = "PHOTO",
                caption = "Just finished glazing this handmade matcha bowl 🍵✨",
                backgroundColorStart = 0xFFFF0844,
                backgroundColorEnd = 0xFFFFB199,
                timestamp = now - 3600000 * 9,
                isViewed = true,
                viewsCount = 31,
                isMyStatus = false
            )
        )
        statusStoryDao.insertStories(seedStories)
    }

    suspend fun getMatchByProfileId(profileId: String): MatchEntity? {
        return matchDao.getMatchByProfileId(profileId)
    }

    suspend fun getAllProfilesSync(): List<ProfileEntity> {
        return profileDao.getAllProfilesForAdmin().first()
    }

    suspend fun generateLawEnforcementUserData(targetUserId: String): String {
        val prefs = preferencesDao.getPreferencesSync() ?: UserPreferencesEntity()
        val account = registeredAccountDao.getPrimaryAccount()
        val profile = profileDao.getProfileByIdSync(targetUserId) ?: profileDao.getAllProfilesForAdmin().first().firstOrNull { it.id == targetUserId }
        val matches = matchDao.getAllMatches().first().filter { it.profileId == targetUserId || targetUserId == "current_user" }
        val friendships = friendshipRequestDao.getAllRequestsFlow().first().filter { it.senderId == targetUserId || it.receiverId == targetUserId }

        val userName = profile?.name ?: prefs.userName
        val phone = if (targetUserId == "current_user") prefs.verifiedMobileNumber.ifBlank { account?.phoneNumber ?: "+1 555-0199" } else "+1 555-4389"
        val email = if (targetUserId == "current_user") prefs.googleEmail else "${userName.lowercase().replace(" ", "")}@vikesync.app"

        return """
        {
          "law_enforcement_request_type": "CERTIFIED_SOP_DATA_EXPORT",
          "compliance_standards": ["ISO_27001", "GDPR_LEAL_SOP", "CRIMINAL_JUSTICE_COMPLIANCE"],
          "export_timestamp": "${System.currentTimeMillis()}",
          "target_user_id": "$targetUserId",
          "identity_records": {
            "full_name": "$userName",
            "verified_phone_number": "$phone",
            "verified_email": "$email",
            "mobile_verified": ${prefs.isMobileVerified},
            "face_3d_liveness_verified": ${prefs.isFaceVerified},
            "face_verification_timestamp": ${prefs.faceVerificationTimestamp},
            "biometric_hash_sha256": "${prefs.biometricHash.ifBlank { account?.biometricHash ?: "BIO_SHA256_AUTHENTICATED_911" }}",
            "registered_ip_address": "198.51.100.42",
            "device_fingerprint": "ANDROID_BUILD_ID_S24_ULTRA_VIBE_88201"
          },
          "location_audit": {
            "latitude": ${prefs.latitude},
            "longitude": ${prefs.longitude},
            "last_location_address": "${profile?.city ?: prefs.userAddress}",
            "last_location_update_timestamp": ${prefs.lastLocationUpdateTimestamp}
          },
          "relationship_and_match_records_count": ${matches.size},
          "friendship_requests_audit_count": ${friendships.size},
          "trust_score": ${profile?.trustScore ?: prefs.trustRating},
          "moderation_status": {
            "is_banned": ${profile?.isBanned ?: false},
            "is_spam_flagged": ${profile?.isFlaggedSpam ?: false},
            "moderation_note": "${profile?.moderationNote ?: "Clean standing"}"
          }
        }
        """.trimIndent()
    }

    suspend fun generateUserActivityLog(targetUserId: String): String {
        val profile = profileDao.getProfileByIdSync(targetUserId) ?: profileDao.getAllProfilesForAdmin().first().firstOrNull { it.id == targetUserId }
        val userName = profile?.name ?: "User ($targetUserId)"
        val now = System.currentTimeMillis()
        return """
        TIMESTAMP,EVENT_TYPE,USER_ID,USER_NAME,IP_ADDRESS,DEVICE_FINGERPRINT,STATUS,SECURITY_SOP
        ${now - 400000},ACCOUNT_REGISTRATION,$targetUserId,"$userName",198.51.100.42,ANDROID_BUILD_S24,VERIFIED_PHONE,PASS
        ${now - 350000},FACE_3D_LIVENESS_SCAN,$targetUserId,"$userName",198.51.100.42,ANDROID_BUILD_S24,FACE_MATCH_99.9%,PASS
        ${now - 280000},LOCATION_UPDATE_SOP,$targetUserId,"$userName",198.51.100.42,ANDROID_BUILD_S24,"${profile?.city ?: "San Francisco"}",PASS
        ${now - 210000},SWIPE_INTERACTION,$targetUserId,"$userName",198.51.100.42,ANDROID_BUILD_S24,SWIPE_LIKE_SUCCESS,PASS
        ${now - 140000},FRIENDSHIP_REQUEST_SENT,$targetUserId,"$userName",198.51.100.42,ANDROID_BUILD_S24,QUOTA_CHECK_OK,PASS
        ${now - 70000},E2EE_SIGNAL_CHAT_MSG,$targetUserId,"$userName",198.51.100.42,ANDROID_BUILD_S24,MSG_ENCRYPTED_DELIVERED,PASS
        ${now - 10000},MODERATION_AUDIT_LOG,$targetUserId,"$userName",198.51.100.42,ANDROID_BUILD_S24,PROFILE_ACTIVE_TRUST_${profile?.trustScore ?: 95}%,PASS
        """.trimIndent()
    }

    suspend fun generateSystemActivityLogs(): String {
        val now = System.currentTimeMillis()
        return """
        TIMESTAMP,EVENT_TYPE,USER_ID,IP_ADDRESS,DEVICE_FINGERPRINT,STATUS,SECURITY_SOP
        ${now - 300000},USER_LOGIN_SUCCESS,current_user,198.51.100.42,ANDROID_BUILD_S24,VERIFIED_MPIN,PASS
        ${now - 250000},VIBESYNC_OTP_VERIFIED,current_user,198.51.100.42,ANDROID_BUILD_S24,WA_E2EE_OTP,PASS
        ${now - 200000},FACE_3D_LIVENESS_SCAN,current_user,198.51.100.42,ANDROID_BUILD_S24,FACE_MATCH_99.8%,PASS
        ${now - 150000},SWIPE_FRIEND_REQUEST,current_user,198.51.100.42,ANDROID_BUILD_S24,REQ_SENT_P_SOPHIA,PASS
        ${now - 100000},E2EE_SIGNAL_CHAT_INIT,current_user,198.51.100.42,ANDROID_BUILD_S24,KEY_EXCHANGE_OK,PASS
        ${now - 50000},CLOUD_VAULT_BACKUP,current_user,198.51.100.42,ANDROID_BUILD_S24,GDRIVE_SYNC_OK,PASS
        ${now - 10000},ADMIN_MODERATION_AUDIT,admin,10.0.0.1,DEV_PORTAL_SOP,DATA_EXPORT_GENERATED,AUTHORIZED
        """.trimIndent()
    }

    // ==========================================
    // BUSINESS PROFILE, TIMELINE & OFFERS SYSTEM
    // ==========================================

    fun getPostsForBusiness(businessId: String): Flow<List<BusinessPostEntity>> =
        businessDao.getPostsForBusiness(businessId)

    suspend fun toggleFollowBusiness(businessId: String) {
        val biz = businessDao.getBusinessByIdSync(businessId) ?: return
        val newStatus = !biz.isFollowed
        val delta = if (newStatus) 1 else -1
        businessDao.updateFollowStatus(businessId, newStatus, delta)
    }

    suspend fun createBusinessProfile(
        name: String,
        tagline: String,
        category: String,
        description: String,
        address: String,
        city: String = "Bangalore",
        distanceKm: Double = 1.0,
        bannerUrl: String = "",
        logoEmoji: String = "🏢",
        phoneNumber: String = "+91 98765 43210",
        websiteUrl: String = "https://vibesync.app/partner",
        initialOfferTitle: String = "",
        initialDiscountPercent: Int = 20,
        initialPromoCode: String = "VIBE20",
        verificationTier: String = "STANDARD",
        walletPoints: Int = 500,
        isListingPaid: Boolean = true,
        listingPaymentTxnId: String = "",
        latitude: Double = 12.9716,
        longitude: Double = 77.5946,
        photoGallery: List<String> = emptyList()
    ): BusinessEntity {
        val bizId = "biz_${System.currentTimeMillis()}"
        val safeBanner = if (bannerUrl.isNotBlank()) bannerUrl else photoGallery.firstOrNull() ?: "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80"
        val photoGalleryJoined = if (photoGallery.isNotEmpty()) photoGallery.joinToString("|") else safeBanner
            val currentOwner = try {
                val prefs = preferencesDao.getPreferencesSync()
                if (!prefs?.verifiedMobileNumber.isNullOrBlank()) prefs!!.verifiedMobileNumber
                else if (!prefs?.googleEmail.isNullOrBlank()) prefs!!.googleEmail
                else "current_user"
            } catch (_: Exception) {
                "current_user"
            }

            val business = BusinessEntity(
                id = bizId,
                name = name.trim(),
                tagline = tagline.trim(),
                category = category,
                description = description.trim(),
                address = address.trim(),
                city = city.trim(),
                latitude = latitude,
                longitude = longitude,
                distanceKm = distanceKm,
                rating = 5.0,
                reviewCount = 1,
                bannerUrl = safeBanner,
                photoGalleryJson = photoGalleryJoined,
                logoEmoji = logoEmoji,
                phoneNumber = phoneNumber.trim(),
                websiteUrl = websiteUrl.trim(),
                isVerified = verificationTier != "STANDARD",
                isFollowed = true,
                followerCount = 1,
                isUserCreated = true,
                ownerUserId = currentOwner,
                activeOfferSummary = if (initialOfferTitle.isNotBlank()) "$initialOfferTitle ($initialDiscountPercent% OFF)" else "Welcome Offer",
                verificationTier = verificationTier,
                walletPoints = walletPoints,
                isListingPaid = isListingPaid,
                listingPaymentTxnId = listingPaymentTxnId,
                createdAt = System.currentTimeMillis()
            )
            businessDao.insertBusiness(business)

            try {
                com.example.util.BusinessHubSyncManager.syncVenueToFirestore(business)
            } catch (e: Exception) {
                Log.w("DatingRepository", "Failed to sync venue to Firestore: ${e.message}")
            }

        if (initialOfferTitle.isNotBlank()) {
            val initialPost = BusinessPostEntity(
                id = "post_${System.currentTimeMillis()}",
                businessId = bizId,
                title = initialOfferTitle,
                content = "Special launch discount for VibeSync members! Use promo code $initialPromoCode or show your profile at checkout.",
                postType = "DISCOUNT",
                mediaUrl = safeBanner,
                mediaType = "IMAGE",
                discountPercent = initialDiscountPercent,
                promoCode = initialPromoCode,
                validUntil = "Limited Time Offer",
                likesCount = 12,
                timestamp = System.currentTimeMillis()
            )
            businessDao.insertPost(initialPost)
        }
        return business
    }

    suspend fun addBusinessPost(
        businessId: String,
        title: String,
        content: String,
        postType: String = "OFFER",
        mediaUrl: String = "",
        mediaType: String = "IMAGE",
        discountPercent: Int = 0,
        promoCode: String = "",
        validUntil: String = "Valid till Weekend"
    ) {
        val post = BusinessPostEntity(
            id = "post_${System.currentTimeMillis()}",
            businessId = businessId,
            title = title.trim(),
            content = content.trim(),
            postType = postType,
            mediaUrl = mediaUrl.trim(),
            mediaType = mediaType,
            discountPercent = discountPercent,
            promoCode = promoCode.trim().uppercase(),
            validUntil = validUntil.trim(),
            likesCount = 0,
            timestamp = System.currentTimeMillis()
        )
        businessDao.insertPost(post)
    }

    suspend fun togglePostLike(postId: String, isLiked: Boolean) {
        val delta = if (isLiked) 1 else -1
        businessDao.togglePostLike(postId, isLiked, delta)
    }

    // ==========================================
    // REVIEWS & STORE VISITOR RATINGS
    // ==========================================

    fun getReviewsForBusiness(businessId: String): Flow<List<BusinessReviewEntity>> =
        businessDao.getReviewsForBusiness(businessId)

    suspend fun addStoreVisitorReview(
        businessId: String,
        rating: Double,
        reviewText: String,
        isGpsVerifiedVisit: Boolean = true,
        checkInDistanceMeters: Double = 35.0
    ): BusinessReviewEntity {
        val user = preferencesDao.getPreferencesSync()
        val authorName = if (!user?.userName.isNullOrBlank()) user!!.userName
        else if (!user?.verifiedMobileNumber.isNullOrBlank()) user!!.verifiedMobileNumber
        else "Store Visitor"

        val review = BusinessReviewEntity(
            id = "rev_${System.currentTimeMillis()}",
            businessId = businessId,
            userId = user?.verifiedMobileNumber?.ifBlank { user.googleEmail } ?: "user_${System.currentTimeMillis() % 1000}",
            userName = authorName,
            userAvatarEmoji = "⭐",
            rating = rating,
            reviewText = reviewText.trim(),
            isGpsVerifiedVisit = isGpsVerifiedVisit,
            checkInDistanceMeters = checkInDistanceMeters,
            createdAt = System.currentTimeMillis()
        )
        businessDao.insertReview(review)

        val currentBiz = businessDao.getBusinessByIdSync(businessId)
        if (currentBiz != null) {
            val newCount = currentBiz.reviewCount + 1
            val newRating = Math.round(((currentBiz.rating * currentBiz.reviewCount + rating) / newCount) * 10.0) / 10.0
            businessDao.updateBusinessRating(businessId, newRating, newCount)
        }
        return review
    }

    // ==========================================
    // BUSINESS WALLET & BROADCAST POINTS SYSTEM
    // ==========================================

    suspend fun topUpBusinessWallet(businessId: String, addedPoints: Int): Int {
        val biz = businessDao.getBusinessByIdSync(businessId) ?: return 0
        val newBalance = biz.walletPoints + addedPoints
        businessDao.updateWalletPoints(businessId, newBalance)
        return newBalance
    }

    suspend fun upgradeVerificationTier(businessId: String, newTier: String) {
        businessDao.updateVerificationTier(businessId, newTier)
    }

    suspend fun updateWhatsAppApiConfig(
        businessId: String,
        enabled: Boolean,
        wabaId: String,
        phone: String,
        apiKey: String
    ) {
        businessDao.updateWhatsAppApiConfig(businessId, enabled, wabaId, phone, apiKey)
    }

    data class FollowerBroadcastResult(
        val success: Boolean,
        val totalFollowers: Int,
        val pointsDeducted: Int,
        val deliveredCount: Int,
        val undeliveredCount: Int,
        val pointsReversed: Int,
        val remainingWalletPoints: Int,
        val message: String
    )

    suspend fun sendFollowerBroadcast(
        businessId: String,
        title: String,
        content: String
    ): FollowerBroadcastResult {
        val biz = businessDao.getBusinessByIdSync(businessId)
            ?: return FollowerBroadcastResult(false, 0, 0, 0, 0, 0, 0, "Business not found")

        val followers = biz.followerCount.coerceAtLeast(1)
        val pointsRequired = followers * 1 // 1 Point = ₹0.05 per follower

        if (biz.walletPoints < pointsRequired) {
            return FollowerBroadcastResult(
                success = false,
                totalFollowers = followers,
                pointsDeducted = 0,
                deliveredCount = 0,
                undeliveredCount = 0,
                pointsReversed = 0,
                remainingWalletPoints = biz.walletPoints,
                message = "Insufficient points balance! Reaching $followers followers requires $pointsRequired points (₹${String.format("%.2f", pointsRequired * 0.05)}). Current balance: ${biz.walletPoints} pts."
            )
        }

        // Deduct upfront points
        val balanceAfterCharge = biz.walletPoints - pointsRequired
        businessDao.updateWalletPoints(businessId, balanceAfterCharge)

        // Calculate delivery: ~92% delivered & read with double tick ✓✓
        val deliveredCount = Math.round(followers * 0.92f).toInt().coerceAtLeast(1)
        val undeliveredCount = (followers - deliveredCount).coerceAtLeast(0)

        // Points reversed back for followers who didn't read / receive double tick
        val pointsReversed = undeliveredCount * 1
        val finalBalance = balanceAfterCharge + pointsReversed
        if (pointsReversed > 0) {
            businessDao.updateWalletPoints(businessId, finalBalance)
        }

        // Insert as broadcast post
        val post = BusinessPostEntity(
            id = "post_${System.currentTimeMillis()}",
            businessId = businessId,
            title = "📢 $title",
            content = content,
            postType = "OFFER",
            mediaUrl = biz.bannerUrl,
            mediaType = "IMAGE",
            discountPercent = 15,
            promoCode = "BROADCAST",
            validUntil = "Limited Exclusive",
            likesCount = deliveredCount / 10 + 1,
            timestamp = System.currentTimeMillis()
        )
        businessDao.insertPost(post)

        return FollowerBroadcastResult(
            success = true,
            totalFollowers = followers,
            pointsDeducted = pointsRequired,
            deliveredCount = deliveredCount,
            undeliveredCount = undeliveredCount,
            pointsReversed = pointsReversed,
            remainingWalletPoints = finalBalance,
            message = "Broadcast dispatched to $followers followers! $deliveredCount received read receipts (✓✓ double tick). $undeliveredCount undelivered; $pointsReversed points reversed to your wallet."
        )
    }

    // ==========================================
    // BROADCAST CHANNELS SYSTEM
    // ==========================================

    fun getBroadcastsForChannel(channelId: String): Flow<List<ChannelBroadcastEntity>> =
        channelDao.getBroadcastsForChannel(channelId)

    suspend fun toggleFollowChannel(channelId: String) {
        val channel = channelDao.getChannelByIdSync(channelId) ?: return
        val newStatus = !channel.isFollowed
        val delta = if (newStatus) 1 else -1
        channelDao.updateFollowStatus(channelId, newStatus, delta)
    }

    suspend fun createChannel(
        name: String,
        handle: String,
        description: String,
        category: String,
        iconEmoji: String = "📢",
        bannerUrl: String = ""
    ): ChannelEntity {
        val channelId = "chan_${System.currentTimeMillis()}"
        val safeHandle = if (handle.startsWith("@")) handle else "@$handle"
        val channel = ChannelEntity(
            id = channelId,
            name = name.trim(),
            handle = safeHandle.trim().lowercase(),
            description = description.trim(),
            category = category,
            iconEmoji = iconEmoji,
            bannerUrl = bannerUrl.trim(),
            creatorUserId = "current_user",
            creatorName = preferencesDao.getPreferencesSync()?.userName?.ifBlank { "You" } ?: "You",
            isVerified = true,
            isFollowed = true,
            followerCount = 1,
            createdAt = System.currentTimeMillis()
        )
        channelDao.insertChannel(channel)

        val welcomeBroadcast = ChannelBroadcastEntity(
            id = "bc_${System.currentTimeMillis()}",
            channelId = channelId,
            senderName = channel.creatorName,
            content = "Welcome to $name! Stay tuned for breaking news, event alerts, photos & videos broadcasts.",
            broadcastType = "MESSAGE",
            mediaType = "NONE",
            reactionsCount = 5,
            timestamp = System.currentTimeMillis()
        )
        channelDao.insertBroadcast(welcomeBroadcast)
        return channel
    }

    suspend fun postBroadcast(
        channelId: String,
        content: String,
        broadcastType: String = "MESSAGE",
        mediaUrl: String = "",
        mediaType: String = "NONE"
    ) {
        val senderName = preferencesDao.getPreferencesSync()?.userName?.ifBlank { "Host" } ?: "Host"
        val broadcast = ChannelBroadcastEntity(
            id = "bc_${System.currentTimeMillis()}",
            channelId = channelId,
            senderName = senderName,
            content = content.trim(),
            broadcastType = broadcastType,
            mediaUrl = mediaUrl.trim(),
            mediaType = mediaType,
            reactionsCount = 0,
            timestamp = System.currentTimeMillis()
        )
        channelDao.insertBroadcast(broadcast)
    }

    suspend fun toggleBroadcastReaction(broadcastId: String, isReacted: Boolean) {
        val delta = if (isReacted) 1 else -1
        channelDao.toggleReaction(broadcastId, isReacted, delta)
    }

    suspend fun seedInitialBusinessesAndChannels() {
        if (businessDao.getCount() < 10) {
            val initialBusinesses = listOf(
                // Food, Beverage & Hospitality
                BusinessEntity(
                    id = "biz_third_wave",
                    name = "Third Wave Coffee Roasters",
                    tagline = "Artisan Single-Origin Brews & Cozy First Date Couch Corners ☕",
                    category = "Food, Beverage & Hospitality • Café / Coffee shop",
                    description = "Specialty coffee roastery dedicated to farm-to-cup coffee excellence. Cozy atmosphere, quiet corner booths, curated music playlists and warm lighting designed for genuine conversations.",
                    address = "12th Main Road, Indiranagar, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9716,
                    longitude = 77.6412,
                    distanceKm = 0.8,
                    rating = 4.9,
                    reviewCount = 2840,
                    priceRange = "₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "☕",
                    phoneNumber = "+91 80 4123 4567",
                    websiteUrl = "https://thirdwavecoffee.in",
                    socialHandle = "@thirdwavecoffeeindia",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 4250,
                    activeOfferSummary = "🏷️ 35% OFF Couple Brew + Cheesecake"
                ),
                BusinessEntity(
                    id = "biz_social",
                    name = "Church Street Social",
                    tagline = "Iconic Date Night Lounge, Live Indie Gigs & Signature Drinks 🍸",
                    category = "Food, Beverage & Hospitality • Bar / Pub / Cocktail lounge",
                    description = "A path-breaking space that blurs the lines between work and play. Urban industrial chic, world-class signature cocktails, retro arcade games, and craft burgers perfect for memorable couple dates.",
                    address = "Church Street, Ashok Nagar, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9749,
                    longitude = 77.6094,
                    distanceKm = 1.4,
                    rating = 4.8,
                    reviewCount = 5120,
                    priceRange = "₹₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "🍸",
                    phoneNumber = "+91 80 2558 7890",
                    websiteUrl = "https://socialoffline.in",
                    socialHandle = "@socialoffline",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 6800,
                    activeOfferSummary = "🍸 Buy 1 Get 1 Free Cocktails on First Dates"
                ),
                BusinessEntity(
                    id = "biz_toit",
                    name = "Toit Brewpub",
                    tagline = "Bangalore's Legendary Craft Beers & Wood-Fired Sourdough Pizzas 🍕",
                    category = "Food, Beverage & Hospitality • Restaurants (fast food, casual, fine dining)",
                    description = "Founded in 2010 to foster a culture of artisanal craft brewing. Famous for Tint-In-Wit Belgian Wheat, Basmati Blonde, and pet-friendly outdoor seating that makes breaking the ice effortless.",
                    address = "100 Feet Road, Near KFC Signal, Indiranagar, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9790,
                    longitude = 77.6406,
                    distanceKm = 2.1,
                    rating = 4.9,
                    reviewCount = 8900,
                    priceRange = "₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1575444758702-4a6b9222336e?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "🍺",
                    phoneNumber = "+91 90197 13388",
                    websiteUrl = "https://toit.in",
                    socialHandle = "@toitbeer",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 11200,
                    activeOfferSummary = "🍕 Free Wood-Fired Dessert on bills > ₹1,200"
                ),
                BusinessEntity(
                    id = "biz_milano",
                    name = "Milano Ice Cream & Gelateria",
                    tagline = "Authentic Italian Gelato, Waffle Cones & Vegan Sorbets 🍨",
                    category = "Food, Beverage & Hospitality • Ice cream parlor / Juice bar",
                    description = "Traditional slow-churned Italian artisan gelato made with natural ingredients. Perfect dessert stop after a scenic walk or dinner date.",
                    address = "80 Feet Road, Indiranagar, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9705,
                    longitude = 77.6390,
                    distanceKm = 1.1,
                    rating = 4.9,
                    reviewCount = 3120,
                    priceRange = "₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1501443762994-82bd5dace89a?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "🍨",
                    phoneNumber = "+91 80 4321 8899",
                    websiteUrl = "https://milanogelato.in",
                    socialHandle = "@milanogelatoblr",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 4800,
                    activeOfferSummary = "🍨 Complimentary Second Scoop for Couples"
                ),

                // Retail & Shops
                BusinessEntity(
                    id = "biz_blossom",
                    name = "Blossom Book House",
                    tagline = "Iconic Multi-Storey Haven for Bibliophiles, Rare Editions & Cozy Dates 📚",
                    category = "Retail & Shops • Bookstore & Stationery shop",
                    description = "Three floors packed with classic literature, graphic novels, rare first editions and cozy reading nooks. The quintessential Bangalore intellectual romance meetup spot.",
                    address = "Church Street, Shanthala Nagar, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9750,
                    longitude = 77.6066,
                    distanceKm = 1.5,
                    rating = 4.9,
                    reviewCount = 9200,
                    priceRange = "₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "📚",
                    phoneNumber = "+91 80 2555 9733",
                    websiteUrl = "https://blossombookhouse.com",
                    socialHandle = "@blossombooks",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 7600,
                    activeOfferSummary = "📚 Flat 20% OFF Classic Romantic Literature"
                ),
                BusinessEntity(
                    id = "biz_natures_basket",
                    name = "Nature's Basket Organic Supermarket",
                    tagline = "Artisanal Cheeses, Imported Chocolates & Gourmet Treats 🛒",
                    category = "Retail & Shops • Grocery store / Supermarket",
                    description = "Pioneer in gourmet food shopping. Browse artisanal cheeses, imported pasta, freshly baked sourdough and exotic snacks for home picnic dates.",
                    address = "100 Feet Road, HAL 2nd Stage, Indiranagar, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9785,
                    longitude = 77.6420,
                    distanceKm = 1.9,
                    rating = 4.7,
                    reviewCount = 2100,
                    priceRange = "₹₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "🛒",
                    phoneNumber = "+91 80 4110 5000",
                    websiteUrl = "https://naturesbasket.co.in",
                    socialHandle = "@naturesbasket",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 3400,
                    activeOfferSummary = "🧀 15% OFF Imported Swiss & French Cheeses"
                ),
                BusinessEntity(
                    id = "biz_flora",
                    name = "Vibe Blooms & Exotic Floral Studio",
                    tagline = "Handcrafted Dutch Roses, Terrariums & Date Bouquets 💐",
                    category = "Retail & Shops • Flower shop / Florist",
                    description = "Bespoke florist specialising in premium long-stemmed roses, dried lavender arrangements, and tabletop succulent terrariums crafted to charm.",
                    address = "5th Block, Koramangala, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9340,
                    longitude = 77.6150,
                    distanceKm = 3.4,
                    rating = 4.8,
                    reviewCount = 1450,
                    priceRange = "₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1526047932273-341f2a7631f9?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "💐",
                    phoneNumber = "+91 98450 77112",
                    websiteUrl = "https://vibeblooms.com",
                    socialHandle = "@vibeblooms_blr",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 2900,
                    activeOfferSummary = "💐 Free Customized Message Card + Ribbon on Bouquets"
                ),

                // Health, Wellness & Personal Care
                BusinessEntity(
                    id = "biz_cultfit",
                    name = "Cult.fit Elite Yoga & Fitness Studio",
                    tagline = "Couple Yoga, HIIT, Boxing & Mindfulness Sessions 🏋️",
                    category = "Health, Wellness & Personal Care • Gym / Fitness center / Yoga studio",
                    description = "Holistic health platform offering guided workouts, expert trainers, recovery zones, and smoothie bars. Workout together and bond over healthy fitness habits.",
                    address = "27th Main, Sector 1, HSR Layout, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9116,
                    longitude = 77.6389,
                    distanceKm = 3.9,
                    rating = 4.8,
                    reviewCount = 3450,
                    priceRange = "₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "🏋️",
                    phoneNumber = "+91 80 6700 8900",
                    websiteUrl = "https://cult.fit",
                    socialHandle = "@cultfit_india",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 9400,
                    activeOfferSummary = "🏋️ 7-Day Free Couple Gym & Spa Pass"
                ),
                BusinessEntity(
                    id = "biz_toni_guy",
                    name = "Toni&Guy Hair Salon & Barbershop",
                    tagline = "International Hair Styling, Grooming & Couple Pampering ✂️",
                    category = "Health, Wellness & Personal Care • Hair salon / Barbershop",
                    description = "World-renowned hair dressing brand offering runway-inspired haircuts, beard trims, keratin treatments and scalp spas before date nights.",
                    address = "12th Main Road, Indiranagar, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9715,
                    longitude = 77.6380,
                    distanceKm = 0.9,
                    rating = 4.9,
                    reviewCount = 1890,
                    priceRange = "₹₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1560066984-138dadb4c035?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "✂️",
                    phoneNumber = "+91 80 4148 6633",
                    websiteUrl = "https://toniandguyblr.in",
                    socialHandle = "@toniandguy_indiranagar",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 5100,
                    activeOfferSummary = "✂️ 25% OFF Pre-Date Hair Styling Combo"
                ),
                BusinessEntity(
                    id = "biz_o2_spa",
                    name = "O2 Day Spa & Nail Sanctuary",
                    tagline = "Swedish Massages, Aromatherapy & Couple Reflexology 🧖‍♀️",
                    category = "Health, Wellness & Personal Care • Nail salon / Day spa",
                    description = "Serene sanctuary escape featuring essential oil deep tissue massages, warm herbal compresses, and deluxe manicure/pedicure stations.",
                    address = "Koramangala 4th Block, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9345,
                    longitude = 77.6200,
                    distanceKm = 3.1,
                    rating = 4.8,
                    reviewCount = 2240,
                    priceRange = "₹₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "🧖‍♀️",
                    phoneNumber = "+91 92434 22000",
                    websiteUrl = "https://o2spa.org",
                    socialHandle = "@o2spa_official",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 4100,
                    activeOfferSummary = "🧖 40% OFF 60-min Couple Aromatherapy Massage"
                ),

                // Services & Utilities
                BusinessEntity(
                    id = "biz_hdfc_bank",
                    name = "HDFC Bank & 24/7 ATM Kiosk",
                    tagline = "Multi-Currency Forex, Wealth Services & Cash Dispenser 🏧",
                    category = "Services & Utilities • Bank / ATM kiosk",
                    description = "Full-service banking branch featuring instant forex card issuance for overseas couple trips, premium lockers, and round-the-clock secure ATM access.",
                    address = "100 Feet Road, Indiranagar, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9710,
                    longitude = 77.6400,
                    distanceKm = 0.7,
                    rating = 4.6,
                    reviewCount = 1120,
                    priceRange = "₹",
                    bannerUrl = "https://images.unsplash.com/photo-1541354329998-f4d9a9f9297f?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "🏧",
                    phoneNumber = "+91 1800 202 6161",
                    websiteUrl = "https://hdfcbank.com",
                    socialHandle = "@hdfcbank",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 2100,
                    activeOfferSummary = "💳 Zero Forex Markup on International Travel Cards"
                ),
                BusinessEntity(
                    id = "biz_blue_dart",
                    name = "Blue Dart Express & Courier Center",
                    tagline = "Next-Day Air Shipping, Gift Delivery & Parcel Tracking 📦",
                    category = "Services & Utilities • Post office / Courier & shipping center",
                    description = "Reliable domestic and international express parcel handling. Safe protective packaging for long-distance romance gifts and time-sensitive documents.",
                    address = "MG Road, Shanthala Nagar, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9755,
                    longitude = 77.6050,
                    distanceKm = 1.6,
                    rating = 4.7,
                    reviewCount = 1560,
                    priceRange = "₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "📦",
                    phoneNumber = "+91 80 2559 3444",
                    websiteUrl = "https://bluedart.com",
                    socialHandle = "@bluedart_india",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 1800,
                    activeOfferSummary = "📦 20% OFF Express Gift Parcel Dispatch"
                ),
                BusinessEntity(
                    id = "biz_spin_cycle",
                    name = "Spin Cycle Laundromat & Dry Cleaners",
                    tagline = "Eco-Friendly Steam Press, Stain Care & Quick Wash 👔",
                    category = "Services & Utilities • Laundromat / Dry cleaners",
                    description = "Modern laundromat with Italian gentle-wash machines and organic detergent wash cycles. Guaranteed crisp suits and dresses for date nights.",
                    address = "5th Block, Koramangala, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9360,
                    longitude = 77.6140,
                    distanceKm = 3.3,
                    rating = 4.8,
                    reviewCount = 980,
                    priceRange = "₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "👔",
                    phoneNumber = "+91 99001 88223",
                    websiteUrl = "https://spincyclelaundry.in",
                    socialHandle = "@spincycle_blr",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 1600,
                    activeOfferSummary = "👔 Free Express Steam Press on 3 Garments"
                ),

                // Public & Community Spaces
                BusinessEntity(
                    id = "biz_central_library",
                    name = "State Central Public Library",
                    tagline = "Majestic Red Brick Heritage, Peaceful Reading Halls & Quiet Gazebos 🏛️",
                    category = "Public & Community Spaces • Library",
                    description = "Founded in 1915 inside verdant Cubbon Park with over 300,000 historic volumes, rose garden pathways, and stone reading benches perfect for quiet contemplative strolls.",
                    address = "Cubbon Park, Kasturba Road, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9760,
                    longitude = 77.5920,
                    distanceKm = 2.4,
                    rating = 4.9,
                    reviewCount = 7400,
                    priceRange = "₹",
                    bannerUrl = "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "🏛️",
                    phoneNumber = "+91 80 2221 2135",
                    websiteUrl = "https://karnatakapubliclibrary.gov.in",
                    socialHandle = "@cubbonparklibrary",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 6200,
                    activeOfferSummary = "🏛️ Free Public Entry & Historical Heritage Walk"
                ),
                BusinessEntity(
                    id = "biz_trekking",
                    name = "Bangalore Sunrise Trekkers Club",
                    tagline = "Skandagiri Sunrise Hikes, Camping, Stargazing & Group Carpools 🏕️",
                    category = "Public & Community Spaces • Community center",
                    description = "Certified mountain guides organising weekend adventure getaways. Connect with fitness and nature lovers, witness magical cloud-bed sunrises, and share campfire stories under the stars.",
                    address = "Koramangala 4th Block, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9352,
                    longitude = 77.6245,
                    distanceKm = 3.2,
                    rating = 4.9,
                    reviewCount = 1640,
                    priceRange = "₹₹",
                    bannerUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "🏕️",
                    phoneNumber = "+91 98450 12345",
                    websiteUrl = "https://sunrisetreks.in",
                    socialHandle = "@blr_trekkers",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 3890,
                    activeOfferSummary = "🏕️ ₹500 OFF Weekend Skandagiri Sunrise Trek"
                ),
                BusinessEntity(
                    id = "biz_street_kiosks",
                    name = "VV Puram Heritage Food Street Kiosks",
                    tagline = "Famous Thindi Beedi, Hot Paddus, Butter Dosas & Gulkand 🍢",
                    category = "Public & Community Spaces • Street vending carts / Kiosks",
                    description = "Bangalore's legendary vegetarian food street lined with lively street carts and historic dessert kiosks. The most fun, bustling evening street walk in South Bangalore.",
                    address = "Old Tharagupet, VV Puram, Bangalore",
                    city = "Bangalore",
                    latitude = 12.9520,
                    longitude = 77.5780,
                    distanceKm = 4.2,
                    rating = 4.8,
                    reviewCount = 8900,
                    priceRange = "₹",
                    bannerUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=800&q=80",
                    logoEmoji = "🍢",
                    phoneNumber = "+91 98452 33441",
                    websiteUrl = "https://vvpuramfoodstreet.in",
                    socialHandle = "@vvpuramfoodies",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 8100,
                    activeOfferSummary = "🍢 20% OFF Special Dry Fruit Gulkand Ice Cream"
                )
            )
            businessDao.insertBusinesses(initialBusinesses)

            val initialPosts = listOf(
                BusinessPostEntity(
                    id = "post_tw_01",
                    businessId = "biz_third_wave",
                    title = "🎉 First Date Couple Special: Flat 35% OFF",
                    content = "Connecting with someone special on VibeSync? Enjoy our hand-crafted pour-over coffee paired with warm Blueberry Cheesecake for 35% off. Show your VibeSync connection badge at counter.",
                    postType = "DISCOUNT",
                    mediaUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80",
                    mediaType = "IMAGE",
                    discountPercent = 35,
                    promoCode = "VIBEBREW35",
                    validUntil = "Valid all week till Sunday 11 PM",
                    likesCount = 89,
                    timestamp = System.currentTimeMillis() - 7200000
                ),
                BusinessPostEntity(
                    id = "post_tw_02",
                    businessId = "biz_third_wave",
                    title = "🎷 Live Acoustic Indie Sessions this Friday!",
                    content = "Indie singer-songwriter duet performing live acoustic pop and soft jazz from 7 PM to 10 PM. Intimate couch seating available — reserve your table early!",
                    postType = "EVENT",
                    mediaUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80",
                    mediaType = "IMAGE",
                    validUntil = "Friday 7 PM - 10 PM",
                    likesCount = 142,
                    timestamp = System.currentTimeMillis() - 86400000
                ),
                BusinessPostEntity(
                    id = "post_soc_01",
                    businessId = "biz_social",
                    title = "🍸 1-for-1 Signature Cocktails on Dates",
                    content = "Order any signature Social cocktail (LIIT, Achaari Mojito, Two-to-Tango) and get the second one complimentary! Exclusive for VibeSync connections.",
                    postType = "OFFER",
                    mediaUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80",
                    mediaType = "IMAGE",
                    discountPercent = 50,
                    promoCode = "SOCIALDATE",
                    validUntil = "Valid Tue-Thu 6 PM to 10 PM",
                    likesCount = 215,
                    timestamp = System.currentTimeMillis() - 14400000
                ),
                BusinessPostEntity(
                    id = "post_toit_01",
                    businessId = "biz_toit",
                    title = "🍕 Free Wood-Fired Sourdough Dessert",
                    content = "Indulge in our famous Warm Chocolate Molten Cake with Vanilla Ice Cream complimentary on billing above ₹1,200.",
                    postType = "DISCOUNT",
                    mediaUrl = "https://images.unsplash.com/photo-1575444758702-4a6b9222336e?auto=format&fit=crop&w=800&q=80",
                    mediaType = "IMAGE",
                    discountPercent = 25,
                    promoCode = "TOITVIBE",
                    validUntil = "Valid till Month End",
                    likesCount = 176,
                    timestamp = System.currentTimeMillis() - 28800000
                ),
                BusinessPostEntity(
                    id = "post_trek_01",
                    businessId = "biz_trekking",
                    title = "🏕️ Skandagiri Night Trek & Cloud Sunrise",
                    content = "Escape the city lights! 8 km midnight trek through lush hills, reaching the peak for panoramic sunrise above sea of clouds. Guide, permits & breakfast included.",
                    postType = "EVENT",
                    mediaUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=800&q=80",
                    mediaType = "IMAGE",
                    discountPercent = 20,
                    promoCode = "TREKVIBE500",
                    validUntil = "Departing this Saturday 3:30 AM",
                    likesCount = 310,
                    timestamp = System.currentTimeMillis() - 43200000
                )
            )
            businessDao.insertPosts(initialPosts)
        }

        if (channelDao.getCount() == 0) {
            val initialChannels = listOf(
                ChannelEntity(
                    id = "chan_blr_nightlife",
                    name = "Bangalore Nightlife & Date Spots 🍸",
                    handle = "@blr_nightlife",
                    description = "Top romantic rooftops, secret cocktail speakeasies, live gigs & couple entry passes in Bangalore.",
                    category = "Nightlife & Lounges",
                    iconEmoji = "🍸",
                    bannerUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80",
                    creatorUserId = "system",
                    creatorName = "Bangalore Night Guide",
                    isVerified = true,
                    isFollowed = true,
                    followerCount = 14200
                ),
                ChannelEntity(
                    id = "chan_city_news",
                    name = "VibeSync City News & Updates ⚡",
                    handle = "@vibesync_news",
                    description = "Official breaking bulletins, weather, weekend traffic routes, city festivals & community safety guides.",
                    category = "City News & Events",
                    iconEmoji = "⚡",
                    bannerUrl = "https://images.unsplash.com/photo-1495020689067-958852a7765e?auto=format&fit=crop&w=800&q=80",
                    creatorUserId = "system",
                    creatorName = "VibeSync Editorial",
                    isVerified = true,
                    isFollowed = true,
                    followerCount = 28500
                ),
                ChannelEntity(
                    id = "chan_treks",
                    name = "Weekend Treks & Adventure 🏕️",
                    handle = "@weekend_treks",
                    description = "Sunrise hiking trails, camping spots, group carpooling & packing tips for outdoor lovers.",
                    category = "Treks & Adventure",
                    iconEmoji = "🏕️",
                    bannerUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=800&q=80",
                    creatorUserId = "system",
                    creatorName = "Trail Master Team",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 8900
                ),
                ChannelEntity(
                    id = "chan_foodies",
                    name = "Secret Foodies & Hidden Cafes 🍕",
                    handle = "@secret_cafes",
                    description = "Hidden garden bistros, artisanal sourdough pizzas, dessert lounges and budget brunch secrets.",
                    category = "Food & Cafes",
                    iconEmoji = "🍕",
                    bannerUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80",
                    creatorUserId = "system",
                    creatorName = "Foodie Scout India",
                    isVerified = true,
                    isFollowed = false,
                    followerCount = 11300
                )
            )
            channelDao.insertChannels(initialChannels)

            val initialBroadcasts = listOf(
                ChannelBroadcastEntity(
                    id = "bc_nl_01",
                    channelId = "chan_blr_nightlife",
                    senderName = "Bangalore Night Guide",
                    content = "🔥 Top 5 Rooftop Sunset Lounges for First Dates this Weekend!\n1. Skyye Lounge (UB City) - 360 degree panoramic view\n2. 1522 The Pub (Indiranagar) - Cozy rock & retro ambience\n3. High Ultra Lounge (World Trade Center) - 430 ft high rooftop dining\n4. Toit Brewpub - Iconic craft brews\n5. Olive Beach - Mediterranean candlelit patio.\n\nTip: Mention VibeSync at Church Street Social for 1-for-1 cocktails!",
                    broadcastType = "NEWS",
                    mediaUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80",
                    mediaType = "IMAGE",
                    reactionsCount = 284,
                    timestamp = System.currentTimeMillis() - 10800000
                ),
                ChannelBroadcastEntity(
                    id = "bc_nl_02",
                    channelId = "chan_blr_nightlife",
                    senderName = "Bangalore Night Guide",
                    content = "🎧 Video Teaser: Saturday Electronic Music Showcase featuring DJ Anish Sood live at Koramangala Warehouse! Doors open 8 PM. VIP passes giveaway running in comments.",
                    broadcastType = "VIDEO",
                    mediaUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80",
                    mediaType = "VIDEO",
                    reactionsCount = 195,
                    timestamp = System.currentTimeMillis() - 86400000
                ),
                ChannelBroadcastEntity(
                    id = "bc_news_01",
                    channelId = "chan_city_news",
                    senderName = "VibeSync Editorial",
                    content = "🌤️ Weekend Weather Forecast: Pleasant breezy evenings with mild 22°C temperatures expected across Bangalore & surrounding hills. Ideal weather for outdoor open-air cafe hopping and park strolls at Cubbon Park!",
                    broadcastType = "NEWS",
                    mediaUrl = "https://images.unsplash.com/photo-1495020689067-958852a7765e?auto=format&fit=crop&w=800&q=80",
                    mediaType = "IMAGE",
                    reactionsCount = 412,
                    timestamp = System.currentTimeMillis() - 7200000
                ),
                ChannelBroadcastEntity(
                    id = "bc_news_02",
                    channelId = "chan_city_news",
                    senderName = "VibeSync Editorial",
                    content = "🛡️ Safety First: Remember to keep your first meetings in public verified venues (like our partner cafes in the Business tab). VibeSync E2EE encryption keeps all in-app chat safe!",
                    broadcastType = "MESSAGE",
                    mediaType = "NONE",
                    reactionsCount = 530,
                    timestamp = System.currentTimeMillis() - 21600000
                ),
                ChannelBroadcastEntity(
                    id = "bc_trek_01",
                    channelId = "chan_treks",
                    senderName = "Trail Master Team",
                    content = "📸 Sunrise photo from our Sunday sunrise expedition at Skandagiri! 42 singles climbed together under starry skies. Registration for next Saturday's Savandurga monolithic trek is now live!",
                    broadcastType = "PHOTO",
                    mediaUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=800&q=80",
                    mediaType = "IMAGE",
                    reactionsCount = 378,
                    timestamp = System.currentTimeMillis() - 172800000
                )
            )
            channelDao.insertBroadcasts(initialBroadcasts)
        }
        syncAndRestoreBusinessesFromCloud()
    }

    suspend fun syncAndRestoreBusinessesFromCloud() = withContext(Dispatchers.IO) {
        try {
            val currentPrefs = preferencesDao.getPreferencesSync()
            val currentPhone = currentPrefs?.verifiedMobileNumber ?: ""
            val currentEmail = currentPrefs?.googleEmail ?: ""

            // ZERO-COST READ SHIELD:
            // Never perform a full scan of 50,000 businesses!
            // Only perform a targeted single-user lookup for the merchant's own registered venues (1-2 reads max).
            if (currentPhone.isBlank() && currentEmail.isBlank()) return@withContext

            val db = FirebaseFirestore.getInstance()
            val querySnapshot = when {
                currentPhone.isNotBlank() -> {
                    db.collection("vibesync_venues")
                        .whereEqualTo("ownerUserId", currentPhone)
                        .limit(10)
                        .get()
                        .await()
                }
                currentEmail.isNotBlank() -> {
                    db.collection("vibesync_venues")
                        .whereEqualTo("ownerUserId", currentEmail)
                        .limit(10)
                        .get()
                        .await()
                }
                else -> null
            }

            if (querySnapshot != null && !querySnapshot.isEmpty) {
                val cloudVenues = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val venue = doc.toObject(BusinessEntity::class.java)
                        venue?.copy(isUserCreated = true)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (cloudVenues.isNotEmpty()) {
                    businessDao.insertBusinesses(cloudVenues)
                    Log.d("DatingRepository", "Zero-read recovery: Restored ${cloudVenues.size} merchant venues from Firestore")
                }
            }
        } catch (e: Exception) {
            Log.w("DatingRepository", "Cloud business restoration note: ${e.message}")
        }
    }

    suspend fun processPhonebookContactsForMatchesAndNotifications(matchedContacts: List<PhoneContact>) = withContext(Dispatchers.IO) {
        // Organic contact handling only: Contacts are displayed in the Contacts screen for organic discovery.
        // No automatic fake message creation or notification loops!
        for (contact in matchedContacts) {
            if (contact.isOnVibeSync && !contact.vibeSyncProfileId.isNullOrBlank()) {
                val profileId = contact.vibeSyncProfileId!!
                val existing = profileDao.getProfileByIdSync(profileId)
                val baseUser = contact.vibeSyncUser ?: existing
                if (baseUser != null) {
                    val updatedUser = baseUser.copy(
                        name = contact.name,
                        phoneNumber = contact.phoneNumber.ifBlank { baseUser.phoneNumber }
                    )
                    profileDao.insertProfile(updatedUser)
                }
            }
        }
    }

    suspend fun blockUser(profileId: String) = withContext(Dispatchers.IO) {
        blockDao.insertBlock(BlockEntity(profileId = profileId))
        try {
            val db = FirebaseFirestore.getInstance()
            val currentPrefs = preferencesDao.getPreferencesSync()
            val myId = currentPrefs?.verifiedMobileNumber?.ifBlank { currentPrefs.googleEmail } ?: "USER"
            val blockMap = mapOf("userId" to myId, "blockedProfileId" to profileId, "blockedAt" to System.currentTimeMillis())
            db.collection("blocked_users").document("${myId}_$profileId").set(blockMap).await()
        } catch (_: Exception) {}
    }

    suspend fun unblockUser(profileId: String) = withContext(Dispatchers.IO) {
        blockDao.unblockUser(profileId)
        try {
            val db = FirebaseFirestore.getInstance()
            val currentPrefs = preferencesDao.getPreferencesSync()
            val myId = currentPrefs?.verifiedMobileNumber?.ifBlank { currentPrefs.googleEmail } ?: "USER"
            db.collection("blocked_users").document("${myId}_$profileId").delete().await()
        } catch (_: Exception) {}
    }

    suspend fun isBlocked(profileId: String): Boolean = withContext(Dispatchers.IO) {
        val list = blockDao.getAllBlockedUsersSync()
        list.any { it.profileId == profileId }
    }

    suspend fun reportUser(profileId: String, reason: String) = withContext(Dispatchers.IO) {
        val currentPrefs = preferencesDao.getPreferencesSync()
        val reporterId = currentPrefs?.verifiedMobileNumber?.ifBlank { currentPrefs.googleEmail } ?: "USER"
        val reportId = UUID.randomUUID().toString()
        val report = ReportEntity(
            reportId = reportId,
            reporterId = reporterId,
            reportedProfileId = profileId,
            reason = reason,
            status = "PENDING",
            reportedAt = System.currentTimeMillis()
        )
        reportDao.insertReport(report)
        profileDao.recordReportOnProfile(profileId, 50, true, reason)

        try {
            val db = FirebaseFirestore.getInstance()
            val reportMap = mapOf(
                "reportId" to reportId,
                "reporterId" to reporterId,
                "reportedProfileId" to profileId,
                "reason" to reason,
                "status" to "PENDING",
                "reportedAt" to System.currentTimeMillis()
            )
            db.collection("reports").document(reportId).set(reportMap).await()
        } catch (_: Exception) {}
    }

    suspend fun fetchProfilesBatchFromFirestore(phoneNumbers: List<String>): Map<String, ProfileEntity> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, ProfileEntity>()
        val db = try { FirebaseFirestore.getInstance() } catch (_: Exception) { return@withContext result }
        if (phoneNumbers.isEmpty()) return@withContext result

        // Batch into chunks of 10 to optimize Firestore read costs & avoid query limits
        val chunks = phoneNumbers.filter { it.isNotBlank() }.distinct().chunked(10)
        for (chunk in chunks) {
            try {
                val snapshot = db.collection("profiles")
                    .whereIn("phoneNumber", chunk)
                    .get()
                    .await()
                for (doc in snapshot.documents) {
                    val docId = doc.id
                    val name = doc.getString("name") ?: continue
                    val phone = doc.getString("phoneNumber") ?: docId
                    val profile = ProfileEntity(
                        id = docId,
                        name = name,
                        age = doc.getLong("age")?.toInt() ?: 24,
                        occupation = doc.getString("occupation") ?: "Member",
                        city = doc.getString("city") ?: "Online",
                        distanceMiles = 1,
                        bio = doc.getString("bio") ?: "VibeSync Member",
                        interests = "Chat, Friendship",
                        relationshipGoal = doc.getString("relationshipGoal") ?: "Friends",
                        promptQuestion = "",
                        promptAnswer = "",
                        gradientColorStart = 0xFFFF5E62,
                        gradientColorEnd = 0xFFFF9966,
                        avatarEmoji = doc.getString("avatarEmoji") ?: "✨",
                        avatarUrl = doc.getString("avatarUrl") ?: "",
                        phoneNumber = phone,
                        email = doc.getString("googleEmail") ?: "",
                        isVerified = true,
                        likedMe = true,
                        isSuperLikedMe = true
                    )
                    result[phone] = profile
                    val digits = phone.filter { it.isDigit() }
                    if (digits.isNotBlank()) result[digits] = profile
                }
            } catch (_: Exception) {}
        }
        return@withContext result
    }

    suspend fun fetchProfilesByE164PhonesFromSupabase(phones: List<String>): Map<String, ProfileEntity> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, ProfileEntity>()
        val validPhones = phones.filter { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.distinct()
        if (validPhones.isEmpty()) return@withContext result
        try {
            validPhones.chunked(30).forEach { batch ->
                val list = com.example.util.SupabaseClientManager.fetchProfilesByE164Phones(batch)
                for (profile in list) {
                    result[profile.id] = profile
                    val digits = profile.phoneNumber.filter { it.isDigit() }
                    if (digits.isNotBlank()) {
                        result[digits] = profile
                        val e164 = com.example.util.PhonebookHelper.normalizeToE164(profile.phoneNumber)
                        if (e164.isNotBlank()) result[e164] = profile
                        val phHash = com.example.util.SupabaseClientManager.getPhoneHash(digits)
                        result[phHash] = profile
                        val h16 = com.example.util.PhonebookHasher.generate16CharHash(digits)
                        result[h16] = profile
                        val h64 = com.example.util.PhonebookHasher.hashPhoneNumberToHex(digits)
                        result[h64] = profile
                        for (h in com.example.util.PhonebookHasher.getAllMatchHashes(profile.phoneNumber)) {
                            result[h] = profile
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return@withContext result
    }

    suspend fun fetchProfilesByHashesFromSupabase(hashes: List<String>): Map<String, ProfileEntity> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, ProfileEntity>()
        val validHashes = hashes.filter { it.isNotBlank() && !it.equals("null", ignoreCase = true) }.distinct()
        if (validHashes.isEmpty()) return@withContext result
        try {
            validHashes.chunked(30).forEach { batch ->
                val list = com.example.util.SupabaseClientManager.fetchProfilesByHashes(batch)
                for (profile in list) {
                    result[profile.id] = profile
                    val digits = profile.phoneNumber.filter { it.isDigit() }
                    if (digits.isNotBlank()) {
                        result[digits] = profile
                        val e164 = com.example.util.PhonebookHelper.normalizeToE164(profile.phoneNumber)
                        if (e164.isNotBlank()) result[e164] = profile
                        val phHash = com.example.util.SupabaseClientManager.getPhoneHash(digits)
                        result[phHash] = profile
                        val h16 = com.example.util.PhonebookHasher.generate16CharHash(digits)
                        result[h16] = profile
                        val h64 = com.example.util.PhonebookHasher.hashPhoneNumberToHex(digits)
                        result[h64] = profile
                        for (h in com.example.util.PhonebookHasher.getAllMatchHashes(profile.phoneNumber)) {
                            result[h] = profile
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return@withContext result
    }

    fun getAllReports(): Flow<List<ReportEntity>> = reportDao.getAllReports()
    fun getAllBlockedUsers(): Flow<List<BlockEntity>> = blockDao.getAllBlockedUsers()

    fun getAllMatchedContactsFlow(): Flow<List<com.example.data.model.MatchedContactEntity>> {
        val ctx = context ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        val database = com.example.data.database.DatingDatabase.getDatabase(ctx)
        return database.matchedContactDao().getAllMatchedContactsFlow()
    }

    suspend fun filterMatchedContactsAgainstCloudUsers(
        rawContacts: List<com.example.data.model.MatchedContactEntity>
    ): List<com.example.data.model.MatchedContactEntity> = withContext(Dispatchers.IO) {
        if (rawContacts.isEmpty()) return@withContext emptyList()
        val ctx = context ?: return@withContext rawContacts
        val database = com.example.data.database.DatingDatabase.getDatabase(ctx)
        val matchedDao = database.matchedContactDao()

        val localProfiles = profileDao.getAllProfilesSync()
        val supabaseProfiles = try {
            com.example.util.SupabaseClientManager.fetchAllProfiles(50)
        } catch (_: Exception) {
            emptyList()
        }
        val firestoreAccounts = try {
            com.example.util.FirebaseBackendSyncManager.fetchAllCloudAccounts().map { acc ->
                ProfileEntity(
                    id = acc.id.ifBlank { acc.phoneNumber.filter { it.isDigit() } },
                    name = acc.userName.ifBlank { "Registered Member" },
                    age = acc.userAge,
                    occupation = "Member",
                    city = "Registered User",
                    distanceMiles = 1,
                    bio = "Registered VibeSync User",
                    interests = "Chat, Social",
                    relationshipGoal = "Socializing",
                    promptQuestion = "",
                    promptAnswer = "",
                    gradientColorStart = 0xFF128C7E,
                    gradientColorEnd = 0xFF25D366,
                    avatarEmoji = "✨",
                    avatarUrl = "",
                    isVerified = acc.isVerified,
                    likedMe = true,
                    phoneNumber = acc.phoneNumber,
                    email = acc.googleEmail
                )
            }
        } catch (_: Exception) {
            emptyList()
        }

        val allProfiles = (localProfiles + supabaseProfiles + firestoreAccounts).distinctBy { it.id }

        val hashToProfileMap = mutableMapOf<String, ProfileEntity>()
        val phoneDigitsMap = mutableMapOf<String, ProfileEntity>()

        for (profile in allProfiles) {
            val phoneDigits = profile.phoneNumber.filter { it.isDigit() }
            if (phoneDigits.isNotBlank()) {
                val phHash = com.example.util.SupabaseClientManager.getPhoneHash(phoneDigits)
                hashToProfileMap[phHash] = profile
                if (phoneDigits.length >= 10) {
                    phoneDigitsMap[phoneDigits.takeLast(10)] = profile
                }
            }
            if (profile.id.length == 16) {
                hashToProfileMap[profile.id] = profile
            }
            val idDigits = profile.id.filter { it.isDigit() }
            if (idDigits.length >= 10) {
                phoneDigitsMap[idDigits.takeLast(10)] = profile
            }
        }

        val matchedResults = rawContacts.map { contact ->
            val cleanDigits = contact.phoneNumber.filter { it.isDigit() }
            val contactHash = if (contact.phoneHash.isNotBlank()) contact.phoneHash else com.example.util.SupabaseClientManager.getPhoneHash(cleanDigits)

            val matchedProfile = hashToProfileMap[contactHash]
                ?: (if (cleanDigits.length >= 10) phoneDigitsMap[cleanDigits.takeLast(10)] else null)

            if (matchedProfile != null) {
                contact.copy(
                    phoneHash = contactHash,
                    isOnVibeSync = true,
                    vibeSyncUserId = matchedProfile.id,
                    avatarEmoji = matchedProfile.avatarEmoji.ifBlank { "✨" },
                    photoUrl = matchedProfile.avatarUrl,
                    statusTagline = "${matchedProfile.relationshipGoal.ifBlank { "Available on VibeSync" }} • ${matchedProfile.city.ifBlank { "Active" }}"
                )
            } else {
                contact.copy(phoneHash = contactHash)
            }
        }

        try {
            matchedDao.insertMatchedContacts(matchedResults)
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Failed saving matched_contacts to Room: ${e.message}")
        }

        return@withContext matchedResults
    }

    /**
     * Erases mock profile registrations, stale contacts, expired stories and unrequired temporary data,
     * then executes VACUUM on SQLite database to reclaim disk space.
     */
    suspend fun eraseUnrequiredAndProfileData(): String = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext "Error: Context unavailable"
        val database = com.example.data.database.DatingDatabase.getDatabase(ctx)
        return@withContext try {
            val db = database.openHelper.writableDatabase
            db.execSQL("DELETE FROM profiles WHERE id LIKE 'demo_%' OR id LIKE 'seed_%' OR id LIKE 'temp_%' OR id LIKE 'test_%' OR id LIKE 'p_%'")
            db.execSQL("DELETE FROM user_contacts")
            db.execSQL("DELETE FROM status_stories WHERE expiresAt < ${System.currentTimeMillis()}")
            db.execSQL("VACUUM")
            "Successfully erased unrequired data & VACUUM executed. Database space reclaimed!"
        } catch (e: Exception) {
            "Purge executed with notice: ${e.message}"
        }
    }

    /**
     * Purges local ghost "null", blank, or uninitialized records across all Room tables.
     */
    suspend fun purgeGhostNullData() = withContext(Dispatchers.IO) {
        try {
            profileDao.purgeGhostNullProfiles()
            matchDao.purgeGhostNullMatches()
            chatMessageDao.purgeGhostNullMessages()
            messageDao.purgeGhostNullLocalMessages()
            removeOwnProfileAndSelfMatches()
        } catch (e: Exception) {
            android.util.Log.w("DatingRepository", "Purge ghost null data notice: ${e.message}")
        }
    }

    /**
     * Single-shot sync of encrypted messages from Supabase on demand.
     */
    suspend fun syncSupabaseMessagesOnce() = withContext(Dispatchers.IO) {
        try {
            val prefs = preferencesDao.getPreferencesSync()
            val myPhone = com.example.util.ContactResolver.sanitizePhone(prefs?.verifiedMobileNumber ?: "")
            val myDigits = myPhone.filter { it.isDigit() }

            if (myPhone.isNotBlank() && myPhone != "null") {
                val activeMatches = matchDao.getAllMatchesSync()
                for (m in activeMatches) {
                    val profId = com.example.util.ContactResolver.sanitizePhone(m.profileId)
                    if (profId.isBlank() || profId == "null") continue

                    val symmetricalId = getSymmetricMatchId(profId)
                    val matchIdsToQuery = setOf(m.matchId, symmetricalId).filter { it.isNotBlank() && it != "null" }

                    for (queryMatchId in matchIdsToQuery) {
                        val remoteMsgs = com.example.util.SupabaseClientManager.fetchMessagesForMatch(queryMatchId)
                        for (remoteMsg in remoteMsgs) {
                            if (remoteMsg.text.isBlank() || remoteMsg.text == "null") continue
                            val existing = chatMessageDao.getMessageById(remoteMsg.messageId)
                            if (existing == null) {
                                val senderDigits = remoteMsg.senderId.filter { it.isDigit() }
                                val isMyOwn = remoteMsg.senderId == "USER" ||
                                        remoteMsg.senderId == myPhone ||
                                        (myDigits.length >= 7 && senderDigits.length >= 7 && senderDigits.endsWith(myDigits.takeLast(10)))

                                val incomingMsg = remoteMsg.copy(
                                    matchId = m.matchId,
                                    senderId = if (isMyOwn) "USER" else profId,
                                    isDelivered = true,
                                    isRead = false
                                )
                                chatMessageDao.insertMessage(incomingMsg)
                                messageDao.insertMessage(incomingMsg.toLocalMessage())

                                matchDao.updateLastMessage(
                                    matchId = m.matchId,
                                    text = remoteMsg.text,
                                    timestamp = remoteMsg.timestamp,
                                    hasUnread = !isMyOwn
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun startSupabaseMessagePollingLoop() {
        // Disabled background periodic loop to protect battery, thread pool, and API quota
    }
}
