package com.example.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserPreferencesEntity
import com.example.ui.theme.CoralPink
import com.example.ui.theme.PassRed
import com.example.ui.theme.VerifiedBadgeBlue
import com.example.ui.theme.VibeSyncTeal

@Composable
fun ProfileHeaderSection(
    preferences: UserPreferencesEntity,
    onUpdatePreferences: (UserPreferencesEntity) -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenWallpaperDialog: () -> Unit,
    onViewFullPhoto: (url: String, title: String) -> Unit,
    onRefreshLiveLocation: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val updated = preferences.copy(avatarUrl = it.toString())
            onUpdatePreferences(updated)
        }
    }

    val galleryPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val currentList = preferences.getPhotoList().toMutableList()
            if (!currentList.contains(it.toString())) {
                currentList.add(it.toString())
                val updatedPhotos = currentList.joinToString("|||")
                onUpdatePreferences(preferences.copy(profilePhotos = updatedPhotos))
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(24.dp)
            )
            .testTag("card_profile_header"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Photo: Pure Round Type with Touch to View Full Photo
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            listOf(VibeSyncTeal, CoralPink, VibeSyncTeal)
                        ),
                        shape = CircleShape
                    )
                    .clickable {
                        onViewFullPhoto(
                            preferences.avatarUrl,
                            "${preferences.userName}'s Profile Photo"
                        )
                    }
                    .testTag("round_profile_photo_touch"),
                contentAlignment = Alignment.Center
            ) {
                if (preferences.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = preferences.avatarUrl,
                        contentDescription = "Profile Photo - Touch to View Full",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VibeSyncTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = VibeSyncTeal,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Touch to view hint (No top uploader button)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = VibeSyncTeal.copy(alpha = 0.12f),
                modifier = Modifier.clickable {
                    onViewFullPhoto(
                        preferences.avatarUrl,
                        "${preferences.userName}'s Profile Photo"
                    )
                }
            ) {
                Text(
                    text = "Touch photo to view full 🔍",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VibeSyncTeal,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name and Genuine Verified Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = preferences.userName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (preferences.isFaceVerified || preferences.isMobileVerified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Genuine Verified Badge",
                        tint = VerifiedBadgeBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-Photo Profile Gallery (Synced Across Platform)
            val photos = preferences.getPhotoList()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Profile Photos (Up to 3)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${photos.size}/3 Photos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibeSyncTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Slot 1
                        val p1 = photos.getOrNull(0) ?: preferences.avatarUrl
                        PhotoThumbnailBox(
                            photoUrl = p1,
                            slotNumber = 1,
                            label = "Main",
                            modifier = Modifier.weight(1f),
                            onView = { if (p1.isNotBlank()) onViewFullPhoto(p1, "${preferences.userName} - Photo 1") },
                            onPick = {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )

                        // Slot 2
                        val p2 = photos.getOrNull(1) ?: ""
                        PhotoThumbnailBox(
                            photoUrl = p2,
                            slotNumber = 2,
                            label = "Photo 2",
                            modifier = Modifier.weight(1f),
                            onView = { if (p2.isNotBlank()) onViewFullPhoto(p2, "${preferences.userName} - Photo 2") },
                            onPick = {
                                galleryPhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )

                        // Slot 3
                        val p3 = photos.getOrNull(2) ?: ""
                        PhotoThumbnailBox(
                            photoUrl = p3,
                            slotNumber = 3,
                            label = "Photo 3",
                            modifier = Modifier.weight(1f),
                            onView = { if (p3.isNotBlank()) onViewFullPhoto(p3, "${preferences.userName} - Photo 3") },
                            onPick = {
                                galleryPhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Edit Profile, Cover Art & Quick Logout Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenEditProfile,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_edit_profile"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibeSyncTeal
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenWallpaperDialog,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_change_wallpaper"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cover Art", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("btn_quick_logout_profile_header"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PassRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PassRed.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Log Out",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Profile Action Buttons
        }
    }
}

@Composable
private fun PhotoThumbnailBox(
    photoUrl: String,
    slotNumber: Int,
    label: String,
    modifier: Modifier = Modifier,
    onView: () -> Unit,
    onPick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (photoUrl.isNotBlank()) VibeSyncTeal else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .height(105.dp)
            .clickable {
                if (photoUrl.isNotBlank()) onView() else onPick()
            }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                )

                Surface(
                    shape = RoundedCornerShape(topStart = 8.dp),
                    color = if (slotNumber == 1) VibeSyncTeal else Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = if (slotNumber == 1) "★ 1" else "$slotNumber",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add $label",
                        tint = VibeSyncTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

