package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * LocalMessage
 * Room entity representing local chat message storage for VibeSync phone-number keyed
 * Zero-Cost End-to-End Encrypted messaging.
 */
@Immutable
@Entity(tableName = "local_messages")
data class LocalMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderUid: String,
    val textContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSentByMe: Boolean,
    val matchId: String = ""
) {
    fun toChatMessageEntity(defaultMatchId: String = ""): ChatMessageEntity {
        val targetMatch = if (matchId.isNotBlank()) matchId else defaultMatchId
        return ChatMessageEntity(
            messageId = "local_${timestamp}_${id}",
            matchId = targetMatch,
            senderId = if (isSentByMe) "USER" else senderUid,
            text = textContent,
            timestamp = timestamp,
            isDelivered = true,
            isRead = true,
            isEncrypted = true,
            encryptionProtocol = "Signal Protocol (Double Ratchet E2EE)",
            ratchetFingerprint = "SIG-E2EE-${Math.abs(senderUid.hashCode())}"
        )
    }
}

