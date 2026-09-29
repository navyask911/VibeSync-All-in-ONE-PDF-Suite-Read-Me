package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reported_users")
data class ReportEntity(
    @PrimaryKey val reportId: String,
    val reporterId: String,
    val reportedProfileId: String,
    val reason: String,
    val status: String = "PENDING", // PENDING, RESOLVED, DISMISSED
    val reportedAt: Long = System.currentTimeMillis()
)
