package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProfileEntity
import com.example.ui.components.DatingAvatar
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.RomanticViolet
import com.example.ui.theme.SuperLikeBlue
import com.example.ui.theme.VerifiedBadgeBlue

@Composable
fun LikedYouScreen(
    likedProfiles: List<ProfileEntity>,
    onMatchInstantly: (profileId: String) -> Unit,
    onViewProfile: (ProfileEntity) -> Unit,
    onResetData: () -> Unit,
    isSpotlightActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedProfileForSafetyCheck by remember { mutableStateOf<ProfileEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Liked You",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        brush = Brush.linearGradient(listOf(CoralPink, RomanticViolet)),
                        letterSpacing = (-0.5).sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CoralPink.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = CoralPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${likedProfiles.size} likes",
                            color = CoralPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rewarded Ad Boost & 100% Ad-Free Core Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = if (isSpotlightActive) "🚀" else "⚡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isSpotlightActive) "Spotlight Active!" else "Ad-Free Matches",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (isSpotlightActive) "Your profile is prioritized" else "Connect with all admirers instantly",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // Grid of Liked Profiles
        if (likedProfiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(text = "💌", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "All Likes Matched!",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You've already connected with everyone who liked you. Check your active matches or explore nearby profiles to find fresh connections!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(likedProfiles, key = { it.id }) { profile ->
                    LikedProfileGridItem(
                        profile = profile,
                        onMatch = { selectedProfileForSafetyCheck = profile },
                        onView = { onViewProfile(profile) }
                    )
                }
            }
        }

        // Safety Protocols & Request Acceptance Modal
        selectedProfileForSafetyCheck?.let { profile ->
            AlertDialog(
                onDismissRequest = { selectedProfileForSafetyCheck = null },
                icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = CoralPink, modifier = Modifier.size(36.dp)) },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Connection Request 💌", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Safety Protocols & Shielded Chat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DatingAvatar(
                                name = profile.name,
                                emoji = profile.avatarEmoji,
                                colorStart = profile.gradientColorStart,
                                colorEnd = profile.gradientColorEnd,
                                size = 48.dp,
                                isVerified = profile.isVerified,
                                avatarUrl = profile.avatarUrl
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("${profile.name}, ${profile.age}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${profile.occupation} • ${profile.place}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LikeGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, LikeGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = LikeGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tight Security & Safety Active", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LikeGreen)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("• Anonymous Phone & Address Masking", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("• 256-bit E2EE Signal Encrypted Chat Tunnel", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("• Trust Score: ${profile.trustScore}% • Real Face Verified 🟢", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Text("Accepting this request opens an anonymous secure chat before deciding to socialize or date.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onMatchInstantly(profile.id)
                            selectedProfileForSafetyCheck = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPink)
                    ) {
                        Text("Accept & Start Anonymous Chat 🛡️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedProfileForSafetyCheck = null }) {
                        Text("Decline Request")
                    }
                }
            )
        }
    }
}

@Composable
private fun LikedProfileGridItem(
    profile: ProfileEntity,
    onMatch: () -> Unit,
    onView: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("liked_card_${profile.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Card Visual Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
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
                DatingAvatar(
                    name = profile.name,
                    emoji = profile.avatarEmoji,
                    colorStart = profile.gradientColorStart,
                    colorEnd = profile.gradientColorEnd,
                    size = 72.dp,
                    isVerified = profile.isVerified,
                    avatarUrl = profile.avatarUrl
                )

                if (profile.isSuperLikedMe) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SuperLikeBlue,
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "SUPER LIKE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Profile info & actions
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${profile.name}, ${profile.age}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (profile.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = VerifiedBadgeBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = profile.occupation,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons: Match Instantly & View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onMatch,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPink,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_match_${profile.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Accept 🛡️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }

                    Surface(
                        onClick = onView,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_view_${profile.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "View Profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
