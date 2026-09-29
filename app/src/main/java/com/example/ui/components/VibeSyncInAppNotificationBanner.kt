package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.util.AppAlertPayload
import com.example.util.AppNotificationManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * VibeSyncInAppNotificationBanner
 * Instant Floating Heads-Up Notification Banner shown at the top of the screen
 * when a real-time match or chat message arrives while the user is actively using the app.
 */
@Composable
fun VibeSyncInAppNotificationBanner(
    onOpenChat: (matchId: String, senderName: String) -> Unit,
    onOpenMatch: (matchId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeAlert by remember { mutableStateOf<AppAlertPayload?>(null) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        AppNotificationManager.inAppAlertEvents.collect { payload ->
            activeAlert = payload
            offsetY = 0f
            // Auto-dismiss after 4.5 seconds
            delay(4500)
            if (activeAlert == payload) {
                activeAlert = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = activeAlert != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it }
            ) + fadeOut()
        ) {
            activeAlert?.let { alert ->
                val isMatch = alert.type == "MATCH"
                val isLike = alert.type == "LIKE"
                val isSuperLike = alert.type == "SUPER_LIKE"
                val isFriendReq = alert.type == "FRIEND_REQUEST"
                val isOtp = alert.type == "OTP"

                val themeColor = when (alert.type) {
                    "MATCH" -> Color(0xFFFF4081)
                    "LIKE" -> Color(0xFFFF5252)
                    "SUPER_LIKE" -> Color(0xFF2196F3)
                    "FRIEND_REQUEST" -> Color(0xFFFF9800)
                    "OTP" -> Color(0xFF25D366)
                    else -> Color(0xFF00A884)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, offsetY.roundToInt()) }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                if (delta < 0) { // Swipe up to dismiss
                                    offsetY += delta
                                    if (offsetY < -120f) {
                                        activeAlert = null
                                    }
                                }
                            }
                        )
                        .shadow(elevation = 14.dp, shape = RoundedCornerShape(20.dp), spotColor = themeColor)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            val targetId = alert.senderOrMatchId
                            activeAlert = null
                            if (isOtp) {
                                // OTP notification dismissed on tap
                            } else if (isMatch) {
                                onOpenMatch(targetId)
                            } else {
                                onOpenChat(targetId, alert.senderName)
                            }
                        }
                        .testTag("vibesync_in_app_notification_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2C34)), // Modern Dark Theme Card
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF233138),
                                        Color(0xFF1F2C34)
                                    )
                                )
                            )
                            .padding(14.dp)
                    ) {
                        // Top App Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = themeColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (alert.type) {
                                                "MATCH", "LIKE" -> Icons.Default.Favorite
                                                "SUPER_LIKE" -> Icons.Default.Star
                                                "FRIEND_REQUEST" -> Icons.Default.PersonAdd
                                                else -> Icons.Default.NotificationsActive
                                            },
                                            contentDescription = null,
                                            tint = themeColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (alert.type) {
                                        "MATCH" -> "VIBESYNC MATCH"
                                        "LIKE" -> "NEW PROFILE LIKE"
                                        "SUPER_LIKE" -> "⭐ SUPER LIKE"
                                        "FRIEND_REQUEST" -> "🤝 FRIEND REQUEST"
                                        "OTP" -> "SECURITY VERIFICATION"
                                        else -> "VIBESYNC CHAT"
                                    },
                                    color = themeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = " • just now",
                                    color = Color(0xFF8696A0),
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { activeAlert = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFF8696A0),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Middle Content Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(modifier = Modifier.size(48.dp)) {
                                if (alert.photoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = alert.photoUrl,
                                        contentDescription = alert.senderName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2A3942))
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = themeColor.copy(alpha = 0.25f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = alert.senderName.take(1).uppercase(),
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Online / Match Indicator Dot
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(if (isMatch) Color(0xFFFF4081) else Color(0xFF00A884))
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Sender & Text
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = alert.body,
                                    color = Color(0xFFD1D7DB),
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Action Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { activeAlert = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8696A0))
                            ) {
                                Text("Dismiss", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(
                                onClick = {
                                    val targetId = alert.senderOrMatchId
                                    activeAlert = null
                                    if (isMatch) {
                                        onOpenMatch(targetId)
                                    } else {
                                        onOpenChat(targetId, alert.senderName)
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = themeColor.copy(alpha = 0.15f),
                                    contentColor = themeColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMatch) Icons.Default.Favorite else Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isMatch) "View Match" else "Reply",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


