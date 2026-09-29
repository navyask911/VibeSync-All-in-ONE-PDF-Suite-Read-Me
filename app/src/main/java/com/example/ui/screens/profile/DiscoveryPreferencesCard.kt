package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserPreferencesEntity
import com.example.ui.theme.VerifiedBadgeBlue
import com.example.ui.theme.VibeSyncTeal
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoveryPreferencesCard(
    preferences: UserPreferencesEntity,
    onUpdatePreferences: (UserPreferencesEntity) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val interestedOptions = listOf("Men", "Women", "Everyone")
    val goalOptions = listOf("All", "Long-term relationship", "Casual connect", "Friendship & Vibes")

    val summary = "${preferences.minAge}-${preferences.maxAge} yrs • ${preferences.maxDistanceMiles} km"

    ProfileSectionDrawer(
        title = "Discovery Preferences",
        icon = Icons.Default.Explore,
        iconTint = Color(0xFF00897B),
        iconBackground = Color(0xFF00897B).copy(alpha = 0.15f),
        statusBadgeText = summary,
        statusBadgeColor = Color(0xFF00897B),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        testTag = "drawer_discovery_preferences"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Interested In
            Column {
                Text(
                    text = "I am interested in connecting with",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    interestedOptions.forEach { opt ->
                        val isSelected = preferences.userInterestedIn.equals(opt, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) VibeSyncTeal else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable {
                                    onUpdatePreferences(preferences.copy(userInterestedIn = opt))
                                }
                                .testTag("opt_interested_$opt")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = opt,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Age Range
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Age Range",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${preferences.minAge} - ${preferences.maxAge} years",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VibeSyncTeal
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                RangeSlider(
                    value = preferences.minAge.toFloat().coerceAtLeast(13f)..preferences.maxAge.toFloat().coerceAtLeast(13f),
                    onValueChange = { range ->
                        onUpdatePreferences(
                            preferences.copy(
                                minAge = range.start.roundToInt().coerceAtLeast(13),
                                maxAge = range.endInclusive.roundToInt().coerceAtMost(60)
                            )
                        )
                    },
                    valueRange = 13f..60f,
                    steps = 46,
                    colors = SliderDefaults.colors(
                        thumbColor = VibeSyncTeal,
                        activeTrackColor = VibeSyncTeal
                    ),
                    modifier = Modifier.testTag("slider_age_range")
                )
            }

            // 3. Max Distance
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Maximum Distance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Up to ${preferences.maxDistanceMiles} km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VibeSyncTeal
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = preferences.maxDistanceMiles.toFloat(),
                    onValueChange = { dist ->
                        onUpdatePreferences(preferences.copy(maxDistanceMiles = dist.roundToInt()))
                    },
                    valueRange = 5f..150f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = VibeSyncTeal,
                        activeTrackColor = VibeSyncTeal
                    ),
                    modifier = Modifier.testTag("slider_max_distance")
                )
            }

            // 4. Relationship Goal Filter
            Column {
                Text(
                    text = "Relationship Intent",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goalOptions.forEach { goal ->
                        val isSelected = preferences.goalFilter == goal
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onUpdatePreferences(preferences.copy(goalFilter = goal))
                            },
                            label = {
                                Text(
                                    text = goal,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VibeSyncTeal.copy(alpha = 0.18f),
                                selectedLabelColor = VibeSyncTeal
                            )
                        )
                    }
                }
            }

            // 5. Only Verified Singles Toggle
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
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = VerifiedBadgeBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Only Verified Singles",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Show only profiles with genuine badge",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = preferences.onlyVerified,
                    onCheckedChange = { checked ->
                        onUpdatePreferences(preferences.copy(onlyVerified = checked))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = VerifiedBadgeBlue
                    ),
                    modifier = Modifier.testTag("switch_only_verified")
                )
            }
        }
    }
}
