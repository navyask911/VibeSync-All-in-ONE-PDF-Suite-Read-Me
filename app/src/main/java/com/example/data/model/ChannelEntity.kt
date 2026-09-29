package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val handle: String, // e.g. "@bangalore_nightlife"
    val description: String,
    val category: String, // "Nightlife & Lounges", "City News & Events", "Dating Advice", "Treks & Adventure", "Tech & Startups", "Food & Cafes"
    val iconEmoji: String = "📢",
    val bannerUrl: String = "",
    val creatorUserId: String = "system", // "current_user" for user-created
    val creatorName: String = "VibeSync Official",
    val isVerified: Boolean = true,
    val isFollowed: Boolean = false,
    val followerCount: Int = 3420,
    val createdAt: Long = System.currentTimeMillis()
)
