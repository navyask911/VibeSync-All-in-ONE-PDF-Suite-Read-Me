package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.FriendshipRequestEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.SwipeEntity
import com.example.ui.theme.CoralPink
import com.example.ui.theme.PassRed
import com.example.ui.theme.RomanticViolet
import com.example.ui.theme.VibeSyncTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionsAndRequestsModal(
    pendingReceivedRequests: List<FriendshipRequestEntity>,
    pendingSentRequests: List<FriendshipRequestEntity>,
    receivedLikes: List<ProfileEntity>,
    allSwipes: List<SwipeEntity>,
    allProfiles: List<ProfileEntity>,
    onAcceptFriendRequest: (String) -> Unit,
    onCancelFriendRequest: (String) -> Unit,
    onLikeBack: (ProfileEntity) -> Unit = {},
    onOpenChat: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    // 0: Received Requests, 1: Sent Requests, 2: Likes & Superlikes
    var mainTab by remember { mutableIntStateOf(0) }
    // Inside Likes & Superlikes: 0: Received Likes, 1: Sent Likes/Superlikes
    var likesSubTab by remember { mutableIntStateOf(0) }

    val profileMap = remember(allProfiles) { allProfiles.associateBy { it.id } }

    val sentSwipes = remember(allSwipes) {
        allSwipes.filter { it.direction == "LIKE" || it.direction == "SUPER_LIKE" }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("modal_interactions_and_requests"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VibeSyncTeal.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = VibeSyncTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Requests & Interactions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Friendships, Sent & Received Likes",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .testTag("btn_close_interactions_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Top Tabs
                val tabItems = listOf(
                    Triple(0, "Received", pendingReceivedRequests.size),
                    Triple(1, "Sent", pendingSentRequests.size),
                    Triple(2, "Likes & Stars", receivedLikes.size + sentSwipes.size)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabItems.forEach { (index, title, count) ->
                            val isSelected = mainTab == index
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) VibeSyncTeal else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { mainTab = index }
                                    .testTag("tab_interactions_$index")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color.White else VibeSyncTeal,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$count",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) VibeSyncTeal else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content for selected tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (mainTab) {
                        0 -> {
                            // Received Friend Requests
                            ReceivedRequestsTab(
                                requests = pendingReceivedRequests,
                                profileMap = profileMap,
                                onAccept = onAcceptFriendRequest,
                                onCancel = onCancelFriendRequest
                            )
                        }
                        1 -> {
                            // Sent Friend Requests
                            SentRequestsTab(
                                requests = pendingSentRequests,
                                profileMap = profileMap,
                                onCancel = onCancelFriendRequest
                            )
                        }
                        2 -> {
                            // Likes & Superlikes (Dedicated Sub-tabs)
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Sub-tab chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = likesSubTab == 0,
                                        onClick = { likesSubTab = 0 },
                                        label = {
                                            Text(
                                                "Received Likes (${receivedLikes.size})",
                                                fontWeight = if (likesSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CoralPink.copy(alpha = 0.15f),
                                            selectedLabelColor = CoralPink,
                                            selectedLeadingIconColor = CoralPink
                                        )
                                    )

                                    FilterChip(
                                        selected = likesSubTab == 1,
                                        onClick = { likesSubTab = 1 },
                                        label = {
                                            Text(
                                                "Sent Likes (${sentSwipes.size})",
                                                fontWeight = if (likesSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00B0FF).copy(alpha = 0.15f),
                                            selectedLabelColor = Color(0xFF00B0FF),
                                            selectedLeadingIconColor = Color(0xFF00B0FF)
                                        )
                                    )
                                }

                                if (likesSubTab == 0) {
                                    ReceivedLikesContent(
                                        likes = receivedLikes,
                                        onLikeBack = onLikeBack,
                                        onOpenChat = onOpenChat
                                    )
                                } else {
                                    SentLikesContent(
                                        swipes = sentSwipes,
                                        profileMap = profileMap,
                                        onOpenChat = onOpenChat
                                    )
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
private fun ReceivedRequestsTab(
    requests: List<FriendshipRequestEntity>,
    profileMap: Map<String, ProfileEntity>,
    onAccept: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyInteractionsView(
            icon = Icons.Default.PersonAdd,
            title = "No Pending Received Requests",
            description = "When other users send you a friend request from the Connect deck, they will appear here with instant Accept or Decline options."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(requests, key = { it.id }) { req ->
                val profile = profileMap[req.senderId]
                val name = profile?.name?.ifBlank { req.senderName } ?: req.senderName
                val avatar = profile?.avatarUrl ?: ""
                val emoji = profile?.avatarEmoji?.ifBlank { req.senderAvatarEmoji } ?: req.senderAvatarEmoji
                val city = profile?.city ?: "Nearby"

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("card_received_request_${req.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar
                            if (avatar.isNotBlank()) {
                                AsyncImage(
                                    model = avatar,
                                    contentDescription = name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(46.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(VibeSyncTeal.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji.ifBlank { name.take(1) }, fontSize = 20.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$city • Wants to be Friends 🤝",
                                    fontSize = 11.5.sp,
                                    color = VibeSyncTeal,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (req.senderBio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "\"${req.senderBio}\"",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons: Accept & Cancel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onCancel(req.id) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PassRed),
                                border = BorderStroke(1.dp, PassRed.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("btn_cancel_request_${req.id}")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { onAccept(req.id) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VibeSyncTeal),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("btn_accept_request_${req.id}")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SentRequestsTab(
    requests: List<FriendshipRequestEntity>,
    profileMap: Map<String, ProfileEntity>,
    onCancel: (String) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyInteractionsView(
            icon = Icons.AutoMirrored.Filled.Send,
            title = "No Pending Sent Requests",
            description = "You have no pending outgoing friend requests. When you swipe down or send a request from Connect, track its status here."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(requests, key = { it.id }) { req ->
                val profile = profileMap[req.receiverId]
                val name = profile?.name ?: "VibeSync Member"
                val avatar = profile?.avatarUrl ?: ""
                val city = profile?.city ?: "Nearby"

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("card_sent_request_${req.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (avatar.isNotBlank()) {
                                AsyncImage(
                                    model = avatar,
                                    contentDescription = name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF00B0FF).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name.take(1), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFF9800).copy(alpha = 0.15f)) {
                                        Text("⏳ Pending", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(city, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Button(
                            onClick = { onCancel(req.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = PassRed.copy(alpha = 0.12f), contentColor = PassRed),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("btn_cancel_sent_${req.id}")
                        ) {
                            Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivedLikesContent(
    likes: List<ProfileEntity>,
    onLikeBack: (ProfileEntity) -> Unit,
    onOpenChat: (String) -> Unit
) {
    if (likes.isEmpty()) {
        EmptyInteractionsView(
            icon = Icons.Default.Favorite,
            title = "No Incoming Likes Yet",
            description = "Profiles who like or superlike you from Connect will appear here. You can like back to instantly start chatting!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(likes, key = { it.id }) { prof ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, CoralPink.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().testTag("card_received_like_${prof.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (prof.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = prof.avatarUrl,
                                    contentDescription = prof.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(46.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(46.dp).clip(CircleShape).background(CoralPink.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(prof.avatarEmoji.ifBlank { prof.name.take(1) }, fontSize = 20.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(prof.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${prof.age} yrs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "Liked your profile ❤️",
                                    fontSize = 11.5.sp,
                                    color = CoralPink,
                                    fontWeight = FontWeight.Medium
                                )
                                if (prof.bio.isNotBlank()) {
                                    Text(
                                        prof.bio.take(30) + if (prof.bio.length > 30) "..." else "",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onLikeBack(prof) },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp).testTag("btn_like_back_${prof.id}")
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Like Back", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SentLikesContent(
    swipes: List<SwipeEntity>,
    profileMap: Map<String, ProfileEntity>,
    onOpenChat: (String) -> Unit
) {
    if (swipes.isEmpty()) {
        EmptyInteractionsView(
            icon = Icons.AutoMirrored.Filled.Send,
            title = "No Sent Likes or Superlikes",
            description = "Profiles you like (swipe right) or superlike (swipe up) in Connect will be saved here."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(swipes, key = { it.profileId + it.direction }) { swipe ->
                val profile = profileMap[swipe.profileId]
                val name = profile?.name ?: "VibeSync Member"
                val avatar = profile?.avatarUrl ?: ""
                val isSuperlike = swipe.direction == "SUPER_LIKE"

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, if (isSuperlike) Color(0xFF00B0FF).copy(alpha = 0.3f) else CoralPink.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth().testTag("card_sent_like_${swipe.profileId}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (avatar.isNotBlank()) {
                                AsyncImage(
                                    model = avatar,
                                    contentDescription = name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background((if (isSuperlike) Color(0xFF00B0FF) else CoralPink).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name.take(1), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSuperlike) Color(0xFF00B0FF).copy(alpha = 0.15f) else CoralPink.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isSuperlike) "⭐ Superliked" else "❤️ Liked",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSuperlike) Color(0xFF00B0FF) else CoralPink,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { onOpenChat(swipe.profileId) },
                            colors = ButtonDefaults.buttonColors(containerColor = VibeSyncTeal),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("btn_chat_sent_like_${swipe.profileId}")
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInteractionsView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
