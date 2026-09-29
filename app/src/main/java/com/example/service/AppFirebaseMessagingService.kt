package com.example.service

import android.util.Log
import com.example.util.AppNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AppFirebaseMessagingService
 * Receives remote push notifications from Firebase Cloud Messaging (FCM)
 * and dispatches them in real-time via AppNotificationManager.
 */
class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AppFirebaseMsgService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM device registration token: $token")
        // Token can be sent to backend / Firestore user profile document
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data
            val type = data["type"] ?: "MESSAGE"
            val senderOrMatchId = data["sender_id"] ?: data["match_id"] ?: "user_remote"
            val name = data["name"] ?: data["sender_name"] ?: "VibeSync Match"
            val text = data["text"] ?: data["message"] ?: data["body"] ?: "New notification"
            val photoUrl = data["photo_url"] ?: data["avatar_url"] ?: ""
            val age = data["age"]?.toIntOrNull() ?: 24
            val city = data["city"] ?: "Bengaluru"
            val score = data["score"]?.toIntOrNull() ?: 94
            val mediaType = data["media_type"] ?: "TEXT"

            when (type.uppercase()) {
                "NEW_USER", "USER_JOINED", "JOINING" -> {
                    // One time alert inside Chat only - NO duplicate notifications for joining user
                    val database = com.example.data.database.DatingDatabase.getDatabase(applicationContext)
                    val cleanSenderId = senderOrMatchId.filter { it.isDigit() }.ifBlank { senderOrMatchId }
                    val fixedMessageId = "join_$cleanSenderId"
                    val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                    val joinMessageText = "👋 $name joined VibeSync! Let's Chat Together 💬"

                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val matchId = if (senderOrMatchId.startsWith("match_")) senderOrMatchId else "match_$cleanSenderId"
                            
                            // Guard 1: Check by deterministic static messageId
                            val existingMsg = database.chatMessageDao().getMessageById(fixedMessageId)
                            if (existingMsg != null) {
                                Log.i(TAG, "Join message already recorded for $cleanSenderId, skipping duplicate.")
                                return@launch
                            }

                            // Guard 2: Check if any existing message in this chat contains "joined VibeSync"
                            val allMatchMsgs = database.chatMessageDao().getMessagesForMatchSync(matchId)
                            if (allMatchMsgs.any { it.text.contains("joined VibeSync", ignoreCase = true) }) {
                                Log.i(TAG, "Join alert already present in match $matchId, skipping duplicate.")
                                return@launch
                            }

                            val systemMsg = com.example.data.model.ChatMessageEntity(
                                messageId = fixedMessageId,
                                matchId = matchId,
                                senderId = cleanSenderId,
                                text = joinMessageText,
                                mediaType = "SYSTEM",
                                mediaUrl = photoUrl,
                                timestamp = timestamp,
                                isEncrypted = true
                            )
                            database.chatMessageDao().insertMessage(systemMsg)

                            val existingMatch = database.matchDao().getMatchByIdSync(matchId)
                            if (existingMatch == null) {
                                val newMatch = com.example.data.model.MatchEntity(
                                    matchId = matchId,
                                    profileId = cleanSenderId,
                                    matchedAt = timestamp,
                                    lastMessage = joinMessageText,
                                    lastMessageTime = timestamp,
                                    hasUnread = true
                                )
                                database.matchDao().insertMatch(newMatch)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error recording new user join alert in chat DB: ${e.message}")
                        }
                    }
                    Log.d(TAG, "New user join alert recorded inside chat for $name. Push notification suppressed.")
                }
                "MATCH" -> {
                    if (!AppNotificationManager.isAppInForeground) {
                        AppNotificationManager.showMatchNotification(
                            context = applicationContext,
                            matchId = senderOrMatchId,
                            matchName = name,
                            matchPhotoUrl = photoUrl,
                            age = age,
                            city = city,
                            score = score
                        )
                    }
                }
                "LIKE" -> {
                    if (!AppNotificationManager.isAppInForeground) {
                        AppNotificationManager.showLikeNotification(
                            context = applicationContext,
                            likerId = senderOrMatchId,
                            likerName = name,
                            likerPhotoUrl = photoUrl,
                            likerCity = city
                        )
                    }
                }
                "MESSAGE" -> {
                    // 1. Persist remote incoming message payload into Room Database so it appears in the chat stream immediately
                    val database = com.example.data.database.DatingDatabase.getDatabase(applicationContext)
                    val messageId = data["message_id"] ?: data["msg_id"] ?: java.util.UUID.randomUUID().toString()
                    val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                    
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val existingMsg = database.chatMessageDao().getMessageById(messageId)
                            if (existingMsg == null) {
                                val matchId = if (senderOrMatchId.startsWith("match_")) senderOrMatchId else "match_$senderOrMatchId"
                                val incomingEntity = com.example.data.model.ChatMessageEntity(
                                    messageId = messageId,
                                    matchId = matchId,
                                    senderId = senderOrMatchId,
                                    text = text,
                                    mediaType = mediaType,
                                    mediaUrl = photoUrl,
                                    timestamp = timestamp,
                                    isEncrypted = true
                                )
                                database.chatMessageDao().insertMessage(incomingEntity)

                                // Ephemeral Cloud Delivery Policy: Purge cloud copy once persisted locally
                                try {
                                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    firestore.collection("chats")
                                        .document(matchId)
                                        .collection("messages")
                                        .document(messageId)
                                        .delete()
                                } catch (_: Exception) {}
                                
                                val existingMatch = database.matchDao().getMatchByIdSync(matchId) ?: database.matchDao().getMatchByProfileId(senderOrMatchId)
                                if (existingMatch == null) {
                                    val newMatch = com.example.data.model.MatchEntity(
                                        matchId = matchId,
                                        profileId = senderOrMatchId,
                                        matchedAt = timestamp,
                                        lastMessage = text.ifBlank { if (mediaType == "VOICE") "Voice message" else "Photo" },
                                        lastMessageTime = timestamp,
                                        hasUnread = true
                                    )
                                    database.matchDao().insertMatch(newMatch)
                                } else {
                                    database.matchDao().updateLastMessage(
                                        matchId = existingMatch.matchId,
                                        text = text.ifBlank { if (mediaType == "VOICE") "Voice message" else "Photo" },
                                        timestamp = timestamp,
                                        hasUnread = true
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error storing FCM incoming message into Room: ${e.message}")
                        }
                    }

                    // Check if user is currently online/in-app session using UserPresenceManager & AppNotificationManager
                    val isOnlineSessionActive = com.example.util.UserPresenceManager.getInstance().shouldSuppressPushNotification(senderOrMatchId)
                    if (isOnlineSessionActive) {
                        Log.d(TAG, "User is currently ONLINE in-app session. System push notification suppressed, message payload received directly in foreground.")
                    } else {
                        AppNotificationManager.showMessageNotification(
                            context = applicationContext,
                            senderId = senderOrMatchId,
                            senderName = name,
                            messageText = text,
                            senderPhotoUrl = photoUrl,
                            mediaType = mediaType
                        )
                    }
                }
                else -> {
                    // Persist default message
                    val database = com.example.data.database.DatingDatabase.getDatabase(applicationContext)
                    val messageId = data["message_id"] ?: data["msg_id"] ?: java.util.UUID.randomUUID().toString()
                    val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                    
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val existingMsg = database.chatMessageDao().getMessageById(messageId)
                            if (existingMsg == null) {
                                val matchId = if (senderOrMatchId.startsWith("match_")) senderOrMatchId else "match_$senderOrMatchId"
                                val incomingEntity = com.example.data.model.ChatMessageEntity(
                                    messageId = messageId,
                                    matchId = matchId,
                                    senderId = senderOrMatchId,
                                    text = text,
                                    mediaType = mediaType,
                                    mediaUrl = photoUrl,
                                    timestamp = timestamp,
                                    isEncrypted = true
                                )
                                database.chatMessageDao().insertMessage(incomingEntity)
                            }
                        } catch (_: Exception) {}
                    }

                    if (!AppNotificationManager.isAppInForeground) {
                        AppNotificationManager.showMessageNotification(
                            context = applicationContext,
                            senderId = senderOrMatchId,
                            senderName = name,
                            messageText = text,
                            senderPhotoUrl = photoUrl,
                            mediaType = mediaType
                        )
                    }
                }
            }
            return
        }

        // Check if message contains a standard notification payload
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "VibeSync Alert"
            val body = notification.body ?: ""
            val imageUri = notification.imageUrl?.toString() ?: ""

            if (title.contains("Match", ignoreCase = true)) {
                AppNotificationManager.showMatchNotification(
                    context = applicationContext,
                    matchId = "remote_match",
                    matchName = title.substringAfter("with ", "Your New Match"),
                    matchPhotoUrl = imageUri,
                    age = 24,
                    city = "Bengaluru",
                    score = 95
                )
            } else {
                AppNotificationManager.showMessageNotification(
                    context = applicationContext,
                    senderId = "remote_user",
                    senderName = title,
                    messageText = body,
                    senderPhotoUrl = imageUri,
                    mediaType = "TEXT"
                )
            }
        }
    }
}
