package com.example.ui.screens.profile

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FriendshipRequestEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.SwipeEntity
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.RomanticViolet
import com.example.ui.theme.SuperLikeBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SentConnectionsDrawer(
    pendingOutgoingRequests: List<FriendshipRequestEntity>,
    allSwipes: List<SwipeEntity>,
    allProfiles: List<ProfileEntity>,
    acceptedFriends: List<FriendshipRequestEntity>,
    onCancelRequest: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Requests Sent, 1: Likes Sent, 2: Superlikes Sent, 3: Accepted Friends

    val likesSwipes = remember(allSwipes) {
        allSwipes.filter { it.direction == "LIKE" }
    }
    val superlikesSwipes = remember(allSwipes) {
        allSwipes.filter { it.direction == "SUPER_LIKE" }
    }

    val profileMap = remember(allProfiles) {
        allProfiles.associateBy { it.id }
    }

    val totalActivityCount = pendingOutgoingRequests.size + likesSwipes.size + superlikesSwipes.size + acceptedFriends.size

    ProfileSectionDrawer(
        title = "Activity & Sent Connections",
        icon = Icons.AutoMirrored.Filled.Send,
        iconTint = Color(0xFF00B0FF),
        iconBackground = Color(0xFF00B0FF).copy(alpha = 0.15f),
        statusBadgeText = "$totalActivityCount Active",
        statusBadgeColor = Color(0xFF00B0FF),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        testTag = "drawer_sent_connections"
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Track all friendship requests sent via swipe down, likes sent via swipe right, superlikes sent via swipe up, and accepted friends.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Category Chips: Requests Sent, Likes Sent, Superlikes Sent, Accepted Friends
            val tabs = listOf(
                "Requests (${pendingOutgoingRequests.size})",
                "Likes (${likesSwipes.size})",
                "Superlikes (${superlikesSwipes.size})",
                "Friends (${acceptedFriends.size})"
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tabs.indices.toList()) { index ->
                    val isSelected = selectedTab == index
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF00B0FF) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedTab = index }
                            .testTag("tab_sent_conn_$index")
                    ) {
                        Text(
                            text = tabs[index],
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // Requests Sent List
                    if (pendingOutgoingRequests.isEmpty()) {
                        EmptyStateCard(
                            message = "No pending friendship requests sent. Swipe down on discovery cards to send friend requests!"
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pendingOutgoingRequests.forEach { req ->
                                val targetProfile = profileMap[req.receiverId]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (targetProfile?.avatarUrl?.isNotBlank() == true) {
                                                AsyncImage(
                                                    model = targetProfile.avatarUrl,
                                                    contentDescription = targetProfile.name,
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(RomanticViolet.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = targetProfile?.name?.take(1) ?: "👤",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = RomanticViolet
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(
                                                    text = targetProfile?.name ?: "User (${req.receiverId.take(8)})",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Sent " + formatTimestamp(req.timestamp) + " • Pending",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { onCancelRequest(req.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp).testTag("btn_cancel_request_${req.id}")
                                        ) {
                                            Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Likes Sent List
                    if (likesSwipes.isEmpty()) {
                        EmptyStateCard(
                            message = "No likes sent yet. Swipe right on discovery profiles to like them!"
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            likesSwipes.forEach { swipe ->
                                val targetProfile = profileMap[swipe.profileId]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = LikeGreen.copy(alpha = 0.15f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = "Like",
                                                    tint = LikeGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = targetProfile?.name ?: "Profile (${swipe.profileId.take(8)})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Liked via Right Swipe • " + formatTimestamp(swipe.timestamp),
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = LikeGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Liked ❤️",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LikeGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Superlikes Sent List
                    if (superlikesSwipes.isEmpty()) {
                        EmptyStateCard(
                            message = "No super likes sent yet. Swipe up on discovery profiles to super like!"
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            superlikesSwipes.forEach { swipe ->
                                val targetProfile = profileMap[swipe.profileId]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = SuperLikeBlue.copy(alpha = 0.15f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Superlike",
                                                    tint = SuperLikeBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = targetProfile?.name ?: "Profile (${swipe.profileId.take(8)})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Super Liked via Top Swipe • " + formatTimestamp(swipe.timestamp),
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SuperLikeBlue.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Super Like ⭐",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SuperLikeBlue,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Accepted Friends List
                    if (acceptedFriends.isEmpty()) {
                        EmptyStateCard(
                            message = "No accepted mutual friends yet. Connect and accept friendship requests to chat!"
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            acceptedFriends.forEach { friendship ->
                                val friendId = if (friendship.senderId == "current_user") friendship.receiverId else friendship.senderId
                                val friendProfile = profileMap[friendId]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (friendProfile?.avatarUrl?.isNotBlank() == true) {
                                                AsyncImage(
                                                    model = friendProfile.avatarUrl,
                                                    contentDescription = friendProfile.name,
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(LikeGreen.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = friendProfile?.name?.take(1) ?: "🤝",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = LikeGreen
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(
                                                    text = friendProfile?.name ?: "Friend ($friendId)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Friends since " + formatTimestamp(friendship.acceptedTimestamp ?: friendship.timestamp),
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { onOpenChat(friendId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = LikeGreen),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp).testTag("btn_chat_friend_$friendId")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Chat,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = Color.Black
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Recently"
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}
