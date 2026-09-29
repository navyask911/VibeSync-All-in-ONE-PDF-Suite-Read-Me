package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "swipes")
data class SwipeEntity(
    @PrimaryKey val profileId: String,
    val direction: String, // "LIKE", "PASS", "SUPER_LIKE"
    val timestamp: Long = System.currentTimeMillis()
)
