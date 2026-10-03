package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity for storing matched and hashed phonebook contacts locally.
 */
@Entity(tableName = "matched_contacts")
data class MatchedContactEntity(
    @PrimaryKey val id: String,
    val contactName: String = "",
    val phoneNumber: String = "",
    val phoneHash: String = "",
    val vibeSyncUserId: String = "",
    val isOnVibeSync: Boolean = false,
    val matchedAt: Long = System.currentTimeMillis(),
    val avatarEmoji: String = "✨",
    val photoUrl: String = "",
    val statusTagline: String = ""
)

/**
 * Typealias representing VibeSync registered contact entity in Room Database
 */
typealias VibeContactEntity = MatchedContactEntity
