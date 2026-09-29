package com.example.data.model

data class PhoneContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val isOnVibeSync: Boolean = false,
    val vibeSyncProfileId: String? = null,
    val vibeSyncUser: ProfileEntity? = null,
    val avatarEmoji: String = "👤",
    val statusTagline: String = "Hey there! I am using VibeSync",
    val photoUrl: String = "",
    val relationshipStatus: String = "FRIENDS",
    val phoneHash: String = ""
)
