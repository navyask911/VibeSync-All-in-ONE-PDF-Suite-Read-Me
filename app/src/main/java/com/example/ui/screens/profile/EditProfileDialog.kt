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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wc
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
import com.example.ui.theme.VibeSyncTeal
import com.example.util.CloudflareStorageManager
import kotlinx.coroutines.launch

@Composable
fun EditProfileDialog(
    preferences: UserPreferencesEntity,
    onUpdatePreferences: (UserPreferencesEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var editName by remember { mutableStateOf(preferences.userName) }
    var editJob by remember { mutableStateOf(preferences.userOccupation) }
    var editQualification by remember { mutableStateOf(preferences.userQualification) }
    var editPlace by remember { mutableStateOf(preferences.userPlace) }
    var editCity by remember { mutableStateOf(preferences.userCity) }
    var editCountry by remember { mutableStateOf(preferences.userCountry) }
    var editAddress by remember { mutableStateOf(preferences.userAddress) }
    var editMaritalStatus by remember { mutableStateOf(preferences.userMaritalStatus) }
    var editBio by remember { mutableStateOf(preferences.userBio) }
    var editInterests by remember { mutableStateOf(preferences.userInterests) }
    var editIsOpenForDating by remember { mutableStateOf(preferences.isOpenForDating) }
    var editIsProfileLocked by remember { mutableStateOf(preferences.isProfileLocked) }
    var editHidePhoneNumber by remember { mutableStateOf(preferences.hidePhoneNumber) }
    var editHideExactLocation by remember { mutableStateOf(preferences.hideExactLocation) }
    var editLocationVisibility by remember { mutableStateOf(preferences.locationVisibility) }
    var editAllowedLocationUserIds by remember { mutableStateOf(preferences.allowedLocationUserIds) }
    var editHideOccupation by remember { mutableStateOf(preferences.hideOccupation) }
    var editWallpaperUrl by remember { mutableStateOf(preferences.profileWallpaperUrl) }
    var editCountryFlag by remember { mutableStateOf(preferences.userCountryFlag) }
    var editLatitude by remember { mutableDoubleStateOf(preferences.latitude) }
    var editLongitude by remember { mutableDoubleStateOf(preferences.longitude) }
    var isFetchingLiveLocation by remember { mutableStateOf(false) }

    var photo1 by remember { mutableStateOf(preferences.avatarUrl) }
    var photo2 by remember { mutableStateOf(preferences.getPhotoList().getOrNull(1) ?: "") }
    var photo3 by remember { mutableStateOf(preferences.getPhotoList().getOrNull(2) ?: "") }
    var activePhotoSlot by remember { mutableIntStateOf(1) }

    val slotPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val uriStr = uri.toString()
            when (activePhotoSlot) {
                1 -> photo1 = uriStr
                2 -> photo2 = uriStr
                3 -> photo3 = uriStr
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isFetchingLiveLocation = true
            coroutineScope.launch {
                val loc = com.example.util.DeviceSimAndIpCountryHelper.fetchDetailedLocation(context)
                if (loc.cityName.isNotBlank()) editCity = loc.cityName
                if (loc.placeName.isNotBlank()) editPlace = loc.placeName
                if (loc.countryName.isNotBlank()) editCountry = loc.countryName
                if (loc.detectedCountry != null) editCountryFlag = loc.detectedCountry.flagEmoji
                if (loc.formattedAddress.isNotBlank()) editAddress = loc.formattedAddress
                val parts = loc.coordsString.split(",")
                editLatitude = parts.getOrNull(0)?.replace("[^0-9.-]".toRegex(), "")?.toDoubleOrNull() ?: editLatitude
                editLongitude = parts.getOrNull(1)?.replace("[^0-9.-]".toRegex(), "")?.toDoubleOrNull() ?: editLongitude
                isFetchingLiveLocation = false
                android.widget.Toast.makeText(context, "📍 Live GPS Location Updated: ${loc.cityName}, ${loc.countryName}", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Location permission is required to fetch live GPS.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Edit Your Profile", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ==========================================
                // SECTION 1: CORE IDENTITY (NAME EDITABLE, GENDER & DOB LOCKED)
                // ==========================================
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFBF4E8)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCC80)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Identity",
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Core Identity",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFBF360C)
                                )
                            }
                        }

                        Text(
                            text = "You can update your display name anytime. Gender and Date of Birth remain permanently locked after registration to maintain platform authenticity.",
                            fontSize = 11.sp,
                            color = Color(0xFF5D4037),
                            lineHeight = 15.sp
                        )

                        HorizontalDivider(
                            color = Color(0xFFFFE0B2),
                            thickness = 0.5.dp
                        )

                        // Editable Name Field
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Name *", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF5D4037))
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                placeholder = { Text("Enter your name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = VibeSyncTeal, modifier = Modifier.size(18.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VibeSyncTeal,
                                    unfocusedBorderColor = Color(0xFFFFCC80)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_edit_profile_name")
                            )
                        }

                        // Read-only Gender (Permanently Locked)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Wc, contentDescription = null, tint = Color(0xFF8D6E63), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Gender", fontSize = 10.sp, color = Color(0xFF8D6E63))
                                Text(preferences.userGender.ifBlank { "Not Specified" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF2E2E2E))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFE0B2)
                            ) {
                                Text("🔒 Locked", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        // Read-only DOB & Age (Permanently Locked)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Cake, contentDescription = null, tint = Color(0xFF8D6E63), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Date of Birth & Age", fontSize = 10.sp, color = Color(0xFF8D6E63))
                                val dobDisplay = preferences.userDob.ifBlank { "Verified" }
                                Text("$dobDisplay • Age ${preferences.userAge} yrs", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF2E2E2E))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFE0B2)
                            ) {
                                Text("🔒 Locked", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp)

                HorizontalDivider(thickness = 0.5.dp)

                // ==========================================
                // SECTION 2: PROFILE PHOTOS (UP TO 3)
                // ==========================================
                Text(
                    text = "My Profile Photos (Up to 3) 📸",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Photos sync across Swipe Cards, Chats, and Profile. Swipeable on your card.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Slot 1
                    EditPhotoSlot(
                        photoUrl = photo1,
                        slotNumber = 1,
                        label = "Main Photo",
                        modifier = Modifier.weight(1f),
                        onPick = {
                            activePhotoSlot = 1
                            slotPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onRemove = { photo1 = "" }
                    )
                    // Slot 2
                    EditPhotoSlot(
                        photoUrl = photo2,
                        slotNumber = 2,
                        label = "Photo 2",
                        modifier = Modifier.weight(1f),
                        onPick = {
                            activePhotoSlot = 2
                            slotPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onRemove = { photo2 = "" }
                    )
                    // Slot 3
                    EditPhotoSlot(
                        photoUrl = photo3,
                        slotNumber = 3,
                        label = "Photo 3",
                        modifier = Modifier.weight(1f),
                        onPick = {
                            activePhotoSlot = 3
                            slotPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onRemove = { photo3 = "" }
                    )
                }

                HorizontalDivider(thickness = 0.5.dp)

                // ==========================================
                // SECTION 3: EDITABLE BIO, OCCUPATION & DETAILS
                // ==========================================
                Text(
                    text = "About Me & Vibe ✨",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("Bio / Status") },
                    placeholder = { Text("Tell suitors and friends about your vibe...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editJob,
                    onValueChange = { editJob = it },
                    label = { Text("Profession / Occupation") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editQualification,
                    onValueChange = { editQualification = it },
                    label = { Text("Highest Qualification") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editInterests,
                    onValueChange = { editInterests = it },
                    label = { Text("Passions & Interests (comma separated)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Open for Connect other People inside Vibesync 🤝", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Allow discovery in Connect deck", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = editIsOpenForDating,
                        onCheckedChange = { editIsOpenForDating = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Profile Lock Privacy Shield 🔒", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Shield private details from non-matched suitors", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = editIsProfileLocked,
                        onCheckedChange = { editIsProfileLocked = it }
                    )
                }

                // Privacy Notice (Background Location Only)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VibeSyncTeal.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VibeSyncTeal.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔒", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Location is kept 100% private in the background for local matching & nearest business perks. Address is never shown on your profile.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPhotoList = listOf(photo1, photo2, photo3).filter { it.isNotBlank() }
                    val effectiveAvatar = finalPhotoList.firstOrNull() ?: preferences.avatarUrl
                    val photoJoinedString = finalPhotoList.joinToString("|||")

                    onUpdatePreferences(
                        preferences.copy(
                            userName = editName.trim().ifBlank { preferences.userName },
                            userAge = preferences.userAge,   // PRESERVED MANDATORY LOCKED
                            userDob = preferences.userDob,   // PRESERVED MANDATORY LOCKED
                            userGender = preferences.userGender, // PRESERVED MANDATORY LOCKED
                            avatarUrl = effectiveAvatar,
                            profilePhotos = photoJoinedString,
                            userOccupation = editJob.trim().ifBlank { preferences.userOccupation },
                            userQualification = editQualification.trim().ifBlank { preferences.userQualification },
                            userPlace = preferences.userPlace,
                            userCity = preferences.userCity,
                            userAddress = "", // Removed visible address
                            userMaritalStatus = editMaritalStatus.trim().ifBlank { preferences.userMaritalStatus },
                            userBio = editBio.trim().ifBlank { preferences.userBio },
                            userInterests = editInterests.trim().ifBlank { preferences.userInterests },
                            userCountry = preferences.userCountry,
                            userCountryFlag = preferences.userCountryFlag,
                            latitude = editLatitude,
                            longitude = editLongitude,
                            lastLocationUpdateTimestamp = System.currentTimeMillis(),
                            breakupCount = preferences.breakupCount,
                            friendsCount = preferences.friendsCount,
                            isOpenForDating = editIsOpenForDating,
                            isProfileLocked = editIsProfileLocked,
                            hidePhoneNumber = editHidePhoneNumber,
                            hideExactLocation = true,
                            locationVisibility = "NO_ONE",
                            allowedLocationUserIds = "",
                            hideOccupation = editHideOccupation,
                            profileWallpaperUrl = editWallpaperUrl.trim()
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CoralPink)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditPhotoSlot(
    photoUrl: String,
    slotNumber: Int,
    label: String,
    modifier: Modifier = Modifier,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (photoUrl.isNotBlank()) VibeSyncTeal else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .height(110.dp)
            .clickable { onPick() }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = label,
                        tint = VibeSyncTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(topStart = 8.dp),
                color = if (slotNumber == 1) VibeSyncTeal else Color.Black.copy(alpha = 0.5f),
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
        }
    }
}

