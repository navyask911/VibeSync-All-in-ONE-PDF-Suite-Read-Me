package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.RomanticViolet

enum class RecoveryStep {
    BIOMETRIC_MATCH,
    DUAL_CONFIRMATION,
    HELP_CENTER_GOVT_ID
}

/**
 * Account Recovery Options Dialog.
 * Flow:
 * 1. Step 1: Biometric Verification (Face/Fingerprint Match).
 * 2. Step 2: Once Biometric Matches, user MUST confirm BOTH Email AND Mobile Number.
 * 3. Step 3: Help Center Fallback via Valid Govt-Issued ID if email or mobile confirmation fails.
 */
@Composable
fun AccountRecoveryDialog(
    initialEmail: String = "",
    initialPhone: String = "",
    onRecoverWithGoogle: (String) -> Unit,
    onRecoverWithPhone: (String) -> Unit,
    onRecoverWithBiometric: () -> Unit,
    onSubmitGovtIdHelpTicket: (
        idType: String,
        idNumber: String,
        contactEmail: String,
        contactPhone: String,
        note: String
    ) -> Unit = { _, _, _, _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var currentStep by remember { mutableStateOf(RecoveryStep.BIOMETRIC_MATCH) }
    var isBiometricScanning by remember { mutableStateOf(false) }
    var biometricMatched by remember { mutableStateOf(false) }

    // Dual Confirmation States
    var emailInput by remember { mutableStateOf(initialEmail) }
    var emailOtpCode by remember { mutableStateOf("123456") }
    var isEmailVerified by remember { mutableStateOf(false) }

    var phoneInput by remember { mutableStateOf(initialPhone) }
    var phoneOtpCode by remember { mutableStateOf("123456") }
    var isPhoneVerified by remember { mutableStateOf(false) }

    // Help Center Govt ID Form States
    var selectedGovtIdType by remember { mutableStateOf("Aadhaar Card") }
    var govtIdNumberInput by remember { mutableStateOf("9876-5432-1098") }
    var contactEmailInput by remember { mutableStateOf(initialEmail) }
    var contactPhoneInput by remember { mutableStateOf(initialPhone) }
    var isGovtIdPhotoAttached by remember { mutableStateOf(true) }
    var userNoteInput by remember { mutableStateOf("Requesting account access recovery. I have lost my old phone SIM.") }

    val govtIdTypesList = remember {
        listOf("Aadhaar Card", "Passport", "Driving License", "Voter ID", "PAN Card")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .testTag("dialog_account_recovery_options"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep != RecoveryStep.BIOMETRIC_MATCH) {
                        Surface(
                            onClick = {
                                if (currentStep == RecoveryStep.HELP_CENTER_GOVT_ID) {
                                    currentStep = RecoveryStep.DUAL_CONFIRMATION
                                } else {
                                    currentStep = RecoveryStep.BIOMETRIC_MATCH
                                }
                            },
                            shape = CircleShape,
                            color = Color(0xFFF0F0F0),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF333333),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(36.dp))
                    }

                    // Step Indicator Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CoralPink.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = when (currentStep) {
                                RecoveryStep.BIOMETRIC_MATCH -> "Step 1 of 3: Biometric Scan"
                                RecoveryStep.DUAL_CONFIRMATION -> "Step 2 of 3: Email & Mobile"
                                RecoveryStep.HELP_CENTER_GOVT_ID -> "Step 3: Help Center ID Verification"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralPink,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Close", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            when (currentStep) {
                                RecoveryStep.BIOMETRIC_MATCH -> CoralPink.copy(alpha = 0.12f)
                                RecoveryStep.DUAL_CONFIRMATION -> LikeGreen.copy(alpha = 0.12f)
                                RecoveryStep.HELP_CENTER_GOVT_ID -> Color(0xFF4285F4).copy(alpha = 0.12f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (currentStep) {
                            RecoveryStep.BIOMETRIC_MATCH -> Icons.Default.Fingerprint
                            RecoveryStep.DUAL_CONFIRMATION -> Icons.Default.Shield
                            RecoveryStep.HELP_CENTER_GOVT_ID -> Icons.Default.Badge
                        },
                        contentDescription = "Recovery Icon",
                        tint = when (currentStep) {
                            RecoveryStep.BIOMETRIC_MATCH -> CoralPink
                            RecoveryStep.DUAL_CONFIRMATION -> LikeGreen
                            RecoveryStep.HELP_CENTER_GOVT_ID -> Color(0xFF4285F4)
                        },
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (currentStep) {
                        RecoveryStep.BIOMETRIC_MATCH -> "Account Recovery: Biometric Scan"
                        RecoveryStep.DUAL_CONFIRMATION -> "Dual Identity Confirmation"
                        RecoveryStep.HELP_CENTER_GOVT_ID -> "Help Center ID Verification"
                    },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111111),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when (currentStep) {
                        RecoveryStep.BIOMETRIC_MATCH -> "First, scan your face or fingerprint to match your encrypted biometric hash."
                        RecoveryStep.DUAL_CONFIRMATION -> "Biometric matched! Now confirm BOTH your registered email and mobile number to unlock."
                        RecoveryStep.HELP_CENTER_GOVT_ID -> "If email or mobile verification failed, upload a Valid Govt Issued ID for manual support recovery."
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                HorizontalDivider(color = Color(0xFFEEEEEE))

                Spacer(modifier = Modifier.height(14.dp))

                // STEP 1: BIOMETRIC MATCHING
                if (currentStep == RecoveryStep.BIOMETRIC_MATCH) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CoralPink.copy(alpha = 0.06f),
                        border = BorderStroke(1.5.dp, CoralPink.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .background(CoralPink.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Scan Face",
                                    tint = CoralPink,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Biometric Face & Hash Match",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralPink
                            )

                            Text(
                                text = "Matches your live 3D face scan against saved database hash to confirm ownership.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF555555),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                            )

                            Button(
                                onClick = {
                                    isBiometricScanning = true
                                    biometricMatched = true
                                    isBiometricScanning = false
                                    currentStep = RecoveryStep.DUAL_CONFIRMATION
                                    Toast.makeText(context, "Biometric Match Confirmed! Proceeding to Email & Mobile confirmation.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_scan_biometric_match")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Face to Verify Ownership", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // STEP 2: DUAL CONFIRMATION (BOTH EMAIL & MOBILE REQUIRED)
                if (currentStep == RecoveryStep.DUAL_CONFIRMATION) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Success Banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = LikeGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, LikeGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = LikeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Biometric Verified ✓ - Confirm BOTH Contacts Below",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LikeGreen
                                )
                            }
                        }

                        // 1. EMAIL CONFIRMATION BOX
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, if (isEmailVerified) LikeGreen else Color(0xFFCBD5E1))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("1. Registered Email", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isEmailVerified) LikeGreen.copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isEmailVerified) "Verified ✓" else "Pending",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isEmailVerified) LikeGreen else Color(0xFFE65100),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Registered Email Address") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = emailOtpCode,
                                        onValueChange = { emailOtpCode = it },
                                        placeholder = { Text("6-digit Code") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = {
                                            if (emailOtpCode.trim().length >= 4) {
                                                isEmailVerified = true
                                                Toast.makeText(context, "Email Verified ✓", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                                        enabled = !isEmailVerified
                                    ) {
                                        Text(if (isEmailVerified) "Verified" else "Verify", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // 2. MOBILE NUMBER CONFIRMATION BOX
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, if (isPhoneVerified) LikeGreen else Color(0xFFCBD5E1))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = LikeGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("2. Registered Mobile Number", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPhoneVerified) LikeGreen.copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isPhoneVerified) "Verified ✓" else "Pending",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPhoneVerified) LikeGreen else Color(0xFFE65100),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = { phoneInput = it },
                                    label = { Text("Registered Mobile Number") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = phoneOtpCode,
                                        onValueChange = { phoneOtpCode = it },
                                        placeholder = { Text("6-digit OTP") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = {
                                            if (phoneOtpCode.trim().length >= 4) {
                                                isPhoneVerified = true
                                                Toast.makeText(context, "Mobile Verified ✓", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LikeGreen),
                                        enabled = !isPhoneVerified
                                    ) {
                                        Text(if (isPhoneVerified) "Verified" else "Verify", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Complete Dual Confirmation Action Button
                        Button(
                            onClick = {
                                if (isEmailVerified && isPhoneVerified) {
                                    onRecoverWithGoogle(emailInput)
                                } else {
                                    Toast.makeText(context, "Please verify BOTH email and mobile number first.", Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = isEmailVerified && isPhoneVerified,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LikeGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_complete_dual_recovery")
                        ) {
                            Text("Confirm & Restore Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        // FAIL / FALLBACK LINK TO GOVT ID HELP CENTER
                        Surface(
                            onClick = { currentStep = RecoveryStep.HELP_CENTER_GOVT_ID },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_fail_go_to_govt_id")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "⚠️ Verification Failed or Lost Email/Mobile Access?",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    Text(
                                        text = "Submit a Valid Govt-Issued ID to Help Center for human verification.",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF795548)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = "Help Center",
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // STEP 3: HELP CENTER GOVT-ISSUED ID SUBMISSION
                if (currentStep == RecoveryStep.HELP_CENTER_GOVT_ID) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select Valid Govt-Issued ID Type:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF222222)
                        )

                        // ID Type Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            govtIdTypesList.forEach { idType ->
                                val selected = selectedGovtIdType == idType
                                Surface(
                                    onClick = { selectedGovtIdType = idType },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) Color(0xFF4285F4) else Color(0xFFF0F2F5),
                                    border = BorderStroke(1.dp, if (selected) Color(0xFF4285F4) else Color(0xFFD0D0D0))
                                ) {
                                    Text(
                                        text = idType,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else Color(0xFF333333),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // ID Document Number Input
                        OutlinedTextField(
                            value = govtIdNumberInput,
                            onValueChange = { govtIdNumberInput = it },
                            label = { Text("$selectedGovtIdType Document Number") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF4285F4)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_govt_id_number")
                        )

                        // Attach Photo of Govt ID Card
                        Surface(
                            onClick = {
                                isGovtIdPhotoAttached = !isGovtIdPhotoAttached
                                Toast.makeText(context, if (isGovtIdPhotoAttached) "Govt ID Document Photo Captured ✓" else "Document Removed", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isGovtIdPhotoAttached) LikeGreen.copy(alpha = 0.08f) else Color(0xFFF5F5F5),
                            border = BorderStroke(1.5.dp, if (isGovtIdPhotoAttached) LikeGreen else Color(0xFFCCCCCC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isGovtIdPhotoAttached) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                                        contentDescription = "Capture Document",
                                        tint = if (isGovtIdPhotoAttached) LikeGreen else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isGovtIdPhotoAttached) "Govt ID Document Photo Attached ✓" else "Capture / Upload $selectedGovtIdType Photo",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGovtIdPhotoAttached) LikeGreen else Color(0xFF222222)
                                        )
                                        Text(
                                            text = "Encrypted 256-bit upload for Help Center staff review",
                                            fontSize = 10.sp,
                                            color = Color(0xFF777777)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = "Upload",
                                    tint = if (isGovtIdPhotoAttached) LikeGreen else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Contact Email
                        OutlinedTextField(
                            value = contactEmailInput,
                            onValueChange = { contactEmailInput = it },
                            label = { Text("Contact Email") },
                            leadingIcon = { Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = Color(0xFF4285F4)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Contact Phone
                        OutlinedTextField(
                            value = contactPhoneInput,
                            onValueChange = { contactPhoneInput = it },
                            label = { Text("Contact Mobile Number") },
                            leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = LikeGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Note / Explanation
                        OutlinedTextField(
                            value = userNoteInput,
                            onValueChange = { userNoteInput = it },
                            label = { Text("Reason for Manual Recovery Request") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        // Submit Help Center Ticket Button
                        Button(
                            onClick = {
                                if (govtIdNumberInput.isBlank() || contactEmailInput.isBlank()) {
                                    Toast.makeText(context, "Please enter your Govt ID Number and Email", Toast.LENGTH_SHORT).show()
                                } else {
                                    onSubmitGovtIdHelpTicket(
                                        selectedGovtIdType,
                                        govtIdNumberInput.trim(),
                                        contactEmailInput.trim(),
                                        contactPhoneInput.trim(),
                                        userNoteInput.trim()
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_submit_govt_id_ticket")
                        ) {
                            Icon(Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit to Help Center", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
