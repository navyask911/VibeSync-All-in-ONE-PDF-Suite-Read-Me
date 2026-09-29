package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.util.BiometricAuthManager
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserPreferencesEntity
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.VibeSyncTeal

@Composable
fun SecurityMpinCard(
    preferences: UserPreferencesEntity,
    onUpdatePreferences: (UserPreferencesEntity) -> Unit,
    onToggleProfileLock: (Boolean) -> Unit,
    onOpenMpinSetup: () -> Unit,
    onSetMpin: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    var showInlineMpinDialog by remember { mutableStateOf(false) }
    var inlinePin by remember { mutableStateOf("") }
    var inlineConfirmPin by remember { mutableStateOf("") }
    var inlinePinError by remember { mutableStateOf<String?>(null) }
    var inlinePinSuccess by remember { mutableStateOf(false) }

    val isMpinActive = preferences.isMpinSet && preferences.userMpin.isNotBlank()
    val statusText = if (isMpinActive) "4-Digit MPIN Active 🔒" else "MPIN Not Set (Tap to Enable)"
    val statusColor = if (isMpinActive) LikeGreen else Color(0xFFE65100)

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var isBiometricEnabled by remember { mutableStateOf(BiometricAuthManager.isBiometricLockEnabled(context)) }
    val isBiometricSupported = remember { BiometricAuthManager.isBiometricAvailable(context) }

    ProfileSectionDrawer(
        title = "Security & 4-Digit MPIN",
        icon = Icons.Default.Lock,
        iconTint = Color(0xFF0288D1),
        iconBackground = Color(0xFF0288D1).copy(alpha = 0.15f),
        statusBadgeText = statusText,
        statusBadgeColor = statusColor,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        testTag = "drawer_security_mpin"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // MPIN Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMpinActive) LikeGreen.copy(alpha = 0.08f) else Color(0xFFE65100).copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isMpinActive) LikeGreen.copy(alpha = 0.3f) else Color(0xFFE65100).copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isMpinActive) LikeGreen else Color(0xFFE65100)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMpinActive) Icons.Default.Lock else Icons.Default.Key,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMpinActive) "4-Digit MPIN Protected" else "Set Your 4-Digit MPIN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isMpinActive) "Instant 30-day session unlock active" else "Fast unlock without typing SMS codes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Set / Change MPIN Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        inlinePin = ""
                        inlineConfirmPin = ""
                        inlinePinError = null
                        inlinePinSuccess = false
                        showInlineMpinDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_set_mpin_dialog"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSyncTeal)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMpinActive) "Change MPIN" else "Set 4-Digit MPIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onOpenMpinSetup,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_open_full_mpin_prompt"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Full PIN Pad",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // Privacy & Profile Lock Switches
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Profile Lock Toggle
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
                            imageVector = if (preferences.isProfileLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (preferences.isProfileLocked) LikeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Profile Lock Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (preferences.isProfileLocked) "Locked: Photos hidden from non-matches" else "Publicly visible to discover feed",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = preferences.isProfileLocked,
                        onCheckedChange = { locked ->
                            onToggleProfileLock(locked)
                            onUpdatePreferences(preferences.copy(isProfileLocked = locked))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = LikeGreen
                        ),
                        modifier = Modifier.testTag("switch_profile_lock")
                    )
                }

                // Hide Phone Number Toggle
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
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hide Phone Number",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Never share mobile number with matches",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = preferences.hidePhoneNumber,
                        onCheckedChange = { hide ->
                            onUpdatePreferences(preferences.copy(hidePhoneNumber = hide))
                        },
                        modifier = Modifier.testTag("switch_hide_phone")
                    )
                }

                // Granular Location Privacy & View Control (User Choice)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text("📍 Location Visibility & Privacy Control", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "User Choice: Control who can view your location (Never shared with anonymous users):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val options = listOf(
                        "EVERYONE" to "🌐 Everyone",
                        "PHONE_CONTACTS" to "📱 Contacts",
                        "FRIENDS_ONLY" to "🤝 Friends",
                        "SELECTED_PERSONS" to "👤 Particular Person",
                        "NO_ONE" to "🔒 No One"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        options.chunked(3).forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowOptions.forEach { (key, label) ->
                                    val isSelected = preferences.locationVisibility == key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            onUpdatePreferences(
                                                preferences.copy(
                                                    locationVisibility = key,
                                                    hideExactLocation = (key == "NO_ONE")
                                                )
                                            )
                                        },
                                        label = { Text(label, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowOptions.size < 3) {
                                    Spacer(modifier = Modifier.weight((3 - rowOptions.size).toFloat()))
                                }
                            }
                        }
                    }

                    if (preferences.locationVisibility == "SELECTED_PERSONS") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = preferences.allowedLocationUserIds,
                            onValueChange = { newAllowed ->
                                onUpdatePreferences(preferences.copy(allowedLocationUserIds = newAllowed))
                            },
                            label = { Text("Particular Allowed Persons (Names / IDs)", fontSize = 11.sp) },
                            placeholder = { Text("e.g. Maya Lin, Alex Rivera", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                    }
                }

                // Biometric Unlock (Fingerprint / Face Unlock) Toggle
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
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (isBiometricEnabled) LikeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Biometric Lock (Fingerprint / Face)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBiometricSupported) {
                                    if (isBiometricEnabled) "Biometric verification active for profile & chats" else "Use fingerprint or 3D Face to unlock app"
                                } else {
                                    "No biometric hardware or lock enrolled on device"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (activity != null) {
                                    BiometricAuthManager.authenticate(
                                        activity = activity,
                                        title = "Enable Biometric Security",
                                        subtitle = "Confirm your fingerprint or face to activate biometric lock",
                                        onResult = { result ->
                                            when (result) {
                                                is BiometricAuthManager.BiometricResult.Success -> {
                                                    BiometricAuthManager.setBiometricLockEnabled(context, true)
                                                    isBiometricEnabled = true
                                                    Toast.makeText(context, "Biometric Lock Activated! 🛡️", Toast.LENGTH_SHORT).show()
                                                }
                                                is BiometricAuthManager.BiometricResult.Error -> {
                                                    Toast.makeText(context, "Biometric error: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                                }
                                                is BiometricAuthManager.BiometricResult.Failed -> {
                                                    Toast.makeText(context, "Unrecognized biometric pattern", Toast.LENGTH_SHORT).show()
                                                }
                                                is BiometricAuthManager.BiometricResult.Cancelled -> {
                                                    Toast.makeText(context, "Biometric setup cancelled", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    BiometricAuthManager.setBiometricLockEnabled(context, true)
                                    isBiometricEnabled = true
                                }
                            } else {
                                BiometricAuthManager.setBiometricLockEnabled(context, false)
                                isBiometricEnabled = false
                                Toast.makeText(context, "Biometric Lock Deactivated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = LikeGreen
                        ),
                        modifier = Modifier.testTag("switch_biometric_lock")
                    )
                }
            }
        }
    }

    // Direct Inline MPIN Entry Dialog
    if (showInlineMpinDialog) {
        AlertDialog(
            onDismissRequest = { showInlineMpinDialog = false },
            title = {
                Text(
                    text = if (isMpinActive) "Change 4-Digit MPIN" else "Set 4-Digit MPIN",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter a memorable 4-digit numeric code to protect your chats and enable quick login.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inlinePin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                inlinePin = it
                                inlinePinError = null
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        placeholder = { Text("••••") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_mpin")
                    )

                    OutlinedTextField(
                        value = inlineConfirmPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                inlineConfirmPin = it
                                inlinePinError = null
                            }
                        },
                        label = { Text("Confirm 4-Digit PIN") },
                        placeholder = { Text("••••") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_mpin")
                    )

                    inlinePinError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (inlinePinSuccess) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LikeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "4-Digit MPIN saved successfully! 🔒",
                                color = LikeGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inlinePin.length != 4) {
                            inlinePinError = "PIN must be exactly 4 digits."
                        } else if (inlinePin != inlineConfirmPin) {
                            inlinePinError = "PINs do not match. Please re-enter."
                        } else {
                            onSetMpin(inlinePin)
                            onUpdatePreferences(
                                preferences.copy(
                                    userMpin = inlinePin,
                                    isMpinSet = true
                                )
                            )
                            inlinePinSuccess = true
                            showInlineMpinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSyncTeal),
                    modifier = Modifier.testTag("btn_save_mpin")
                ) {
                    Text("Save MPIN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInlineMpinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
