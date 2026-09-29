package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_contacts")
data class UserContactEntity(
    @PrimaryKey val id: String,
    val userId: String = "current_user",
    val contactName: String,
    val phoneNumber: String,
    val isOnVibeSync: Boolean = false,
    val photoUrl: String = "",
    val statusTagline: String = "",
    val syncedToFirestore: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
