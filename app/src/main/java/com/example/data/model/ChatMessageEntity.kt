package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "chat_messages",
    indices = [androidx.room.Index(value = ["messageId"], unique = true)]
)
data class ChatMessageEntity(
    @PrimaryKey val messageId: String,
    val matchId: String,
    val senderId: String, // "USER" or profileId
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDelivered: Boolean = false,
    val isRead: Boolean = false,
    // VibeSync E2EE Protocol End-to-End Encryption (E2EE)
    val isEncrypted: Boolean = true,
    val encryptionProtocol: String = "VibeSync E2EE Protocol (Double Ratchet)",
    val ratchetFingerprint: String = "4829-0392-5920",
    // Media Message Support (TEXT, IMAGE, VOICE)
    val mediaType: String = "TEXT", // "TEXT", "IMAGE", "VOICE"
    val mediaUrl: String = "",
    val voiceDurationSeconds: Int = 0,
    // Modern Chat: Edit (10 min), Reply, Forward, Star, Delete for Me/Everyone
    val isEdited: Boolean = false,
    val editedTimestamp: Long = 0L,
    val isStarred: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val isDeletedForEveryone: Boolean = false,
    val isDeletedForMe: Boolean = false,
    val isForwarded: Boolean = false
) {
    @androidx.room.Ignore
    val senderUid: String = senderId

    @androidx.room.Ignore
    val textContent: String = text

    @androidx.room.Ignore
    val isSentByMe: Boolean = (senderId == "USER")

    fun toLocalMessage(): LocalMessage = LocalMessage(
        senderUid = senderId,
        textContent = text,
        timestamp = timestamp,
        isSentByMe = (senderId == "USER"),
        matchId = matchId
    )
}

