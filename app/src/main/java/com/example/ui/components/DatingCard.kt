package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProfileEntity
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.PassRed
import com.example.ui.theme.SuperLikeBlue
import com.example.ui.theme.VerifiedBadgeBlue
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import coil.request.CachePolicy
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DatingCard(
    profile: ProfileEntity,
    isTopCard: Boolean,
    onSwipe: (direction: String) -> Unit,
    onInfoClick: () -> Unit,
    mutualFriends: List<ProfileEntity> = emptyList(),
    isBreakupShared: Boolean = false,
    isBreakupPending: Boolean = false,
    onRequestBreakupShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember(profile.id) { Animatable(0f) }
    val offsetY = remember(profile.id) { Animatable(0f) }

    androidx.compose.runtime.LaunchedEffect(profile.id) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
    }

    val photoList = remember(profile.avatarUrl, profile.id) {
        profile.getPhotoList()
    }
    var currentPhotoIndex by remember(profile.id) { androidx.compose.runtime.mutableIntStateOf(0) }
    val activePhoto = remember(photoList, currentPhotoIndex, profile.id) {
        if (photoList.isNotEmpty()) {
            photoList.getOrElse(currentPhotoIndex % photoList.size) { photoList[0] }
        } else {
            profile.getEffectiveAvatarUrl()
        }
    }

    val swipeThreshold = 260f

    val dragModifier = if (isTopCard) {
        Modifier.pointerInput(profile.id) {
            detectDragGestures(
                onDragEnd = {
                    coroutineScope.launch {
                        if (offsetX.value > swipeThreshold) {
                            // Right swipe = LIKE
                            offsetX.animateTo(1000f, spring())
                            onSwipe("LIKE")
                        } else if (offsetX.value < -swipeThreshold) {
                            // Left swipe = DISLIKE (NOPE)
                            offsetX.animateTo(-1000f, spring())
                            onSwipe("PASS")
                        } else if (offsetY.value < -swipeThreshold) {
                            // Top swipe = SUPER LIKE
                            offsetY.animateTo(-1000f, spring())
                            onSwipe("SUPER_LIKE")
                        } else if (offsetY.value > swipeThreshold) {
                            // Below swipe = ASK FRIENDSHIP
                            offsetY.animateTo(1000f, spring())
                            onSwipe("FRIEND_REQUEST")
                        } else {
                            // Snap back to center
                            launch { offsetX.animateTo(0f, spring()) }
                            launch { offsetY.animateTo(0f, spring()) }
                        }
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    coroutineScope.launch {
                        offsetX.snapTo(offsetX.value + dragAmount.x)
                        offsetY.snapTo(offsetY.value + dragAmount.y)
                    }
                }
            )
        }
    } else {
        Modifier
    }

    val rotation = (offsetX.value / 25f).coerceIn(-22f, 22f)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .rotate(rotation)
            .then(dragModifier)
            .pointerInput(profile.id) {
                detectTapGestures(
                    onDoubleTap = { onInfoClick() }
                )
            }
            .fillMaxSize()
            .testTag("dating_card_${profile.id}")
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .shadow(12.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Hero Visual (Photos 1, 2, 3 with Multi-Photo Swiping / Tapping)
                    val context = LocalContext.current
                    val effectivePhoto = activePhoto.ifBlank { profile.getEffectiveAvatarUrl() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(profile.gradientColorStart),
                                        Color(profile.gradientColorEnd)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (effectivePhoto.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(effectivePhoto)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = "${profile.name} photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        if (photoList.size > 1) {
                                            currentPhotoIndex = (currentPhotoIndex + 1) % photoList.size
                                        } else {
                                            onInfoClick()
                                        }
                                    },
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = Color.White.copy(alpha = 0.85f),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                },
                                error = {
                                    // Elegant styled portrait fallback if network image fails
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(86.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.25f))
                                                    .border(2.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = profile.avatarEmoji.ifBlank { "✨" },
                                                    fontSize = 42.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = profile.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            )
                            // Elegant soft gradient overlay on the image so text and badges are readable
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.45f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.35f)
                                            )
                                        )
                                    )
                            )
                        } else {
                            // Stylized Emoji Avatar Portrait when no photo is uploaded
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .border(2.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.avatarEmoji,
                                    fontSize = 48.sp
                                )
                            }
                        }

                        // Photo Pagination Indicators (If multiple photos uploaded)
                        if (photoList.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp, start = 12.dp, end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                photoList.indices.forEach { idx ->
                                    val isSelected = (currentPhotoIndex % photoList.size) == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(3.5.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)
                                            )
                                    )
                                }
                            }
                        }

                        // Top Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = if (photoList.size > 1) 18.dp else 10.dp, start = 12.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Genuine Member Badge (No location / address shown)
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                contentColor = Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = Color(0xFF00E676)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (photoList.size > 1) "Photo ${ (currentPhotoIndex % photoList.size) + 1 }/${photoList.size}" else "Verified Member",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Free Liked You Indicator (If this user swiped on current user!)
                            if (profile.likedMe) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFF4757),
                                    contentColor = Color.White
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "★ Liked You!",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Profile Info Details (Lower half of card, stacked below)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        // Name, Age & Verified
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${profile.age}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                                if (profile.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified profile",
                                        tint = VerifiedBadgeBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Info Button to open detailed profile sheet
                            IconButton(
                                onClick = onInfoClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("btn_info_${profile.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "View details",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Occupation, Qualification & Marital Status (No location / address)
                        Text(
                            text = "💼 ${profile.occupation} • 🎓 ${profile.qualification}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "💍 ${profile.maritalStatus}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Looking for: ${profile.relationshipGoal}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        // Mutual Friends Pill (Automatically shown to user)
                        if (mutualFriends.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1565C0).copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("👥", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${mutualFriends.size} Mutual Friend${if (mutualFriends.size > 1) "s" else ""}: ${mutualFriends.joinToString(", ") { it.name }}",
                                        color = Color(0xFF1565C0),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Relationship Ethics & Demographics Accountability Pill Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isBreakupShared) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE53935).copy(alpha = 0.08f),
                                    contentColor = Color(0xFFC62828)
                                ) {
                                    Text(
                                        text = "💔 ${profile.breakupCount} Breakups",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isBreakupPending) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFA000).copy(alpha = 0.12f),
                                    contentColor = Color(0xFFF57C00)
                                ) {
                                    Text(
                                        text = "💔 🔒 Pending...",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable {
                                        onRequestBreakupShare?.invoke()
                                    }
                                ) {
                                    Text(
                                        text = "💔 🔒 Private (Ask 🔓)",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1976D2).copy(alpha = 0.08f),
                                contentColor = Color(0xFF1565C0)
                            ) {
                                Text(
                                    text = "👥 ${profile.friendsCount} Friends",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    text = "${profile.countryFlag} ${profile.country}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Interests Pills
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            profile.getInterestList().take(3).forEach { interest ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(
                                        text = interest,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Prominent Prompt Quote Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = profile.promptQuestion,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = profile.promptAnswer,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // SWIPE ACTION OVERLAYS (STAMPS)
                // "NOPE / DISLIKE" Stamp when dragged LEFT
                if (offsetX.value < -60f) {
                    val alpha = (-offsetX.value / 250f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 28.dp, top = 60.dp)
                            .rotate(-18f)
                            .border(4.dp, PassRed.copy(alpha = alpha), RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f * alpha))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "NOPE",
                            color = PassRed.copy(alpha = alpha),
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // "LIKE" Stamp when dragged RIGHT
                if (offsetX.value > 60f) {
                    val alpha = (offsetX.value / 250f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 28.dp, top = 60.dp)
                            .rotate(18f)
                            .border(4.dp, LikeGreen.copy(alpha = alpha), RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f * alpha))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "LIKE",
                            color = LikeGreen.copy(alpha = alpha),
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // "SUPER LIKE" Stamp when dragged UP (TOP)
                if (offsetY.value < -80f) {
                    val alpha = (-offsetY.value / 250f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 120.dp)
                            .border(4.dp, SuperLikeBlue.copy(alpha = alpha), RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f * alpha))
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SUPER LIKE ⭐",
                            color = SuperLikeBlue.copy(alpha = alpha),
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // "ASK FRIENDSHIP" Stamp when dragged DOWN (BELOW)
                if (offsetY.value > 80f) {
                    val alpha = (offsetY.value / 250f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 100.dp)
                            .border(4.dp, Color(0xFF00BFA5).copy(alpha = alpha), RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.2f * alpha))
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "ASK FRIENDSHIP 🤝",
                            color = Color(0xFF00BFA5).copy(alpha = alpha),
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
