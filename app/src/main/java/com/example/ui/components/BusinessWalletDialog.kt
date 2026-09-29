package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BusinessEntity
import com.example.ui.theme.LikeGreen

data class PointsPackage(
    val points: Int,
    val costInr: Int,
    val title: String,
    val popularTag: String = ""
)

data class BroadcastDeliveryLog(
    val id: String,
    val title: String,
    val totalFollowers: Int,
    val deliveredDoubleTick: Int,
    val unreachedCount: Int,
    val pointsCharged: Int,
    val pointsReversed: Int,
    val timestamp: String
)

@Composable
fun BusinessWalletDialog(
    business: BusinessEntity,
    onTopUpSuccess: (Int) -> Unit = {},
    onBroadcastSent: (String, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    var walletPoints by remember { mutableIntStateOf(business.walletPoints) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Wallet & Top-Up, 1: Broadcast to Followers, 2: History

    // Top-up packages
    val packages = listOf(
        PointsPackage(500, 25, "Starter Pack", ""),
        PointsPackage(2000, 100, "Growth Pack", "POPULAR"),
        PointsPackage(5000, 250, "Pro Partner", "BEST VALUE"),
        PointsPackage(10000, 500, "Enterprise", "MEGA REACH")
    )

    // Broadcast composer state
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var lastBroadcastResult by remember { mutableStateOf<BroadcastDeliveryLog?>(null) }
    var isSendingBroadcast by remember { mutableStateOf(false) }

    // Delivery log
    var deliveryLogs by remember {
        mutableStateOf(
            listOf(
                BroadcastDeliveryLog(
                    id = "log_1",
                    title = "Weekend 30% OFF Couple Brew",
                    totalFollowers = 2450,
                    deliveredDoubleTick = 2278,
                    unreachedCount = 172,
                    pointsCharged = 2450,
                    pointsReversed = 172,
                    timestamp = "Yesterday, 17:40"
                )
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("business_wallet_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF11141E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${business.name} Wallet",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "1 Point = ₹0.05 (5 Paisa) • Broadcast Engine",
                                color = Color(0xFF8A99AD),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF1E2330), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B2030), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val tabNames = listOf("💳 Wallet", "📢 Broadcast", "📊 Logs")
                    tabNames.forEachIndexed { index, name ->
                        val isSelected = activeTab == index
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = index }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Wallet Balance Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))),
                            RoundedCornerShape(18.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1C2233), Color(0xFF12151F))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AVAILABLE BALANCE",
                                    color = Color(0xFF8A99AD),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LikeGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "⚡ Instant UPI Verified",
                                        color = LikeGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$walletPoints pts",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "= ₹${String.format("%.2f", walletPoints * 0.05)} INR",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFF252D42))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Broadcast Cost", color = Color(0xFF8A99AD), fontSize = 10.sp)
                                    Text("1 pt / follower (₹0.05)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Read Verification", color = Color(0xFF8A99AD), fontSize = 10.sp)
                                    Text("✓✓ Double Tick Only", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Unreached Policy", color = Color(0xFF8A99AD), fontSize = 10.sp)
                                    Text("100% Points Reversed", color = LikeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TAB CONTENT
                when (activeTab) {
                    0 -> {
                        // WALLET & TOP-UP PACKAGES
                        Text("Top-Up Points Packages via Payment Gate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("100 points = ₹5.00 • Instant balance activation", color = Color(0xFF8A99AD), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        packages.forEach { pkg ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF181D2A),
                                border = BorderStroke(1.dp, Color(0xFF252D42)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(pkg.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (pkg.popularTag.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFFF9100)
                                                ) {
                                                    Text(pkg.popularTag, color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                        Text("${pkg.points} Broadcast Points (₹${String.format("%.2f", pkg.points * 0.05)})", color = Color(0xFF8A99AD), fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            walletPoints += pkg.points
                                            onTopUpSuccess(pkg.points)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00E5FF),
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Buy ₹${pkg.costInr}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // BROADCAST TO FOLLOWERS
                        Text("Broadcast Message to All Followers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val followers = business.followerCount.coerceAtLeast(1)
                        val pointsNeeded = followers * 1
                        Text(
                            text = "Reaches $followers registered followers • Costs $pointsNeeded points (₹${String.format("%.2f", pointsNeeded * 0.05)})",
                            color = Color(0xFF8A99AD),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it },
                            label = { Text("Broadcast Title / Headline", color = Color(0xFF8A99AD)) },
                            placeholder = { Text("e.g. 🍷 Flash 30% OFF First Date Cocktails Tonight!") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF2E384D)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = broadcastMessage,
                            onValueChange = { broadcastMessage = it },
                            label = { Text("Message Body", color = Color(0xFF8A99AD)) },
                            placeholder = { Text("Special announcement, coupon promo, or weekend acoustic music event update...") },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF2E384D)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (broadcastTitle.isNotBlank()) {
                                    isSendingBroadcast = true
                                    val delivered = Math.round(followers * 0.92f).toInt().coerceAtLeast(1)
                                    val unreached = (followers - delivered).coerceAtLeast(0)
                                    val reversed = unreached * 1
                                    val finalBalance = walletPoints - pointsNeeded + reversed
                                    walletPoints = finalBalance.coerceAtLeast(0)

                                    val log = BroadcastDeliveryLog(
                                        id = "log_${System.currentTimeMillis()}",
                                        title = broadcastTitle,
                                        totalFollowers = followers,
                                        deliveredDoubleTick = delivered,
                                        unreachedCount = unreached,
                                        pointsCharged = pointsNeeded,
                                        pointsReversed = reversed,
                                        timestamp = "Just now"
                                    )
                                    lastBroadcastResult = log
                                    deliveryLogs = listOf(log) + deliveryLogs
                                    onBroadcastSent(broadcastTitle, broadcastMessage)
                                    isSendingBroadcast = false
                                }
                            },
                            enabled = broadcastTitle.isNotBlank() && walletPoints >= pointsNeeded && !isSendingBroadcast,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (walletPoints >= pointsNeeded) "Dispatch Broadcast (${pointsNeeded} pts)" else "Insufficient Points (Need $pointsNeeded pts)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (lastBroadcastResult != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1B2A1E),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LikeGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Broadcast Delivered Successfully", color = LikeGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "✓✓ Double Tick Read Receipts: ${lastBroadcastResult!!.deliveredDoubleTick} followers",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "🔄 ${lastBroadcastResult!!.unreachedCount} followers not reached; ${lastBroadcastResult!!.pointsReversed} points automatically reversed to wallet!",
                                        color = Color(0xFF81C784),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // LOGS & REVERSAL AUDIT
                        Text("Broadcast Read Receipts & Points Reversals", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Transparent audit: Unreached recipient points refunded", color = Color(0xFF8A99AD), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        if (deliveryLogs.isEmpty()) {
                            Text("No broadcasts dispatched yet.", color = Color(0xFF8A99AD), fontSize = 12.sp)
                        } else {
                            deliveryLogs.forEach { log ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF181D2A),
                                    border = BorderStroke(1.dp, Color(0xFF252D42)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(log.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(log.timestamp, color = Color(0xFF8A99AD), fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${log.deliveredDoubleTick} Read (✓✓)", color = Color.White, fontSize = 11.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Replay, contentDescription = null, tint = LikeGreen, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("+${log.pointsReversed} pts reversed", color = LikeGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
