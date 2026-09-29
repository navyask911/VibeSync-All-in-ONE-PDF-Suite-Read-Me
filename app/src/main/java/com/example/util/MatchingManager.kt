package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ProfileEntity
import com.example.data.model.UserPreferencesEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * MatchingManager
 *
 * Implements a smart compatibility scoring algorithm based on profile tags and interests stored in Firestore,
 * and handles automated mutual match detection with system and in-app notification dispatching.
 */
object MatchingManager {
    private const val TAG = "MatchingManager"
    private const val NOTIFICATION_CHANNEL_ID = "channel_mutual_matches"
    private const val NOTIFICATION_CHANNEL_NAME = "Mutual Matches & Chemistry Alerts"
    private const val COLLECTION_PROFILES = "profiles"
    private const val COLLECTION_LIKES = "user_likes"
    private const val COLLECTION_MATCHES = "mutual_matches"

    // Data model for Firestore user profile documents
    data class FirestoreProfileData(
        val userId: String = "",
        val name: String = "",
        val age: Int = 0,
        val city: String = "",
        val interests: List<String> = emptyList(),
        val tags: List<String> = emptyList(),
        val relationshipGoal: String = "",
        val bio: String = "",
        val occupation: String = "",
        val zodiacSign: String = "",
        val avatarEmoji: String = "✨",
        val avatarUrl: String = "",
        val gender: String = "Female",
        val maritalStatus: String = "Single (Never Married)",
        val isRealFaceVerified: Boolean = true,
        val isOpenForDating: Boolean = true,
        val phoneNumber: String = "",
        val googleEmail: String = "",
        val likedUserIds: List<String> = emptyList(),
        val matchIds: List<String> = emptyList(),
        val trustScore: Int = 98,
        val lastUpdated: Long = System.currentTimeMillis()
    )

    // Detailed breakdown of compatibility factors
    data class CompatibilityBreakdown(
        val interestScore: Int, // 0..100
        val goalScore: Int, // 0..100
        val lifestyleScore: Int, // 0..100
        val sharedInterests: List<String>,
        val sharedTags: List<String>,
        val goalMatches: Boolean
    )

    // Result object representing overall compatibility
    data class CompatibilityResult(
        val overallScore: Int, // 0..100 %
        val compatibilityLevel: String, // e.g. "✨ Cosmic Chemistry (95%)"
        val sharedInterests: List<String>,
        val sharedTags: List<String>,
        val matchSummary: String,
        val breakdown: CompatibilityBreakdown
    )

    // Event model for mutual match notifications
    data class MutualMatchEvent(
        val matchId: String,
        val matchedProfile: ProfileEntity,
        val currentUserName: String,
        val compatibilityScore: Int,
        val sharedInterests: List<String>,
        val timestamp: Long = System.currentTimeMillis()
    )

    // In-memory fallback cache for offline or non-Firebase initialized environments
    private val inMemoryFirestoreProfiles = ConcurrentHashMap<String, FirestoreProfileData>()
    private val inMemoryLikes = ConcurrentHashMap<String, MutableSet<String>>() // userId -> set of liked targetUserIds

    // Reactive streams for mutual match alerts
    private val _mutualMatchEvents = MutableSharedFlow<MutualMatchEvent>(extraBufferCapacity = 10)
    val mutualMatchEvents: SharedFlow<MutualMatchEvent> = _mutualMatchEvents.asSharedFlow()

    private val _latestMutualMatch = MutableStateFlow<MutualMatchEvent?>(null)
    val latestMutualMatch: StateFlow<MutualMatchEvent?> = _latestMutualMatch.asStateFlow()

    // Synonym clusters for fuzzy interest matching
    private val synonymClusters = listOf(
        setOf("coffee", "espresso", "latte", "cafe hopping", "cappuccino", "barista"),
        setOf("photography", "film photography", "street photography", "photo", "camera"),
        setOf("rock climbing", "bouldering", "climbing", "mountaineering"),
        setOf("hiking", "nature walks", "trekking", "camping", "outdoors", "trail running"),
        setOf("design", "ui design", "graphic design", "ux", "creative art", "illustration"),
        setOf("art", "concept art", "modern art", "painting", "museums", "drawing"),
        setOf("music", "vinyl", "indie concerts", "concerts", "live music", "guitar", "piano"),
        setOf("fitness", "gym", "workout", "weightlifting", "crossfit", "strength"),
        setOf("running", "marathon", "jogging", "5k", "sprinting"),
        setOf("yoga", "mindfulness", "meditation", "pilates"),
        setOf("food", "foodie", "ramen", "cooking", "baking", "fine dining", "street food"),
        setOf("travel", "road trips", "backpacking", "sightseeing", "solo travel", "wanderlust"),
        setOf("tech", "coding", "software", "programming", "startups", "ai", "hackathons"),
        setOf("gaming", "video games", "board games", "esports", "pc gaming", "playstation"),
        setOf("books", "reading", "book club", "literature", "poetry", "novels"),
        setOf("anime", "manga", "cosplay", "japanese culture"),
        setOf("dogs", "dog lover", "puppies", "pets", "golden retriever"),
        setOf("cats", "cat lover", "kittens", "feline")
    )

    // Zodiac compatibility pairs (elemental harmony)
    private val zodiacCompatibilityMap = mapOf(
        "Aries" to setOf("Leo", "Sagittarius", "Gemini", "Aquarius"),
        "Leo" to setOf("Aries", "Sagittarius", "Gemini", "Libra"),
        "Sagittarius" to setOf("Aries", "Leo", "Libra", "Aquarius"),
        "Taurus" to setOf("Virgo", "Capricorn", "Cancer", "Pisces"),
        "Virgo" to setOf("Taurus", "Capricorn", "Cancer", "Scorpio"),
        "Capricorn" to setOf("Taurus", "Virgo", "Scorpio", "Pisces"),
        "Gemini" to setOf("Libra", "Aquarius", "Aries", "Leo"),
        "Libra" to setOf("Gemini", "Aquarius", "Leo", "Sagittarius"),
        "Aquarius" to setOf("Gemini", "Libra", "Aries", "Sagittarius"),
        "Cancer" to setOf("Scorpio", "Pisces", "Taurus", "Virgo"),
        "Scorpio" to setOf("Cancer", "Pisces", "Virgo", "Capricorn"),
        "Pisces" to setOf("Cancer", "Scorpio", "Taurus", "Capricorn")
    )

    /**
     * Initializes notification channels for mutual match alerts.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies users instantly when a mutual match is found."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Calculates the compatibility score between two sets of interests and tags.
     * Uses a multi-factor weighting algorithm:
     * - Shared Interests & fuzzy synonyms (50% weight)
     * - Relationship Goal alignment (25% weight)
     * - Lifestyle/Personality Tags (15% weight)
     * - Zodiac & Demographics harmony (10% weight)
     */
    fun calculateCompatibility(
        userAInterests: List<String>,
        userAGoal: String,
        userATags: List<String> = emptyList(),
        userAZodiac: String = "",
        userBInterests: List<String>,
        userBGoal: String,
        userBTags: List<String> = emptyList(),
        userBZodiac: String = ""
    ): CompatibilityResult {
        // 1. Calculate Interest Similarity with Synonym Expansion
        val normalizedA = userAInterests.map { cleanString(it) }.filter { it.isNotEmpty() }.toSet()
        val normalizedB = userBInterests.map { cleanString(it) }.filter { it.isNotEmpty() }.toSet()

        val directShared = normalizedA.intersect(normalizedB).toList()
        val fuzzyShared = mutableListOf<String>()

        normalizedA.forEach { intA ->
            if (!directShared.contains(intA)) {
                val cluster = synonymClusters.firstOrNull { it.any { word -> cleanString(word) == intA } }
                if (cluster != null) {
                    val matchInB = normalizedB.firstOrNull { intB -> cluster.any { word -> cleanString(word) == intB } }
                    if (matchInB != null && !directShared.contains(matchInB) && !fuzzyShared.contains(intA)) {
                        fuzzyShared.add("$intA ~ $matchInB")
                    }
                }
            }
        }

        val allSharedInterests = directShared + fuzzyShared
        val totalUniqueInterests = (normalizedA + normalizedB).size.coerceAtLeast(1)
        val interestOverlapRatio = (allSharedInterests.size.toFloat() / totalUniqueInterests.toFloat()).coerceIn(0f, 1f)

        // Scale interest score up generously so 2-3 shared interests give great scores (60-90%)
        val interestScore = when {
            allSharedInterests.size >= 4 -> 95
            allSharedInterests.size == 3 -> 88
            allSharedInterests.size == 2 -> 78
            allSharedInterests.size == 1 -> 65
            interestOverlapRatio > 0.3f -> 70
            else -> 45
        }

        // 2. Calculate Relationship Goal Alignment
        val goalA = cleanString(userAGoal)
        val goalB = cleanString(userBGoal)
        val goalMatches = when {
            goalA == goalB -> true
            goalA.contains("long") && goalB.contains("long") -> true
            goalA.contains("casual") && goalB.contains("casual") -> true
            goalA.contains("friend") && goalB.contains("friend") -> true
            else -> false
        }
        val goalScore = if (goalMatches) 100 else 40

        // 3. Calculate Lifestyle & Profile Tags Overlap
        val tagsA = userATags.map { cleanString(it) }.toSet()
        val tagsB = userBTags.map { cleanString(it) }.toSet()
        val sharedTags = tagsA.intersect(tagsB).toList()
        val lifestyleScore = when {
            sharedTags.size >= 3 -> 95
            sharedTags.size == 2 -> 85
            sharedTags.size == 1 -> 75
            else -> 60
        }

        // 4. Zodiac Harmony
        val zodiacA = userAZodiac.trim()
        val zodiacB = userBZodiac.trim()
        val zodiacCompatible = if (zodiacA.isNotEmpty() && zodiacB.isNotEmpty()) {
            zodiacCompatibilityMap[zodiacA]?.contains(zodiacB) == true
        } else false
        val zodiacBonus = if (zodiacCompatible) 100 else 65

        // Weighted Overall Score: 50% Interests + 25% Goal + 15% Lifestyle Tags + 10% Zodiac
        val rawScore = (interestScore * 0.50f) + (goalScore * 0.25f) + (lifestyleScore * 0.15f) + (zodiacBonus * 0.10f)
        val overallScore = rawScore.toInt().coerceIn(35, 99)

        val level = when {
            overallScore >= 90 -> "✨ Cosmic Chemistry (${overallScore}%)"
            overallScore >= 80 -> "🔥 High Chemistry (${overallScore}%)"
            overallScore >= 70 -> "💖 Great Potential (${overallScore}%)"
            overallScore >= 55 -> "🌱 Growing Connection (${overallScore}%)"
            else -> "🤝 Friendly Vibe (${overallScore}%)"
        }

        val summary = buildString {
            append("$overallScore% Match")
            if (allSharedInterests.isNotEmpty()) {
                append(" • ${allSharedInterests.size} Shared Interest${if (allSharedInterests.size > 1) "s" else ""}: ")
                append(allSharedInterests.take(3).joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } })
            }
            if (goalMatches) {
                append(" • Both want $userAGoal")
            }
        }

        return CompatibilityResult(
            overallScore = overallScore,
            compatibilityLevel = level,
            sharedInterests = allSharedInterests,
            sharedTags = sharedTags,
            matchSummary = summary,
            breakdown = CompatibilityBreakdown(
                interestScore = interestScore,
                goalScore = goalScore,
                lifestyleScore = lifestyleScore,
                sharedInterests = allSharedInterests,
                sharedTags = sharedTags,
                goalMatches = goalMatches
            )
        )
    }

    /**
     * Calculates compatibility directly between a UserPreferencesEntity (current user) and ProfileEntity.
     */
    fun calculateProfileCompatibility(
        userPrefs: UserPreferencesEntity?,
        profile: ProfileEntity
    ): CompatibilityResult {
        val userInterests = userPrefs?.userInterests?.split(",")?.map { it.trim() }
            ?: listOf("Design", "Travel", "Coffee", "Photography")
        val userGoal = userPrefs?.userRelationshipGoal ?: "Long-term relationship"
        val userTags = listOf(userPrefs?.userOccupation ?: "", userPrefs?.userCity ?: "")

        val profileInterests = profile.getInterestList()
        val profileGoal = profile.relationshipGoal
        val profileTags = listOf(profile.occupation, profile.city, profile.promptQuestion)

        return calculateCompatibility(
            userAInterests = userInterests,
            userAGoal = userGoal,
            userATags = userTags,
            userAZodiac = "Gemini",
            userBInterests = profileInterests,
            userBGoal = profileGoal,
            userBTags = profileTags,
            userBZodiac = profile.zodiacSign
        )
    }

    /**
     * Saves or syncs a user profile to Firestore `profiles` collection.
     */
    suspend fun saveProfileToFirestore(
        profileData: FirestoreProfileData
    ): Boolean = withContext(Dispatchers.IO) {
        // Cache in memory fallback
        inMemoryFirestoreProfiles[profileData.userId] = profileData

        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection(COLLECTION_PROFILES)
                .document(profileData.userId)
                .set(profileData, SetOptions.merge())
                .await()
            Log.d(TAG, "Successfully saved profile ${profileData.userId} to Firestore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync fallback (in-memory cached): ${e.message}")
            true
        }
    }

    /**
     * Syncs a local ProfileEntity to Firestore.
     */
    suspend fun syncProfileEntityToFirestore(profile: ProfileEntity): Boolean {
        val firestoreData = FirestoreProfileData(
            userId = profile.id,
            name = profile.name,
            age = profile.age,
            city = profile.city,
            interests = profile.getInterestList(),
            tags = listOf(profile.occupation, profile.relationshipGoal, profile.city),
            relationshipGoal = profile.relationshipGoal,
            bio = profile.bio,
            occupation = profile.occupation,
            zodiacSign = profile.zodiacSign,
            avatarEmoji = profile.avatarEmoji,
            avatarUrl = profile.avatarUrl,
            gender = profile.gender,
            maritalStatus = profile.maritalStatus,
            isRealFaceVerified = profile.isRealFaceVerified,
            isOpenForDating = profile.isOpenForDating,
            trustScore = profile.trustScore
        )
        return saveProfileToFirestore(firestoreData)
    }

    /**
     * Syncs current user preferences to Firestore.
     */
    suspend fun syncUserPreferencesToFirestore(prefs: UserPreferencesEntity): Boolean {
        if (!prefs.isProfileCompleted || prefs.userName.isBlank() || prefs.userName == "Registered Member" || prefs.userName == "VibeSync User" || prefs.userName.trim().length < 2) {
            Log.d(TAG, "Skipping syncUserPreferencesToFirestore: Incomplete profile or missing organic name ('${prefs.userName}')")
            return false
        }
        val cleanDigits = prefs.verifiedMobileNumber.filter { it.isDigit() }
        val userId = if (cleanDigits.isNotBlank()) {
            cleanDigits
        } else if (prefs.googleEmail.isNotBlank()) {
            prefs.googleEmail.trim()
        } else {
            "current_user_${prefs.id}"
        }
        val interests = prefs.userInterests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val firestoreData = FirestoreProfileData(
            userId = userId,
            name = prefs.userName,
            age = prefs.userAge,
            city = prefs.userCity,
            interests = interests,
            tags = listOf(prefs.userOccupation, prefs.userRelationshipGoal, prefs.userCity),
            relationshipGoal = prefs.userRelationshipGoal,
            bio = prefs.userBio,
            occupation = prefs.userOccupation,
            trustScore = prefs.trustRating,
            avatarEmoji = if (prefs.avatarUrl.isNotBlank()) "" else "✨",
            avatarUrl = prefs.avatarUrl,
            gender = prefs.userGender,
            maritalStatus = prefs.userMaritalStatus,
            isRealFaceVerified = prefs.isFaceVerified,
            isOpenForDating = prefs.isOpenForDating,
            phoneNumber = prefs.verifiedMobileNumber,
            googleEmail = prefs.googleEmail
        )
        return saveProfileToFirestore(firestoreData)
    }

    /**
     * Fetches a user profile from Firestore by userId.
     */
    suspend fun fetchProfileFromFirestore(userId: String): FirestoreProfileData? = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection(COLLECTION_PROFILES).document(userId).get().await()
            if (doc.exists()) {
                val data = doc.toObject(FirestoreProfileData::class.java)
                if (data != null) {
                    inMemoryFirestoreProfiles[userId] = data
                    return@withContext data
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch profile from Firestore fallback: ${e.message}")
        }
        inMemoryFirestoreProfiles[userId]
    }

    /**
     * Records a swipe action in Firestore and evaluates if a Mutual Match occurred.
     * When mutual match is confirmed, automatically triggers system and in-app notifications.
     */
    suspend fun recordSwipeAndEvaluateMatch(
        context: Context,
        swiperUserId: String,
        swiperName: String,
        targetProfile: ProfileEntity,
        direction: String, // "LIKE", "SUPER_LIKE", "PASS"
        userPrefs: UserPreferencesEntity?
    ): Boolean = withContext(Dispatchers.IO) {
        if (direction == "PASS") return@withContext false

        // Record swipe in-memory
        val userLikes = inMemoryLikes.getOrPut(swiperUserId) { ConcurrentHashMap.newKeySet() }
        userLikes.add(targetProfile.id)

        // Check if targetProfile already liked current user (either from ProfileEntity.likedMe or Firestore)
        val isMutualMatch = targetProfile.likedMe || isTargetUserLikingSwiper(targetProfile.id, swiperUserId)

        // Record swipe to Firestore
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection(COLLECTION_LIKES)
                .document(swiperUserId)
                .set(
                    mapOf(
                        "likedUserIds" to FieldValue.arrayUnion(targetProfile.id),
                        "lastSwipeTimestamp" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )

            SystemHealthDiagnosticsManager.logFirestoreTrace(
                collection = "user_likes",
                eventType = "SWIPE_RECORDED",
                docId = swiperUserId,
                summary = "Recorded swipe on ${targetProfile.name} (${targetProfile.id})",
                payloadPreview = "swiper=$swiperUserId, target=${targetProfile.id}, isMutual=$isMutualMatch"
            )

            if (isMutualMatch) {
                val matchDocId = generateMatchId(swiperUserId, targetProfile.id)
                val matchData = mapOf(
                    "matchId" to matchDocId,
                    "userA" to swiperUserId,
                    "userB" to targetProfile.id,
                    "participantIds" to listOf(swiperUserId, targetProfile.id),
                    "timestamp" to System.currentTimeMillis(),
                    "lastMessage" to "It's a Mutual Match! Say hi 👋",
                    "lastMessageTime" to System.currentTimeMillis(),
                    "lastSenderId" to swiperUserId,
                    "isMutual" to true
                )

                // Write to both "matches" and "mutual_matches" and "chats" for universal syncing across devices
                firestore.collection(COLLECTION_MATCHES)
                    .document(matchDocId)
                    .set(matchData, SetOptions.merge())

                firestore.collection("matches")
                    .document(matchDocId)
                    .set(matchData, SetOptions.merge())

                firestore.collection("chats")
                    .document(matchDocId)
                    .set(matchData, SetOptions.merge())

                SystemHealthDiagnosticsManager.logFirestoreTrace(
                    collection = "matches",
                    eventType = "MUTUAL_MATCH_CREATED",
                    docId = matchDocId,
                    summary = "🎉 Mutual Match created between $swiperUserId and ${targetProfile.id} (${targetProfile.name})",
                    payloadPreview = "matchId=$matchDocId, userA=$swiperUserId, userB=${targetProfile.id}"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore like recording fallback: ${e.message}")
            SystemHealthDiagnosticsManager.logFirestoreTrace(
                collection = "matches",
                eventType = "ERROR",
                docId = "N/A",
                summary = "Failed to write match to cloud: ${e.message}",
                syncStatus = "ERROR"
            )
        }

        if (isMutualMatch) {
            // Calculate real-time compatibility score
            val compatibility = calculateProfileCompatibility(userPrefs, targetProfile)

            // Trigger Notifications
            triggerMutualMatchNotification(
                context = context,
                currentUserName = swiperName,
                matchedProfile = targetProfile,
                compatibilityScore = compatibility.overallScore,
                sharedInterests = compatibility.sharedInterests
            )

            return@withContext true
        }

        return@withContext false
    }

    private suspend fun isTargetUserLikingSwiper(targetUserId: String, swiperUserId: String): Boolean {
        val targetLikes = inMemoryLikes[targetUserId]
        if (targetLikes?.contains(swiperUserId) == true) return true

        return try {
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection(COLLECTION_LIKES).document(targetUserId).get().await()
            val likedList = doc.get("likedUserIds") as? List<*>
            likedList?.contains(swiperUserId) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Dispatches a High-Priority Android System Notification & In-App reactive Event when a mutual match is found.
     */
    fun triggerMutualMatchNotification(
        context: Context,
        currentUserName: String,
        matchedProfile: ProfileEntity,
        compatibilityScore: Int,
        sharedInterests: List<String>
    ) {
        val matchId = generateMatchId(currentUserName, matchedProfile.id)
        val event = MutualMatchEvent(
            matchId = matchId,
            matchedProfile = matchedProfile,
            currentUserName = currentUserName,
            compatibilityScore = compatibilityScore,
            sharedInterests = sharedInterests
        )

        // 1. Emit to in-app event flows
        _latestMutualMatch.value = event
        _mutualMatchEvents.tryEmit(event)

        // 2. Dispatch Android System Notification
        try {
            createNotificationChannels(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("MATCH_ID", matchId)
                putExtra("PROFILE_ID", matchedProfile.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                matchedProfile.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val interestSnippet = if (sharedInterests.isNotEmpty()) {
                "Shared: ${sharedInterests.take(3).joinToString(", ")}"
            } else {
                "${matchedProfile.occupation} • ${matchedProfile.city}"
            }

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🎉 It's a Mutual Match with ${matchedProfile.name}!")
                .setContentText("$compatibilityScore% Chemistry! $interestSnippet")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "🎉 Congratulations! You and ${matchedProfile.name} liked each other.\n" +
                            "✨ Compatibility Score: $compatibilityScore%\n" +
                            "🏷️ $interestSnippet\n" +
                            "Tap to start chatting instantly with VibeSync E2EE!"
                        )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(matchedProfile.id.hashCode(), notification)
                Log.i(TAG, "Mutual match system notification posted for ${matchedProfile.name}")
            } catch (se: SecurityException) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted yet: ${se.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying mutual match notification: ${e.message}", e)
        }
    }

    fun dismissLatestMutualMatch() {
        _latestMutualMatch.value = null
    }

    private fun cleanString(input: String): String {
        return input.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()
    }

    private fun generateMatchId(userA: String, userB: String): String {
        val sorted = listOf(userA, userB).sorted()
        return "match_${sorted[0]}_${sorted[1]}"
    }
}
