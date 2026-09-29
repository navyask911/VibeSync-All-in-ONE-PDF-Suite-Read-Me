package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "channel_broadcasts")
data class ChannelBroadcastEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val senderName: String,
    val content: String,
    val broadcastType: String = "MESSAGE", // "MESSAGE", "NEWS", "PHOTO", "VIDEO", "EVENT"
    val mediaUrl: String = "",
    val mediaType: String = "NONE", // "NONE", "IMAGE", "VIDEO", "AUDIO"
    val reactionsCount: Int = 18,
    val isReacted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
