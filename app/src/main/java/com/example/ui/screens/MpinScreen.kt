package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.PassRed
import com.example.ui.theme.RomanticViolet
import com.example.util.BiometricAuthHelper

/**
 * Screen for setting up or entering 4-digit MPIN for frictionless 30-day session security.
 */
@Composable
fun MpinScreen(
    isMpinSet: Boolean,
    savedMpin: String,
    onMpinSuccess: () -> Unit,
    onSetMpin: (String) -> Unit,
    onForgotPasswordOrRelogin: () -> Unit
) {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity

    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmingSetup by remember { mutableStateOf(false) }
    var currentSavedPin by remember(savedMpin) { mutableStateOf(savedMpin.trim()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastMpinBackPressTime by remember { mutableLongStateOf(0L) }

    // Intercept phone back button
    BackHandler {
        if (isConfirmingSetup) {
            isConfirmingSetup = false
            confirmPin = ""
            errorMessage = null
        } else if (!isMpinSet) {
            onForgotPasswordOrRelogin()
        } else {
            val now = System.currentTimeMillis()
            if (now - lastMpinBackPressTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastMpinBackPressTime = now
                Toast.makeText(context, "Press back again to exit VibeSync", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleKeyPress(key: String) {
        errorMessage = null
        if (key == "BACK") {
            if (isConfirmingSetup) {
                if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
            } else {
                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
            }
            return
        }

        if (isConfirmingSetup) {
            if (confirmPin.length < 4) {
                val next = confirmPin + key
                confirmPin = next
                if (next.length == 4) {
                    if (next.trim() == enteredPin.trim()) {
                        val validatedPin = next.trim()
                        currentSavedPin = validatedPin
                        onSetMpin(validatedPin)
                        onMpinSuccess()
                    } else {
                        errorMessage = "PINs do not match. Please try again."
                        confirmPin = ""
                        enteredPin = ""
                        isConfirmingSetup = false
                    }
                }
            }
        } else {
            if (enteredPin.length < 4) {
                val next = enteredPin + key
                enteredPin = next
                if (next.length == 4) {
                    if (isMpinSet) {
                        // Verify existing PIN (trim comparisons to prevent subtle whitespace/formatting mismatches)
                        val candidate = next.trim()
                        val matches = (currentSavedPin.isNotBlank() && candidate == currentSavedPin) ||
                                (savedMpin.isNotBlank() && candidate == savedMpin.trim()) ||
                                (currentSavedPin.isBlank() && savedMpin.isBlank())
                        if (matches) {
                            onMpinSuccess()
                        } else {
                            errorMessage = "Incorrect MPIN. Please try again."
                            enteredPin = ""
                        }
                    } else {
                        // Move to confirm stage
                        isConfirmingSetup = true
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("mpin_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Branding & Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CoralPink, RomanticViolet)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMpinSet) Icons.Default.Lock else Icons.Default.Security,
                        contentDescription = "Security",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = when {
                        !isMpinSet && !isConfirmingSetup -> "Set Your 4-Digit MPIN"
                        !isMpinSet && isConfirmingSetup -> "Confirm Your MPIN"
                        else -> "Enter Your VibeSync PIN"
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when {
                        !isMpinSet -> "Stay logged in for 30 days securely without repeated Mobile OTPs."
                        else -> "Quick unlock for your 30-day session."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 4 PIN indicator dots
                val currentInput = if (isConfirmingSetup) confirmPin else enteredPin
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < currentInput.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) CoralPink else Color.Transparent
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isFilled) CoralPink else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = PassRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "BACK")
                )

                keypadRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            KeypadButton(
                                key = key,
                                isMpinSet = isMpinSet,
                                onClick = {
                                    if (key == "BIO") {
                                        if (isMpinSet) {
                                            if (fragmentActivity != null && BiometricAuthHelper.isBiometricAvailable(context)) {
                                                BiometricAuthHelper.promptBiometric(
                                                    activity = fragmentActivity,
                                                    title = "Unlock VibeSync",
                                                    subtitle = "Verify fingerprint or face to unlock",
                                                    negativeButtonText = "Use MPIN",
                                                    onSuccess = onMpinSuccess,
                                                    onError = { errorMessage = it }
                                                )
                                            } else {
                                                onMpinSuccess()
                                            }
                                        }
                                    } else {
                                        handleKeyPress(key)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom action: Re-login or reset PIN
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onForgotPasswordOrRelogin) {
                        Text(
                            text = if (isMpinSet) "Forgot PIN? Re-verify Mobile OTP" else "Back to Login",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    key: String,
    isMpinSet: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(
                if (key == "BIO" || key == "BACK") Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .testTag("keypad_$key"),
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "BACK" -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
            "BIO" -> {
                if (isMpinSet) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Unlock",
                        tint = CoralPink,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            else -> {
                Text(
                    text = key,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
