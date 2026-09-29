package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_users")
data class BlockEntity(
    @PrimaryKey val profileId: String,
    val blockedAt: Long = System.currentTimeMillis()
)
