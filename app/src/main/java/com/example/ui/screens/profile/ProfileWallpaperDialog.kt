package com.example.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserPreferencesEntity
import com.example.ui.theme.CoralPink
import com.example.ui.theme.RomanticViolet

@Composable
fun ProfileWallpaperDialog(
    preferences: UserPreferencesEntity,
    onUpdatePreferences: (UserPreferencesEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var tempWallpaperUrl by remember { mutableStateOf(preferences.profileWallpaperUrl) }
    val dedicatedWallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { tempWallpaperUrl = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wallpaper, contentDescription = null, tint = CoralPink, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update Profile Wallpaper", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Customize your profile cover art. Choose a photo from your gallery, enter an image link, or select a curated aesthetic background.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Live Wallpaper Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, CoralPink.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    if (tempWallpaperUrl.isNotBlank()) {
                        AsyncImage(
                            model = tempWallpaperUrl,
                            contentDescription = "Wallpaper Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(CoralPink.copy(alpha = 0.85f), RomanticViolet.copy(alpha = 0.85f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Romantic Sunset Gradient (Default)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Gallery Upload Button & Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            dedicatedWallpaperPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_upload_from_photos")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload from Photos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (tempWallpaperUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = { tempWallpaperUrl = "" },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Reset", fontSize = 12.sp)
                        }
                    }
                }

                // URL Input Field
                OutlinedTextField(
                    value = tempWallpaperUrl,
                    onValueChange = { tempWallpaperUrl = it },
                    label = { Text("Or Paste Image Link / URL") },
                    placeholder = { Text("https://example.com/cover.jpg") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_wallpaper_url_dedicated")
                )

                // Presets
                Text(
                    text = "Aesthetic Themes:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(
                        "Sunset Glow" to "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80",
                        "Neon City" to "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800&q=80",
                        "Warm Coffee" to "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800&q=80",
                        "Starry Night" to "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&q=80",
                        "Greenery" to "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?w=800&q=80"
                    )
                    items(presets) { (title, url) ->
                        FilterChip(
                            selected = tempWallpaperUrl == url,
                            onClick = { tempWallpaperUrl = url },
                            label = { Text(title, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdatePreferences(preferences.copy(profileWallpaperUrl = tempWallpaperUrl.trim()))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CoralPink)
            ) {
                Text("Apply Wallpaper")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
