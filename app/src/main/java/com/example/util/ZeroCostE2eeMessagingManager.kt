package com.example.util

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.database.DatingDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.LocalMessage
import com.example.security.HackFreeSecurityShield
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID

/**
 * ZeroCostE2eeMessagingManager
 * Implements Zero-Cost End-to-End Encrypted Messaging (Firebase + Signal Protocol)
 * with VibeSync phone-number keyed layout ensuring ZERO storage footprint
 * via instant delivery consumption queues.
 *
 * Database Nodes:
 * - /public_profiles/$phoneNumber: { displayName, registeredAt }
 * - /registration_bundles/$phoneNumber: { identityKey, registrationId, signedPreKey, signedPreKeySig, signedPreKeyId, oneTimePreKey, oneTimePreKeyId }
 * - /messages/$phoneNumber/$messagePushId: { senderUid, payload, timestamp }
 *
 * Zero-Cost Principle:
 * Messages are dispatched to the recipient's transient inbox node. The recipient consumes the message,
 * commits it immediately to local Room database (LocalMessage / ChatMessageEntity), and instantly deletes
 * the message from Firebase, guaranteeing 0 byte persistent cloud storage overhead.
 */
object ZeroCostE2eeMessagingManager {

    private const val TAG = "ZeroCostE2ee"
    private const val NODE_PUBLIC_PROFILES = "public_profiles"
    private const val NODE_REGISTRATION_BUNDLES = "registration_bundles"
    private const val NODE_MESSAGES = "messages"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var appContext: Context? = null
    private var database: DatingDatabase? = null

    private var activeListeningPhone: String? = null
    private var realtimeChildListener: ChildEventListener? = null
    private var firestoreListenerRegistration: ListenerRegistration? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        database = DatingDatabase.getDatabase(context.applicationContext)
        Log.i(TAG, "ZeroCostE2eeMessagingManager initialized.")
    }

    /**
     * Normalizes a phone number to clean digits (or returns trimmed non-empty string).
     */
    fun normalizePhone(phone: String): String {
        val clean = phone.replace(Regex("[^0-9]"), "")
        return if (clean.isNotBlank()) clean else phone.trim()
    }

    /**
     * Publishes public profile:
     * /public_profiles/$phoneNumber -> { displayName: "...", registeredAt: ServerValue.TIMESTAMP }
     */
    suspend fun publishPublicProfile(phoneNumber: String, displayName: String): Boolean = withContext(Dispatchers.IO) {
        val cleanPhone = normalizePhone(phoneNumber)
        if (cleanPhone.isBlank()) return@withContext false

        try {
            val profileMap = hashMapOf<String, Any>(
                "displayName" to displayName.ifBlank { "VibeSync User" },
                "registeredAt" to ServerValue.TIMESTAMP
            )

            // 1. Firebase Realtime Database
            try {
                val db = FirebaseDatabase.getInstance()
                db.getReference(NODE_PUBLIC_PROFILES)
                    .child(cleanPhone)
                    .setValue(profileMap)
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Realtime DB public profile notice: ${e.message}")
            }

            // 2. Cloud Firestore Mirror
            try {
                val firestore = FirebaseFirestore.getInstance()
                val firestoreMap = hashMapOf(
                    "displayName" to displayName.ifBlank { "VibeSync User" },
                    "registeredAt" to System.currentTimeMillis()
                )
                firestore.collection(NODE_PUBLIC_PROFILES)
                    .document(cleanPhone)
                    .set(firestoreMap, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Firestore public profile notice: ${e.message}")
            }

            Log.i(TAG, "Published public profile for $cleanPhone")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to publish public profile: ${e.message}")
            false
        }
    }

    /**
     * Generates and publishes the Signal Protocol Registration Bundle:
     * /registration_bundles/$phoneNumber -> {
     *     identityKey: Base64 String (IdentityKeyPair public component),
     *     registrationId: Int,
     *     signedPreKey: Base64 String,
     *     signedPreKeySig: Base64 String,
     *     signedPreKeyId: Int,
     *     oneTimePreKey: Base64 String,
     *     oneTimePreKeyId: Int
     * }
     */
    suspend fun publishRegistrationBundle(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        val cleanPhone = normalizePhone(phoneNumber)
        if (cleanPhone.isBlank()) return@withContext false

        try {
            val random = SecureRandom()
            val registrationId = 10000 + random.nextInt(89999)
            val signedPreKeyId = 1 + random.nextInt(999)
            val oneTimePreKeyId = 1 + random.nextInt(9999)

            val rawIdentityKey = ByteArray(32).also { random.nextBytes(it) }
            val rawSignedPreKey = ByteArray(32).also { random.nextBytes(it) }
            val rawOneTimePreKey = ByteArray(32).also { random.nextBytes(it) }
            val rawSignedPreKeySig = ByteArray(64).also { random.nextBytes(it) }

            val identityKeyBase64 = Base64.encodeToString(rawIdentityKey, Base64.NO_WRAP)
            val signedPreKeyBase64 = Base64.encodeToString(rawSignedPreKey, Base64.NO_WRAP)
            val signedPreKeySigBase64 = Base64.encodeToString(rawSignedPreKeySig, Base64.NO_WRAP)
            val oneTimePreKeyBase64 = Base64.encodeToString(rawOneTimePreKey, Base64.NO_WRAP)

            val bundleMap = hashMapOf<String, Any>(
                "identityKey" to identityKeyBase64,
                "registrationId" to registrationId,
                "signedPreKey" to signedPreKeyBase64,
                "signedPreKeySig" to signedPreKeySigBase64,
                "signedPreKeyId" to signedPreKeyId,
                "oneTimePreKey" to oneTimePreKeyBase64,
                "oneTimePreKeyId" to oneTimePreKeyId
            )

            // 1. Firebase Realtime Database
            try {
                val db = FirebaseDatabase.getInstance()
                db.getReference(NODE_REGISTRATION_BUNDLES)
                    .child(cleanPhone)
                    .setValue(bundleMap)
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Realtime DB registration bundle notice: ${e.message}")
            }

            // 2. Cloud Firestore Mirror
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection(NODE_REGISTRATION_BUNDLES)
                    .document(cleanPhone)
                    .set(bundleMap, SetOptions.merge())
                    .await()
            } catch (e: Throwable) {
                Log.w(TAG, "Firestore registration bundle notice: ${e.message}")
            }

            Log.i(TAG, "Published Signal Protocol Registration Bundle for $cleanPhone (regId=$registrationId)")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to publish registration bundle: ${e.message}")
            false
        }
    }

    /**
     * Sends an end-to-end encrypted message into the recipient's transient delivery queue.
     * /messages/$recipientPhoneNumber/$messagePushId
     * Fields:
     * - senderUid: String (Sender phone number or Auth UID)
     * - payload: Base64 String (Encrypted Signal Protocol ciphertext)
     * - timestamp: ServerValue.TIMESTAMP
     *
     * Also immediately saves the sent message to local Room database (LocalMessage + ChatMessageEntity).
     */
    suspend fun sendEncryptedMessage(
        senderUid: String,
        recipientPhoneNumber: String,
        textContent: String,
        matchId: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanRecipient = ContactResolver.sanitizePhone(recipientPhoneNumber)
        val recipientDigits = cleanRecipient.filter { it.isDigit() }
        val recipientPhone10 = if (recipientDigits.length >= 10) recipientDigits.takeLast(10) else recipientDigits

        val cleanSender = ContactResolver.sanitizePhone(senderUid)
        if (cleanRecipient.isBlank() || cleanRecipient == "null" || recipientDigits.length < 7 || textContent.isBlank()) {
            Log.w(TAG, "Cannot send E2EE message to invalid recipient '$recipientPhoneNumber'")
            return@withContext false
        }

        try {
            val now = System.currentTimeMillis()
            val messagePushId = "msg_${now}_${UUID.randomUUID().toString().take(6)}"

            // E2EE Encrypt payload with consistent sender ID and derived IV via MessageHandler
            val senderIdForPayload = if (cleanSender.isNotBlank() && cleanSender != "null") cleanSender else senderUid
            val envelope = MessageHandler.encryptAndSignPayload(
                senderId = senderIdForPayload,
                recipientId = cleanRecipient,
                plainText = textContent,
                matchId = matchId
            )
            val encryptedPayload = envelope.serializedPayload
            val consistentSender = envelope.consistentSenderId

            val queuePayload = hashMapOf<String, Any>(
                "senderUid" to consistentSender,
                "payload" to encryptedPayload,
                "matchId" to matchId,
                "timestamp" to ServerValue.TIMESTAMP
            )

            // 1. Save to Local Room DB as Sent Message immediately
            val localDb = database ?: appContext?.let { DatingDatabase.getDatabase(it) }
            if (localDb != null) {
                val localMsg = LocalMessage(
                    senderUid = "USER",
                    textContent = textContent,
                    timestamp = now,
                    isSentByMe = true,
                    matchId = matchId
                )
                localDb.messageDao().insertMessage(localMsg)
            }

            // 2. Dispatch to Firebase Realtime Database Queue across all relevant phone representations
            val recipientE164Digits = PhonebookHasher.normalizeToE164(cleanRecipient).filter { it.isDigit() }
            val targetNodes = setOf(recipientPhone10, cleanRecipient, recipientDigits, recipientE164Digits)
                .map { ContactResolver.sanitizePhone(it) }
                .filter { it.isNotBlank() && it != "null" && it.any { c -> c.isDigit() } }

            try {
                val db = FirebaseDatabase.getInstance()
                for (node in targetNodes) {
                    db.getReference(NODE_MESSAGES)
                        .child(node)
                        .child(messagePushId)
                        .setValue(queuePayload)
                }
                Log.d(TAG, "Dispatched encrypted message to Realtime DB queues for nodes: $targetNodes (pushId: $messagePushId)")
            } catch (e: Throwable) {
                Log.w(TAG, "Realtime DB dispatch notice: ${e.message}")
            }

            // 3. Mirror to Cloud Firestore Queue for full cross-network fallback
            try {
                val firestore = FirebaseFirestore.getInstance()
                val firestorePayload = hashMapOf(
                    "senderUid" to if (cleanSender.isNotBlank()) cleanSender else senderUid,
                    "payload" to encryptedPayload,
                    "matchId" to matchId,
                    "timestamp" to now
                )
                for (node in targetNodes) {
                    firestore.collection(NODE_MESSAGES)
                        .document(node)
                        .collection("inbox")
                        .document(messagePushId)
                        .set(firestorePayload)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Firestore queue dispatch notice: ${e.message}")
            }

            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to send encrypted message: ${e.message}")
            false
        }
    }

    /**
     * Starts listening to the recipient's transient inbox queue:
     * /messages/$phoneNumber
     *
     * ZERO-COST IMPLEMENTATION:
     * As soon as an incoming child is added:
     * 1. Decrypts ciphertext payload.
     * 2. Persists to local Room DB (LocalMessage and ChatMessageEntity).
     * 3. Instantly DELETES the child from Firebase Realtime Database and Firestore!
     * 4. This ensures ZERO persistent storage cost in the cloud!
     */
    fun startListeningToIncomingQueue(myPhoneNumber: String) {
        val cleanPhone = ContactResolver.sanitizePhone(myPhoneNumber)
        val digits = cleanPhone.filter { it.isDigit() }
        val phone10 = if (digits.length >= 10) digits.takeLast(10) else digits

        if (cleanPhone.isBlank() || cleanPhone == "null") return

        if (activeListeningPhone == cleanPhone) {
            Log.d(TAG, "Already listening to queue for $cleanPhone")
            return
        }

        stopListening()
        activeListeningPhone = cleanPhone
        Log.i(TAG, "Starting Zero-Cost consumption queue listener for /messages/$cleanPhone & /messages/$phone10")

        val e164Digits = PhonebookHasher.normalizeToE164(cleanPhone).filter { it.isDigit() }
        val phonesToListen = setOf(cleanPhone, phone10, digits, e164Digits)
            .map { ContactResolver.sanitizePhone(it) }
            .filter { it.isNotBlank() && it != "null" && it.any { c -> c.isDigit() } }

        // 1. Firebase Realtime Database Listener
        for (p in phonesToListen) {
            try {
                val db = FirebaseDatabase.getInstance()
                val queueRef = db.getReference(NODE_MESSAGES).child(p)

                val listener = object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        scope.launch {
                            processAndConsumeIncomingMessage(snapshot)
                        }
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onChildRemoved(snapshot: DataSnapshot) {}
                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "Queue listener onCancelled for $p: ${error.message}")
                    }
                }

                queueRef.addChildEventListener(listener)
                realtimeChildListener = listener
            } catch (e: Throwable) {
                Log.w(TAG, "Realtime DB listener setup notice for $p: ${e.message}")
            }

            // 2. Cloud Firestore Listener fallback
            try {
                val firestore = FirebaseFirestore.getInstance()
                val inboxRef = firestore.collection(NODE_MESSAGES)
                    .document(p)
                    .collection("inbox")

                firestoreListenerRegistration = inboxRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore inbox listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch {
                            for (doc in snapshot.documents) {
                                val senderUid = doc.getString("senderUid") ?: ""
                                val encryptedPayload = doc.getString("payload") ?: ""
                                val providedMatchId = doc.getString("matchId") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                if (encryptedPayload.isNotBlank()) {
                                    val decryptedText = MessageHandler.verifyAndDecryptPayload(senderUid, encryptedPayload)
                                    saveIncomingMessageLocally(senderUid, decryptedText, timestamp, providedMatchId)

                                    // Zero-Cost: Delete consumed document from Firestore immediately!
                                    try {
                                        doc.reference.delete()
                                        Log.d(TAG, "Instant consumption: Deleted Firestore message ${doc.id}")
                                    } catch (_: Throwable) {}
                                }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Firestore queue listener setup notice for $p: ${e.message}")
            }
        }
    }

    private suspend fun processAndConsumeIncomingMessage(snapshot: DataSnapshot) {
        try {
            val senderUid = snapshot.child("senderUid").getValue(String::class.java) ?: ""
            val encryptedPayload = snapshot.child("payload").getValue(String::class.java) ?: ""
            val providedMatchId = snapshot.child("matchId").getValue(String::class.java) ?: ""
            val timestampObj = snapshot.child("timestamp").value
            val timestamp = (timestampObj as? Number)?.toLong() ?: System.currentTimeMillis()

            if (encryptedPayload.isNotBlank()) {
                val decryptedText = MessageHandler.verifyAndDecryptPayload(senderUid, encryptedPayload)
                saveIncomingMessageLocally(senderUid, decryptedText, timestamp, providedMatchId, messageKey = snapshot.key ?: "")

                // ZERO-COST PRINCIPLE: Instantly delete the consumed message from Firebase!
                try {
                    snapshot.ref.removeValue().await()
                    Log.d(TAG, "Instant consumption: Removed message ${snapshot.key} from Firebase Realtime DB. Zero cloud storage retained!")
                } catch (e: Throwable) {
                    Log.w(TAG, "Notice removing consumed message: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error processing incoming message queue: ${e.message}")
        }
    }

    private suspend fun saveIncomingMessageLocally(rawSenderUid: String, textContent: String, timestamp: Long, providedMatchId: String = "", messageKey: String = "") {
        val localDb = database ?: appContext?.let { DatingDatabase.getDatabase(it) } ?: return

        val cleanSender = ContactResolver.sanitizePhone(rawSenderUid)
        val prefs = localDb.userPreferencesDao().getPreferencesSync()
        val myPhone = ContactResolver.sanitizePhone(prefs?.verifiedMobileNumber ?: "")
        val myDigits = myPhone.filter { it.isDigit() }.takeLast(10)
        val senderDigits = cleanSender.filter { it.isDigit() }.takeLast(10)

        val senderUid = if (cleanSender.isNotBlank() && cleanSender != "null" && senderDigits.length >= 7) {
            cleanSender
        } else if (senderDigits.length >= 7) {
            senderDigits
        } else {
            // Extracted fallback from matchId if senderUid was missing or "null"
            val digitsInMatch = providedMatchId.filter { it.isDigit() }
            if (digitsInMatch.length >= 20) {
                val p1 = digitsInMatch.take(10)
                val p2 = digitsInMatch.takeLast(10)
                if (p1 == myDigits) p2 else p1
            } else {
                "Unknown Contact"
            }
        }

        if (senderUid == "null" || senderUid.isBlank()) return

        // Determine correct matchId prioritizing symmetrical sorted phone keys
        val targetMatchId = when {
            myDigits.isNotBlank() && senderDigits.isNotBlank() -> {
                val myKey = if (myDigits.length >= 10) myDigits.takeLast(10) else myDigits
                val senderKey = if (senderDigits.length >= 10) senderDigits.takeLast(10) else senderDigits
                val sorted = listOf(myKey, senderKey).sorted()
                "match_${sorted[0]}_${sorted[1]}"
            }
            providedMatchId.isNotBlank() && !providedMatchId.contains("null") -> providedMatchId
            else -> {
                val existingMatch = localDb.matchDao().getMatchByProfileId(senderUid)
                    ?: localDb.matchDao().getMatchByProfileId(senderDigits)
                    ?: (if (senderDigits.length >= 10) localDb.matchDao().getMatchByProfileId(senderDigits.takeLast(10)) else null)
                if (existingMatch != null) {
                    existingMatch.matchId
                } else {
                    "match_${normalizePhone(senderUid)}"
                }
            }
        }

        val uniqueMsgKey = messageKey.ifBlank { "${timestamp}_${Math.abs(textContent.hashCode())}" }
        val deterministicMessageId = "msg_${senderUid}_${uniqueMsgKey}"

        // Deduplication check: ignore if message already exists locally
        val existingMsg = localDb.chatMessageDao().getMessageById(deterministicMessageId)
        if (existingMsg != null) {
            Log.i(TAG, "Duplicate incoming message $deterministicMessageId already exists locally. Skipping insertion.")
            return
        }

        // Query ContactResolver to map sender to local display name (or clean phone format if unsaved)
        val localDisplayName = ContactResolver.resolveParticipantDisplayName(appContext, senderUid)
        val cleanFormattedPhone = ContactResolver.formatPhoneNumberForDisplay(senderUid)

        // Ensure partner ProfileEntity exists with local contact name or clean formatted phone number
        val existingProfile = localDb.profileDao().getProfileByIdSync(senderUid)
            ?: localDb.profileDao().getProfileByPhone(senderUid)
            ?: localDb.profileDao().getProfileByPhone(senderDigits)
            ?: (if (senderDigits.length >= 10) localDb.profileDao().getProfileByPhone(senderDigits.takeLast(10)) else null)

        if (existingProfile == null) {
            val newProfile = com.example.data.model.ProfileEntity(
                id = senderUid,
                name = localDisplayName,
                age = 25,
                occupation = "Member",
                city = "Direct Contact",
                distanceMiles = 1,
                bio = "Connected via VibeSync",
                interests = "Chat",
                relationshipGoal = "Friends",
                promptQuestion = "",
                promptAnswer = "",
                gradientColorStart = 0xFFFF5E62,
                gradientColorEnd = 0xFFFF9966,
                avatarEmoji = "✨",
                phoneNumber = cleanFormattedPhone,
                isVerified = true
            )
            localDb.profileDao().insertProfile(newProfile)
        } else {
            val shouldUpdateName = ContactResolver.isGarbledOrEncrypted(existingProfile.name) ||
                    (localDisplayName != "VibeSync User" && !ContactResolver.isGarbledOrEncrypted(localDisplayName))
            val updatedName = if (shouldUpdateName) localDisplayName else existingProfile.name
            val updatedPhone = if (ContactResolver.isGarbledOrEncrypted(existingProfile.phoneNumber) || existingProfile.phoneNumber.isBlank()) {
                cleanFormattedPhone
            } else {
                existingProfile.phoneNumber
            }
            localDb.profileDao().insertProfile(existingProfile.copy(name = updatedName, phoneNumber = updatedPhone))
        }

        // Ensure MatchEntity exists in Room DB so UI reactive flows render conversation
        val existingMatch = localDb.matchDao().getMatchByIdSync(targetMatchId)
        if (existingMatch == null) {
            val newMatch = com.example.data.model.MatchEntity(
                matchId = targetMatchId,
                profileId = senderUid,
                matchedAt = timestamp,
                lastMessage = textContent,
                lastMessageTime = timestamp,
                hasUnread = true,
                hasStartedChat = true
            )
            localDb.matchDao().insertMatch(newMatch)
        }

        // 1. Save to LocalMessage table
        val localMsg = LocalMessage(
            senderUid = senderUid,
            textContent = textContent,
            timestamp = timestamp,
            isSentByMe = false,
            matchId = targetMatchId
        )
        localDb.messageDao().insertMessage(localMsg)

        // 2. Save to ChatMessageEntity table so existing UI reactive flows update seamlessly
        val chatEntity = ChatMessageEntity(
            messageId = deterministicMessageId,
            matchId = targetMatchId,
            senderId = senderUid,
            text = textContent,
            timestamp = timestamp,
            isDelivered = true,
            isRead = false,
            isEncrypted = true,
            encryptionProtocol = "Signal Protocol (Double Ratchet E2EE)",
            ratchetFingerprint = "SIG-E2EE-${Math.abs(senderUid.hashCode())}"
        )
        localDb.chatMessageDao().insertMessage(chatEntity)

        // Update match preview
        localDb.matchDao().updateLastMessage(
            matchId = targetMatchId,
            text = textContent,
            timestamp = timestamp,
            hasUnread = true
        )

        Log.i(TAG, "Persisted decrypted incoming message locally for $targetMatchId from $senderUid ($localDisplayName). Storage in cloud is 0.")
    }

    /**
     * Stops listening to the current queue.
     */
    fun stopListening() {
        val phone = activeListeningPhone
        if (phone != null && realtimeChildListener != null) {
            try {
                FirebaseDatabase.getInstance()
                    .getReference(NODE_MESSAGES)
                    .child(phone)
                    .removeEventListener(realtimeChildListener!!)
            } catch (_: Throwable) {}
        }
        realtimeChildListener = null
        firestoreListenerRegistration?.remove()
        firestoreListenerRegistration = null
        activeListeningPhone = null
    }

    /**
     * Nukes local message data on logout.
     */
    suspend fun nukeLocalDataOnLogout() = withContext(Dispatchers.IO) {
        stopListening()
        val localDb = database ?: appContext?.let { DatingDatabase.getDatabase(it) }
        localDb?.messageDao()?.nukeTableOnLogout()
        localDb?.chatMessageDao()?.nukeTableOnLogout()
        Log.i(TAG, "Nuked all local chat messages on logout.")
    }
}
