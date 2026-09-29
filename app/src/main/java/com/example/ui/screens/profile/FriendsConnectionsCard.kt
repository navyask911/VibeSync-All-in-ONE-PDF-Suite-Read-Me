package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FriendshipRequestEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.UserPreferencesEntity
import com.example.ui.theme.VibeSyncTeal

@Composable
fun FriendsConnectionsCard(
    preferences: UserPreferencesEntity,
    acceptedFriends: List<FriendshipRequestEntity>,
    allProfiles: List<ProfileEntity>,
    mutualFriendsMap: Map<String, List<ProfileEntity>>,
    onToggleHideFriends: (Boolean) -> Unit,
    onOpenFriendsList: () -> Unit,
    onOpenChat: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val totalFriends = acceptedFriends.size
    val statusText = "$totalFriends Connection${if (totalFriends != 1) "s" else ""} • Chat Active"

    ProfileSectionDrawer(
        title = "Friends & Mutual Connections",
        icon = Icons.Default.People,
        iconTint = Color(0xFF1565C0),
        iconBackground = Color(0xFF1565C0).copy(alpha = 0.15f),
        statusBadgeText = statusText,
        statusBadgeColor = Color(0xFF1565C0),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        testTag = "drawer_friends_connections"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Privacy Switch for Friends List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (preferences.hideFriendsList) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (preferences.hideFriendsList) Color(0xFF1565C0) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hide Friends List from Public",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (preferences.hideFriendsList) "Private: Only you can see your friends list" else "Visible to mutual connections",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = preferences.hideFriendsList,
                    onCheckedChange = { hide ->
                        onToggleHideFriends(hide)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF1565C0)
                    ),
                    modifier = Modifier.testTag("switch_hide_friends_profile")
                )
            }

            // Accepted Friends with Instant Chat Action
            if (acceptedFriends.isNotEmpty()) {
                Text(
                    text = "My Friends (${acceptedFriends.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    acceptedFriends.take(4).forEach { friendReq ->
                        val targetId = if (friendReq.senderId == "user_me") friendReq.receiverId else friendReq.senderId
                        val prof = allProfiles.find { it.id == targetId }
                        val name = prof?.name ?: friendReq.senderName
                        val emoji = prof?.avatarEmoji ?: friendReq.senderAvatarEmoji
                        val colorStart = prof?.gradientColorStart ?: friendReq.senderColorStart
                        val colorEnd = prof?.gradientColorEnd ?: friendReq.senderColorEnd
                        val city = prof?.city ?: "Nearby"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(Color(colorStart), Color(colorEnd))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji.ifBlank { name.take(1) },
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$city • Connected 🤝",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { onOpenChat(targetId) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("btn_chat_friend_$targetId")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Chat",
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Mutual Connections
            val allMutuals = mutualFriendsMap.values.flatten().distinctBy { it.id }
            if (allMutuals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mutual Connections (${allMutuals.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allMutuals.take(3).forEach { mutualProf ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(VibeSyncTeal.copy(alpha = 0.08f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(
                                                    Color(mutualProf.gradientColorStart),
                                                    Color(mutualProf.gradientColorEnd)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mutualProf.avatarEmoji.ifBlank { mutualProf.name.take(1) },
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = mutualProf.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${mutualProf.city} • Mutual Friend 👥",
                                        fontSize = 11.sp,
                                        color = VibeSyncTeal
                                    )
                                }
                            }

                            Button(
                                onClick = { onOpenChat(mutualProf.id) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VibeSyncTeal),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("btn_chat_mutual_${mutualProf.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Chat",
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Open Full Friends List Dialog Button
            OutlinedButton(
                onClick = onOpenFriendsList,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("btn_open_full_friends_dialog"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Full Friends & Requests Directory", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
