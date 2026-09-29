package com.example.util.backup

import com.example.data.model.ChatMessageEntity
import com.example.data.model.FriendshipRequestEntity
import com.example.data.model.MatchEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.UserContactEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Structured snapshot model for Chat and Social Backups to Google Drive AppFolder.
 */
data class ChatBackupPayload(
    val version: Int = 1,
    val backupTimestamp: Long = System.currentTimeMillis(),
    val appIdentifier: String = "com.aistudio.datingapp",
    val accountEmail: String = "",
    val totalMessages: Int = 0,
    val totalMatches: Int = 0,
    val totalContacts: Int = 0,
    val messages: List<ChatMessageEntity> = emptyList(),
    val matches: List<MatchEntity> = emptyList(),
    val friendshipRequests: List<FriendshipRequestEntity> = emptyList(),
    val userContacts: List<UserContactEntity> = emptyList(),
    val myProfileSnapshot: ProfileEntity? = null
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("backupTimestamp", backupTimestamp)
        root.put("appIdentifier", appIdentifier)
        root.put("accountEmail", accountEmail)
        root.put("totalMessages", messages.size)
        root.put("totalMatches", matches.size)
        root.put("totalContacts", userContacts.size)

        // Messages Array
        val msgArray = JSONArray()
        for (m in messages) {
            val obj = JSONObject()
            obj.put("messageId", m.messageId)
            obj.put("matchId", m.matchId)
            obj.put("senderId", m.senderId)
            obj.put("text", m.text)
            obj.put("timestamp", m.timestamp)
            obj.put("isDelivered", m.isDelivered)
            obj.put("isRead", m.isRead)
            obj.put("isEncrypted", m.isEncrypted)
            obj.put("encryptionProtocol", m.encryptionProtocol)
            obj.put("ratchetFingerprint", m.ratchetFingerprint)
            obj.put("mediaType", m.mediaType)
            obj.put("mediaUrl", m.mediaUrl)
            obj.put("voiceDurationSeconds", m.voiceDurationSeconds)
            obj.put("isEdited", m.isEdited)
            obj.put("editedTimestamp", m.editedTimestamp)
            obj.put("isStarred", m.isStarred)
            obj.put("replyToMessageId", m.replyToMessageId ?: "")
            obj.put("replyToText", m.replyToText ?: "")
            obj.put("replyToSender", m.replyToSender ?: "")
            obj.put("isDeletedForEveryone", m.isDeletedForEveryone)
            obj.put("isDeletedForMe", m.isDeletedForMe)
            obj.put("isForwarded", m.isForwarded)
            msgArray.put(obj)
        }
        root.put("messages", msgArray)

        // Matches Array
        val matchArray = JSONArray()
        for (m in matches) {
            val obj = JSONObject()
            obj.put("matchId", m.matchId)
            obj.put("profileId", m.profileId)
            obj.put("matchedAt", m.matchedAt)
            obj.put("lastMessage", m.lastMessage)
            obj.put("lastMessageTime", m.lastMessageTime)
            obj.put("hasUnread", m.hasUnread)
            obj.put("isDatingMatch", m.isDatingMatch)
            obj.put("status", m.status)
            obj.put("relationshipStatus", m.relationshipStatus)
            obj.put("hasStartedChat", m.hasStartedChat)
            obj.put("isPhonebookContact", m.isPhonebookContact)
            matchArray.put(obj)
        }
        root.put("matches", matchArray)

        // Friendship Requests
        val frArray = JSONArray()
        for (f in friendshipRequests) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("senderId", f.senderId)
            obj.put("senderName", f.senderName)
            obj.put("senderAge", f.senderAge)
            obj.put("senderOccupation", f.senderOccupation)
            obj.put("senderCity", f.senderCity)
            obj.put("senderCountry", f.senderCountry)
            obj.put("senderCountryFlag", f.senderCountryFlag)
            obj.put("senderBio", f.senderBio)
            obj.put("senderAvatarEmoji", f.senderAvatarEmoji)
            obj.put("senderColorStart", f.senderColorStart)
            obj.put("senderColorEnd", f.senderColorEnd)
            obj.put("senderInterests", f.senderInterests)
            obj.put("receiverId", f.receiverId)
            obj.put("receiverName", f.receiverName)
            obj.put("status", f.status)
            obj.put("timestamp", f.timestamp)
            obj.put("acceptedTimestamp", f.acceptedTimestamp ?: 0L)
            obj.put("isSwipeDownInitiated", f.isSwipeDownInitiated)
            frArray.put(obj)
        }
        root.put("friendshipRequests", frArray)

        // User Contacts
        val contactArray = JSONArray()
        for (c in userContacts) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("userId", c.userId)
            obj.put("contactName", c.contactName)
            obj.put("phoneNumber", c.phoneNumber)
            obj.put("isOnVibeSync", c.isOnVibeSync)
            obj.put("photoUrl", c.photoUrl)
            obj.put("statusTagline", c.statusTagline)
            obj.put("syncedToFirestore", c.syncedToFirestore)
            obj.put("updatedAt", c.updatedAt)
            contactArray.put(obj)
        }
        root.put("userContacts", contactArray)

        return root.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): ChatBackupPayload {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            val backupTimestamp = root.optLong("backupTimestamp", System.currentTimeMillis())
            val appIdentifier = root.optString("appIdentifier", "com.aistudio.datingapp")
            val accountEmail = root.optString("accountEmail", "")

            val messagesList = mutableListOf<ChatMessageEntity>()
            val msgArray = root.optJSONArray("messages")
            if (msgArray != null) {
                for (i in 0 until msgArray.length()) {
                    val obj = msgArray.getJSONObject(i)
                    messagesList.add(
                        ChatMessageEntity(
                            messageId = obj.optString("messageId", java.util.UUID.randomUUID().toString()),
                            matchId = obj.optString("matchId", ""),
                            senderId = obj.optString("senderId", "USER"),
                            text = obj.optString("text", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isDelivered = obj.optBoolean("isDelivered", true),
                            isRead = obj.optBoolean("isRead", true),
                            isEncrypted = obj.optBoolean("isEncrypted", true),
                            encryptionProtocol = obj.optString("encryptionProtocol", "VibeSync E2EE Protocol (Double Ratchet)"),
                            ratchetFingerprint = obj.optString("ratchetFingerprint", "4829-0392-5920"),
                            mediaType = obj.optString("mediaType", "TEXT"),
                            mediaUrl = obj.optString("mediaUrl", ""),
                            voiceDurationSeconds = obj.optInt("voiceDurationSeconds", 0),
                            isEdited = obj.optBoolean("isEdited", false),
                            editedTimestamp = obj.optLong("editedTimestamp", 0L),
                            isStarred = obj.optBoolean("isStarred", false),
                            replyToMessageId = obj.optString("replyToMessageId").ifBlank { null },
                            replyToText = obj.optString("replyToText").ifBlank { null },
                            replyToSender = obj.optString("replyToSender").ifBlank { null },
                            isDeletedForEveryone = obj.optBoolean("isDeletedForEveryone", false),
                            isDeletedForMe = obj.optBoolean("isDeletedForMe", false),
                            isForwarded = obj.optBoolean("isForwarded", false)
                        )
                    )
                }
            }

            val matchesList = mutableListOf<MatchEntity>()
            val matchArray = root.optJSONArray("matches")
            if (matchArray != null) {
                for (i in 0 until matchArray.length()) {
                    val obj = matchArray.getJSONObject(i)
                    matchesList.add(
                        MatchEntity(
                            matchId = obj.optString("matchId", java.util.UUID.randomUUID().toString()),
                            profileId = obj.optString("profileId", ""),
                            matchedAt = obj.optLong("matchedAt", System.currentTimeMillis()),
                            lastMessage = obj.optString("lastMessage", ""),
                            lastMessageTime = obj.optLong("lastMessageTime", System.currentTimeMillis()),
                            hasUnread = obj.optBoolean("hasUnread", false),
                            isDatingMatch = obj.optBoolean("isDatingMatch", false),
                            status = obj.optString("status", "ACTIVE"),
                            relationshipStatus = obj.optString("relationshipStatus", "FRIENDS"),
                            hasStartedChat = obj.optBoolean("hasStartedChat", true),
                            isPhonebookContact = obj.optBoolean("isPhonebookContact", false)
                        )
                    )
                }
            }

            val frList = mutableListOf<FriendshipRequestEntity>()
            val frArray = root.optJSONArray("friendshipRequests")
            if (frArray != null) {
                for (i in 0 until frArray.length()) {
                    val obj = frArray.getJSONObject(i)
                    val accTimestamp = obj.optLong("acceptedTimestamp", 0L)
                    frList.add(
                        FriendshipRequestEntity(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            senderId = obj.optString("senderId", ""),
                            senderName = obj.optString("senderName", ""),
                            senderAge = obj.optInt("senderAge", 24),
                            senderOccupation = obj.optString("senderOccupation", ""),
                            senderCity = obj.optString("senderCity", ""),
                            senderCountry = obj.optString("senderCountry", "India"),
                            senderCountryFlag = obj.optString("senderCountryFlag", "🇮🇳"),
                            senderBio = obj.optString("senderBio", ""),
                            senderAvatarEmoji = obj.optString("senderAvatarEmoji", "☕"),
                            senderColorStart = obj.optLong("senderColorStart", 0xFFFF5E62),
                            senderColorEnd = obj.optLong("senderColorEnd", 0xFFFF9966),
                            senderInterests = obj.optString("senderInterests", ""),
                            receiverId = obj.optString("receiverId", "current_user"),
                            receiverName = obj.optString("receiverName", ""),
                            status = obj.optString("status", "ACCEPTED"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            acceptedTimestamp = if (accTimestamp > 0) accTimestamp else null,
                            isSwipeDownInitiated = obj.optBoolean("isSwipeDownInitiated", true)
                        )
                    )
                }
            }

            val contactList = mutableListOf<UserContactEntity>()
            val contactArray = root.optJSONArray("userContacts")
            if (contactArray != null) {
                for (i in 0 until contactArray.length()) {
                    val obj = contactArray.getJSONObject(i)
                    contactList.add(
                        UserContactEntity(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            userId = obj.optString("userId", "current_user"),
                            contactName = obj.optString("contactName", ""),
                            phoneNumber = obj.optString("phoneNumber", ""),
                            isOnVibeSync = obj.optBoolean("isOnVibeSync", false),
                            photoUrl = obj.optString("photoUrl", ""),
                            statusTagline = obj.optString("statusTagline", ""),
                            syncedToFirestore = obj.optBoolean("syncedToFirestore", true),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            return ChatBackupPayload(
                version = version,
                backupTimestamp = backupTimestamp,
                appIdentifier = appIdentifier,
                accountEmail = accountEmail,
                totalMessages = messagesList.size,
                totalMatches = matchesList.size,
                totalContacts = contactList.size,
                messages = messagesList,
                matches = matchesList,
                friendshipRequests = frList,
                userContacts = contactList
            )
        }
    }
}
