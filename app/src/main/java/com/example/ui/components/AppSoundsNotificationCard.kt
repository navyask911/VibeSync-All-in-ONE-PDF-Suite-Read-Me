package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserPreferencesEntity
import com.example.util.AlertSoundAndVibrationManager

@Composable
fun AppSoundsNotificationCard(
    preferences: UserPreferencesEntity,
    onUpdatePreferences: (UserPreferencesEntity) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    val tealColor = Color(0xFF00A884)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_app_sounds_settings"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(tealColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "App Sounds",
                            tint = tealColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "🔔 App Sounds & Call Ringtone Settings",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Set custom alert sounds for messages, calls & matches",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Drawer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Global Sound & Vibration Switches
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔊 In-App Sound Alerts",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = preferences.soundAlertsEnabled,
                            onCheckedChange = { enabled ->
                                onUpdatePreferences(preferences.copy(soundAlertsEnabled = enabled))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = tealColor
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📳 Haptic Vibration Feedback",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = preferences.vibrationAlertsEnabled,
                            onCheckedChange = { enabled ->
                                onUpdatePreferences(preferences.copy(vibrationAlertsEnabled = enabled))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = tealColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Incoming Chat Message Tone Selector
                    SoundCategorySection(
                        title = "💬 Incoming Message Alert Tone",
                        selectedToneKey = preferences.messageSoundTone,
                        options = listOf(
                            SoundOption("DEFAULT", "Default System Chime"),
                            SoundOption("CLASSIC_CHIME", "Classic Chime"),
                            SoundOption("CRYSTAL_DROP", "Crystal Drop"),
                            SoundOption("WHISTLE_BREEZE", "Whistle Breeze"),
                            SoundOption("GENTLE_VIBE", "Gentle Vibe"),
                            SoundOption("SILENT", "Silent (No Sound)")
                        ),
                        onSelectTone = { toneKey ->
                            onUpdatePreferences(preferences.copy(messageSoundTone = toneKey))
                        },
                        onPreviewSound = { toneKey ->
                            AlertSoundAndVibrationManager.playPreviewSound(context, toneKey)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Mutual Match Celebration Sound
                    SoundCategorySection(
                        title = "✨ Mutual Match & Like Alert Sound",
                        selectedToneKey = preferences.matchSoundTone,
                        options = listOf(
                            SoundOption("CELEBRATION", "Celebration Pulse"),
                            SoundOption("COSMIC_MATCH", "Cosmic Match Chime"),
                            SoundOption("SWEET_BELL", "Sweet Bell"),
                            SoundOption("SILENT", "Silent (No Sound)")
                        ),
                        onSelectTone = { toneKey ->
                            onUpdatePreferences(preferences.copy(matchSoundTone = toneKey))
                        },
                        onPreviewSound = { toneKey ->
                            AlertSoundAndVibrationManager.playPreviewSound(context, toneKey)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Voice & Video Call Ringtone
                    SoundCategorySection(
                        title = "📞 Voice & Video Call Ringtone",
                        selectedToneKey = preferences.callRingtone,
                        options = listOf(
                            SoundOption("STANDARD_RING", "Standard Phone Ring"),
                            SoundOption("DIGITAL_WAVE", "Digital Wave Tone"),
                            SoundOption("MARIMBA", "Marimba Tone"),
                            SoundOption("SILENT", "Silent (No Ringtone)")
                        ),
                        onSelectTone = { toneKey ->
                            onUpdatePreferences(preferences.copy(callRingtone = toneKey))
                        },
                        onPreviewSound = { toneKey ->
                            AlertSoundAndVibrationManager.playPreviewSound(context, toneKey)
                        }
                    )
                }
            }
        }
    }
}

data class SoundOption(val key: String, val label: String)

@Composable
private fun SoundCategorySection(
    title: String,
    selectedToneKey: String,
    options: List<SoundOption>,
    onSelectTone: (String) -> Unit,
    onPreviewSound: (String) -> Unit
) {
    val tealColor = Color(0xFF00A884)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            ),
            color = tealColor
        )
        Spacer(modifier = Modifier.height(6.dp))

        options.forEach { option ->
            val isSelected = selectedToneKey.equals(option.key, ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTone(option.key) }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectTone(option.key) },
                        colors = RadioButtonDefaults.colors(selectedColor = tealColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = option.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!option.key.equals("SILENT", ignoreCase = true)) {
                    Surface(
                        shape = CircleShape,
                        color = tealColor.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { onPreviewSound(option.key) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Sound",
                                tint = tealColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Test",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = tealColor
                            )
                        }
                    }
                }
            }
        }
    }
}
