@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.data.model.BusinessEntity
import com.example.ui.theme.LikeGreen
import com.example.util.BusinessHubSyncManager
import com.example.util.CategoryShare
import com.example.util.EngagementDayPoint
import com.example.util.MenuItemAnalytics
import com.example.util.TrafficPoint
import com.example.util.VenueAnalyticsData
import com.example.util.VenueReportExporter
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BusinessDashboardScreen(
    venue: BusinessEntity,
    allVenues: List<BusinessEntity> = emptyList(),
    onSelectVenue: (BusinessEntity) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val analyticsMap by BusinessHubSyncManager.realtimeAnalytics.collectAsState()
    val currentAnalytics = analyticsMap[venue.id] ?: remember(venue.id) {
        BusinessHubSyncManager.getAnalyticsForVenue(venue.id, venue.name)
    }

    var selectedTimeframe by remember { mutableStateOf("This Week") } // "Today", "This Week", "This Month", "All Time"
    var selectedMetricTab by remember { mutableIntStateOf(0) } // 0: Traffic & Footfall, 1: Engagement & Leads, 2: Popular Menu
    var showExportDialog by remember { mutableStateOf(false) }

    // Live Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    if (showExportDialog) {
        ExportReportDialog(
            venue = venue,
            analytics = currentAnalytics,
            onDismiss = { showExportDialog = false }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF10131B),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF1A1F2C), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Business Intelligence",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "LIVE CLOUD",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = venue.name,
                                    color = Color(0xFF8A99AD),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick Export Button
                            IconButton(
                                onClick = { showExportDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E2330), CircleShape)
                                    .testTag("btn_export_report_top")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Export Report",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Live pulse badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF14241C),
                                border = BorderStroke(1.dp, LikeGreen.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(LikeGreen.copy(alpha = pulseAlpha))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${currentAnalytics.activeBrowsingNow} Live",
                                        color = LikeGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Venue Selector Row (if multiple venues available)
                    if (allVenues.size > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allVenues) { v ->
                                val isSelected = v.id == venue.id
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E2330),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF2E384D)),
                                    modifier = Modifier.clickable { onSelectVenue(v) }
                                ) {
                                    Text(
                                        text = v.name,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0B0D14)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("business_dashboard_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Timeframe & Metric Tabs Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Timeframe filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Today", "This Week", "This Month", "All Time").forEach { timeframe ->
                            val isSelected = selectedTimeframe == timeframe
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTimeframe = timeframe },
                                label = { Text(timeframe, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00E5FF),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF161B26),
                                    labelColor = Color(0xFF8A99AD)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFF283144),
                                    selectedBorderColor = Color(0xFF00E5FF)
                                )
                            )
                        }
                    }

                    // Key Metric Highlights Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricKpiCard(
                            title = "Total Views",
                            value = formatNumber(currentAnalytics.totalImpressions),
                            growth = "+${currentAnalytics.growthRatePercentage}%",
                            icon = Icons.Default.Visibility,
                            iconColor = Color(0xFF00E5FF),
                            modifier = Modifier.weight(1f)
                        )
                        MetricKpiCard(
                            title = "Footfall Visitors",
                            value = formatNumber(currentAnalytics.totalVisitors),
                            growth = "+18.2%",
                            icon = Icons.Default.People,
                            iconColor = LikeGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricKpiCard(
                            title = "Map Navigations",
                            value = currentAnalytics.directionRequests.toString(),
                            growth = "High intent",
                            icon = Icons.Default.Directions,
                            iconColor = Color(0xFFFFB300),
                            modifier = Modifier.weight(1f)
                        )
                        MetricKpiCard(
                            title = "Direct Inquiries",
                            value = (currentAnalytics.callInquiries + currentAnalytics.couponRedemptions).toString(),
                            growth = "Avg dwell ${currentAnalytics.avgDwellMinutes}m",
                            icon = Icons.Default.Call,
                            iconColor = Color(0xFFFF007A),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 2: Primary Visual Tabs
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF141923),
                    border = BorderStroke(1.dp, Color(0xFF242C3D))
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedMetricTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF00E5FF)
                    ) {
                        Tab(
                            selected = selectedMetricTab == 0,
                            onClick = { selectedMetricTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Traffic Trend", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = selectedMetricTab == 1,
                            onClick = { selectedMetricTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Engagement", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = selectedMetricTab == 2,
                            onClick = { selectedMetricTab = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Menu & Revenue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }
            }

            // Section 3: Interactive Graphs & Visualizations
            when (selectedMetricTab) {
                0 -> {
                    // TAB 0: Venue Traffic & Hourly Peak Volume
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                            border = BorderStroke(1.dp, Color(0xFF242C3D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Venue Traffic & Visitor Peaks",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Hourly visitor density with touch scrubbing",
                                            color = Color(0xFF8A99AD),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Peak 8 PM", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Recharts / D3-style Custom Area Graph in Jetpack Compose
                                SmoothAreaTrafficChart(
                                    points = currentAnalytics.hourlyTraffic,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Graph Legends
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E5FF))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Active Visitors", color = Color.White, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFF007A))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Date Couples", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: User Engagement & Conversion Funnel
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                            border = BorderStroke(1.dp, Color(0xFF242C3D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Weekly Engagement & Lead Conversion",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Impressions vs Actions taken (directions, calls & coupons)",
                                    color = Color(0xFF8A99AD),
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // D3-style Animated Multi-Bar Chart
                                EngagementBarChart(
                                    dailyData = currentAnalytics.dailyEngagement,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Conversion metrics summary
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1B2230),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Story Views", color = Color(0xFF8A99AD), fontSize = 11.sp)
                                            Text("${currentAnalytics.storyViews}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Coupons Claimed", color = Color(0xFF8A99AD), fontSize = 11.sp)
                                            Text("${currentAnalytics.couponRedemptions}", color = LikeGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Lead Conv Rate", color = Color(0xFF8A99AD), fontSize = 11.sp)
                                            Text("14.8%", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Popular Menu Items & Revenue Share
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                            border = BorderStroke(1.dp, Color(0xFF242C3D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Popular Dishes & Category Revenue",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Revenue distribution and top ordered date specials",
                                    color = Color(0xFF8A99AD),
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Interactive Donut / Pie Chart
                                PopularMenuItemsDonutChart(
                                    categories = currentAnalytics.categoryBreakdown,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Top Selling Menu Items",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                currentAnalytics.popularMenuItems.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1A202C),
                                        border = BorderStroke(1.dp, Color(0xFF283144)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.itemName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "${item.category} • ₹${item.price.toInt()}",
                                                    color = Color(0xFF8A99AD),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${item.ordersCount} orders",
                                                    color = Color(0xFF00E5FF),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = "₹${formatNumber(item.revenue.toInt())}",
                                                    color = LikeGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Export Performance Reports (Offline Records)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_performance_report_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Export Offline Reports",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Store official records & spreadsheets on device",
                                        color = Color(0xFF8A99AD),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E2638)
                            ) {
                                Text(
                                    text = "OFFLINE READY",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Download complete audit reports containing hourly customer footfall, lead conversions, and top-selling menu items with total revenue.",
                            color = Color(0xFFB0BDD0),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Export PDF Button
                            Button(
                                onClick = {
                                    val pdfFile = VenueReportExporter.exportPdfReport(context, venue, currentAnalytics)
                                    if (pdfFile != null) {
                                        Toast.makeText(context, "PDF Report generated! Opening share/save...", Toast.LENGTH_SHORT).show()
                                        VenueReportExporter.shareReportFile(context, pdfFile, "application/pdf", "${venue.name} Performance Report (PDF)")
                                    } else {
                                        Toast.makeText(context, "Failed to generate PDF report", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_export_pdf_direct"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PDF Report (A4)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Export CSV Button
                            OutlinedButton(
                                onClick = {
                                    val csvFile = VenueReportExporter.exportCsvReport(context, venue, currentAnalytics)
                                    if (csvFile != null) {
                                        Toast.makeText(context, "CSV Spreadsheet generated! Opening share/save...", Toast.LENGTH_SHORT).show()
                                        VenueReportExporter.shareReportFile(context, csvFile, "text/csv", "${venue.name} Analytics (CSV)")
                                    } else {
                                        Toast.makeText(context, "Failed to generate CSV report", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_export_csv_direct"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CSV Data (Excel)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 5: Live Simulation Action & Refresh
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF141A26),
                    border = BorderStroke(1.dp, Color(0xFF263044))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Real-Time Firestore Sync", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Auto-synced with Cloud Firestore collection `venue_analytics`", color = Color(0xFF8A99AD), fontSize = 11.sp)
                            }
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    BusinessHubSyncManager.recordVenueInteraction(venue.id, venue.name, "impression")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                            ) {
                                Text("+1 Live View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    BusinessHubSyncManager.recordVenueInteraction(venue.id, venue.name, "visitor")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                            ) {
                                Text("+1 Lead Check-in", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Metric KPI Card with growth badge
 */
@Composable
fun MetricKpiCard(
    title: String,
    value: String,
    growth: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
        border = BorderStroke(1.dp, Color(0xFF242C3D)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color(0xFF8A99AD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            Text(
                text = growth,
                color = LikeGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Custom Cubic Bézier Smooth Area Graph in Jetpack Compose Canvas (Recharts/D3 Equivalent)
 */
@Composable
fun SmoothAreaTrafficChart(
    points: List<TrafficPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val maxCount = remember(points) { (points.maxOfOrNull { it.count } ?: 100).coerceAtLeast(10) }

    Box(
        modifier = modifier
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val sectionWidth = size.width / (points.size - 1).coerceAtLeast(1)
                    val idx = (offset.x / sectionWidth).toInt().coerceIn(0, points.size - 1)
                    selectedIndex = if (selectedIndex == idx) -1 else idx
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val bottomPadding = 36.dp.toPx()
            val topPadding = 20.dp.toPx()
            val chartHeight = height - bottomPadding - topPadding

            if (points.size < 2) return@Canvas

            val stepX = width / (points.size - 1)

            // Draw horizontal gridlines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = topPadding + chartHeight * (i.toFloat() / gridLines)
                drawLine(
                    color = Color(0xFF242C3D).copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }

            // Generate Path for Primary Visitors (Cyan)
            val strokePath = Path()
            val fillPath = Path()

            val secondaryStrokePath = Path()
            val secondaryFillPath = Path()

            val primaryCoords = points.mapIndexed { index, p ->
                val x = index * stepX
                val y = topPadding + chartHeight * (1f - (p.count.toFloat() / maxCount))
                Offset(x, y)
            }

            val secondaryCoords = points.mapIndexed { index, p ->
                val x = index * stepX
                val y = topPadding + chartHeight * (1f - (p.secondaryCount.toFloat() / maxCount))
                Offset(x, y)
            }

            // Draw smooth Bézier curve for primary
            buildSmoothPath(strokePath, primaryCoords)
            buildSmoothPath(secondaryStrokePath, secondaryCoords)

            fillPath.addPath(strokePath)
            fillPath.lineTo(width, height - bottomPadding)
            fillPath.lineTo(0f, height - bottomPadding)
            fillPath.close()

            secondaryFillPath.addPath(secondaryStrokePath)
            secondaryFillPath.lineTo(width, height - bottomPadding)
            secondaryFillPath.lineTo(0f, height - bottomPadding)
            secondaryFillPath.close()

            // Draw Gradients
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.35f), Color(0xFF00E5FF).copy(alpha = 0.0f)),
                    startY = topPadding,
                    endY = height - bottomPadding
                )
            )

            drawPath(
                path = secondaryFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF007A).copy(alpha = 0.25f), Color(0xFFFF007A).copy(alpha = 0.0f)),
                    startY = topPadding,
                    endY = height - bottomPadding
                )
            )

            // Draw Strokes
            drawPath(
                path = strokePath,
                color = Color(0xFF00E5FF),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            drawPath(
                path = secondaryStrokePath,
                color = Color(0xFFFF007A),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw Circles on Points
            primaryCoords.forEachIndexed { idx, point ->
                val isSelected = selectedIndex == idx
                drawCircle(
                    color = Color(0xFF141923),
                    radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = if (isSelected) 5.dp.toPx() else 2.5.dp.toPx(),
                    center = point
                )
            }
        }

        // Bottom Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, p ->
                Text(
                    text = p.label,
                    color = if (selectedIndex == index) Color(0xFF00E5FF) else Color(0xFF8A99AD),
                    fontSize = 9.sp,
                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Floating Tooltip on scrub
        if (selectedIndex in points.indices) {
            val pt = points[selectedIndex]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1F293D),
                border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pt.label}: ${pt.count} Visitors (${pt.secondaryCount} couples)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Custom D3-style Animated Multi-Bar Graph in Jetpack Compose
 */
@Composable
fun EngagementBarChart(
    dailyData: List<EngagementDayPoint>,
    modifier: Modifier = Modifier
) {
    if (dailyData.isEmpty()) return

    val maxVal = remember(dailyData) {
        (dailyData.maxOfOrNull { maxOf(it.views, it.interactions) } ?: 500).coerceAtLeast(10)
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val barGroupWidth = width / dailyData.size
                val barWidth = barGroupWidth * 0.32f
                val spacing = barGroupWidth * 0.08f

                // Draw subtle horizontal gridlines
                for (i in 0..3) {
                    val y = height * (i.toFloat() / 3)
                    drawLine(
                        color = Color(0xFF242C3D).copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                dailyData.forEachIndexed { index, item ->
                    val groupStartX = index * barGroupWidth + (barGroupWidth - (barWidth * 2 + spacing)) / 2

                    // Views Bar (Cyan gradient)
                    val viewsHeight = (item.views.toFloat() / maxVal) * height
                    val viewsTop = height - viewsHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFF007799))),
                        topLeft = Offset(groupStartX, viewsTop),
                        size = Size(barWidth, viewsHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Interactions Bar (Pink/Rose gradient)
                    val interHeight = (item.interactions.toFloat() / maxVal) * height
                    val interTop = height - interHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFF007A), Color(0xFF990044))),
                        topLeft = Offset(groupStartX + barWidth + spacing, interTop),
                        size = Size(barWidth, interHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of week Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dailyData.forEach { day ->
                Text(
                    text = day.day,
                    color = Color(0xFF8A99AD),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Custom D3-style Donut Chart for Popular Menu Category Shares
 */
@Composable
fun PopularMenuItemsDonutChart(
    categories: List<CategoryShare>,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Donut Canvas
        Box(
            modifier = Modifier
                .size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                var startAngle = -90f

                categories.forEach { cat ->
                    val sweepAngle = (cat.sharePercentage / 100f) * 360f
                    drawArc(
                        color = Color(cat.colorHex),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "100%",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Menu Share",
                    color = Color(0xFF8A99AD),
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Category Legend List
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(cat.colorHex))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.category,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${cat.sharePercentage.toInt()}%",
                        color = Color(cat.colorHex),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun buildSmoothPath(path: Path, points: List<Offset>) {
    if (points.isEmpty()) return
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val p0 = points[i - 1]
        val p1 = points[i]
        val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
        val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)
        path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
    }
}

private fun formatNumber(number: Int): String {
    return NumberFormat.getNumberInstance(Locale.US).format(number)
}

@Composable
fun ExportReportDialog(
    venue: BusinessEntity,
    analytics: VenueAnalyticsData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var isGeneratingCsv by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("export_report_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121622),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
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
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export Venue Report",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = venue.name,
                                color = Color(0xFF8A99AD),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF1E2433), CircleShape)
                    ) {
                        Text("✕", color = Color.White, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF232A3B))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select your preferred offline report format:",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 1: PDF Document Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF171D2B),
                    border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isGeneratingPdf = true
                            val file = VenueReportExporter.exportPdfReport(context, venue, analytics)
                            isGeneratingPdf = false
                            if (file != null) {
                                Toast.makeText(context, "PDF Report generated!", Toast.LENGTH_SHORT).show()
                                VenueReportExporter.shareReportFile(context, file, "application/pdf", "${venue.name} Performance Report")
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE91E63).copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("PDF", color = Color(0xFFE91E63), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("A4 Executive PDF Audit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE91E63)
                                ) {
                                    Text(
                                        "RECOMMENDED",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "Includes formatted tables, hourly visitor curves, revenue breakdown & offline verification seal.",
                                color = Color(0xFF8A99AD),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: CSV Spreadsheet Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF171D2B),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isGeneratingCsv = true
                            val file = VenueReportExporter.exportCsvReport(context, venue, analytics)
                            isGeneratingCsv = false
                            if (file != null) {
                                Toast.makeText(context, "CSV Spreadsheet generated!", Toast.LENGTH_SHORT).show()
                                VenueReportExporter.shareReportFile(context, file, "text/csv", "${venue.name} Analytics Data")
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Failed to generate CSV", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("CSV", color = Color(0xFF00E5FF), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Raw CSV Spreadsheet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "Tabular raw data ready to import into Microsoft Excel, Google Sheets or accounting tools.",
                                color = Color(0xFF8A99AD),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242C3D))
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
