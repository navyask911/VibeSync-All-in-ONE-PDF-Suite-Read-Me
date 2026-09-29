package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_posts")
data class BusinessPostEntity(
    @PrimaryKey val id: String = "",
    val businessId: String = "",
    val title: String = "",
    val content: String = "",
    val postType: String = "OFFER", // "OFFER", "DISCOUNT", "NEWS", "EVENT", "PROMO"
    val mediaUrl: String = "",
    val mediaType: String = "IMAGE", // "IMAGE", "VIDEO", "NONE"
    val discountPercent: Int = 0,
    val promoCode: String = "",
    val validUntil: String = "Valid till Sunday",
    val likesCount: Int = 34,
    val isLiked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
