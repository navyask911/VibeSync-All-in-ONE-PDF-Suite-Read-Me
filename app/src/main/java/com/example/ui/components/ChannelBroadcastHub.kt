package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.RomanticViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BroadcastPost(
    val id: String,
    val title: String,
    val content: String,
    val mediaType: String, // "TEXT", "NEWS", "PHOTO", "VIDEO"
    val mediaTag: String = "",
    val timestamp: Long,
    val views: Int,
    var reactions: MutableMap<String, Int> = mutableMapOf("❤️" to 42, "🔥" to 89, "👏" to 25)
)

data class ChannelItem(
    val id: String,
    val name: String,
    val category: String,
    val emoji: String,
    val description: String,
    var followersCount: Int,
    val isVerified: Boolean = true,
    var isFollowed: Boolean = false,
    val creatorName: String = "VibeSync Official",
    val posts: MutableList<BroadcastPost> = mutableListOf()
)

@Composable
fun ChannelBroadcastHub(
    channels: List<ChannelItem>,
    onToggleFollow: (channelId: String) -> Unit,
    onCreateChannel: (name: String, category: String, emoji: String, desc: String) -> Unit,
    onBroadcastPost: (channelId: String, title: String, content: String, type: String) -> Unit
) {
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    var selectedChannelForDetail by remember { mutableStateOf<ChannelItem?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📢 Public Broadcast Channels",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Follow broadcast channels for news, videos, photos & updates",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showCreateChannelDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = RomanticViolet),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp).testTag("btn_create_channel")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Carousel of Featured Channels
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(channels) { channel ->
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedChannelForDetail = channel }
                        .testTag("card_channel_${channel.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (channel.isFollowed) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.Transparent
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(channel.emoji, fontSize = 20.sp)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF00E5FF).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = channel.category,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00B0FF),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = channel.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (channel.isVerified) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Channel",
                                    tint = Color(0xFF00B0FF),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = channel.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 4.dp),
                            lineHeight = 14.sp
                        )

                        Text(
                            text = "${channel.followersCount} followers",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onToggleFollow(channel.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (channel.isFollowed) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF00E5FF),
                                contentColor = if (channel.isFollowed) MaterialTheme.colorScheme.onSurfaceVariant else Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        ) {
                            Text(
                                text = if (channel.isFollowed) "Following ✓" else "Follow",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Create New Channel Dialog
    if (showCreateChannelDialog) {
        CreateChannelDialog(
            onDismiss = { showCreateChannelDialog = false },
            onCreate = { name, cat, emoji, desc ->
                onCreateChannel(name, cat, emoji, desc)
                showCreateChannelDialog = false
            }
        )
    }

    // Channel Detail & Broadcast View Dialog
    selectedChannelForDetail?.let { channel ->
        ChannelViewerDialog(
            channel = channel,
            onDismiss = { selectedChannelForDetail = null },
            onToggleFollow = { onToggleFollow(channel.id) },
            onBroadcast = { title, content, type ->
                onBroadcastPost(channel.id, title, content, type)
            }
        )
    }
}

@Composable
fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, category: String, emoji: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("News & Daily") }
    var emoji by remember { mutableStateOf("📢") }
    var desc by remember { mutableStateOf("") }

    val categories = listOf("News & Daily", "Tech & AI", "Nightlife & Dates", "Fitness & Sports", "Humor & Memes", "Local Community")
    val emojis = listOf("📢", "🚀", "☕", "🎉", "🏋️", "💡", "🍔", "🎬", "✨", "🔥")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Public Broadcast Channel", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Anyone can create a broadcast channel to share messages, photos, videos, and news updates with unlimited subscribers.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Name") },
                    placeholder = { Text("e.g. Bangalore Connect & Weekend Spots") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_channel_name")
                )

                // Select Emoji
                Text("Select Channel Icon:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis) { em ->
                        Surface(
                            shape = CircleShape,
                            color = if (emoji == em) RomanticViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (emoji == em) RomanticViolet else Color.Transparent
                            ),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { emoji = em }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(em, fontSize = 18.sp)
                            }
                        }
                    }
                }

                // Select Category
                Text("Select Category:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (category == cat) CoralPink else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.5.sp,
                                fontWeight = if (category == cat) FontWeight.Bold else FontWeight.Normal,
                                color = if (category == cat) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Channel Description") },
                    placeholder = { Text("Describe what you will broadcast...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), category, emoji, desc.trim().ifBlank { "Official broadcast channel" })
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RomanticViolet),
                modifier = Modifier.testTag("btn_confirm_create_channel")
            ) {
                Text("Create Channel", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ChannelViewerDialog(
    channel: ChannelItem,
    onDismiss: () -> Unit,
    onToggleFollow: () -> Unit,
    onBroadcast: (title: String, content: String, type: String) -> Unit
) {
    val context = LocalContext.current
    var showBroadcastInput by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastText by remember { mutableStateOf("") }
    var broadcastType by remember { mutableStateOf("TEXT") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Channel Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(RomanticViolet.copy(alpha = 0.85f), CoralPink.copy(alpha = 0.85f))
                            )
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(channel.emoji, fontSize = 24.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channel.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${channel.followersCount} subscribers • ${channel.category}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onToggleFollow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (channel.isFollowed) Color.White.copy(alpha = 0.2f) else Color.White,
                                contentColor = if (channel.isFollowed) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (channel.isFollowed) "Following ✓" else "+ Follow",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Channel Description Subtitle
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = channel.description,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Broadcast Posts Timeline
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (channel.posts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No broadcasts yet. Tap '+ Broadcast Update' below to share news, photos, or announcements!",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(channel.posts) { post ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (post.mediaType) {
                                                "NEWS" -> CoralPink.copy(alpha = 0.15f)
                                                "VIDEO" -> RomanticViolet.copy(alpha = 0.15f)
                                                "PHOTO" -> LikeGreen.copy(alpha = 0.15f)
                                                else -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = post.mediaTag.ifBlank { post.mediaType },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (post.mediaType) {
                                                    "NEWS" -> CoralPink
                                                    "VIDEO" -> RomanticViolet
                                                    "PHOTO" -> LikeGreen
                                                    else -> Color(0xFF00B0FF)
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = formatBroadcastTime(post.timestamp) + " • 👁️ ${post.views}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = post.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = post.content,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    // Interactive Emoji Reactions Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        post.reactions.forEach { (emoji, count) ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                                modifier = Modifier.clickable {
                                                    post.reactions[emoji] = count + 1
                                                    Toast.makeText(context, "Reacted with $emoji!", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(emoji, fontSize = 12.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Broadcast Composer Action Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(10.dp)
                ) {
                    if (showBroadcastInput) {
                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            placeholder = { Text("Headline / Title...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = broadcastText,
                            onValueChange = { broadcastText = it },
                            placeholder = { Text("Write broadcast message, video/photo link...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("TEXT", "NEWS", "PHOTO", "VIDEO").forEach { type ->
                                    FilterChip(
                                        selected = broadcastType == type,
                                        onClick = { broadcastType = type },
                                        label = { Text(type, fontSize = 10.sp) }
                                    )
                                }
                            }

                            Row {
                                TextButton(onClick = { showBroadcastInput = false }) { Text("Cancel") }
                                Button(
                                    onClick = {
                                        if (broadcastTitle.isNotBlank() || broadcastText.isNotBlank()) {
                                            onBroadcast(broadcastTitle.trim().ifBlank { "Channel Update" }, broadcastText.trim(), broadcastType)
                                            broadcastTitle = ""
                                            broadcastText = ""
                                            showBroadcastInput = false
                                            Toast.makeText(context, "Broadcasted to ${channel.followersCount} subscribers! 🚀", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RomanticViolet)
                                ) {
                                    Text("Send 📢", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { showBroadcastInput = true },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RomanticViolet)
                        ) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Broadcast Post to Subscribers", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatBroadcastTime(time: Long): String {
    if (time <= 0L) return "Recently"
    val diff = System.currentTimeMillis() - time
    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(time))
    }
}
