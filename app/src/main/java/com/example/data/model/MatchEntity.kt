package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val matchId: String,
    val profileId: String,
    val matchedAt: Long = System.currentTimeMillis(),
    val lastMessage: String = "You matched! Say hello 👋",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val hasUnread: Boolean = true,
    // Relationship Ethics: When matched/swiped, start as connected friends (isDatingMatch = false).
    // Only becomes isDatingMatch = true if both parties agree to an official Dating Relationship by mutual consent!
    val isDatingMatch: Boolean = false,
    val status: String = "ACTIVE", // "ACTIVE", "BROKEN_UP", "MUTUAL_BROKEN_UP"
    val relationshipStatus: String = "FRIENDS", // "FRIENDS", "PROPOSAL_SENT", "PROPOSAL_RECEIVED", "IN_RELATIONSHIP", "BREAKUP_PENDING_USER", "BREAKUP_PENDING_PARTNER", "MUTUAL_BROKEN_UP"
    val hasStartedChat: Boolean = true,
    val isPhonebookContact: Boolean = false
)
