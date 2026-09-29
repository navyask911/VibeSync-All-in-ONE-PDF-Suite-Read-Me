package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "friendship_requests")
data class FriendshipRequestEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val senderId: String,          // User ID who swiped down
    val senderName: String,
    val senderAge: Int,
    val senderOccupation: String,
    val senderCity: String,
    val senderCountry: String,
    val senderCountryFlag: String = "🇺🇸",
    val senderBio: String,
    val senderAvatarEmoji: String = "☕",
    val senderColorStart: Long = 0xFFFF5E62,
    val senderColorEnd: Long = 0xFFFF9966,
    val senderInterests: String = "",
    val receiverId: String,        // Target User ID
    val receiverName: String,
    val status: String,            // "PENDING", "ACCEPTED", "DECLINED"
    val timestamp: Long = System.currentTimeMillis(),
    val acceptedTimestamp: Long? = null,
    val isSwipeDownInitiated: Boolean = true
) {
    fun getInterestsList(): List<String> {
        return senderInterests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
