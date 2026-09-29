package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_reviews")
data class BusinessReviewEntity(
    @PrimaryKey val id: String = "",
    val businessId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarEmoji: String = "✨",
    val rating: Double = 5.0,
    val reviewText: String = "",
    val isGpsVerifiedVisit: Boolean = true,
    val checkInDistanceMeters: Double = 35.0,
    val createdAt: Long = System.currentTimeMillis()
)
