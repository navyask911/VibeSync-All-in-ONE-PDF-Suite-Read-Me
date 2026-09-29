package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "status_stories")
data class StatusStoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatarEmoji: String = "✨",
    val userAvatarUrl: String = "",
    val mediaType: String = "PHOTO", // "PHOTO" or "VIDEO"
    val mediaUrl: String = "",
    val videoDurationSeconds: Int = 0, // Strictly <= 60 seconds
    val caption: String = "",
    val backgroundColorStart: Long = 0xFF00A884,
    val backgroundColorEnd: Long = 0xFF128C7E,
    val timestamp: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false,
    val viewsCount: Int = 1,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isMyStatus: Boolean = false,
    val privacy: String = "FRIENDS_AND_MATCHES", // "FRIENDS_AND_MATCHES" or "ALL_NETWORK"
    val isAd: Boolean = false,
    val adActionType: String = "BUSINESS_PROFILE",
    val adTargetUrl: String = "",
    val adCtaText: String = "Claim Offer ✨",
    val adBusinessId: String = "",
    val adCampaignId: String = ""
) {
    val isExpired: Boolean
        get() = !isAd && (System.currentTimeMillis() - timestamp) > 24 * 60 * 60 * 1000L
}
