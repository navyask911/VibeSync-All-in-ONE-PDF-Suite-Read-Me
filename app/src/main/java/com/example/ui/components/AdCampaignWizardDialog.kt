package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.BusinessEntity
import com.example.ui.DatingViewModel
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.PassRed
import com.example.ui.theme.RewindGold
import com.example.util.AdCampaign
import com.example.util.AdManager
import com.example.util.RazorpayConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdCampaignWizardDialog(
    viewModel: DatingViewModel? = null,
    business: BusinessEntity? = null,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Wizard Step State (0: Creative, 1: Placement & Target, 2: Duration & Blitz, 3: CPM Bidding, 4: Checkout & Payment)
    var currentStep by remember { mutableIntStateOf(0) }
    var showLivePreview by remember { mutableStateOf(false) }
    var previewFormat by remember { mutableStateOf("SWIPES") } // "SWIPES" or "STORIES"
    var showRazorpaySetupDialog by remember { mutableStateOf(false) }

    // Campaign Form Fields
    var businessName by remember {
        mutableStateOf(business?.name ?: "The Urban Roastery Cafe")
    }
    var campaignName by remember {
        mutableStateOf("Cozy First Date Coffee & Treats")
    }
    var headline by remember {
        mutableStateOf("Buy 1 Get 1 Free Espresso for Matches ☕")
    }
    var description by remember {
        mutableStateOf("Show your VibeSync profile at counter for instant 50% discount on craft pastries & drinks.")
    }
    var mediaType by remember { mutableStateOf("IMAGE") } // "IMAGE" or "VIDEO"
    var mediaUrl by remember {
        val defaultUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80"
        val initialUrl = business?.bannerUrl?.ifBlank { defaultUrl } ?: defaultUrl
        mutableStateOf(initialUrl)
    }
    var videoDurationSeconds by remember { mutableIntStateOf(15) } // Max 30s
    var ctaText by remember { mutableStateOf("Claim Cafe Coupon 🎁") }

    // Placement
    var selectedPlacement by remember { mutableStateOf("BOTH") } // "SWIPES", "STORIES", "BOTH"

    // Targeting
    var targetCity by remember { mutableStateOf(business?.city ?: "All Cities") }
    var targetRadiusKm by remember { mutableIntStateOf(25) }
    var actionType by remember { mutableStateOf("BUSINESS_PROFILE") } // "BUSINESS_PROFILE", "WEBSITE_URL", "DIRECT_CHAT"
    var websiteUrl by remember { mutableStateOf("https://urbanroastery.com/offer") }

    // Duration & Blitz Options
    var durationMode by remember { mutableStateOf("DAILY_24H") } // "BLITZ_15", "BLITZ_30", "BLITZ_45", "BLITZ_60", "DAILY_24H", "WEEKLY_7D", "MONTHLY_30D"
    val isBlitz = durationMode.startsWith("BLITZ")

    // CPM Packages & Impressions
    var selectedImpressions by remember { mutableIntStateOf(5000) } // 1000, 5000, 10000, 25000, 50000
    var contactEmail by remember { mutableStateOf("partner@urbanroastery.com") }

    // Calculated Costs via CPM Engine
    val calculatedCostInr = remember(selectedImpressions, mediaType, isBlitz, targetRadiusKm) {
        AdManager.calculateCpmCost(
            impressions = selectedImpressions,
            mediaType = mediaType,
            isBlitz = isBlitz,
            radiusKm = targetRadiusKm
        )
    }
    val calculatedWalletPoints = remember(calculatedCostInr) {
        AdManager.convertInrToWalletPoints(calculatedCostInr)
    }

    // Payment Selection
    var selectedPaymentMethod by remember { mutableStateOf("UPI") } // "UPI", "CARD", "WALLET_POINTS", "RAZORPAY"
    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentSuccess by remember { mutableStateOf(false) }

    val presetImages = listOf(
        "Cafe / Lounge" to "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80",
        "Concert & Music" to "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80",
        "Rooftop & Dining" to "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80",
        "Fitness & Spa" to "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80"
    )

    val presetVideos = listOf(
        "Live DJ & Dance (15s)" to "https://assets.mixkit.co/videos/preview/mixkit-concert-crowd-raising-hands-4148-large.mp4",
        "Artisan Barista Pour (10s)" to "https://assets.mixkit.co/videos/preview/mixkit-coffee-being-poured-into-a-cup-39832-large.mp4",
        "Cozy Date Night (20s)" to "https://assets.mixkit.co/videos/preview/mixkit-people-in-a-bar-raising-their-glasses-4384-large.mp4"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp)
                .testTag("dialog_ad_campaign_wizard"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(CoralPink, Color(0xFFFF5252)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Join with Us",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = CoralPink.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "CPM Bidding",
                                        color = CoralPink,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "In-Feed Swipes & Stories Ad Campaign Wizard",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showRazorpaySetupDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Payment Setup",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Step Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val stepTitles = listOf("1. Creative", "2. Target", "3. Duration", "4. CPM", "5. Pay")
                    stepTitles.forEachIndexed { index, title ->
                        val isDone = currentStep > index
                        val isCurrent = currentStep == index
                        Surface(
                            onClick = { if (index <= currentStep) currentStep = index },
                            color = when {
                                isCurrent -> CoralPink
                                isDone -> LikeGreen
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = title,
                                    color = if (isCurrent || isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Live Preview Switch Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = CoralPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Interactive Ad Preview",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = showLivePreview,
                        onCheckedChange = { showLivePreview = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CoralPink)
                    )
                }

                // Interactive Live Ad Preview Box
                AnimatedVisibility(visible = showLivePreview) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.5.dp, CoralPink.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👁️ User Experience Preview (Zero Compulsory Delay)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralPink
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    onClick = { previewFormat = "SWIPES" },
                                    color = if (previewFormat == "SWIPES") CoralPink else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Swipe Card",
                                        fontSize = 10.sp,
                                        color = if (previewFormat == "SWIPES") Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    onClick = { previewFormat = "STORIES" },
                                    color = if (previewFormat == "STORIES") CoralPink else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Story Ad",
                                        fontSize = 10.sp,
                                        color = if (previewFormat == "STORIES") Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (previewFormat == "SWIPES") {
                            // Swipe Card Preview
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(260.dp),
                                shape = RoundedCornerShape(18.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CoralPink.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "📢 Sponsored • ${businessName.take(18)}",
                                                color = CoralPink,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Instant Skip (No Lock)",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.DarkGray)
                                    ) {
                                        AsyncImage(
                                            model = mediaUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (mediaType == "VIDEO") {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.6f),
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Column {
                                        Text(text = headline, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text(text = description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                    }

                                    Button(
                                        onClick = { Toast.makeText(context, "Redirecting to $actionType!", Toast.LENGTH_SHORT).show() },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Text(text = ctaText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Story Ad Preview
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(300.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = mediaUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                                )
                                            )
                                    )

                                    // Top Bar in Story Ad
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                            .align(Alignment.TopCenter)
                                    ) {
                                        // Story Progress Line
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().height(3.dp),
                                            color = CoralPink,
                                            shape = RoundedCornerShape(2.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Store, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(businessName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = CoralPink,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("AD", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                            Text("Instant Skip ✕", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                        }
                                    }

                                    // Bottom CTA in Story Ad
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                            .align(Alignment.BottomCenter),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(headline, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(description, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { Toast.makeText(context, "Redirecting to $actionType!", Toast.LENGTH_SHORT).show() },
                                            colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().height(38.dp)
                                        ) {
                                            Text("$ctaText 🚀", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // STEP 0: CREATIVE & MEDIA ASSETS
                // ==========================================
                if (currentStep == 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Step 1: Ad Creative & Media",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Brand / Business Name") },
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_wizard_biz_name")
                        )

                        OutlinedTextField(
                            value = campaignName,
                            onValueChange = { campaignName = it },
                            label = { Text("Campaign Identifier") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_wizard_campaign_name")
                        )

                        OutlinedTextField(
                            value = headline,
                            onValueChange = { headline = it },
                            label = { Text("Catchy Headline (e.g. 50% Off First Date Drinks 🍸)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_wizard_headline")
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Ad Description & Offer Details") },
                            modifier = Modifier.fillMaxWidth().testTag("input_wizard_description")
                        )

                        // Media Type Switch (Image vs Video up to 30s)
                        Text(
                            text = "Ad Media Format Support:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { mediaType = "IMAGE" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (mediaType == "IMAGE") CoralPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (mediaType == "IMAGE") BorderStroke(1.5.dp, CoralPink) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = if (mediaType == "IMAGE") CoralPink else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("High-Res Image", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (mediaType == "IMAGE") CoralPink else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Surface(
                                onClick = { mediaType = "VIDEO" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (mediaType == "VIDEO") CoralPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (mediaType == "VIDEO") BorderStroke(1.5.dp, CoralPink) else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = if (mediaType == "VIDEO") CoralPink else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Video (≤30s)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (mediaType == "VIDEO") CoralPink else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Presets
                        if (mediaType == "IMAGE") {
                            Text("Curated Image Presets:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetImages.forEach { (label, url) ->
                                    val isSel = mediaUrl == url
                                    Surface(
                                        onClick = { mediaUrl = url },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) CoralPink.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSel) BorderStroke(1.5.dp, CoralPink) else null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) CoralPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("Video Creative Presets (≤30s max):", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetVideos.forEach { (label, url) ->
                                    val isSel = mediaUrl == url
                                    Surface(
                                        onClick = { mediaUrl = url },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) CoralPink.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSel) BorderStroke(1.5.dp, CoralPink) else null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) CoralPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = mediaUrl,
                            onValueChange = { mediaUrl = it },
                            label = { Text("Custom Media URL / Asset Link") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_wizard_media_url")
                        )

                        OutlinedTextField(
                            value = ctaText,
                            onValueChange = { ctaText = it },
                            label = { Text("Call to Action (CTA) Button Text") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_wizard_cta")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { currentStep = 1 },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Next: Placement & Targeting ➔", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ==========================================
                // STEP 1: PLACEMENT & AUDIENCE TARGETING
                // ==========================================
                if (currentStep == 1) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Step 2: Placement & Audience Targeting",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text("Select In-App Ad Placement:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        // 3 Placement options
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Triple("BOTH", "Both: Swipes & Stories (Recommended)", "Injected every 5 card swipes and every 5 user stories viewed"),
                                Triple("SWIPES", "Swipes Deck Only", "Injected every 5 profile cards with instant swipe-away capability"),
                                Triple("STORIES", "User Stories Interstitial Only", "Injected every 5 viewed stories with instant tap-skip capability")
                            ).forEach { (key, title, desc) ->
                                val isSelected = selectedPlacement == key
                                Surface(
                                    onClick = { selectedPlacement = key },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) CoralPink.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) BorderStroke(1.5.dp, CoralPink) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = if (isSelected) CoralPink else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSelected) CoralPink else MaterialTheme.colorScheme.onSurface)
                                            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Target Action Redirect:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "BUSINESS_PROFILE" to "VibeSync Profile 🏢",
                                "WEBSITE_URL" to "External Web 🌐",
                                "DIRECT_CHAT" to "Direct Chat 💬"
                            ).forEach { (type, label) ->
                                val isSel = actionType == type
                                Surface(
                                    onClick = { actionType = type },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) CoralPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSel) BorderStroke(1.5.dp, CoralPink) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) CoralPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        if (actionType == "WEBSITE_URL") {
                            OutlinedTextField(
                                value = websiteUrl,
                                onValueChange = { websiteUrl = it },
                                label = { Text("Destination Website URL") },
                                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = targetCity,
                            onValueChange = { targetCity = it },
                            label = { Text("Target City (or All Cities)") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Radius Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Audience Radius: ${targetRadiusKm} km", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (targetRadiusKm <= 15) "🎯 Hyperlocal" else "🌍 Metro Region", fontSize = 11.sp, color = CoralPink)
                            }
                            Slider(
                                value = targetRadiusKm.toFloat(),
                                onValueChange = { targetRadiusKm = it.toInt() },
                                valueRange = 5f..100f,
                                steps = 19,
                                colors = SliderDefaults.colors(thumbColor = CoralPink, activeTrackColor = CoralPink)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = 0 },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = { currentStep = 2 },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                                modifier = Modifier.weight(1.5f).height(48.dp)
                            ) {
                                Text("Next: Duration & Blitz ➔", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ==========================================
                // STEP 2: DURATION & BLITZ RUNTIME
                // ==========================================
                if (currentStep == 2) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Step 3: Active Duration & Blitz Surge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "🔥 < 60 Mins Active Blitz Deals (Date Night Rush):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = RewindGold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "BLITZ_15" to "15 Mins ⚡",
                                "BLITZ_30" to "30 Mins ⚡",
                                "BLITZ_45" to "45 Mins ⚡",
                                "BLITZ_60" to "60 Mins ⚡"
                            ).forEach { (mode, label) ->
                                val isSel = durationMode == mode
                                Surface(
                                    onClick = { durationMode = mode },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) RewindGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSel) BorderStroke(1.5.dp, RewindGold) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) RewindGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                    )
                                }
                            }
                        }

                        Text("📅 Scheduled Ongoing Campaigns:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "DAILY_24H" to "24 Hours",
                                "WEEKLY_7D" to "7 Days",
                                "MONTHLY_30D" to "30 Days"
                            ).forEach { (mode, label) ->
                                val isSel = durationMode == mode
                                Surface(
                                    onClick = { durationMode = mode },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) CoralPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSel) BorderStroke(1.5.dp, CoralPink) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) CoralPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        // Non-Intrusive Policy Reminder
                        Surface(
                            color = LikeGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LikeGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VibeSync Non-Intrusive Guarantee: Users have 100% freedom to swipe or skip instantly with 0 forced delay or screen lock.",
                                    fontSize = 11.sp,
                                    color = LikeGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = 1 },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = { currentStep = 3 },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                                modifier = Modifier.weight(1.5f).height(48.dp)
                            ) {
                                Text("Next: CPM Bidding ➔", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ==========================================
                // STEP 3: CPM BIDDING & IMPRESSION PACKS
                // ==========================================
                if (currentStep == 3) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Step 4: Standard Market CPM Monetization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Select Impression Package (Cost Per Mille / 1,000 Impressions):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        listOf(
                            Triple(1000, "1,000 Views (Starter Tier)", "Base programmatic CPM rate"),
                            Triple(5000, "5,000 Views (Growth Tier)", "Popular for weekend date boosts • Save 10%"),
                            Triple(10000, "10,000 Views (Pro Business)", "High volume feed injection • Save 20%"),
                            Triple(25000, "25,000 Views (Brand Domination)", "Maximum city reach across Swipes & Stories"),
                            Triple(50000, "50,000 Views (Enterprise Partner)", "Comprehensive multi-city banner blitz")
                        ).forEach { (count, label, desc) ->
                            val isSel = selectedImpressions == count
                            val pkgCost = AdManager.calculateCpmCost(count, mediaType, isBlitz, targetRadiusKm)
                            Surface(
                                onClick = { selectedImpressions = count },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) CoralPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSel) BorderStroke(1.5.dp, CoralPink) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSel) CoralPink else MaterialTheme.colorScheme.onSurface)
                                        Text(desc, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹${pkgCost.toInt()} INR", fontWeight = FontWeight.Black, fontSize = 14.sp, color = CoralPink)
                                        Text("${(pkgCost * 20).toInt()} Pts", fontSize = 10.sp, color = RewindGold)
                                    }
                                }
                            }
                        }

                        // CPM Pricing Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Effective CPM Rate:", fontSize = 12.sp)
                                    Text("₹${(calculatedCostInr / (selectedImpressions / 1000.0)).toInt()} / 1K views", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Targeted Impressions:", fontSize = 12.sp)
                                    Text("$selectedImpressions Guaranteed", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Wallet Points Equivalent:", fontSize = 12.sp)
                                    Text("$calculatedWalletPoints Points", fontWeight = FontWeight.Bold, color = RewindGold, fontSize = 12.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Notification & Billing Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = 2 },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = { currentStep = 4 },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                                modifier = Modifier.weight(1.5f).height(48.dp)
                            ) {
                                Text("Proceed to Checkout ➔", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ==========================================
                // STEP 4: CHECKOUT & PAYMENT GATEWAY (UPI / CARD / WALLET / RAZORPAY)
                // ==========================================
                if (currentStep == 4) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Step 5: Pay CPM Package & Launch",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Grand Total Box
                        Surface(
                            color = CoralPink.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CoralPink),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Total Amount Payable", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${calculatedCostInr.toInt()} INR", fontSize = 28.sp, fontWeight = FontWeight.Black, color = CoralPink)
                                Text("for $selectedImpressions Guaranteed Impressions (${durationMode})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Text("Select Payment Gateway / Method:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        listOf(
                            Triple("UPI", "UPI Instant (GPay / PhonePe / Paytm)", "Zero transaction surcharge • Instant live approval"),
                            Triple("CARD", "Credit / Debit Card / NetBanking", "Visa, MasterCard, RuPay with 3D Secure OTP"),
                            Triple("WALLET_POINTS", "VibeSync Points Wallet ($calculatedWalletPoints Pts)", "Deduct points directly from your Business Wallet"),
                            Triple("RAZORPAY", "VibeSync Unified Merchant Gateway", "Single-window secure merchant checkout")
                        ).forEach { (method, name, desc) ->
                            val isSel = selectedPaymentMethod == method
                            Surface(
                                onClick = { selectedPaymentMethod = method },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) CoralPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSel) BorderStroke(1.5.dp, CoralPink) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (method) {
                                            "UPI" -> Icons.Default.QrCode2
                                            "CARD" -> Icons.Default.CreditCard
                                            "WALLET_POINTS" -> Icons.Default.Wallet
                                            else -> Icons.Default.Payment
                                        },
                                        contentDescription = null,
                                        tint = if (isSel) CoralPink else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSel) CoralPink else MaterialTheme.colorScheme.onSurface)
                                        Text(desc, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        if (selectedPaymentMethod == "WALLET_POINTS") {
                            Surface(
                                color = RewindGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Wallet, contentDescription = null, tint = RewindGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$calculatedWalletPoints points will be debited from your venue balance upon launch.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isProcessingPayment) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = CoralPink, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Connecting with $selectedPaymentMethod Gateway...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 3 },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) {
                                    Text("Back")
                                }

                                Button(
                                    onClick = {
                                        isProcessingPayment = true
                                        scope.launch {
                                            delay(1400)
                                            isProcessingPayment = false
                                            paymentSuccess = true

                                            // Register campaign into AdManager
                                            val newCampaign = AdCampaign(
                                                campaignName = campaignName.ifBlank { "VibeSync Ad Deal" },
                                                businessName = businessName.ifBlank { "Local Business" },
                                                businessId = business?.id ?: "biz_${System.currentTimeMillis().toString().takeLast(4)}",
                                                headline = headline.ifBlank { "Special VibeSync Offer!" },
                                                description = description.ifBlank { "Tap to claim exclusive local member perk." },
                                                mediaType = mediaType,
                                                bannerImageUrl = mediaUrl,
                                                videoUrl = if (mediaType == "VIDEO") mediaUrl else "",
                                                videoDurationSeconds = videoDurationSeconds,
                                                placement = selectedPlacement,
                                                actionType = actionType,
                                                ctaText = ctaText.ifBlank { "Claim Offer ✨" },
                                                targetLinkUrl = websiteUrl,
                                                targetCity = targetCity.ifBlank { "All Cities" },
                                                targetRadiusKm = targetRadiusKm,
                                                maxViewCountLimit = selectedImpressions,
                                                contactEmail = contactEmail,
                                                budgetAmount = "₹${calculatedCostInr.toInt()} INR",
                                                paidAmountNum = calculatedCostInr,
                                                cpmRate = (calculatedCostInr / (selectedImpressions / 1000.0)),
                                                paymentMethod = selectedPaymentMethod,
                                                paymentStatus = "COMPLETED",
                                                durationType = durationMode,
                                                isBlitz = isBlitz,
                                                status = "APPROVED"
                                            )

                                            AdManager.submitCampaign(newCampaign)
                                            Toast.makeText(context, "🎉 Campaign Launched! Injected across ${selectedPlacement}!", Toast.LENGTH_LONG).show()
                                            delay(500)
                                            onDismissRequest()
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                                    modifier = Modifier.weight(2f).height(50.dp).testTag("btn_pay_and_launch_ad")
                                ) {
                                    Text("Pay ₹${calculatedCostInr.toInt()} & Launch 🚀", fontWeight = FontWeight.Black, fontSize = 13.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // VibeSync Merchant Gateway Setup Dialog Modal
    if (showRazorpaySetupDialog) {
        RazorpayGatewaySetupDialog(
            onDismiss = { showRazorpaySetupDialog = false }
        )
    }
}

@Composable
fun RazorpayGatewaySetupDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig by AdManager.razorpayConfig.collectAsState()

    var keyId by remember { mutableStateOf(currentConfig.keyId) }
    var keySecret by remember { mutableStateOf(currentConfig.keySecret) }
    var merchantName by remember { mutableStateOf(currentConfig.merchantName) }
    var currency by remember { mutableStateOf(currentConfig.currency) }
    var isLiveMode by remember { mutableStateOf(currentConfig.isLiveMode) }
    var webhookUrl by remember { mutableStateOf(currentConfig.autoWebhookUrl) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = CoralPink.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Payment, contentDescription = null, tint = CoralPink)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("VibeSync Merchant Gateway Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Merchant & Ad CPM API Credentials", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(thickness = 0.5.dp)

                OutlinedTextField(
                    value = keyId,
                    onValueChange = { keyId = it },
                    label = { Text("VibeSync Merchant Key ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = keySecret,
                    onValueChange = { keySecret = it },
                    label = { Text("VibeSync Merchant Security Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text("Merchant Brand Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Environment: ${if (isLiveMode) "🟢 Live Production" else "🟡 Test Sandbox"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Switch(checked = isLiveMode, onCheckedChange = { isLiveMode = it })
                }

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("Payment Webhook URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        AdManager.updateRazorpayConfig(
                            RazorpayConfig(
                                keyId = keyId,
                                keySecret = keySecret,
                                merchantName = merchantName,
                                currency = currency,
                                isLiveMode = isLiveMode,
                                autoWebhookUrl = webhookUrl,
                                isGatewayConfigured = true
                            )
                        )
                        Toast.makeText(context, "VibeSync Merchant Gateway Configuration Saved! 💳", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Save Gateway Configuration", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
