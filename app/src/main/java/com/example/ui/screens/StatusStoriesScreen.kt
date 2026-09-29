package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import com.example.util.AdManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.StatusStoryEntity
import com.example.ui.DatingViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom Colors matching VibeSync Status UI exactly
val VibeSyncGreen = Color(0xFF00A884)
val VibeSyncLightGrey = Color(0xFFE9EDEF)
val VibeSyncDarkText = Color(0xFF111B21)
val VibeSyncSubText = Color(0xFF667781)
val VibeSyncModeBarBg = Color(0xFF1C2329)
val VibeSyncSelectedModeChip = Color(0xFF2B3842)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusStoriesScreen(
    viewModel: DatingViewModel,
    modifier: Modifier = Modifier
) {
    val myStories by viewModel.myStatusStories.collectAsState()
    val friendsStories by viewModel.friendsStatusStories.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()
    val showUploadSheet by viewModel.showStatusUploadSheet.collectAsState()
    val uploadInitialTab by viewModel.statusUploadInitialTab.collectAsState()
    val activeViewingStory by viewModel.activeViewingStory.collectAsState()

    var showTextStatusComposer by remember { mutableStateOf(false) }
    var showCameraAddStatusSheet by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTopOverflowMenu by remember { mutableStateOf(false) }
    var selectedPrivacySetting by remember { mutableStateOf("MY_CONTACTS") }
    var pendingMediaUpload by remember { mutableStateOf<Triple<Uri, String, String>?>(null) } // (mediaUri, mediaType, defaultCaption)

    // Automatically fetch and update the feed with new Stories from mutual contacts upon opening
    LaunchedEffect(Unit) {
        viewModel.fetchMutualFriendsStories()
    }

    // Intercept back button to dismiss overlays cleanly
    BackHandler(enabled = activeViewingStory != null || showTextStatusComposer || showCameraAddStatusSheet || showPrivacyDialog || pendingMediaUpload != null) {
        if (activeViewingStory != null) {
            viewModel.closeStoryViewer()
        } else if (pendingMediaUpload != null) {
            pendingMediaUpload = null
        } else if (showTextStatusComposer) {
            showTextStatusComposer = false
        } else if (showCameraAddStatusSheet) {
            showCameraAddStatusSheet = false
        } else if (showPrivacyDialog) {
            showPrivacyDialog = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // --- TOP APP BAR ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stories",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.fetchMutualFriendsStories() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showTopOverflowMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showTopOverflowMenu,
                                onDismissRequest = { showTopOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Stories privacy", fontSize = 13.5.sp) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        showPrivacyDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Create channel", fontSize = 13.5.sp) },
                                    onClick = { showTopOverflowMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings", fontSize = 13.5.sp) },
                                    onClick = { showTopOverflowMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 1: STORIES ---
            item {
                Text(
                    text = "Stories",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )

                val latestMyStory = myStories.firstOrNull()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (latestMyStory != null) {
                                viewModel.openStoryViewer(latestMyStory)
                            } else {
                                showCameraAddStatusSheet = true
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (latestMyStory?.mediaUrl?.isNotBlank() == true) {
                                AsyncImage(
                                    model = latestMyStory.mediaUrl,
                                    contentDescription = "My stories",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = userPrefs?.userName?.take(1)?.uppercase() ?: "👤",
                                    fontSize = 20.sp
                                )
                            }
                        }

                        // VibeSync style Green + badge on bottom right of avatar
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(VibeSyncGreen)
                                .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Stories",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add Stories",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (latestMyStory != null) {
                                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(latestMyStory.timestamp))
                                "Today, $timeStr"
                            } else {
                                "Disappears after 24 hours"
                            },
                            fontSize = 12.sp,
                            color = VibeSyncSubText
                        )
                    }
                }
            }

            // --- SECTION 2: RECENT STORIES ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Stories",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VibeSyncSubText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            // Live Friends Status Stories from Database / ViewModel
            if (friendsStories.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(VibeSyncLightGrey),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = VibeSyncSubText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No recent stories",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Stories from your mutual contacts will appear here",
                            fontSize = 11.5.sp,
                            color = VibeSyncSubText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(friendsStories) { story ->
                    VibeSyncStatusRow(
                        title = story.userName,
                        subtitle = formatRelativeTime(story.timestamp),
                        avatarEmoji = story.userAvatarEmoji,
                        avatarUrl = story.mediaUrl,
                        isViewed = story.isViewed,
                        onClick = { viewModel.openStoryViewer(story) }
                    )
                }
            }
        }

        // --- FLOATING ACTION BUTTONS ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Upper Pencil/Pen FAB
            FloatingActionButton(
                onClick = { showTextStatusComposer = true },
                containerColor = VibeSyncLightGrey,
                contentColor = Color(0xFF3B4A54),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .size(38.dp)
                    .testTag("fab_pen_text_status")
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Pencil text status",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Lower Camera FAB
            FloatingActionButton(
                onClick = { showCameraAddStatusSheet = true },
                containerColor = VibeSyncGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("fab_camera_add_status")
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Camera add status",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    // --- SCREENSHOT 2: "TYPE A STATUS" SCREEN (Opened when clicking Pen FAB) ---
    if (showTextStatusComposer) {
        TypeAStatusComposerScreen(
            onDismiss = { showTextStatusComposer = false },
            onPostStatus = { text, fontStyle, bgStart, bgEnd ->
                viewModel.uploadStatus(
                    mediaType = "PHOTO",
                    caption = text,
                    mediaUrl = "",
                    videoDurationSeconds = 0,
                    backgroundColorStart = bgStart,
                    backgroundColorEnd = bgEnd,
                    privacy = selectedPrivacySetting
                )
                showTextStatusComposer = false
            },
            onSwitchToCamera = {
                showTextStatusComposer = false
                showCameraAddStatusSheet = true
            }
        )
    }

    // --- SCREENSHOT 3: "ADD STATUS" BOTTOM SHEET / GALLERY PICKER (Opened when clicking Camera FAB) ---
    if (showCameraAddStatusSheet) {
        AddStatusBottomSheet(
            onDismiss = { showCameraAddStatusSheet = false },
            onSelectTextMode = {
                showCameraAddStatusSheet = false
                showTextStatusComposer = true
            },
            onPostMediaStatus = { uri, caption, mediaType ->
                pendingMediaUpload = Triple(uri, mediaType, caption)
                showCameraAddStatusSheet = false
            }
        )
    }

    // --- CAPTION & PREVIEW WINDOW BEFORE UPLOADING ---
    pendingMediaUpload?.let { (uri, mediaType, initialCaption) ->
        StatusCaptionUploadPreviewDialog(
            mediaUri = uri,
            mediaType = mediaType,
            initialCaption = initialCaption,
            onDismiss = { pendingMediaUpload = null },
            onPostStatus = { finalCaption, bgStart, bgEnd ->
                viewModel.uploadStatus(
                    mediaType = mediaType,
                    caption = finalCaption,
                    mediaUrl = uri.toString(),
                    videoDurationSeconds = if (mediaType == "VIDEO") 15 else 0,
                    backgroundColorStart = bgStart,
                    backgroundColorEnd = bgEnd,
                    privacy = selectedPrivacySetting
                )
                pendingMediaUpload = null
            }
        )
    }

    // --- STORIES PRIVACY DIALOG ---
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = VibeSyncGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stories Privacy", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Who can see my stories:",
                        fontSize = 13.5.sp,
                        color = VibeSyncSubText
                    )

                    listOf(
                        "MY_CONTACTS" to "My Contacts",
                        "SELECTED_CONTACTS" to "Selected Contacts",
                        "EVERYONE" to "Everyone"
                    ).forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedPrivacySetting = key }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPrivacySetting == key,
                                onClick = { selectedPrivacySetting = key },
                                colors = RadioButtonDefaults.colors(selectedColor = VibeSyncGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSyncGreen)
                ) {
                    Text("Done")
                }
            }
        )
    }

    // --- FULL-SCREEN STATUS STORY VIEWER ---
    activeViewingStory?.let { story ->
        val allStoriesList = remember(myStories, friendsStories) {
            val base = mutableListOf<StatusStoryEntity>()
            base.addAll(myStories)
            base.addAll(friendsStories)
            val distinct = base.distinctBy { it.id }
            val listWithAds = mutableListOf<StatusStoryEntity>()
            val storyAds = AdManager.getActiveApprovedAds(placement = "STORIES")
            var adIdx = 0

            distinct.forEachIndexed { index, item ->
                listWithAds.add(item)
                // Rule: Automatically play 1 ad story after every 5 user stories viewed (5 stories -> 1 ad story)
                if ((index + 1) % 5 == 0 && storyAds.isNotEmpty()) {
                    val ad = storyAds[adIdx % storyAds.size]
                    adIdx++
                    listWithAds.add(
                        StatusStoryEntity(
                            id = "ad_story_${ad.id}_${index}",
                            userId = ad.businessId.ifBlank { "partner_${ad.id}" },
                            userName = ad.businessName,
                            userAvatarEmoji = "📢",
                            mediaType = if (ad.mediaType == "VIDEO") "VIDEO" else "PHOTO",
                            mediaUrl = if (ad.mediaType == "VIDEO") ad.videoUrl.ifBlank { ad.bannerImageUrl } else ad.bannerImageUrl,
                            videoDurationSeconds = if (ad.mediaType == "VIDEO") ad.videoDurationSeconds else 0,
                            caption = "${ad.headline}\n${ad.description}",
                            backgroundColorStart = 0xFF1A1A2E,
                            backgroundColorEnd = 0xFF16213E,
                            isAd = true,
                            adActionType = ad.actionType,
                            adTargetUrl = ad.targetLinkUrl,
                            adCtaText = ad.ctaText,
                            adBusinessId = ad.businessId,
                            adCampaignId = ad.id
                        )
                    )
                }
            }
            if (listWithAds.isEmpty() && storyAds.isNotEmpty()) {
                val ad = storyAds.first()
                listWithAds.add(
                    StatusStoryEntity(
                        id = "ad_story_${ad.id}_sample",
                        userId = ad.businessId.ifBlank { "partner_${ad.id}" },
                        userName = ad.businessName,
                        userAvatarEmoji = "📢",
                        mediaType = if (ad.mediaType == "VIDEO") "VIDEO" else "PHOTO",
                        mediaUrl = if (ad.mediaType == "VIDEO") ad.videoUrl.ifBlank { ad.bannerImageUrl } else ad.bannerImageUrl,
                        videoDurationSeconds = if (ad.mediaType == "VIDEO") ad.videoDurationSeconds else 0,
                        caption = "${ad.headline}\n${ad.description}",
                        backgroundColorStart = 0xFF1A1A2E,
                        backgroundColorEnd = 0xFF16213E,
                        isAd = true,
                        adActionType = ad.actionType,
                        adTargetUrl = ad.targetLinkUrl,
                        adCtaText = ad.ctaText,
                        adBusinessId = ad.businessId,
                        adCampaignId = ad.id
                    )
                )
            }
            listWithAds
        }
        val initialIndex = remember(story.id, allStoriesList) {
            allStoriesList.indexOfFirst { it.id == story.id }.coerceAtLeast(0)
        }
        StatusStoryViewer(
            story = story,
            allUserStories = if (allStoriesList.isEmpty()) listOf(story) else allStoriesList,
            initialIndex = initialIndex,
            onClose = { viewModel.closeStoryViewer() },
            onDelete = { viewModel.deleteStatusStory(story.id) },
            onToggleLike = { storyId -> viewModel.toggleLikeStatus(storyId) },
            onSendReply = { userId, userName, caption, replyText ->
                viewModel.sendStatusReply(userId, userName, caption, replyText)
            },
            onSendReaction = { userId, userName, caption, emoji ->
                viewModel.sendEmojiReaction(userId, userName, caption, emoji)
            }
        )
    }
}

// Single Status Update Row Component
@Composable
fun VibeSyncStatusRow(
    title: String,
    subtitle: String,
    avatarEmoji: String,
    avatarUrl: String = "",
    isViewed: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            // VibeSync Status Ring around Avatar
            Canvas(modifier = Modifier.size(45.dp)) {
                drawCircle(
                    color = if (isViewed) Color(0xFFD1D7DB) else VibeSyncGreen,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Avatar circle thumbnail
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8EC)),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = avatarEmoji,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = VibeSyncSubText,
                maxLines = 1
            )
        }
    }
}

// --- SCREENSHOT 2 COMPONENT: "TYPE A STATUS" FULL SCREEN COMPOSER ---
@Composable
fun TypeAStatusComposerScreen(
    onDismiss: () -> Unit,
    onPostStatus: (text: String, fontStyle: String, bgStart: Long, bgEnd: Long) -> Unit,
    onSwitchToCamera: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    // Vibrant background colors (default blue #4C7BEE matching Screenshot 2)
    val backgroundColors = remember {
        listOf(
            Pair(0xFF4C7BEE, 0xFF3B5998), // Vibrant Royal Blue (Screenshot 2 exact match)
            Pair(0xFF00A884, 0xFF075E54), // VibeSync Emerald
            Pair(0xFF7B1FA2, 0xFF512DA8), // Deep Purple
            Pair(0xFFC2185B, 0xFFAD1457), // Crimson Pink
            Pair(0xFFE65100, 0xFFF57C00), // Sunset Orange
            Pair(0xFF00796B, 0xFF004D40), // Dark Teal
            Pair(0xFF263238, 0xFF102027)  // Dark Slate
        )
    }
    var activeBgIndex by remember { mutableIntStateOf(0) }

    // Text fonts
    val fontStyles = remember {
        listOf(
            FontFamily.Default to "Sans-serif",
            FontFamily.Serif to "Serif",
            FontFamily.Monospace to "Monospace",
            FontFamily.Cursive to "Cursive"
        )
    }
    var activeFontIndex by remember { mutableIntStateOf(0) }

    val (currentBgStart, currentBgEnd) = backgroundColors[activeBgIndex]
    val (currentFontFamily, fontName) = fontStyles[activeFontIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(currentBgStart), Color(currentBgEnd))))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close 'X' button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Font Selector 'Aa' button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f))
                                .clickable {
                                    activeFontIndex = (activeFontIndex + 1) % fontStyles.size
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aa",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = currentFontFamily
                            )
                        }

                        // Color Palette selector button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f))
                                .clickable {
                                    activeBgIndex = (activeBgIndex + 1) % backgroundColors.size
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Color palette",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Center Input Area ("Type a status" placeholder centered)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (textInput.isEmpty()) {
                        Text(
                            text = "Type a story",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = currentFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }

                    BasicTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = currentFontFamily,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_type_a_status")
                    )
                }

                // Bottom Mode Bar (Video | Photo | Text | Voice)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp, start = 14.dp, end = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(20.dp))
                            .background(VibeSyncModeBarBg.copy(alpha = 0.95f))
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Video",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable(onClick = onSwitchToCamera)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )

                        Text(
                            text = "Photo",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable(onClick = onSwitchToCamera)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )

                        // "Text" chip selected
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(VibeSyncSelectedModeChip)
                                .padding(horizontal = 13.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Text",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Voice",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Bottom Right Post FAB (Send status)
                    if (textInput.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(VibeSyncGreen)
                                .clickable {
                                    onPostStatus(textInput, fontName, currentBgStart, currentBgEnd)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SCREENSHOT 3 COMPONENT: "ADD STATUS" BOTTOM SHEET / GALLERY PICKER ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStatusBottomSheet(
    onDismiss: () -> Unit,
    onSelectTextMode: () -> Unit,
    onPostMediaStatus: (Uri, String, String) -> Unit // (uri, caption, mediaType)
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val sampleMediaThumbnails = remember {
        listOf(
            "https://picsum.photos/seed/stat1/400/400",
            "https://picsum.photos/seed/stat2/400/400",
            "https://picsum.photos/seed/stat3/400/400",
            "https://picsum.photos/seed/stat4/400/400",
            "https://picsum.photos/seed/stat5/400/400",
            "https://picsum.photos/seed/stat6/400/400",
            "https://picsum.photos/seed/stat7/400/400",
            "https://picsum.photos/seed/stat8/400/400"
        )
    }

    // Photo/Video picker launcher from real device
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: ""
            val isVid = mime.startsWith("video") || uri.toString().lowercase().contains("video")
            onPostMediaStatus(uri, "Captured moment 📸", if (isVid) "VIDEO" else "PHOTO")
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            val uri = Uri.parse("data:image/jpeg;base64,$base64")
            onPostMediaStatus(uri, "Camera story 📷", "PHOTO")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission needed to take story photos.", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            // Top Bar: 'X' close button and "Add Stories" title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Add Stories",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Action Circles Row: Text | Music | Layout | Voice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Text Option
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F2F5))
                            .clickable(onClick = onSelectTextMode),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Text",
                            tint = VibeSyncDarkText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text("Text", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                // 2. Music Option
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F2F5))
                            .clickable {
                                Toast.makeText(context, "Music status picker ready!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = "Music",
                            tint = VibeSyncDarkText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text("Music", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                // 3. Layout Option
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F2F5))
                            .clickable {
                                galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "Layout",
                            tint = VibeSyncDarkText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text("Layout", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                // 4. Voice Option
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F2F5))
                            .clickable {
                                Toast.makeText(context, "Voice note status recorder active!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = VibeSyncDarkText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text("Voice", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subheader: Recents ▾ Dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recents",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Recents dropdown",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3-Column Media Grid
            Box(modifier = Modifier.fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    // Tile 1: Camera Tile
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(Color(0xFFF7F9FA))
                                .clickable {
                                    val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasCam) {
                                        cameraLauncher.launch(null)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Camera",
                                    tint = VibeSyncGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Camera",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = VibeSyncGreen
                                )
                            }
                        }
                    }

                    // Remaining Tiles: Gallery Media Thumbnails
                    items(sampleMediaThumbnails) { photoUrl ->
                        val isVideo = photoUrl.contains("video")
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    onPostMediaStatus(Uri.parse(photoUrl), "Photo status update 📸", if (isVideo) "VIDEO" else "PHOTO")
                                }
                        ) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Gallery media",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Floating Folder/Gallery button on bottom right of sheet
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 10.dp, end = 14.dp)
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F2F5))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = "Open device gallery",
                        tint = VibeSyncDarkText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Full Screen Caption & Media Preview Dialog before uploading
@Composable
fun StatusCaptionUploadPreviewDialog(
    mediaUri: Uri?,
    mediaType: String, // "PHOTO" or "VIDEO"
    initialCaption: String = "",
    onDismiss: () -> Unit,
    onPostStatus: (caption: String, bgStart: Long, bgEnd: Long) -> Unit
) {
    var captionText by remember { mutableStateOf(initialCaption) }
    var activeBgIndex by remember { mutableIntStateOf(0) }

    val backgroundColors = remember {
        listOf(
            Pair(0xFF00A884, 0xFF075E54), // VibeSync Emerald
            Pair(0xFF4C7BEE, 0xFF3B5998), // Royal Blue
            Pair(0xFF7B1FA2, 0xFF512DA8), // Deep Purple
            Pair(0xFFC2185B, 0xFFAD1457), // Crimson Pink
            Pair(0xFFE65100, 0xFFF57C00), // Sunset Orange
            Pair(0xFF263238, 0xFF102027)  // Dark Slate
        )
    }

    val (currentBgStart, currentBgEnd) = backgroundColors[activeBgIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(currentBgStart), Color(currentBgEnd))))
                .statusBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Back button, Title & Color palette picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close preview", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (mediaType == "VIDEO") Icons.Default.Videocam else Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (mediaType == "VIDEO") "Video Story Preview" else "Photo Story Preview",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { activeBgIndex = (activeBgIndex + 1) % backgroundColors.size },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.25f))
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "Change background color", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // Middle: Flexible Preview Media fitting dynamically
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (mediaUri != null && mediaUri.toString().isNotBlank()) {
                    AsyncImage(
                        model = mediaUri,
                        contentDescription = "Story media preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // If VIDEO, show Play Icon overlay in center
                    if (mediaType == "VIDEO") {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Caption Bar & Upload Send Button with safe area edge-to-edge window insets
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        placeholder = { Text("Add a caption...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_status_caption"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VibeSyncGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.35f)
                        ),
                        maxLines = 3
                    )

                    IconButton(
                        onClick = {
                            onPostStatus(captionText, currentBgStart, currentBgEnd)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(VibeSyncGreen)
                            .testTag("btn_send_status")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send story", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// Full screen Story Viewer dialog
@Composable
fun StatusStoryViewer(
    story: StatusStoryEntity,
    allUserStories: List<StatusStoryEntity> = listOf(story),
    initialIndex: Int = 0,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onToggleLike: (String) -> Unit = {},
    onSendReply: (storyUserId: String, userName: String, storyCaption: String, replyText: String) -> Unit = { _, _, _, _ -> },
    onSendReaction: (storyUserId: String, userName: String, storyCaption: String, emoji: String) -> Unit = { _, _, _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentIndex by remember(story.id, initialIndex) { mutableIntStateOf(initialIndex) }
    val activeStory = allUserStories.getOrElse(currentIndex) { story }

    // Dynamic impression tracking when an Ad Story enters the viewport
    LaunchedEffect(activeStory.id) {
        if (activeStory.isAd && activeStory.adCampaignId.isNotBlank()) {
            AdManager.recordAdImpression(activeStory.adCampaignId, "STORIES")
        }
    }

    var replyText by remember { mutableStateOf("") }
    var isLiked by remember(activeStory.id, activeStory.isLikedByMe) { mutableStateOf(activeStory.isLikedByMe) }
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }

    val quickReactionEmojis = remember { listOf("❤️", "😂", "😮", "😢", "🙏", "🔥", "👏", "💯") }
    var activeFloatingReaction by remember { mutableStateOf<String?>(null) }

    val storyDurationMs = remember(activeStory.id, activeStory.mediaType, activeStory.videoDurationSeconds) {
        if (activeStory.mediaType == "VIDEO" && activeStory.videoDurationSeconds > 0) {
            (activeStory.videoDurationSeconds * 1000).coerceIn(3000, 30000)
        } else {
            5000
        }
    }

    val remainingSeconds = remember(progress.value, storyDurationMs) {
        val remainingMs = ((1f - progress.value) * storyDurationMs).toLong()
        ((remainingMs / 1000) + 1).coerceAtLeast(1)
    }

    LaunchedEffect(activeFloatingReaction) {
        if (activeFloatingReaction != null) {
            delay(1500)
            activeFloatingReaction = null
        }
    }

    LaunchedEffect(activeStory.id, currentIndex, isPaused) {
        if (!isPaused) {
            progress.snapTo(0f)
            val result = progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = storyDurationMs, easing = LinearEasing)
            )
            // Auto-advance when current story timer reaches completion (1f)
            if (result.endReason == AnimationEndReason.Finished) {
                if (currentIndex < allUserStories.size - 1) {
                    currentIndex++
                } else {
                    onClose()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.99f)
                    .fillMaxHeight(0.98f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(activeStory.backgroundColorStart),
                                Color(activeStory.backgroundColorEnd)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .pointerInput(activeStory.id, currentIndex) {
                        detectTapGestures(
                            onPress = {
                                isPaused = true
                                tryAwaitRelease()
                                isPaused = false
                            },
                            onTap = { offset ->
                                val width = size.width
                                if (offset.x < width * 0.3f) {
                                    // Tap Left 30%: Previous story (Instant skip - zero forced lock)
                                    if (currentIndex > 0) {
                                        currentIndex--
                                    } else {
                                        onClose()
                                    }
                                } else {
                                    // Tap Right 70%: Next story (Instant skip - zero forced lock)
                                    if (currentIndex < allUserStories.size - 1) {
                                        currentIndex++
                                    } else {
                                        onClose()
                                    }
                                }
                            }
                        )
                    }
            ) {
                // Media background/foreground image
                if (activeStory.mediaUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 76.dp, bottom = 125.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = activeStory.mediaUrl,
                            contentDescription = "Story media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentScale = ContentScale.Fit
                        )

                        if (activeStory.mediaType == "VIDEO") {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Video playing",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // Top Header Overlay (Hidden when paused / holding)
                if (!isPaused) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Multi-Segment Visual Progress Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            allUserStories.forEachIndexed { index, _ ->
                                val segmentProgress = when {
                                    index < currentIndex -> 1f
                                    index > currentIndex -> 0f
                                    else -> progress.value
                                }
                                LinearProgressIndicator(
                                    progress = { segmentProgress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(CircleShape),
                                    color = if (activeStory.isAd) Color(0xFFFF4081) else Color.White,
                                    trackColor = Color.White.copy(alpha = 0.35f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (activeStory.isAd) Color(0xFFFF4081).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = activeStory.userAvatarEmoji, fontSize = 16.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = activeStory.userName,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (activeStory.isAd) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFFFF4081),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "SPONSORED",
                                                    color = Color.White,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (activeStory.mediaType == "VIDEO") {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Videocam,
                                                contentDescription = "Video status",
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (activeStory.isAd) "Verified Partner • 0s forced wait" else formatRelativeTime(activeStory.timestamp),
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Black.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = if (activeStory.isAd) "Skip ✕" else "⏱️ ${remainingSeconds}s",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (activeStory.isMyStatus) {
                                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Center Caption
                if (activeStory.caption.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = activeStory.caption,
                            color = Color.White,
                            fontSize = if (activeStory.isAd) 16.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Center Animated Floating Emoji Reaction Burst
                AnimatedVisibility(
                    visible = activeFloatingReaction != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    activeFloatingReaction?.let { emoji ->
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 60.sp)
                        }
                    }
                }

                // Bottom Bar: Ad CTA or Standard Reply Bar (Hidden when paused)
                if (!isPaused) {
                    if (activeStory.isAd) {
                        // High-Conversion Non-Intrusive Sponsored Story CTA Action Bar
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    if (activeStory.adCampaignId.isNotBlank()) {
                                        AdManager.recordAdClick(activeStory.adCampaignId)
                                    }
                                    if (activeStory.adTargetUrl.isNotBlank()) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeStory.adTargetUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Opening ${activeStory.userName}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Visiting ${activeStory.userName} ✨", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "${activeStory.adCtaText.ifBlank { "Claim Offer" }} 🚀",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                onClick = {
                                    if (currentIndex < allUserStories.size - 1) {
                                        currentIndex++
                                    } else {
                                        onClose()
                                    }
                                },
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Tap or Swipe to Skip Ad Freely (0s Delay) ✕",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 10.5.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    } else {
                        // Standard User Story Reply & Reactions
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Quick Emoji Reaction Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                quickReactionEmojis.forEach { emoji ->
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                activeFloatingReaction = emoji
                                                onSendReaction(activeStory.userId, activeStory.userName, activeStory.caption, emoji)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 19.sp)
                                    }
                                }
                            }

                            // Reply Input Row + Heart Like Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    placeholder = { Text("Reply to ${activeStory.userName}...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true
                                )

                                if (replyText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            onSendReply(activeStory.userId, activeStory.userName, activeStory.caption, replyText)
                                            replyText = ""
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(VibeSyncGreen, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            isLiked = !isLiked
                                            onToggleLike(activeStory.id)
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                    ) {
                                        Icon(
                                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Like",
                                            tint = if (isLiked) Color.Red else Color.White,
                                            modifier = Modifier.size(24.dp)
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
}

// Utility to format relative time strings for status updates
fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = (diff / (60 * 1000)).coerceAtLeast(1)
    return when {
        minutes < 60 -> "$minutes minutes ago"
        minutes < 1440 -> "${minutes / 60} hours ago"
        else -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
