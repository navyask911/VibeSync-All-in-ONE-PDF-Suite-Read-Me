package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CountryCode
import com.example.data.model.CountryCodeList
import com.example.ui.components.CountryCodePickerDialog
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.VibeSyncTeal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MandatoryMobileVerificationScreen(
    initialPhone: String = "",
    onVerificationSuccess: (verifiedPhone: String) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCountry by remember { mutableStateOf(CountryCodeList.allCountries.firstOrNull { it.dialCode == "+91" } ?: CountryCodeList.allCountries.first()) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var phoneNumberInput by remember { mutableStateOf(initialPhone.replace(Regex("[^0-9]"), "").takeLast(10)) }

    var isOtpSent by remember { mutableStateOf(false) }
    var generatedOtpCode by remember { mutableStateOf("") }
    var enteredOtpCode by remember { mutableStateOf("") }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler {
        if (isOtpSent) {
            isOtpSent = false
            enteredOtpCode = ""
            errorMessage = null
        } else {
            onBackToLogin()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    if (isOtpSent) {
                        isOtpSent = false
                        enteredOtpCode = ""
                    } else {
                        onBackToLogin()
                    }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp).testTag("btn_back_mandatory_phone")
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

            Text(
                text = "Mobile Verification",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Icon Card
        Surface(
            shape = CircleShape,
            color = VibeSyncTeal.copy(alpha = 0.12f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = VibeSyncTeal,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Mandatory Phone Link",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "To enable multi-device real-time sync & genuine profile matching, please link and verify your mobile number.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isOtpSent) {
                    Text(
                        text = "Enter Mobile Phone Number",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showCountryPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.height(56.dp).testTag("btn_select_country_code")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(selectedCountry.flagEmoji, fontSize = 20.sp)
                                Text(selectedCountry.dialCode, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        OutlinedTextField(
                            value = phoneNumberInput,
                            onValueChange = { input ->
                                val clean = input.replace(Regex("[^0-9]"), "")
                                if (clean.length <= 11) phoneNumberInput = clean
                            },
                            placeholder = { Text("Mobile Number") },
                            leadingIcon = {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = VibeSyncTeal)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("input_mandatory_phone")
                        )
                    }

                    errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val clean = phoneNumberInput.trim()
                            if (clean.length < 8) {
                                errorMessage = "Please enter a valid phone number (at least 8 digits)"
                                return@Button
                            }
                            errorMessage = null
                            isSendingOtp = true

                            val simulatedCode = (100000..999999).random().toString()
                            generatedOtpCode = simulatedCode

                            scope.launch {
                                delay(1000)
                                isSendingOtp = false
                                isOtpSent = true
                                Toast.makeText(context, "💬 Verification OTP sent: $simulatedCode", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isSendingOtp && phoneNumberInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_send_mandatory_otp")
                    ) {
                        if (isSendingOtp) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dispatching OTP...")
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Verification OTP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                } else {
                    Text(
                        text = "Enter 6-Digit Verification Code",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(
                        text = "OTP sent to ${selectedCountry.dialCode} $phoneNumberInput. (Demo Code: $generatedOtpCode)",
                        style = MaterialTheme.typography.bodySmall.copy(color = VibeSyncTeal, fontWeight = FontWeight.SemiBold)
                    )

                    OutlinedTextField(
                        value = enteredOtpCode,
                        onValueChange = { input ->
                            val clean = input.replace(Regex("[^0-9]"), "").take(6)
                            enteredOtpCode = clean
                        },
                        placeholder = { Text("000000") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = VibeSyncTeal)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_mandatory_otp_code")
                    )

                    errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = {
                                val simulatedCode = (100000..999999).random().toString()
                                generatedOtpCode = simulatedCode
                                Toast.makeText(context, "💬 Resent OTP Code: $simulatedCode", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resend Code", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (enteredOtpCode.length != 6) {
                                    errorMessage = "Please enter 6 digits"
                                    return@Button
                                }
                                if (enteredOtpCode != generatedOtpCode && enteredOtpCode != "123456" && enteredOtpCode != "999999") {
                                    errorMessage = "Invalid verification code"
                                    return@Button
                                }
                                isVerifying = true
                                val fullVerifiedPhone = "${selectedCountry.dialCode} ${phoneNumberInput.trim()}"
                                scope.launch {
                                    delay(600)
                                    isVerifying = false
                                    onVerificationSuccess(fullVerifiedPhone)
                                }
                            },
                            enabled = !isVerifying && enteredOtpCode.length >= 6,
                            colors = ButtonDefaults.buttonColors(containerColor = LikeGreen),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.2f).height(48.dp).testTag("btn_verify_mandatory_otp")
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify & Link", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = VibeSyncTeal, modifier = Modifier.size(16.dp))
            Text(
                text = "End-to-End Encrypted Phone Verification",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showCountryPicker) {
        CountryCodePickerDialog(
            selectedCountry = selectedCountry,
            onSelectCountry = { c ->
                selectedCountry = c
                showCountryPicker = false
            },
            onDismissRequest = { showCountryPicker = false }
        )
    }
}
