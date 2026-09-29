package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.PassRed
import com.example.ui.theme.RewindGold
import com.example.util.AdCampaign
import com.example.util.AdManager

@Composable
fun AdvertiseCampaignCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    val allCampaigns by AdManager.campaigns.collectAsState()
    val myCampaigns = allCampaigns.filter { it.submitterUserId == "current_user" || it.id.startsWith("ad_") }

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Launch, 1: My Running Ads

    // Campaign Form State
    var businessName by remember { mutableStateOf("The Urban Roastery Cafe") }
    var campaignName by remember { mutableStateOf("Cozy First Date Coffee Deal") }
    var headline by remember { mutableStateOf("Buy 1 Get 1 Free Pastry for VibeSync Matches ☕") }
    var description by remember { mutableStateOf("Visit our cozy lounge! Show your active VibeSync profile at counter for instant 50% discount on gourmet coffee.") }
    var mediaType by remember { mutableStateOf("IMAGE") } // "IMAGE" or "VIDEO"
    var mediaUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80") }
    var destinationUrl by remember { mutableStateOf("https://urbanroastery.com/vibesync-offer") }
    var ctaText by remember { mutableStateOf("Claim Cafe Coupon ☕") }
    var targetCity by remember { mutableStateOf("All Cities") }
    var selectedTargetViews by remember { mutableIntStateOf(5000) }
    var contactEmail by remember { mutableStateOf("partner@urbanroastery.com") }

    // Pricing calculation (Standard India CPM Model)
    // Image CPM = ₹250 per 1000 views ($3 USD)
    // Video CPM = ₹450 per 1000 views ($5.50 USD)
    val cpmRate = if (mediaType == "VIDEO") 450.0 else 250.0
    val calculatedCostInr = (selectedTargetViews / 1000.0) * cpmRate
    val calculatedCostUsd = (calculatedCostInr / 83.0)

    // Payment Checkout Dialog State
    var showPaymentDialog by remember { mutableStateOf(false) }

    ProfileSectionDrawer(
        title = "Ad With Us / Launch Campaign",
        icon = Icons.Default.Campaign,
        iconTint = CoralPink,
        iconBackground = CoralPink.copy(alpha = 0.15f),
        statusBadgeText = "Standard India CPM • UPI Pay",
        statusBadgeColor = CoralPink,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        testTag = "drawer_ad_with_us"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sub Tabs: Launch New vs My Running Ads
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = CoralPink,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("🚀 Launch Campaign", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊 My Running Ads", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            if (myCampaigns.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(shape = CircleShape, color = CoralPink) {
                                    Text(
                                        text = "${myCampaigns.size}",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            if (selectedSubTab == 0) {
                // LAUNCH CAMPAIGN FORM
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Brand / Business Name") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_ad_drawer_business")
                    )

                    OutlinedTextField(
                        value = headline,
                        onValueChange = { headline = it },
                        label = { Text("Promotional Headline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_ad_drawer_headline")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Ad Description & Offer Details") },
                        modifier = Modifier.fillMaxWidth().testTag("input_ad_drawer_description")
                    )

                    // Media Type Selector (Image vs Video <60s)
                    Text("Ad Creative Format:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                mediaType = "IMAGE"
                                mediaUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80"
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (mediaType == "IMAGE") CoralPink.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (mediaType == "IMAGE") androidx.compose.foundation.BorderStroke(1.5.dp, CoralPink) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = if (mediaType == "IMAGE") CoralPink else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Image Banner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (mediaType == "IMAGE") CoralPink else MaterialTheme.colorScheme.onSurface)
                                    Text("CPM ₹250 / 1k", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Surface(
                            onClick = {
                                mediaType = "VIDEO"
                                mediaUrl = "https://vibesync.app/video_sample_60s.mp4"
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (mediaType == "VIDEO") CoralPink.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (mediaType == "VIDEO") androidx.compose.foundation.BorderStroke(1.5.dp, CoralPink) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = if (mediaType == "VIDEO") CoralPink else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Video (<60 sec)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (mediaType == "VIDEO") CoralPink else MaterialTheme.colorScheme.onSurface)
                                    Text("CPM ₹450 / 1k", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Media URL Paste Box
                    OutlinedTextField(
                        value = mediaUrl,
                        onValueChange = { mediaUrl = it },
                        label = { Text(if (mediaType == "IMAGE") "Image URL Paste Box" else "Video URL Paste Box (<60s MP4/CDN)") },
                        leadingIcon = { Icon(if (mediaType == "IMAGE") Icons.Default.Image else Icons.Default.PlayCircle, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_ad_drawer_media_url")
                    )

                    // Destination Redirect URL Paste Box
                    OutlinedTextField(
                        value = destinationUrl,
                        onValueChange = { destinationUrl = it },
                        label = { Text("Destination Redirect URL (Website / Offer Link)") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_ad_drawer_destination_url")
                    )

                    OutlinedTextField(
                        value = ctaText,
                        onValueChange = { ctaText = it },
                        label = { Text("Call To Action Button Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_ad_drawer_cta")
                    )

                    // Target Views Picker
                    Text("Target View Impressions:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1000, 5000, 10000, 25000).forEach { viewLimit ->
                            val isSelected = selectedTargetViews == viewLimit
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTargetViews = viewLimit },
                                label = { Text("${viewLimit / 1000}k Views", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetCity,
                            onValueChange = { targetCity = it },
                            label = { Text("Target City") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_ad_drawer_city")
                        )

                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Contact Email") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("input_ad_drawer_email")
                        )
                    }

                    // Cost Breakdown Card (Standard India CPM Model)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Standard India Ad Network Pricing (CPM Rate):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${cpmRate.toInt()} / 1,000 Views", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Target Views Selected:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$selectedTargetViews Impressions", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Total Campaign Cost:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("₹${calculatedCostInr.toInt()} INR (~$${String.format("%.1f", calculatedCostUsd)} USD)", fontSize = 14.sp, fontWeight = FontWeight.Black, color = CoralPink)
                            }
                        }
                    }

                    // Pay & Launch Campaign Button
                    Button(
                        onClick = { showPaymentDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPink, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_ad_proceed_payment")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Proceed to UPI Payment (₹${calculatedCostInr.toInt()}) 💳", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
            } else {
                // MY RUNNING ADS DASHBOARD
                if (myCampaigns.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No active campaigns found. Launch your first ad campaign using the 'Launch Campaign' tab above!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        myCampaigns.forEach { campaign ->
                            val isCompleted = campaign.status == "TARGET_COMPLETED" || campaign.currentViewCount >= campaign.maxViewCountLimit
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(campaign.businessName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when {
                                                isCompleted -> PassRed.copy(alpha = 0.2f)
                                                campaign.status == "APPROVED" -> LikeGreen.copy(alpha = 0.2f)
                                                else -> RewindGold.copy(alpha = 0.2f)
                                            }
                                        ) {
                                            Text(
                                                text = when {
                                                    isCompleted -> "TARGET COMPLETED ✓ (INACTIVE)"
                                                    campaign.status == "APPROVED" -> "RUNNING LIVE 🟢"
                                                    campaign.status == "PENDING" -> "PENDING APPROVAL ⏳"
                                                    else -> campaign.status
                                                },
                                                color = when {
                                                    isCompleted -> PassRed
                                                    campaign.status == "APPROVED" -> LikeGreen
                                                    else -> RewindGold
                                                },
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(campaign.headline, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CoralPink)

                                    // View Counter & Progress Bar
                                    val progress = if (campaign.maxViewCountLimit > 0) campaign.currentViewCount.toFloat() / campaign.maxViewCountLimit else 0f
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Real-Time View Count:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${campaign.currentViewCount} / ${campaign.maxViewCountLimit} Views", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { progress.coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = if (isCompleted) PassRed else CoralPink
                                        )
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Clicks: ${campaign.clickCount}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Paid: ${campaign.budgetAmount}", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Text("Txn ID: ${campaign.paymentId} (${campaign.paymentStatus})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // UPI / Gateway Checkout Modal Dialog
    if (showPaymentDialog) {
        val totalCost = calculatedCostInr.toInt()
        val gstTax = (totalCost * 0.18).toInt()
        val grandTotal = totalCost + gstTax
        var paymentMethodSelected by remember { mutableStateOf("UPI_GPAY") }

        Dialog(
            onDismissRequest = { showPaymentDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = LikeGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("UPI Payment Checkout 💳", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        IconButton(onClick = { showPaymentDialog = false }) {
                            Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    // Order Summary
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Merchant: VibeSync Ads Network", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Campaign: $campaignName", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Impressions: $selectedTargetViews Views ($mediaType)", fontSize = 11.sp)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Ad Budget:", fontSize = 11.sp)
                                Text("₹$totalCost INR", fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("18% GST Tax:", fontSize = 11.sp)
                                Text("₹$gstTax INR", fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Total Payable:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                Text("₹$grandTotal INR", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = LikeGreen)
                            }
                        }
                    }

                    // UPI Options
                    Text("Select Payment Gateway / UPI App:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "UPI_GPAY" to "Google Pay / PhonePe UPI 📱",
                            "UPI_PAYTM" to "Paytm / BHIM UPI 🅿️",
                            "CARD" to "Credit / Debit Card / NetBanking 💳"
                        ).forEach { (id, label) ->
                            val isSel = paymentMethodSelected == id
                            Surface(
                                onClick = { paymentMethodSelected = id },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) LikeGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, LikeGreen) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = isSel, onClick = { paymentMethodSelected = id })
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(label, fontSize = 11.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val generatedTxnId = "UPI_TXN_${System.currentTimeMillis().toString().takeLast(10)}"
                            val newAd = AdCampaign(
                                campaignName = campaignName.ifBlank { "VibeSync Sponsored Deal" },
                                businessName = businessName.ifBlank { "Local Business Partner" },
                                headline = headline.ifBlank { "Special VibeSync Discount Offer!" },
                                description = description.ifBlank { "Tap to claim exclusive local member perk." },
                                mediaType = mediaType,
                                bannerImageUrl = if (mediaType == "IMAGE") mediaUrl else "",
                                videoUrl = if (mediaType == "VIDEO") mediaUrl else "",
                                ctaText = ctaText.ifBlank { "Claim Offer ✨" },
                                targetLinkUrl = destinationUrl.ifBlank { "https://vibesync.app/ad" },
                                targetCity = targetCity.ifBlank { "All Cities" },
                                maxViewCountLimit = selectedTargetViews,
                                contactEmail = contactEmail,
                                budgetAmount = "₹$grandTotal INR",
                                paidAmountNum = grandTotal.toDouble(),
                                paymentId = generatedTxnId,
                                paymentStatus = "COMPLETED",
                                paymentMethod = paymentMethodSelected,
                                submitterUserId = "current_user",
                                status = "PENDING"
                            )
                            AdManager.submitCampaign(newAd)
                            showPaymentDialog = false
                            selectedSubTab = 1 // Switch to My Running Ads tab
                            Toast.makeText(context, "Payment Successful! Txn ID: $generatedTxnId. Ad submitted for review 🚀", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LikeGreen, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pay ₹$grandTotal via UPI & Launch 🚀", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
            }
        }
    }
}
