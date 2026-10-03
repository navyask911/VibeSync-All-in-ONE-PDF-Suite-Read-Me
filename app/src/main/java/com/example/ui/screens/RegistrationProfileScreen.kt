package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CoralPink
import com.example.ui.theme.RomanticViolet
import com.example.ui.theme.VibeSyncTeal
import com.example.util.DeviceSimAndIpCountryHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegistrationProfileScreen(
    initialName: String = "",
    initialOccupation: String = "",
    initialAvatarUrl: String = "",
    onBackToLogin: () -> Unit = {},
    onCompleteRegistration: (
        name: String,
        age: Int,
        dob: String,
        gender: String,
        interestedIn: String,
        goal: String,
        country: String,
        countryFlag: String,
        place: String,
        qualification: String,
        occupation: String,
        bio: String,
        interests: String,
        maritalStatus: String,
        isOpenForDating: Boolean,
        address: String,
        city: String,
        locationCoords: String,
        avatarEmoji: String,
        photos: List<String>
    ) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    BackHandler {
        onBackToLogin()
    }

    // 1. Profile Photo
    var avatarUri by remember { mutableStateOf(initialAvatarUrl) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri.toString()
        }
    }

    // 2. Full Name (Max 60 chars) - Clear prefilled placeholder values
    val cleanInitialName = if (
        initialName.startsWith("User ", ignoreCase = true) ||
        initialName.startsWith("User0", ignoreCase = true) ||
        initialName.equals("Registered Member", ignoreCase = true) ||
        initialName.equals("VibeSync User", ignoreCase = true)
    ) "" else initialName

    var fullName by remember { mutableStateOf(cleanInitialName) }

    val bioFocusRequester = remember { FocusRequester() }
    val interestsFocusRequester = remember { FocusRequester() }

    // 3. Date of Birth & Calculated Age (Enforce manual selection and 13+ validation)
    var birthDateString by remember { mutableStateOf("") }
    var formattedDobDisplay by remember { mutableStateOf("") }
    var calculatedAge by remember { mutableIntStateOf(0) }

    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - year
                if (today.get(Calendar.DAY_OF_YEAR) < selectedCal.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }
                calculatedAge = age
                birthDateString = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
                formattedDobDisplay = displayFormat.format(selectedCal.time)
            },
            currentYear - 20,
            5,
            15
        ).apply {
            datePicker.maxDate = Calendar.getInstance().timeInMillis
        }
    }

    // 4. Gender (Pills)
    val genderOptions = listOf("Woman", "Man", "Non-binary", "Other")
    var selectedGender by remember { mutableStateOf("Woman") }

    // 5. Relationship Status (Pills)
    val maritalOptions = listOf("Single", "In a relationship", "Divorced", "Complicated")
    var selectedMaritalStatus by remember { mutableStateOf("Single") }

    // 6. Bio (Max 160 chars)
    var bioText by remember { mutableStateOf("") }

    // 7. Passions / Interests
    var interestsText by remember { mutableStateOf("Music, Travel, Coffee") }
    val presetInterests = listOf("Music 🎵", "Travel ✈️", "Coffee ☕", "Photography 📷", "Fitness 🏋️", "Coding 💻", "Art 🎨", "Movies 🍿")

    // 8. Discovery & Contact Matching Toggle
    var isDiscoveryEnabled by remember { mutableStateOf(true) }

    // Detected location fallback
    var detectedCity by remember { mutableStateOf("Bangalore") }
    var detectedState by remember { mutableStateOf("Karnataka") }
    var detectedCountry by remember { mutableStateOf("India") }
    var detectedCountryFlag by remember { mutableStateOf("🇮🇳") }
    var detectedCoords by remember { mutableStateOf("12.9716,77.5946") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val loc = DeviceSimAndIpCountryHelper.fetchDetailedLocation(context)
                detectedCity = loc.cityName.ifBlank { "Bangalore" }
                detectedState = loc.stateOrRegion.ifBlank { "Karnataka" }
                detectedCountry = loc.countryName.ifBlank { "India" }
                detectedCountryFlag = loc.detectedCountry?.flagEmoji ?: "🇮🇳"
                detectedCoords = loc.coordsString.ifBlank { "12.9716,77.5946" }
            } catch (_: Exception) {}
        }
    }

    val isUnder13 = birthDateString.isNotBlank() && calculatedAge < 13
    val isAgeValid = birthDateString.isNotBlank() && calculatedAge >= 13
    val isFormValid = fullName.trim().length >= 2 && isAgeValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Tagline
            Text(
                text = "Welcome to VibeSync ✨",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Set up your public persona and zero-knowledge contact matching",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // 1. Profile Photo Avatar Picker
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .testTag("profile_photo_picker")
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(104.dp)
                        .shadow(6.dp, CircleShape),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(2.dp, Brush.linearGradient(listOf(CoralPink, VibeSyncTeal)))
                ) {
                    if (avatarUri.isNotBlank()) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Avatar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                }

                // Camera Badge Icon at bottom right
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .shadow(4.dp, CircleShape),
                    shape = CircleShape,
                    color = VibeSyncTeal,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = if (avatarUri.isBlank()) "Tap to add profile photo" else "Tap to change photo",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            // 2. Full Name Input (Max 60 chars)
            OutlinedTextField(
                value = fullName,
                onValueChange = { if (it.length <= 60) fullName = it },
                label = { Text("Full Name *") },
                placeholder = { Text("Enter your full name") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = VibeSyncTeal)
                },
                supportingText = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (fullName.trim().length < 2 && fullName.isNotEmpty()) "Minimum 2 characters" else "Required")
                        Text("${fullName.length}/60")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { bioFocusRequester.requestFocus() }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibeSyncTeal,
                    focusedLabelColor = VibeSyncTeal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Date of Birth (Read-only text field triggering native DatePicker with 13+ validation)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
            ) {
                OutlinedTextField(
                    value = if (birthDateString.isBlank()) "" else "$formattedDobDisplay ($calculatedAge yrs)",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false, // Prevents keyboard while keeping clickable wrapper
                    label = { Text("Date of Birth * (13+ only)") },
                    placeholder = { Text("Select Date of Birth (DD/MM/YYYY)") },
                    leadingIcon = {
                        Icon(Icons.Default.Cake, contentDescription = null, tint = if (isUnder13) MaterialTheme.colorScheme.error else CoralPink)
                    },
                    supportingText = {
                        if (isUnder13) {
                            Text(
                                text = "You must be at least 13 years old to use VibeSync",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (birthDateString.isBlank()) {
                            Text("Required (13+ only)")
                        }
                    },
                    isError = isUnder13,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (isUnder13) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = if (isUnder13) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        disabledLabelColor = if (isUnder13) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = if (isUnder13) MaterialTheme.colorScheme.error else CoralPink,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dob_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Gender Selection (Single-select chips)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Gender",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    genderOptions.forEach { option ->
                        val isSelected = selectedGender.equals(option, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGender = option },
                            label = { Text(option, fontSize = 13.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VibeSyncTeal.copy(alpha = 0.15f),
                                selectedLabelColor = VibeSyncTeal,
                                selectedLeadingIconColor = VibeSyncTeal
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Core Identity Permanent Lock Notice Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0F7F6)
                ),
                border = BorderStroke(1.dp, VibeSyncTeal.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = VibeSyncTeal,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Important: Full Name can be updated later, but Date of Birth and Gender are permanently locked to your Core Identity and cannot be changed after registration.",
                        fontSize = 12.sp,
                        color = Color(0xFF1E3A34),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Relationship Status (Single-select chips)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Relationship Status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    maritalOptions.forEach { option ->
                        val isSelected = selectedMaritalStatus.equals(option, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMaritalStatus = option },
                            label = { Text(option, fontSize = 13.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoralPink.copy(alpha = 0.15f),
                                selectedLabelColor = CoralPink,
                                selectedLeadingIconColor = CoralPink
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Bio Input (Multi-line with visible 160-char counter)
            OutlinedTextField(
                value = bioText,
                onValueChange = { if (it.length <= 160) bioText = it },
                label = { Text("About Me / Bio") },
                placeholder = { Text("Share a short line about your vibe...") },
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = "${bioText.length}/160",
                            color = if (bioText.length > 150) CoralPink else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { interestsFocusRequester.requestFocus() }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibeSyncTeal,
                    focusedLabelColor = VibeSyncTeal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(bioFocusRequester)
                    .testTag("bio_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 7. Passions / Interests
            OutlinedTextField(
                value = interestsText,
                onValueChange = { interestsText = it },
                label = { Text("Passions / Interests (comma-separated)") },
                placeholder = { Text("e.g. Music, Travel, Art") },
                leadingIcon = {
                    Icon(Icons.Default.Interests, contentDescription = null, tint = RomanticViolet)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RomanticViolet,
                    focusedLabelColor = RomanticViolet
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(interestsFocusRequester)
                    .testTag("interests_input")
            )

            // Quick add interest chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                presetInterests.take(5).forEach { tag ->
                    val cleanTag = tag.substringBefore(" ")
                    val isAlreadyPresent = interestsText.contains(cleanTag, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAlreadyPresent) VibeSyncTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isAlreadyPresent) BorderStroke(1.dp, VibeSyncTeal) else null,
                        modifier = Modifier.clickable {
                            if (!isAlreadyPresent) {
                                interestsText = if (interestsText.isBlank()) cleanTag else "$interestsText, $cleanTag"
                            }
                        }
                    ) {
                        Text(
                            text = "+ $tag",
                            fontSize = 11.sp,
                            color = if (isAlreadyPresent) VibeSyncTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 8. Discovery & Zero-Knowledge Contact Matching Toggle
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(VibeSyncTeal.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = VibeSyncTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contact Discovery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Allow your saved contacts on VibeSync to find and chat with you securely",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isDiscoveryEnabled,
                        onCheckedChange = { isDiscoveryEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = VibeSyncTeal
                        ),
                        modifier = Modifier.testTag("discovery_switch")
                    )
                }
            }

            // Zero-Knowledge Privacy Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = VibeSyncTeal,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Protected by Zero-Knowledge SHA-256 E2EE hashing",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 9. Full-Width Primary Save & Register Button
            Button(
                onClick = {
                    if (isFormValid) {
                        val finalBio = bioText.ifBlank { "Hey there! I am using VibeSync ✨" }
                        val finalPhotoList = if (avatarUri.isNotBlank()) listOf(avatarUri) else emptyList()

                        onCompleteRegistration(
                            fullName.trim(),
                            calculatedAge,
                            birthDateString,
                            selectedGender,
                            "Everyone",
                            "Connection",
                            detectedCountry,
                            detectedCountryFlag,
                            detectedCity,
                            "Graduate",
                            initialOccupation.ifBlank { "Member" },
                            finalBio,
                            interestsText,
                            selectedMaritalStatus,
                            isDiscoveryEnabled,
                            "$detectedCity, $detectedState",
                            detectedCity,
                            detectedCoords,
                            if (avatarUri.isBlank()) "✨" else avatarUri,
                            finalPhotoList
                        )
                    } else {
                        Toast.makeText(context, "Please enter your name and select date of birth.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibeSyncTeal,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_profile_button")
            ) {
                Text(
                    text = "Save & Complete Profile ✨",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFormValid) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
