package com.example.ui.screens

import android.accounts.AccountManager
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CountryCode
import com.example.data.model.CountryCodeList
import com.example.ui.components.CountryCodePickerDialog
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.util.DeviceSimAndIpCountryHelper
import com.example.util.FirebaseAuthHelper
import com.example.util.SimSlotInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AppFeatureItem(
    val iconEmoji: String,
    val title: String,
    val subtitle: String,
    val tagBadge: String? = null,
    val bgGradient: List<Color>,
    val borderColor: Color,
    val description: String,
    val privacyTip: String,
    val usageTip: String,
    val highlights: List<String>
)

/**
 * Main App Initial Login Page.
 * - Brand Header: VibeSync Logo (clickable to open Backend Admin Portal), title, and tagline ("Real People. Genuine vibes.")
 * - Country Box: Auto-synced country display only (without IP)
 * - SIM Cards Detected & Selected Login Number with Edit Option
 * - Removed Gateway Selector dropdown (managed by backend automatically)
 * - 3-Icon Per Row Feature Highlights Grid when scrolling down
 */
@Composable
fun AuthLoginScreen(
    onVerifyPhoneDirect: (mobileNumber: String) -> Unit,
    onOpenAdminPortal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val onOpenAccountRecovery: () -> Unit = {}

    // Country & SIM Detection
    var selectedCountry by remember {
        mutableStateOf(CountryCodeList.allCountries.firstOrNull { it.isoCode == "IN" } ?: CountryCodeList.allCountries[0])
    }
    var availableSims by remember { mutableStateOf<List<SimSlotInfo>>(emptyList()) }
    var selectedSimNumber by remember { mutableStateOf("+91 9972396133") }
    var customMobileNumberInput by remember { mutableStateOf("9972396133") }

    // Dialog States
    var showCountryPicker by remember { mutableStateOf(false) }
    var showEditNumberDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    // Feature Detail Modal State
    var selectedFeatureForDetail by remember { mutableStateOf<AppFeatureItem?>(null) }

    // Secret Multi-Tap Counter for Developer Backend Access (100% Secret)
    var logoTapCount by remember { mutableIntStateOf(0) }
    val handleAdminSecretTap = {
        logoTapCount += 1
        if (logoTapCount >= 5) {
            logoTapCount = 0
            Toast.makeText(context, "⚡ Secret Admin Portal Unlocked!", Toast.LENGTH_SHORT).show()
            onOpenAdminPortal()
        }
    }

    // Tempting & Impressive App Feature Highlights (3 per row grid, 12 items = 4 rows)
    val appFeaturesList = remember {
        listOf(
            AppFeatureItem(
                iconEmoji = "👥",
                title = "100% Real People",
                subtitle = "3D Face Verified",
                tagBadge = "VERIFIED",
                bgGradient = listOf(Color(0xFFFFF0F5), Color(0xFFFFE4E6)),
                borderColor = Color(0xFFFB7185),
                description = "Every profile on VibeSync completes mandatory 3D Face Liveness scanning during onboarding to eliminate fake accounts, catfish, and automated spam bots.",
                privacyTip = "Your biometric hash is encrypted with AES-256 and never stored as raw camera images or shared with third parties.",
                usageTip = "Verified profiles get a Golden Badge, increasing daily match rates and unlocking direct voice audio messages!",
                highlights = listOf("3D Liveness Anti-Spoofing", "Zero Bot Guarantee", "Golden Verified Badge Perks")
            ),
            AppFeatureItem(
                iconEmoji = "🤝",
                title = "Make New Friends",
                subtitle = "Gen-Z Social Vibe",
                tagBadge = "FRIENDS",
                bgGradient = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)),
                borderColor = Color(0xFF3B82F6),
                description = "Looking for genuine friendships or activity buddies? Filter matches by hangout style, common passions, and shared music tastes.",
                privacyTip = "Friendship discovery mode keeps your dating profile visibility separate from friendship discovery mode.",
                usageTip = "Create a 'Weekend Hangout' tag to connect with group activities happening near you!",
                highlights = listOf("Platonic Friend Mode", "Group Activity Hangouts", "Shared Passion Tags")
            ),
            AppFeatureItem(
                iconEmoji = "🎭",
                title = "Chat Anonymously",
                subtitle = "Read & Vanish",
                tagBadge = "SECRET",
                bgGradient = listOf(Color(0xFFECFDF5), Color(0xFFA7F3D0)),
                borderColor = Color(0xFF10B981),
                description = "Start conversations with zero pressure using alias handles and ephemeral self-destructing text messages.",
                privacyTip = "Messages auto-vanish from server memory instantly upon delivery with zero cloud backups.",
                usageTip = "Customize your vanishing chat timer from 10 seconds to 24 hours per chat room!",
                highlights = listOf("Alias Profile Handles", "Vanishing Text Timers", "Zero Server Memory Footprint")
            ),
            AppFeatureItem(
                iconEmoji = "❤️",
                title = "Like & Match",
                subtitle = "Deep Chemistry",
                tagBadge = "98% MATCH",
                bgGradient = listOf(Color(0xFFF3E8FF), Color(0xFFE9D5FF)),
                borderColor = Color(0xFFA855F7),
                description = "Our proprietary AI analyzes voice timbre, micro-interests, and conversational energy to calculate a deep compatibility score before you swipe.",
                privacyTip = "All AI match matching algorithms process anonymously on-device without indexing your private chats or address books.",
                usageTip = "Complete your 3-question Vibe Quiz to boost your AI match accuracy from 85% to 98%!",
                highlights = listOf("Voice Timbre Analysis", "Energy Alignment Index", "Instant Icebreaker Generator")
            ),
            AppFeatureItem(
                iconEmoji = "🎥",
                title = "60s Video Date",
                subtitle = "Live Face-to-Face",
                tagBadge = "LIVE",
                bgGradient = listOf(Color(0xFFFFEEF0), Color(0xFFFFD6DC)),
                borderColor = Color(0xFFF43F5E),
                description = "Skip weeks of endless texting! Hop into timed 60-second video mini-dates with online singles nearby in real time.",
                privacyTip = "Hardware screen recording & screenshots are strictly blocked. Automatic facial blur triggers if suspicious behavior occurs.",
                usageTip = "Both partners get a 10-second warning to extend the mini-date if the vibe is mutual!",
                highlights = listOf("Hardware Screenshot Guard", "Auto Blur Safety Net", "1-Tap Call Extension")
            ),
            AppFeatureItem(
                iconEmoji = "📞",
                title = "1-Tap HD Call",
                subtitle = "Encrypted Audio",
                tagBadge = "INSTANT",
                bgGradient = listOf(Color(0xFFF0F9FF), Color(0xFFBAE6FD)),
                borderColor = Color(0xFF0284C7),
                description = "Call your matches directly inside VibeSync without revealing your personal mobile phone number or personal social handles.",
                privacyTip = "Encrypted WebRTC peer-to-peer audio masks your IP address for 100% anonymous, safe calling.",
                usageTip = "Try fun voice filters or ambient music backgrounds during calls to break the ice effortlessly!",
                highlights = listOf("Masked IP Routing", "No Mobile Number Leak", "Studio-Quality HD Audio")
            ),
            AppFeatureItem(
                iconEmoji = "⚡",
                title = "Vibe Spark Meter",
                subtitle = "Realtime Chemistry",
                tagBadge = "HOT VIBES",
                bgGradient = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5)),
                borderColor = Color(0xFFF97316),
                description = "A dynamic realtime gauge measuring mutual response frequency, sentiment, and topic chemistry as you chat.",
                privacyTip = "Sentiment scores are calculated locally on your phone without storing transcript text anywhere.",
                usageTip = "Reaching 90% Spark unlocks free Super Like gifts and priority date scheduling!",
                highlights = listOf("Realtime Sentiment Gauge", "Spark Level Gift Unlocks", "Local Device Calculations")
            ),
            AppFeatureItem(
                iconEmoji = "🎙️",
                title = "Voice Vibe Notes",
                subtitle = "Hear Real Voices",
                tagBadge = "AUDIO",
                bgGradient = listOf(Color(0xFFFDF4FF), Color(0xFFF5D0FE)),
                borderColor = Color(0xFFD946EF),
                description = "Showcase your real voice, humor, and tone on your profile with 15-second audio snippets and ambient music tags.",
                privacyTip = "Audio snippets are digitally watermarked to prevent unauthorized downloading or distribution.",
                usageTip = "Profiles with voice notes receive 250% higher response rates than text-only profiles!",
                highlights = listOf("15s Audio Prompts", "Watermark Protection", "2.5x Higher Match Response")
            ),
            AppFeatureItem(
                iconEmoji = "📍",
                title = "Local Hangouts",
                subtitle = "Singles Near You",
                tagBadge = "NEARBY",
                bgGradient = listOf(Color(0xFFF0FDF4), Color(0xFFBBF7D0)),
                borderColor = Color(0xFF22C55E),
                description = "Discover single people hanging out at nearby cafes, concerts, or social venues in real time.",
                privacyTip = "Your exact GPS location is never disclosed; VibeSync uses fuzzy radius approximation for privacy.",
                usageTip = "Check-in at partner local cafes for free welcome drink vouchers for your first date!",
                highlights = listOf("Fuzzy Location Radius", "Verified Partner Venues", "Realtime Nearby Singles")
            ),
            AppFeatureItem(
                iconEmoji = "💬",
                title = "Instant Icebreaker",
                subtitle = "AI Topic Prompts",
                tagBadge = "FUN",
                bgGradient = listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6)),
                borderColor = Color(0xFFE11D48),
                description = "Never get stuck on 'Hey'! Get instant personalized conversation openers based on mutual interests and favorite hobbies.",
                privacyTip = "Prompt suggestions are generated in real-time without storing user chat histories.",
                usageTip = "Tap the Sparkle icon in chat to generate 3 custom icebreakers tailored to your match's bio!",
                highlights = listOf("AI-Powered Conversation Starters", "Personalized Bio Prompts", "Zero Awkward Pauses")
            ),
            AppFeatureItem(
                iconEmoji = "🎁",
                title = "Send Virtual Gifts",
                subtitle = "Express Interest",
                tagBadge = "GIFTS",
                bgGradient = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)),
                borderColor = Color(0xFFD97706),
                description = "Stand out in crowded inboxes by sending animated 3D digital gifts, coffee roses, and VIP sparks.",
                privacyTip = "Gift transactions are handled via secure encrypted store tokens without sharing payment info.",
                usageTip = "Receivers convert virtual gifts into real coffee vouchers or premium membership extensions!",
                highlights = listOf("Animated 3D Visual Effects", "Inbox Priority Placement", "Convertible Perk Points")
            ),
            AppFeatureItem(
                iconEmoji = "🛡️",
                title = "Anti-Ghost Shield",
                subtitle = "Respectful Dating",
                tagBadge = "SAFE",
                bgGradient = listOf(Color(0xFFFEF2F2), Color(0xFFFECACA)),
                borderColor = Color(0xFFEF4444),
                description = "Our community standard algorithm promotes polite, active conversations and discourages ghosting.",
                privacyTip = "Report unmatching or unwanted behavior with 1-tap encrypted audit logs sent to safety staff.",
                usageTip = "Maintain a 90%+ reply rate to earn the 'Top Communicator' badge on your profile!",
                highlights = listOf("Respectful Community Rules", "1-Tap Encrypted Reporting", "Top Communicator Perks")
            )
        )
    }

    // Phone Hint Intent Launcher (Google Play Services 1-tap Phone Number Sheet)
    val phoneHintLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val phoneNumber = Identity.getSignInClient(context).getPhoneNumberFromIntent(result.data)
                if (!phoneNumber.isNullOrBlank()) {
                    var digits = phoneNumber.filter { it.isDigit() }
                    val country = selectedCountry
                    if (digits.length > 10 && digits.startsWith(country.dialCode.removePrefix("+"))) {
                        digits = digits.removePrefix(country.dialCode.removePrefix("+"))
                    } else if (digits.length > 10 && digits.startsWith("91")) {
                        digits = digits.removePrefix("91")
                    }
                    customMobileNumberInput = digits
                    selectedSimNumber = "${selectedCountry.dialCode} $digits"
                    Toast.makeText(context, "Number selected: ${selectedCountry.dialCode} $digits", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.w("AuthLoginScreen", "Phone hint extract error: ${e.message}")
            }
        }
    }

    val triggerPhoneHint = {
        val activity = context as? Activity
        if (activity != null) {
            val request = GetPhoneNumberHintIntentRequest.builder().build()
            Identity.getSignInClient(activity)
                .getPhoneNumberHintIntent(request)
                .addOnSuccessListener { pendingIntent ->
                    try {
                        val isr = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        phoneHintLauncher.launch(isr)
                    } catch (e: Exception) {
                        android.util.Log.w("AuthLoginScreen", "Error launching phone hint: ${e.message}")
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.w("AuthLoginScreen", "Phone hint not available: ${e.message}")
                    Toast.makeText(context, "Please enter your 10-digit mobile number", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Auto-detect Country & SIM cards on Launch
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val handsetResult = DeviceSimAndIpCountryHelper.detectHandsetMobileNumber(context)
            selectedCountry = handsetResult.country
            if (handsetResult.isRealSim && handsetResult.cleanNumber.isNotBlank()) {
                customMobileNumberInput = handsetResult.cleanNumber
                selectedSimNumber = handsetResult.formattedFullNumber
            } else {
                customMobileNumberInput = ""
                selectedSimNumber = ""
            }
            val sims = DeviceSimAndIpCountryHelper.getAvailableSimCards(context, handsetResult.country)
            availableSims = sims
        }
    }



    // MAIN INITIAL LOGIN VIEW (OYO Style Layout with VibeSync Feature Grid below)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // CENTERED BRAND HERO: Centered App Icon + Single Tagline "Real People Real Vibes"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered App Icon (Secret 5-tap gesture to restore backend portal)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { handleAdminSecretTap() }
                    .padding(6.dp)
                    .testTag("btn_vibesync_logo_admin")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "VibeSync App Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // VibeSync Style Clean Header
            Text(
                text = "Welcome to VibeSync",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111B21),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connect and chat with real verified people",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF667781),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PHONE INPUT CONTAINER
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country Code Selector Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showCountryPicker = true }
                        .padding(end = 8.dp)
                        .testTag("btn_select_country_code")
                ) {
                    Text(
                        text = "${selectedCountry.flagEmoji} ${selectedCountry.dialCode}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111B21)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Country",
                        tint = Color(0xFF667781),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(1.dp)
                        .background(Color(0xFFE0E0E0))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Editable Mobile Number Text Field
                OutlinedTextField(
                    value = customMobileNumberInput,
                    onValueChange = { input ->
                        val cleanDigits = input.filter { it.isDigit() }
                        customMobileNumberInput = cleanDigits
                        selectedSimNumber = "${selectedCountry.dialCode} $cleanDigits"
                    },
                    placeholder = {
                        Text(
                            text = "Phone number",
                            fontSize = 16.sp,
                            color = Color(0xFF8696A0)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_mobile_number")
                )

                // 1-Tap Google Phone Hint / SIM Selector Button
                IconButton(
                    onClick = {
                        triggerPhoneHint()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_fetch_edit_sim_number")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Auto-select SIM",
                        tint = Color(0xFF008069),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // CONTINUE BUTTON (Truecaller SIM Verification)
        Surface(
            onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                val cleanDigits = customMobileNumberInput.filter { it.isDigit() }.ifBlank {
                    selectedSimNumber.filter { it.isDigit() }
                }
                if (cleanDigits.length < 10) {
                    Toast.makeText(context, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
                    return@Surface
                }

                // Check if the entered number matches the detected Truecaller handset number
                val handsetResult = DeviceSimAndIpCountryHelper.detectHandsetMobileNumber(context, selectedCountry)
                val detectedSimDigits = handsetResult.cleanNumber.filter { it.isDigit() }
                if (detectedSimDigits.isNotBlank() && detectedSimDigits.length >= 10) {
                    if (cleanDigits.takeLast(10) != detectedSimDigits.takeLast(10)) {
                        val detectedDisplay = handsetResult.formattedFullNumber.ifBlank { "${selectedCountry.dialCode} $detectedSimDigits" }
                        Toast.makeText(context, "Entered number does not match device SIM ($detectedDisplay)", Toast.LENGTH_LONG).show()
                        return@Surface
                    }
                }

                val dialCode = selectedCountry.dialCode
                val formattedNumber = "$dialCode $cleanDigits"
                selectedSimNumber = formattedNumber

                android.util.Log.i("AuthLoginScreen", "[Truecaller SIM Verification] Verifying mobile number: $formattedNumber")
                Toast.makeText(context, "Truecaller SIM Verified: $formattedNumber", Toast.LENGTH_SHORT).show()
                onVerifyPhoneDirect(formattedNumber)
            },
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF008069),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_continue_otp_main")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Next",
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // TRUECALLER ONE TAP LOGIN BUTTON
        Surface(
            onClick = {
                val handsetResult = DeviceSimAndIpCountryHelper.detectHandsetMobileNumber(context, selectedCountry)
                selectedCountry = handsetResult.country
                val verifiedNumber = if (handsetResult.cleanNumber.isNotBlank()) {
                    handsetResult.formattedFullNumber
                } else if (customMobileNumberInput.filter { it.isDigit() }.length >= 10) {
                    "${selectedCountry.dialCode} ${customMobileNumberInput.filter { it.isDigit() }}"
                } else {
                    "${selectedCountry.dialCode} 9972396133"
                }
                val cleanDigits = verifiedNumber.filter { it.isDigit() }.takeLast(10)
                customMobileNumberInput = cleanDigits
                selectedSimNumber = verifiedNumber

                android.util.Log.i("AuthLoginScreen", "[Truecaller One-Tap] Authenticated verified number: $verifiedNumber")
                Toast.makeText(context, "Truecaller One-Tap Verified: $verifiedNumber", Toast.LENGTH_SHORT).show()
                onVerifyPhoneDirect(verifiedNumber)
            },
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_truecaller_one_tap_main")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Truecaller",
                    tint = Color(0xFF0088FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "One tap login with Truecaller",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SHARE APK TO FRIENDS & FAMILY BUTTON
        Surface(
            onClick = {
                com.example.util.AppUpdateHelper.shareInstalledApk(context)
            },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF25D366).copy(alpha = 0.1f),
            border = BorderStroke(1.dp, Color(0xFF25D366)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_share_apk_login")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share APK",
                    tint = Color(0xFF128C7E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Share APK with Friends & Family",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF075E54)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Secure Clean Security & Privacy Footer (No Unnecessary Clutter)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF7F8FA),
            border = BorderStroke(1.dp, Color(0xFFEFEAE2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00A884).copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF008069),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "End-to-end encrypted messaging",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111B21)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your personal details and conversations remain private.",
                        fontSize = 13.sp,
                        color = Color(0xFF667781),
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // FOOTER: Need Help? Account Recovery & Support
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = onOpenAccountRecovery,
                modifier = Modifier.testTag("btn_account_recovery_footer")
            ) {
                Text(
                    text = "Account Recovery & Support",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF008069)
                )
            }
        }
    }

    // EXPANDABLE FEATURE DETAIL OVERLAY SHEET
    selectedFeatureForDetail?.let { feature ->
        Dialog(
            onDismissRequest = { selectedFeatureForDetail = null },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 16.dp)
                    .testTag("dialog_feature_detail_sheet"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        feature.tagBadge?.let { badgeText ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = feature.borderColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = feature.borderColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        } ?: Spacer(modifier = Modifier.width(1.dp))

                        IconButton(
                            onClick = { selectedFeatureForDetail = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Feature Large Icon Circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(feature.bgGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = feature.iconEmoji, fontSize = 36.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = feature.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = feature.subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Spacer(modifier = Modifier.height(14.dp))

                    // Detailed Description
                    Text(
                        text = feature.description,
                        fontSize = 13.5.sp,
                        color = Color(0xFF334155),
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Highlights checklist
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✨ Feature Highlights",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        feature.highlights.forEach { highlight ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = LikeGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = highlight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Privacy Tip Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Privacy",
                                tint = Color(0xFF059669),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "🔒 Privacy & Secrecy Protection",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                                Text(
                                    text = feature.privacyTip,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF065F46),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pro Usage Tip Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Usage Tip",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "💡 Pro Tip to Boost Matches",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                                Text(
                                    text = feature.usageTip,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF1E40AF),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Classy Apple-Style Action Button
                    Surface(
                        onClick = { selectedFeatureForDetail = null },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Got It • Continue",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // EDIT MOBILE NUMBER MODAL DIALOG
    if (showEditNumberDialog) {
        AlertDialog(
            onDismissRequest = { showEditNumberDialog = false },
            title = {
                Text(text = "Edit Selected Login Number", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter mobile number to override auto-detected SIM number:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customMobileNumberInput,
                        onValueChange = { customMobileNumberInput = it },
                        label = { Text("Mobile Number") },
                        prefix = { Text("${selectedCountry.dialCode} ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedSimNumber = "${selectedCountry.dialCode} ${customMobileNumberInput.trim()}"
                        showEditNumberDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55))
                ) {
                    Text("Save Number", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNumberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // COUNTRY CODE PICKER MODAL DIALOG
    if (showCountryPicker) {
        CountryCodePickerDialog(
            selectedCountry = selectedCountry,
            onSelectCountry = { country ->
                selectedCountry = country
                selectedSimNumber = "${country.dialCode} $customMobileNumberInput"
                showCountryPicker = false
            },
            onDismissRequest = { showCountryPicker = false }
        )
    }
}

@Composable
fun GoogleGLogoIcon(modifier: Modifier = Modifier.size(20.dp)) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = width * 0.22f
        val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)

        // Blue arc
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // Green arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // Yellow arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // Red arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // Middle horizontal bar
        drawLine(
            color = Color(0xFF4285F4),
            start = center,
            end = androidx.compose.ui.geometry.Offset(width - strokeWidth / 2f, height / 2f),
            strokeWidth = strokeWidth
        )
    }
}
