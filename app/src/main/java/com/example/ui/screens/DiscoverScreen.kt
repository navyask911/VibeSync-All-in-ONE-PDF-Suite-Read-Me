package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import coil.compose.AsyncImage
import com.example.data.model.BusinessEntity
import com.example.ui.DatingViewModel
import com.example.ui.theme.VibeSyncTeal
import com.example.ui.components.BusinessCard
import com.example.ui.components.BusinessHubDialog
import com.example.ui.components.BusinessProfileDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.util.AdManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FriendshipRequestEntity
import com.example.data.model.ProfileEntity
import com.example.ui.components.DatingActionButton
import com.example.ui.components.DatingCard
import com.example.ui.theme.BoostPurple
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.PassRed
import com.example.ui.theme.RewindGold
import com.example.ui.theme.RomanticViolet
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.theme.SuperLikeBlue
import kotlinx.coroutines.launch

private data class BannerItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun DiscoverScreen(
    candidates: List<ProfileEntity>,
    onSwipe: (profileId: String, direction: String) -> Unit,
    onRewind: () -> Unit,
    onOpenProfileDetail: (ProfileEntity) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenAdmin: () -> Unit = {},
    onResetData: () -> Unit,
    superLikesCount: Int = 5,
    activeDatingProfile: ProfileEntity? = null,
    onOpenActiveChat: () -> Unit = {},
    onBreakupActiveMatch: () -> Unit = {},
    isOpenForDating: Boolean = true,
    onEnableOpenForDating: () -> Unit = {},
    mutualFriendsMap: Map<String, List<ProfileEntity>> = emptyMap(),
    onOpenReviewRequest: (FriendshipRequestEntity) -> Unit = {},
    onOpenInteractionsModal: () -> Unit = {},
    userPreferences: com.example.data.model.UserPreferencesEntity? = null,
    onUpdatePreferences: (com.example.data.model.UserPreferencesEntity) -> Unit = {},
    likedMeProfiles: List<ProfileEntity> = emptyList(),
    sharedBreakupCounts: Set<String> = emptySet(),
    pendingBreakupRequests: Set<String> = emptySet(),
    onRequestBreakupShare: ((ProfileEntity) -> Unit)? = null,
    viewModel: DatingViewModel? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showPreferencesDialog by remember { mutableStateOf(false) }
    var dismissedRequestIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var followedBusinessIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    val pendingFriendRequestsState = viewModel?.pendingFriendRequests?.collectAsState()
    val pendingRequests = pendingFriendRequestsState?.value ?: emptyList()

    // Business & Channels Integration
    val allBusinessesState = viewModel?.allBusinesses?.collectAsState()
    val allBusinesses = allBusinessesState?.value ?: emptyList()
    val sortedNearestBusinesses = remember(allBusinesses) {
        allBusinesses.sortedBy { it.distanceKm }
    }

    var swipeCounter by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    var showInjectedBusinessCard by rememberSaveable { mutableStateOf(false) }
    var activeInjectedBusiness by remember { mutableStateOf<BusinessEntity?>(null) }
    var selectedBusinessForDetail by remember { mutableStateOf<BusinessEntity?>(null) }
    var showBusinessHubDialog by rememberSaveable { mutableStateOf(false) }
    var businessHubInitialTab by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }

    // Intercept phone back button to dismiss preferences dialog if open
    BackHandler(enabled = showPreferencesDialog) {
        showPreferencesDialog = false
    }

    // Combine all request sources into a single banner list (Friendship, Like, Superlike)
    val bannerItems = remember(pendingRequests, likedMeProfiles, dismissedRequestIds) {
        val list = mutableListOf<BannerItem>()

        // 1. Friendship requests
        pendingRequests.forEach { req ->
            val uniqueId = "friend_${req.id}"
            if (uniqueId !in dismissedRequestIds) {
                list.add(
                    BannerItem(
                        id = uniqueId,
                        title = "Friendship Request from ${req.senderName}",
                        subtitle = "Swiped down in Connect. Tap to view profile & bio.",
                        icon = "🤝",
                        color = Color(0xFF1565C0),
                        onClick = { onOpenReviewRequest(req) }
                    )
                )
            }
        }

        // 2. Superlike requests
        likedMeProfiles.filter { it.isSuperLikedMe }.forEach { profile ->
            val uniqueId = "superlike_${profile.id}"
            if (uniqueId !in dismissedRequestIds) {
                list.add(
                    BannerItem(
                        id = uniqueId,
                        title = "Super Like from ${profile.name} 🌟",
                        subtitle = "Wants to stand out! Tap to see if you match.",
                        icon = "⭐",
                        color = Color(0xFF6200EE),
                        onClick = { onOpenProfileDetail(profile) }
                    )
                )
            }
        }

        // 3. Normal like requests
        likedMeProfiles.filter { !it.isSuperLikedMe }.forEach { profile ->
            val uniqueId = "like_${profile.id}"
            if (uniqueId !in dismissedRequestIds) {
                list.add(
                    BannerItem(
                        id = uniqueId,
                        title = "Like from ${profile.name}",
                        subtitle = "Someone is interested in you! Tap to reveal and match.",
                        icon = "💖",
                        color = Color(0xFFFF4757),
                        onClick = { onOpenProfileDetail(profile) }
                    )
                )
            }
        }

        list
    }

    if (showPreferencesDialog) {
        val prefs = userPreferences ?: com.example.data.model.UserPreferencesEntity()
        var tempMinAge by remember { mutableFloatStateOf(prefs.minAge.toFloat()) }
        var tempMaxAge by remember { mutableFloatStateOf(prefs.maxAge.toFloat()) }
        var tempMaxDistance by remember { mutableFloatStateOf(prefs.maxDistanceMiles.toFloat()) }
        var tempGender by remember { mutableStateOf(prefs.userInterestedIn.ifBlank { "Any" }) }
        var tempIsOpenForDating by remember { mutableStateOf(prefs.isOpenForDating) }
        var tempOnlyVerified by remember { mutableStateOf(prefs.onlyVerified) }
        var tempIsProfileLocked by remember { mutableStateOf(prefs.isProfileLocked) }

        AlertDialog(
            onDismissRequest = { showPreferencesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = CoralPink)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Connect Preferences", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Interested In Gender Filter
                    Column {
                        Text("I'm interested in meeting:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Any", "Women", "Men").forEach { option ->
                                FilterChip(
                                    selected = tempGender.equals(option, ignoreCase = true),
                                    onClick = { tempGender = option },
                                    label = { Text(option, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    // Age Range
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Age Range Preference:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${tempMinAge.toInt()} - ${tempMaxAge.toInt()} yrs", fontWeight = FontWeight.Bold, color = CoralPink, fontSize = 13.sp)
                        }
                        RangeSlider(
                            value = tempMinAge..tempMaxAge,
                            onValueChange = { range ->
                                tempMinAge = range.start
                                tempMaxAge = range.endInclusive
                            },
                            valueRange = 18f..80f,
                            steps = 62,
                            modifier = Modifier.fillMaxWidth().testTag("slider_age_range")
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    // Maximum Distance
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Maximum Distance:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${tempMaxDistance.toInt()} miles", fontWeight = FontWeight.Bold, color = CoralPink, fontSize = 13.sp)
                        }
                        Slider(
                            value = tempMaxDistance,
                            onValueChange = { tempMaxDistance = it },
                            valueRange = 1f..100f,
                            steps = 99,
                            modifier = Modifier.fillMaxWidth().testTag("slider_max_distance")
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    // Open for Dating Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Open for Connect other People inside Vibesync 🤝", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Show your profile in the Connect deck", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = tempIsOpenForDating,
                            onCheckedChange = { tempIsOpenForDating = it }
                        )
                    }

                    // Verified Profiles Only Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Verified Profiles Only ⚡", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Show only 3D Liveness face-verified people", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = tempOnlyVerified,
                            onCheckedChange = { tempOnlyVerified = it }
                        )
                    }

                    // Profile Lock Privacy Shield
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Profile Lock Privacy Shield 🔒", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Shield phone and exact street location", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = tempIsProfileLocked,
                            onCheckedChange = { tempIsProfileLocked = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdatePreferences(
                            prefs.copy(
                                userInterestedIn = tempGender,
                                minAge = tempMinAge.toInt(),
                                maxAge = tempMaxAge.toInt(),
                                maxDistanceMiles = tempMaxDistance.toInt(),
                                isOpenForDating = tempIsOpenForDating,
                                onlyVerified = tempOnlyVerified,
                                isProfileLocked = tempIsProfileLocked
                            )
                        )
                        showPreferencesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                    modifier = Modifier.testTag("btn_save_connect_preferences")
                ) {
                    Text("Save Preferences ✨")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreferencesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Title & Verified Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VibeSync",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.linearGradient(listOf(CoralPink, RomanticViolet, Color(0xFF00F2FE))),
                        letterSpacing = (-0.5).sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LikeGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "100% GENUINE",
                        color = LikeGreen,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Right Actions: Super Likes, Admin & Filters
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Daily Super Likes Indicator Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuperLikeBlue.copy(alpha = 0.15f),
                    modifier = Modifier.testTag("btn_super_likes_counter")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = SuperLikeBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userPreferences?.remainingSuperLikesToday ?: 1}/1 ⭐",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SuperLikeBlue
                        )
                    }
                }

                IconButton(
                    onClick = onOpenInteractionsModal,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VibeSyncTeal.copy(alpha = 0.15f))
                        .testTag("btn_interactions_modal_discover")
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Requests & Interactions",
                        tint = VibeSyncTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { showPreferencesDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("btn_discovery_filters")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Preferences and Filters",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Daily Quotas & Gender Referral Lock Status Indicator Bar
        val remainingLikes = userPreferences?.remainingLikesToday ?: 5
        val remainingSuperLikes = userPreferences?.remainingSuperLikesToday ?: 1
        val remainingRequests = userPreferences?.remainingFriendRequestsToday ?: 100
        val targetGender = userPreferences?.userInterestedIn ?: "Any"

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            onClick = { showPreferencesDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .testTag("surface_connect_preferences")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚙️ ", fontSize = 11.sp)
                    Text(
                        text = "Preference: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (targetGender.isBlank()) "Any" else targetGender,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralPink
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "✏️ Edit",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "⭐ $remainingSuperLikes/1",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuperLikeBlue
                    )
                    Text(
                        text = "💚 $remainingLikes/5",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = LikeGreen
                    )
                    Text(
                        text = "🤝 $remainingRequests/100",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                }
            }
        }

        // Connect Feature Quick Tag ("Join with Us")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                onClick = {
                    businessHubInitialTab = 1
                    showBusinessHubDialog = true
                },
                modifier = Modifier.testTag("btn_connect_add_with_us")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Join with Us 🤝", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                }
            }
        }

        // Restore Hidden Banners (Open/Close requests mechanism)
        if (dismissedRequestIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    onClick = { dismissedRequestIds = emptySet() },
                    modifier = Modifier.testTag("btn_restore_dismissed_banners")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔄 Restore Hidden Banners (${dismissedRequestIds.size})",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Combined Pending Banner (Friendship, Like, Superlike) with drop-hide close dismiss
        if (bannerItems.isNotEmpty()) {
            val banner = bannerItems.first()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("banner_${banner.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = banner.color.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { banner.onClick() }
                    ) {
                        Text(banner.icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = banner.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = banner.color
                            )
                            Text(
                                text = banner.subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = banner.color,
                            modifier = Modifier.clickable { banner.onClick() }
                        ) {
                            Text(
                                text = "Review",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = { dismissedRequestIds = dismissedRequestIds + banner.id },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = banner.color,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Swiping Deck Area OR 1-on-1 Monogamous Active Connection Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (!isOpenForDating) {
                // OPEN FOR DATING CLOSED / PAUSED STATE
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .padding(vertical = 16.dp)
                        .testTag("card_open_for_dating_paused"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CoralPink.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "⏸️",
                                fontSize = 42.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Connect Discovery is Paused",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "You have closed 'Open for Connect other People inside Vibesync'. While paused, member cards are hidden and other verified users cannot discover your profile.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CoralPink.copy(alpha = 0.10f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✨ Turn on Open for Connect other People inside Vibesync to meet genuine people within 100 miles!",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = CoralPink,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = onEnableOpenForDating,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_enable_open_for_dating")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enable Open for Connect other People inside Vibesync 🤝",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else if (activeDatingProfile != null) {
                // EXCLUSIVE 1-ON-1 ACTIVE DATING CONNECTION LOCK SCREEN
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .padding(vertical = 12.dp)
                        .testTag("active_dating_lock_card"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Badge
                        Surface(
                            shape = CircleShape,
                            color = CoralPink.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔒", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "1-on-1 Romantic Dating Active",
                                    color = CoralPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Partner Avatar
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(activeDatingProfile.gradientColorStart),
                                            Color(activeDatingProfile.gradientColorEnd)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(activeDatingProfile.avatarEmoji, fontSize = 46.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Connected with ${activeDatingProfile.name}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = "${activeDatingProfile.occupation} • ${activeDatingProfile.city}, ${activeDatingProfile.countryFlag}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Ethics & Transparency Stats Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Relationship Ethics & Accountability:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = CoralPink
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• One Person, One Chat: In VibeSync, multiple dating chats are paused to protect relationship values and romantic focus.\n• Accountability Record: ${activeDatingProfile.name} has 💔 ${activeDatingProfile.breakupCount} past breakups & 👥 ${activeDatingProfile.friendsCount} friends.\n• Once Breakup is done, discovery deck is immediately unlocked for both parties.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Open Exclusive Chat Button
                        Button(
                            onClick = onOpenActiveChat,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_open_exclusive_chat")
                        ) {
                            Text(
                                text = "Open Exclusive Chat (VibeSync E2EE) 💬",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Respectfully Break Up Button
                        androidx.compose.material3.OutlinedButton(
                            onClick = onBreakupActiveMatch,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_ethical_breakup")
                        ) {
                            Text(
                                text = "Respectfully Break Up & Unlock Discover 💔",
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else if (candidates.isEmpty()) {
                // Empty Deck State
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CoralPink.copy(alpha = 0.12f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🎉", fontSize = 28.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "You've Seen Everyone Nearby!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "VibeSync is 100% Free with no subscriptions! Supported by local verified business partners.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "🚀 Featured Partners to Follow",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CoralPink,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "Follow premium verified business accounts in India for free double-date discounts and event passes!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 10.dp),
                            textAlign = TextAlign.Start
                        )

                        // Nearest Indian Business & Partner Listings
                        val displayBusinesses = if (sortedNearestBusinesses.isNotEmpty()) sortedNearestBusinesses else emptyList()

                        // Add with Us Callout Button
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                            onClick = {
                                businessHubInitialTab = 1
                                showBusinessHubDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .testTag("btn_empty_deck_add_with_us")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2E7D32).copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Are you a business owner?", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Color(0xFF1B5E20))
                                    Text("Create your business profile under 'Join with Us' & broadcast offers", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Join +", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                            }
                        }

                        displayBusinesses.forEach { biz ->
                            val isFollowed = biz.isFollowed
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedBusinessForDetail = biz
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = when (biz.category) {
                                                    "Cafe & Dining" -> "☕"
                                                    "Nightlife & Bar" -> "🍹"
                                                    "Entertainment" -> "🎟️"
                                                    "Fitness & Wellness" -> "🏋️"
                                                    "Salon & Spa" -> "💇"
                                                    else -> "🏢"
                                                },
                                                fontSize = 20.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = biz.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (biz.isVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("✓", color = Color(0xFF00BFA5), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                        Text(
                                            text = "${biz.category} • ${biz.city} • ${biz.distanceKm} km away",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (biz.activeOfferSummary.isNotBlank()) {
                                            Text(
                                                text = "🏷️ ${biz.activeOfferSummary}",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CoralPink,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            viewModel?.toggleFollowBusiness(biz.id)
                                            // Once user follow button pressed, user can view whole business profile and timeline
                                            selectedBusinessForDetail = biz
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFollowed) LikeGreen else CoralPink
                                        ),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = if (isFollowed) "Following ✓" else "Follow",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Render bottom peek card (if at least 2 candidates)
                if (candidates.size > 1) {
                    val nextProfile = candidates[1]
                    key(nextProfile.id) {
                        DatingCard(
                            profile = nextProfile,
                            isTopCard = false,
                            onSwipe = {},
                            onInfoClick = {},
                            mutualFriends = mutualFriendsMap[nextProfile.id] ?: emptyList(),
                            isBreakupShared = nextProfile.id in sharedBreakupCounts,
                            isBreakupPending = nextProfile.id in pendingBreakupRequests,
                            onRequestBreakupShare = { onRequestBreakupShare?.invoke(nextProfile) },
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(0.95f)
                                .padding(bottom = 12.dp)
                        )
                    }
                }

                // Render top active card
                val topProfile = candidates[0]
                val currentAd = AdManager.getAdForDisplay()

                if (currentAd != null) {
                    // Render Responsive Verified Local Sponsor Card in Deck (Non-Intrusive, 0 Mandatory Watch)
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("sponsored_ad_card"),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CoralPink.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "📢 SPONSORED",
                                                color = CoralPink,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentAd.sponsorName,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = { AdManager.dismissAd() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Instant Skip (0 sec wait)",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Responsive Media Asset (Image or Video)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (currentAd.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = currentAd.imageUrl,
                                        contentDescription = currentAd.title,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = CoralPink,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }

                                if (currentAd.mediaType == "VIDEO") {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("HD Video Ad (≤30s)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Non-Intrusive free skip badge
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Instant Skip ✕",
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Headline & Description
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentAd.title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentAd.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                    maxLines = 3
                                )
                            }

                            // Action CTA + Skip Row
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        AdManager.recordAdClick(currentAd.id)
                                        when (currentAd.actionType) {
                                            "BUSINESS_PROFILE" -> {
                                                val biz = allBusinesses.firstOrNull { it.id == currentAd.businessId || it.name.contains(currentAd.sponsorName, ignoreCase = true) }
                                                if (biz != null) {
                                                    selectedBusinessForDetail = biz
                                                } else if (currentAd.targetUrl.isNotBlank()) {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentAd.targetUrl))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Opening ${currentAd.sponsorName}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            "DIRECT_CHAT" -> {
                                                Toast.makeText(context, "Opening Direct Partner Chat with ${currentAd.sponsorName} 💬", Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                if (currentAd.targetUrl.isNotBlank()) {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentAd.targetUrl))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Opening ${currentAd.targetUrl}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                        AdManager.dismissAd()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("btn_ad_cta_redirect")
                                ) {
                                    Text(
                                        text = "${currentAd.actionText} 🚀",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = { AdManager.dismissAd() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("btn_skip_ad_instant")
                                ) {
                                    Text("Skip Ad & Resume Swiping", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                } else if (showInjectedBusinessCard && activeInjectedBusiness != null) {
                    val currentBiz = allBusinesses.firstOrNull { it.id == activeInjectedBusiness?.id } ?: activeInjectedBusiness!!
                    key("business_${currentBiz.id}") {
                        BusinessCard(
                            business = currentBiz,
                            onFollowClick = {
                                viewModel?.toggleFollowBusiness(currentBiz.id)
                                selectedBusinessForDetail = currentBiz
                            },
                            onViewProfileClick = {
                                selectedBusinessForDetail = currentBiz
                            },
                            onSwipePass = {
                                showInjectedBusinessCard = false
                            },
                            onSwipeFollow = {
                                viewModel?.toggleFollowBusiness(currentBiz.id)
                                selectedBusinessForDetail = currentBiz
                                showInjectedBusinessCard = false
                            },
                            isSwipable = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    key(topProfile.id) {
                        DatingCard(
                            profile = topProfile,
                            isTopCard = true,
                            onSwipe = { direction ->
                                AdManager.recordCardSwipe()
                                swipeCounter++
                                if (swipeCounter % 5 == 0 && sortedNearestBusinesses.isNotEmpty()) {
                                    val bizIndex = ((swipeCounter / 5) - 1) % sortedNearestBusinesses.size
                                    activeInjectedBusiness = sortedNearestBusinesses[bizIndex]
                                    showInjectedBusinessCard = true
                                }
                                onSwipe(topProfile.id, direction)
                            },
                            onInfoClick = {
                                onOpenProfileDetail(topProfile)
                            },
                            mutualFriends = mutualFriendsMap[topProfile.id] ?: emptyList(),
                            isBreakupShared = topProfile.id in sharedBreakupCounts,
                            isBreakupPending = topProfile.id in pendingBreakupRequests,
                            onRequestBreakupShare = { onRequestBreakupShare?.invoke(topProfile) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Bottom Action Controls (Rewind, Pass, SuperLike, Like, Boost/Filter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Free Unlimited Rewind Button
            DatingActionButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Free Rewind",
                tint = RewindGold,
                onClick = {
                    if (showInjectedBusinessCard) {
                        showInjectedBusinessCard = false
                    } else {
                        onRewind()
                    }
                },
                size = 48.dp,
                iconSize = 22.dp,
                testTag = "btn_action_rewind"
            )

            // Pass Button
            DatingActionButton(
                icon = Icons.Default.Close,
                contentDescription = "Pass",
                tint = PassRed,
                onClick = {
                    if (showInjectedBusinessCard) {
                        showInjectedBusinessCard = false
                    } else if (candidates.isNotEmpty()) {
                        AdManager.recordCardSwipe()
                        swipeCounter++
                        if (swipeCounter % 5 == 0 && sortedNearestBusinesses.isNotEmpty()) {
                            val bizIndex = ((swipeCounter / 5) - 1) % sortedNearestBusinesses.size
                            activeInjectedBusiness = sortedNearestBusinesses[bizIndex]
                            showInjectedBusinessCard = true
                        }
                        onSwipe(candidates[0].id, "PASS")
                    }
                },
                size = 56.dp,
                iconSize = 28.dp,
                testTag = "btn_action_pass"
            )

            // Super Like Button (Freemium: Extra from Rewarded Ad)
            DatingActionButton(
                icon = Icons.Default.Star,
                contentDescription = "Super Like",
                tint = SuperLikeBlue,
                onClick = {
                    if (showInjectedBusinessCard && activeInjectedBusiness != null) {
                        val b = activeInjectedBusiness!!
                        viewModel?.toggleFollowBusiness(b.id)
                        selectedBusinessForDetail = b
                        showInjectedBusinessCard = false
                    } else if (candidates.isNotEmpty()) {
                        AdManager.recordCardSwipe()
                        swipeCounter++
                        if (swipeCounter % 5 == 0 && sortedNearestBusinesses.isNotEmpty()) {
                            val bizIndex = ((swipeCounter / 5) - 1) % sortedNearestBusinesses.size
                            activeInjectedBusiness = sortedNearestBusinesses[bizIndex]
                            showInjectedBusinessCard = true
                        }
                        onSwipe(candidates[0].id, "SUPER_LIKE")
                    }
                },
                size = 48.dp,
                iconSize = 24.dp,
                testTag = "btn_action_superlike"
            )

            // Like Button (Left swipe action)
            DatingActionButton(
                icon = Icons.Default.Favorite,
                contentDescription = "Like",
                tint = LikeGreen,
                onClick = {
                    if (showInjectedBusinessCard && activeInjectedBusiness != null) {
                        val b = activeInjectedBusiness!!
                        viewModel?.toggleFollowBusiness(b.id)
                        selectedBusinessForDetail = b
                        showInjectedBusinessCard = false
                    } else if (candidates.isNotEmpty()) {
                        AdManager.recordCardSwipe()
                        swipeCounter++
                        if (swipeCounter % 5 == 0 && sortedNearestBusinesses.isNotEmpty()) {
                            val bizIndex = ((swipeCounter / 5) - 1) % sortedNearestBusinesses.size
                            activeInjectedBusiness = sortedNearestBusinesses[bizIndex]
                            showInjectedBusinessCard = true
                        }
                        onSwipe(candidates[0].id, "LIKE")
                    }
                },
                size = 56.dp,
                iconSize = 28.dp,
                testTag = "btn_action_like"
            )

            // Ask Friendship Button (Below swipe action 🤝)
            DatingActionButton(
                icon = Icons.Default.PersonAdd,
                contentDescription = "Ask Friendship",
                tint = Color(0xFF00BFA5),
                onClick = {
                    if (showInjectedBusinessCard) {
                        showInjectedBusinessCard = false
                    } else if (candidates.isNotEmpty()) {
                        AdManager.recordCardSwipe()
                        swipeCounter++
                        if (swipeCounter % 5 == 0 && sortedNearestBusinesses.isNotEmpty()) {
                            val bizIndex = ((swipeCounter / 5) - 1) % sortedNearestBusinesses.size
                            activeInjectedBusiness = sortedNearestBusinesses[bizIndex]
                            showInjectedBusinessCard = true
                        }
                        onSwipe(candidates[0].id, "FRIEND_REQUEST")
                    }
                },
                size = 48.dp,
                iconSize = 22.dp,
                testTag = "btn_action_friendship"
            )

            // Filter / Preferences Shortcut
            DatingActionButton(
                icon = Icons.Default.Tune,
                contentDescription = "Filters",
                tint = BoostPurple,
                onClick = { showPreferencesDialog = true },
                size = 48.dp,
                iconSize = 22.dp,
                testTag = "btn_action_filters"
            )
        }
    }

    // Business Hub Dialog ("Add with Us" & Nearest businesses)
    if (showBusinessHubDialog && viewModel != null) {
        BusinessHubDialog(
            viewModel = viewModel,
            initialTab = businessHubInitialTab,
            onDismissRequest = { showBusinessHubDialog = false }
        )
    }

    // Full Business Profile & Timeline Dialog (when follow button or profile is tapped)
    selectedBusinessForDetail?.let { biz ->
        if (viewModel != null) {
            val latestBiz = allBusinesses.firstOrNull { it.id == biz.id } ?: biz
            BusinessProfileDialog(
                business = latestBiz,
                viewModel = viewModel,
                onDismissRequest = { selectedBusinessForDetail = null }
            )
        }
    }
}
