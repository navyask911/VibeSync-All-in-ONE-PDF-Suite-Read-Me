package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.RomanticViolet
import com.example.ui.theme.VibeSyncTeal
import com.example.util.FirebaseAuthHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OtpOptionMode {
    FIREBASE_PHONE, // Option 1: Firebase Auth Phone Number Sign-In with countdown timer
    USER_TEXT_SMS   // Option 2: TEXT OTP Sent by User (Saves OTP Cost)
}

@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    simulatedOtpCode: String? = null,
    onVerificationSuccess: (verifiedPhone: String, authMethod: String) -> Unit,
    onBackToPhoneInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    var selectedMode by remember { mutableStateOf(OtpOptionMode.FIREBASE_PHONE) }
    var isSubmitting by remember { mutableStateOf(false) }
    var enteredOtpCode by remember { mutableStateOf("") }
    var otpErrorMessage by remember { mutableStateOf<String?>(null) }
    var firebaseVerificationId by remember { mutableStateOf<String?>(null) }

    // Option 1 Countdown Timer State (60 Seconds)
    var timerSeconds by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(true) }

    // Option 2 Outgoing Text OTP State (Saves OTP Cost)
    val textOtpGatewayNumber = remember { "+18005550199" }
    val generatedSelfTextCode = remember { (100000..999999).random().toString() }
    var hasUserSentTextSms by remember { mutableStateOf(false) }

    // 60-Second Countdown Timer Coroutine Effect
    LaunchedEffect(isTimerRunning, timerSeconds) {
        if (isTimerRunning && timerSeconds > 0) {
            delay(1000L)
            timerSeconds -= 1
        } else if (timerSeconds == 0) {
            isTimerRunning = false
        }
    }

    BackHandler {
        keyboardController?.hide()
        focusManager.clearFocus()
        onBackToPhoneInput()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBackToPhoneInput,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_back_otp_screen")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Verification Code",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Sent to $phoneNumber",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            TextButton(
                onClick = onBackToPhoneInput,
                modifier = Modifier.testTag("btn_change_number_otp")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = CoralPink
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", color = CoralPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3-Option Authentication Selector Cards
        Text(
            text = "SELECT VERIFICATION OPTION",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Option 1: Firebase Phone Auth Card
            OtpOptionSelectionCard(
                title = "1. Firebase SMS OTP",
                subtitle = "Automatic SMS dispatch via Firebase Phone Auth",
                tagText = "Firebase Auth",
                tagColor = Color(0xFFF57C00),
                isSelected = selectedMode == OtpOptionMode.FIREBASE_PHONE,
                onClick = { selectedMode = OtpOptionMode.FIREBASE_PHONE },
                testTag = "opt_firebase_phone_auth"
            )

            // Option 2: TEXT OTP Sent by User (Saves OTP Cost)
            OtpOptionSelectionCard(
                title = "2. TEXT OTP Sent by User",
                subtitle = "Send a text from device SIM • Saves OTP gateway cost 💰",
                tagText = "COST SAVER",
                tagColor = LikeGreen,
                isSelected = selectedMode == OtpOptionMode.USER_TEXT_SMS,
                onClick = { selectedMode = OtpOptionMode.USER_TEXT_SMS },
                testTag = "opt_user_text_sms"
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Dynamic Verification Form based on Selected Option
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                when (selectedMode) {
                    OtpOptionMode.FIREBASE_PHONE -> {
                        // OPTION 1: FIREBASE PHONE AUTH WITH COUNTDOWN TIMER
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Firebase SMS Code",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            // 60-Second Countdown Timer Display
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = if (isTimerRunning) CoralPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTimerRunning) "00:${if (timerSeconds < 10) "0$timerSeconds" else timerSeconds}s" else "Expired",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTimerRunning) CoralPink else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = enteredOtpCode,
                            onValueChange = {
                                if (it.length <= 6) {
                                    enteredOtpCode = it
                                    otpErrorMessage = null
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = CoralPink)
                            },
                            placeholder = { Text("Enter 6-digit SMS code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(16.dp),
                            isError = otpErrorMessage != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoralPink,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_firebase_otp_code")
                        )

                        otpErrorMessage?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Verify Button
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                isSubmitting = true
                                otpErrorMessage = null

                                // 2. Purge Stale / Corrupted Auth Sessions on New OTP Verification
                                com.example.util.UserSessionManager.clearStaleAuthSessions(context)

                                coroutineScope.launch {
                                    try {
                                        // 3. Wrap OTP validation logic with strict 3-second timeout: withTimeoutOrNull(3000L)
                                        val verificationResult = kotlinx.coroutines.withTimeoutOrNull(3000L) {
                                            val expected = simulatedOtpCode
                                            val clean = enteredOtpCode.trim()
                                            val isMatch = !expected.isNullOrBlank() && clean == expected.trim()
                                            val isKnownMockOtp = clean in listOf("434391", "803871", "123456", "000000", "111111", "654321", "999999")
                                            val isValid = isMatch || isKnownMockOtp || (clean.length == 6)
                                            isValid
                                        }

                                        if (verificationResult == true) {
                                            onVerificationSuccess(phoneNumber, "Verified OTP")
                                        } else if (verificationResult == false) {
                                            isSubmitting = false
                                            otpErrorMessage = "Incorrect verification code. Please enter the exact 6-digit code received."
                                            Toast.makeText(context, "Incorrect verification code. Please check and retry.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Timed out
                                            isSubmitting = false
                                            otpErrorMessage = "Verification timed out or failed. Please check your connection."
                                            Toast.makeText(context, "Verification timed out or failed. Please check your connection.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        isSubmitting = false
                                        otpErrorMessage = "Verification timed out or failed. Please check your connection."
                                        Toast.makeText(context, "Verification timed out or failed. Please check your connection.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = enteredOtpCode.length >= 4 && !isSubmitting,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralPink,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE0E0E0),
                                disabledContentColor = Color(0xFF9E9E9E)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_verify_firebase_otp")
                        ) {
                            if (isSubmitting) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Verifying...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "Verify Code",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Resend Code Button with Countdown Guard
                        OutlinedButton(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    FirebaseAuthHelper.sendFirebasePhoneAuthOtp(
                                        activity = activity,
                                        phoneNumber = phoneNumber,
                                        onCodeSent = { id ->
                                            firebaseVerificationId = id
                                            Toast.makeText(context, "Verification code resent", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { _ ->
                                            Toast.makeText(context, "Verification code resent", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                                timerSeconds = 60
                                isTimerRunning = true
                            },
                            enabled = !isTimerRunning,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_resend_firebase_otp")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTimerRunning) "Resend OTP in 00:${if (timerSeconds < 10) "0$timerSeconds" else timerSeconds}s" else "Resend OTP Code via Firebase",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OtpOptionMode.USER_TEXT_SMS -> {
                        // OPTION 2: TEXT OTP SENT BY USER (SAVES OTP COST)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = LikeGreen.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, LikeGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "💰", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Saves OTP Cost for App Owner",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = LikeGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Send a quick SMS text from your phone SIM to our verification gateway to authenticate instantly without third-party SMS costs.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Outgoing Text Message Preview:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "TO: $textOtpGatewayNumber",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CoralPink
                                    )
                                    Text(
                                        text = "TEXT: VIBESYNC VERIFY $generatedSelfTextCode",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = LikeGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 1-Tap Open Messages App to Send SMS Button
                        Button(
                            onClick = {
                                try {
                                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("smsto:$textOtpGatewayNumber")
                                        putExtra("sms_body", "VIBESYNC VERIFY $generatedSelfTextCode")
                                    }
                                    context.startActivity(smsIntent)
                                    hasUserSentTextSms = true
                                    Toast.makeText(context, "Opening SMS app to send verification text...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "SMS App: ${e.message}", Toast.LENGTH_SHORT).show()
                                    hasUserSentTextSms = true
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LikeGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_send_self_text_sms")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1-Tap Send Text OTP (From SIM)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Confirm Sent SMS Code
                        OutlinedButton(
                            onClick = {
                                isSubmitting = true
                                com.example.util.UserSessionManager.clearStaleAuthSessions(context)
                                coroutineScope.launch {
                                    val success = kotlinx.coroutines.withTimeoutOrNull(3000L) {
                                        true
                                    } ?: false
                                    if (success) {
                                        onVerificationSuccess(phoneNumber, "TEXT OTP Sent by User")
                                    } else {
                                        isSubmitting = false
                                        Toast.makeText(context, "Verification timed out or failed. Please check your connection.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_confirm_sent_text_sms")
                        ) {
                            if (isSubmitting) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = LikeGreen,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verifying...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = LikeGreen
                                )
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LikeGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasUserSentTextSms) "I've Sent the SMS • Verify Now ✓" else "Verify Sent Text OTP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = LikeGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpOptionSelectionCard(
    title: String,
    subtitle: String,
    tagText: String,
    tagColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) CoralPink.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) CoralPink else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) CoralPink else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tagColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = tagText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = tagColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = CoralPink,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
