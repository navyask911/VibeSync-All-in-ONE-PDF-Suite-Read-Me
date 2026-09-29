@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.data.model.BusinessCategories
import com.example.data.model.BusinessCategory
import com.example.data.model.VerificationTier
import com.example.data.model.tierEnum
import com.example.util.LocationTrackerHelper
import com.example.util.GpsCoordinate
import kotlinx.coroutines.launch
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BusinessEntity
import com.example.ui.DatingViewModel
import com.example.ui.theme.CoralPink

@Composable
fun BusinessHubDialog(
    viewModel: DatingViewModel,
    initialTab: Int = 0, // 0: Nearest Businesses, 1: Add with Us, 2: My Businesses
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val allBusinesses by viewModel.allBusinesses.collectAsState()
    val myCreatedBusinesses by viewModel.myCreatedBusinesses.collectAsState()
    var selectedBusinessForProfile by remember { mutableStateOf<BusinessEntity?>(null) }
    var selectedVenueForDashboard by remember { mutableStateOf<BusinessEntity?>(null) }
    var showAdCampaignWizard by remember { mutableStateOf(false) }
    var activeWalletBiz by remember { mutableStateOf<BusinessEntity?>(null) }
    var activeBroadcastBiz by remember { mutableStateOf<BusinessEntity?>(null) }
    var activeDirectApiBiz by remember { mutableStateOf<BusinessEntity?>(null) }
    var activeUpgradeBiz by remember { mutableStateOf<BusinessEntity?>(null) }
    var activeQrStickerBiz by remember { mutableStateOf<BusinessEntity?>(null) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modern Optimized Single-Line Top Bar & Header Structure
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                IconButton(
                                    onClick = onDismissRequest,
                                    modifier = Modifier.size(34.dp).testTag("btn_back_business_hub")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "VibeSync • Business Suite",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { showAdCampaignWizard = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("btn_navbar_add_with_us")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddBusiness,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "+ Add with us",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Wallet & Broadcast Dropdown Summary Chip
                        val primaryVenue = myCreatedBusinesses.firstOrNull() ?: allBusinesses.firstOrNull()
                        val walletPoints = primaryVenue?.walletPoints ?: 500
                        val walletInr = walletPoints * 0.01
                        var showWalletDropdown by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showWalletDropdown = !showWalletDropdown }
                                    .testTag("chip_wallet_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("💳", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Wallet: $walletPoints Pts (₹${String.format(Locale.getDefault(), "%.2f", walletInr)}) ▾",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                    Text(
                                        text = if (showWalletDropdown) "Close Menu" else "Manage Hub ▾",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showWalletDropdown,
                                onDismissRequest = { showWalletDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("⚡ Top-Up Points (+500 Pts)", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showWalletDropdown = false
                                        activeWalletBiz = primaryVenue ?: BusinessEntity(
                                            id = "biz_default",
                                            name = "My Business",
                                            category = "Partner",
                                            description = "VibeSync Partner Venue",
                                            address = "Local Hub",
                                            walletPoints = 500
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("📢 Broadcast to Followers") },
                                    onClick = {
                                        showWalletDropdown = false
                                        activeBroadcastBiz = primaryVenue ?: BusinessEntity(
                                            id = "biz_default",
                                            name = "My Business",
                                            category = "Partner",
                                            description = "VibeSync Partner Venue",
                                            address = "Local Hub",
                                            walletPoints = 500
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("🏷️ Print Counter QR & Stickers") },
                                    onClick = {
                                        showWalletDropdown = false
                                        activeQrStickerBiz = primaryVenue ?: allBusinesses.firstOrNull() ?: BusinessEntity(
                                            id = "biz_default",
                                            name = "My Business",
                                            category = "Partner",
                                            description = "VibeSync Partner Venue",
                                            address = "Local Hub",
                                            walletPoints = 500
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Compact Segmented Navigation Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = listOf(
                                Triple(0, "Nearest (${allBusinesses.size})", Icons.Default.Store),
                                Triple(1, "Add with Us", Icons.Default.AddBusiness),
                                Triple(2, "My Venues (${myCreatedBusinesses.size})", Icons.Default.Business),
                                Triple(3, "Analytics", Icons.Default.ShowChart),
                                Triple(4, "QR Stickers", Icons.Default.QrCode2)
                            )
                            tabs.forEach { (index, title, icon) ->
                                val isSelected = selectedTab == index
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clickable { selectedTab = index }
                                        .testTag("tab_business_hub_$index")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = title,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab Content
                when (selectedTab) {
                    0 -> NearestBusinessesTab(
                        businesses = allBusinesses,
                        onFollow = { viewModel.toggleFollowBusiness(it.id) },
                        onSelect = { selectedBusinessForProfile = it }
                    )
                    1 -> AddWithUsTab(
                        viewModel = viewModel,
                        onSuccess = { createdBiz ->
                            selectedTab = 0
                            selectedBusinessForProfile = createdBiz
                        }
                    )
                    2 -> MyBusinessesTab(
                        businesses = myCreatedBusinesses,
                        viewModel = viewModel,
                        onSelect = { selectedBusinessForProfile = it },
                        onViewAnalytics = { biz ->
                            selectedVenueForDashboard = biz
                            selectedTab = 3
                        },
                        onAddNew = { selectedTab = 1 },
                        onRestore = { viewModel.restoreUserBusinesses() }
                    )
                    3 -> {
                        val activeVenue = selectedVenueForDashboard 
                            ?: myCreatedBusinesses.firstOrNull() 
                            ?: allBusinesses.firstOrNull()
                        if (activeVenue != null) {
                            BusinessDashboardScreen(
                                venue = activeVenue,
                                allVenues = if (myCreatedBusinesses.isNotEmpty()) myCreatedBusinesses else allBusinesses,
                                onSelectVenue = { selectedVenueForDashboard = it },
                                onNavigateBack = { selectedTab = 0 }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No venues registered to display live analytics.", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdCampaignWizard) {
        AdCampaignWizardDialog(
            viewModel = viewModel,
            business = myCreatedBusinesses.firstOrNull(),
            onDismissRequest = { showAdCampaignWizard = false }
        )
    }

    selectedBusinessForProfile?.let { biz ->
        // Retrieve latest entity state from allBusinesses if available
        val currentBiz = allBusinesses.firstOrNull { it.id == biz.id } ?: biz
        BusinessProfileDialog(
            business = currentBiz,
            viewModel = viewModel,
            onDismissRequest = { selectedBusinessForProfile = null }
        )
    }

    activeWalletBiz?.let { biz ->
        val currentBiz = allBusinesses.firstOrNull { it.id == biz.id } ?: biz
        BusinessWalletDialog(
            business = currentBiz,
            viewModel = viewModel,
            onDismiss = { activeWalletBiz = null }
        )
    }

    activeBroadcastBiz?.let { biz ->
        val currentBiz = allBusinesses.firstOrNull { it.id == biz.id } ?: biz
        SendFollowerBroadcastDialog(
            business = currentBiz,
            viewModel = viewModel,
            onDismiss = { activeBroadcastBiz = null }
        )
    }

    activeDirectApiBiz?.let { biz ->
        val currentBiz = allBusinesses.firstOrNull { it.id == biz.id } ?: biz
        VibeSyncCloudApiConfigDialog(
            business = currentBiz,
            viewModel = viewModel,
            onDismiss = { activeDirectApiBiz = null }
        )
    }

    activeUpgradeBiz?.let { biz ->
        val currentBiz = allBusinesses.firstOrNull { it.id == biz.id } ?: biz
        UpgradeTierDialog(
            business = currentBiz,
            viewModel = viewModel,
            onDismiss = { activeUpgradeBiz = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NearestBusinessesTab(
    businesses: List<BusinessEntity>,
    onFollow: (BusinessEntity) -> Unit,
    onSelect: (BusinessEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(BusinessCategories.ALL) }
    var selectedSubCategory by remember { mutableStateOf<String?>(null) }
    var activeDropdownParent by remember { mutableStateOf<String?>(null) }

    // GPS Location State
    var currentCoordinate by remember { mutableStateOf(LocationTrackerHelper.DEFAULT_BANGALORE_COORDINATES) }
    var isGpsRefreshing by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(LocationTrackerHelper.hasLocationPermission(context)) }

    val refreshLocation: () -> Unit = {
        scope.launch {
            isGpsRefreshing = true
            currentCoordinate = LocationTrackerHelper.getDeviceLocation(context)
            hasLocationPermission = LocationTrackerHelper.hasLocationPermission(context)
            isGpsRefreshing = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            refreshLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            refreshLocation()
        }
    }

    // Dynamic GPS Calculation & Tiered Search Preference & Ranking Algorithm:
    // 1st Priority: Gold Verified venues (highest ranking)
    // 2nd Priority: Silver Verified venues
    // 3rd Priority: Blue Tick Verified venues
    // 4th Priority: Unverified (standard ₹99 listed) venues
    // Secondary sorting within tier: Customer star ratings (DESC), then Proximity in km (ASC)
    val processedBusinesses = remember(businesses, currentCoordinate) {
        businesses.map { biz ->
            val realDist = LocationTrackerHelper.calculateDistanceKm(
                deviceLat = currentCoordinate.latitude,
                deviceLon = currentCoordinate.longitude,
                venueLat = biz.latitude,
                venueLon = biz.longitude
            )
            biz.copy(distanceKm = realDist)
        }.sortedWith(
            compareByDescending<BusinessEntity> {
                when (it.verificationTier.uppercase()) {
                    "GOLD" -> 4
                    "SILVER" -> 3
                    "BLUE_TICK", "BLUE" -> 2
                    else -> 1 // STANDARD / Unverified
                }
            }
            .thenByDescending { it.rating }
            .thenBy { it.distanceKm }
        )
    }

    // Filter by Search Query & Category / Subcategory
    val filteredBusinesses = remember(processedBusinesses, searchQuery, selectedCategory, selectedSubCategory) {
        processedBusinesses.filter { biz ->
            val matchesQuery = searchQuery.isBlank() ||
                    biz.name.contains(searchQuery, ignoreCase = true) ||
                    biz.tagline.contains(searchQuery, ignoreCase = true) ||
                    biz.address.contains(searchQuery, ignoreCase = true) ||
                    biz.category.contains(searchQuery, ignoreCase = true)

            val matchesCat = BusinessCategories.matchesCategory(
                bizCategory = biz.category,
                bizName = biz.name,
                bizTagline = biz.tagline,
                selectedParentCategory = selectedCategory,
                selectedSubCategory = selectedSubCategory
            )

            matchesQuery && matchesCat
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Collapsible Location & Sorting Info Accordion Drawer
            var showLocationAccordion by remember { mutableStateOf(false) }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLocationAccordion = !showLocationAccordion }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📍 Live GPS: Bengaluru (${String.format(Locale.getDefault(), "%.2f", currentCoordinate.latitude)}, ${String.format(Locale.getDefault(), "%.2f", currentCoordinate.longitude)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = if (showLocationAccordion) "Hide Info ▴" else "Location & Sorting Info ▾",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showLocationAccordion) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sorting Priority Legend:", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text("1. Gold Verified Spots\n2. Silver Verified Venues\n3. Blue Tick Verified\n4. Proximity & Rating", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { refreshLocation() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isGpsRefreshing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp))
                                } else {
                                    Text("Locate Me", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search shops, cafes, clinics, utilities...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            // 1. Horizontal Category Bar (All + 5 Parent Categories with dropdown sub-navigation)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All" Pill Button
                val isAllSelected = selectedCategory == BusinessCategories.ALL && selectedSubCategory == null
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable {
                        selectedCategory = BusinessCategories.ALL
                        selectedSubCategory = null
                        activeDropdownParent = null
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 5 Main Parent Categories
                BusinessCategories.PARENT_CATEGORIES.forEach { categoryItem ->
                    val parentName = categoryItem.name
                    val isParentSelected = selectedCategory == parentName
                    val isMenuOpen = activeDropdownParent == parentName

                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isParentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isMenuOpen) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    selectedCategory = parentName
                                    activeDropdownParent = if (isMenuOpen) null else parentName
                                },
                                onLongClick = {
                                    selectedCategory = parentName
                                    activeDropdownParent = parentName
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = categoryItem.iconEmoji,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = parentName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isParentSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand $parentName subcategories",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isParentSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 2. Drop-Down Sub-Category Navigation Menu
                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { activeDropdownParent = null },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .widthIn(min = 250.dp, max = 340.dp)
                        ) {
                            // "All [Parent Category]" option
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "All in $parentName",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isParentSelected && selectedSubCategory == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isParentSelected && selectedSubCategory == null) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCategory = parentName
                                    selectedSubCategory = null
                                    activeDropdownParent = null
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Sub-category items
                            categoryItem.subCategories.forEach { sub ->
                                val isSubSelected = isParentSelected && selectedSubCategory == sub
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = sub,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSubSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSubSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedCategory = parentName
                                        selectedSubCategory = sub
                                        activeDropdownParent = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sub-category active indicator chip
        if (selectedSubCategory != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sub-category: ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = selectedSubCategory!!,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { selectedSubCategory = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear subcategory filter",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            // 3. Real-Time GPS Proximity Status & Counter Bar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (currentCoordinate.isRealTimeGps) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = if (currentCoordinate.isRealTimeGps) BorderStroke(1.dp, Color(0xFF81C784)) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (currentCoordinate.isRealTimeGps) Icons.Default.MyLocation else Icons.Default.NearMe,
                            contentDescription = null,
                            tint = if (currentCoordinate.isRealTimeGps) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (currentCoordinate.isRealTimeGps) "📍 Live GPS: ${currentCoordinate.label}" else "📍 ${currentCoordinate.label}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentCoordinate.isRealTimeGps) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (currentCoordinate.isRealTimeGps) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant
                                ) {
                                    Text(
                                        text = if (currentCoordinate.isRealTimeGps) "PROXIMITY GPS" else "DEFAULT COORDS",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Priority: ⭐ Gold > 🛡️ Silver > ✓ Blue Tick > Standard, then Rating & GPS Proximity • ${filteredBusinesses.size} venues",
                                fontSize = 10.sp,
                                color = if (currentCoordinate.isRealTimeGps) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!hasLocationPermission) {
                        OutlinedButton(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Enable GPS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(
                            onClick = { refreshLocation() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh GPS Location",
                                tint = if (currentCoordinate.isRealTimeGps) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (filteredBusinesses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 38.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No venues found in this category",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try switching category, clearing sub-filter, or widening your search.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                selectedCategory = BusinessCategories.ALL
                                selectedSubCategory = null
                                searchQuery = ""
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset All Filters")
                        }
                    }
                }
            }
        } else {
            items(filteredBusinesses, key = { it.id }) { biz ->
                BusinessListItemCard(
                    business = biz,
                    onFollow = { onFollow(biz) },
                    onSelect = { onSelect(biz) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BusinessListItemCard(
    business: BusinessEntity,
    onFollow: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("business_list_item_${business.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            // Thumbnail Banner with Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = business.bannerUrl.ifBlank { "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80" },
                    contentDescription = business.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                // Verification Badge, Distance, and Customer Star Rating
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VerificationBadgeView(tier = business.tierEnum, showRank = true)

                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("${business.distanceKm} km", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${business.rating} (${business.reviewCount})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Business Name & Category on bottom of banner
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(business.logoEmoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = business.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${business.category} • ${business.city}",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Body content
            Column(modifier = Modifier.padding(14.dp)) {
                if (business.activeOfferSummary.isNotBlank()) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = business.activeOfferSummary,
                                color = Color(0xFFBF360C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = business.tagline,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥 ${business.followerCount} followers",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onFollow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (business.isFollowed) Color(0xFF2E7D32) else Color(0xFFE91E63)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (business.isFollowed) Icons.Default.Check else Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (business.isFollowed) "Following" else "Follow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onSelect,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("View Offers", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddWithUsTab(
    viewModel: DatingViewModel,
    onSuccess: (BusinessEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var businessName by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var parentCategory by remember { mutableStateOf(BusinessCategories.FOOD_AND_HOSPITALITY) }
    var subCategory by remember { mutableStateOf("Café / Coffee shop") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Bangalore") }
    var distanceKm by remember { mutableStateOf("1.2") }
    var detectedLatitude by remember { mutableDoubleStateOf(12.9716) }
    var detectedLongitude by remember { mutableDoubleStateOf(77.5946) }
    var isLocatingGps by remember { mutableStateOf(false) }
    var gpsCapturedSuccess by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("+91 98765 43210") }
    var websiteUrl by remember { mutableStateOf("https://") }
    var photo1 by remember { mutableStateOf("https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80") }
    var photo2 by remember { mutableStateOf("https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=800&q=80") }
    var photo3 by remember { mutableStateOf("https://images.unsplash.com/photo-1559925393-8be0ec4767c8?auto=format&fit=crop&w=800&q=80") }
    var logoEmoji by remember { mutableStateOf("☕") }
    var offerTitle by remember { mutableStateOf("Welcome Couple Discount: Flat 25% OFF") }
    var discountPercent by remember { mutableStateOf("25") }
    var promoCode by remember { mutableStateOf("WELCOME25") }
    var selectedVerificationTier by remember { mutableStateOf("STANDARD") }
    var isListingPaid by remember { mutableStateOf(false) }
    var paymentTxnId by remember { mutableStateOf("") }
    var showPaymentGateway by remember { mutableStateOf(false) }

    val baseListingFee = 99
    val tierAddOnFee = when (selectedVerificationTier) {
        "GOLD" -> 499
        "SILVER" -> 299
        "BLUE_TICK" -> 199
        else -> 0
    }
    val totalPayableAmount = baseListingFee + tierAddOnFee
    val bonusPoints = when (selectedVerificationTier) {
        "GOLD" -> 2500
        "SILVER" -> 1500
        "BLUE_TICK" -> 900
        else -> 500
    }

    if (showPaymentGateway) {
        VenturePaymentGatewayDialog(
            title = "VibeSync Venture Registration",
            purpose = "Annual Listing (₹99) + ${VerificationTier.fromId(selectedVerificationTier).badgeLabel} (₹$tierAddOnFee)",
            amount = totalPayableAmount,
            onSuccess = { txnId ->
                isListingPaid = true
                paymentTxnId = txnId
                showPaymentGateway = false
            },
            onDismiss = { showPaymentGateway = false }
        )
    }

    val emojis = listOf("☕", "🍸", "🍺", "🍕", "🛍️", "📚", "🏋️", "✂️", "🧖‍♀️", "🏧", "📦", "🏛️")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        Surface(
            color = Color(0xFFE8F5E9),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VibeSync Business Suite • ₹99/yr Plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1B5E20)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Publish your business profile on VibeSync for ₹99/year to be visible in local search and GPS proximity listings. (Note: Listing does not include paid ad placements; to run ad campaigns, use 'Ad with us'. Use Broadcast messages to reach followers and Badges to boost your search priority.)",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF2E7D32)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Business Information", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Business / Venue Name *") },
            placeholder = { Text("e.g. Blue Tokai Coffee Roasters") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_biz_name")
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tagline,
            onValueChange = { tagline = it },
            label = { Text("Short Pitch / Tagline *") },
            placeholder = { Text("e.g. Cozy Corner Couches & Handcrafted Pour-overs") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text("Select Main Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BusinessCategories.PARENT_CATEGORIES.forEach { catItem ->
                val isSelected = parentCategory == catItem.name
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable {
                        parentCategory = catItem.name
                        subCategory = catItem.subCategories.firstOrNull() ?: ""
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(catItem.iconEmoji, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = catItem.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Select Sub-Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))

        val currentSubs = BusinessCategories.getSubcategories(parentCategory)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            currentSubs.forEach { sub ->
                val isSelected = subCategory == sub
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.clickable { subCategory = sub }
                ) {
                    Text(
                        text = sub,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Choose Icon Emoji", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            emojis.forEach { emoji ->
                val isSelected = logoEmoji == emoji
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { logoEmoji = emoji }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Full Description & Vibe *") },
            placeholder = { Text("Tell singles why your venue is the ultimate first date spot...") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Physical Address & Landmark *") },
            placeholder = { Text("e.g. 100 Ft Road, Indiranagar, Bangalore") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Auto-Detect GPS Location Section (Requirement 3)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, if (gpsCapturedSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (gpsCapturedSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Venue GPS Coordinates *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = if (gpsCapturedSuccess) "Locked: ${String.format(java.util.Locale.getDefault(), "%.4f, %.4f", detectedLatitude, detectedLongitude)} • $city" else "Capture exact coordinates for in-app map navigation",
                            fontSize = 11.sp,
                            color = if (gpsCapturedSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLocatingGps = true
                                val loc = LocationTrackerHelper.getDeviceLocation(context)
                                detectedLatitude = loc.latitude
                                detectedLongitude = loc.longitude
                                gpsCapturedSuccess = true
                                city = if (loc.latitude in 12.8..13.2 && loc.longitude in 77.4..77.8) "Bangalore" else "Local Metro"
                                distanceKm = "0.8"
                                isLocatingGps = false
                                Toast.makeText(context, "📍 GPS Coordinates captured: ${String.format(java.util.Locale.getDefault(), "%.4f, %.4f", loc.latitude, loc.longitude)}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gpsCapturedSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_auto_detect_gps")
                    ) {
                        if (isLocatingGps) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (gpsCapturedSuccess) "GPS Locked ✓" else "Auto-Detect GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = websiteUrl,
                onValueChange = { websiteUrl = it },
                label = { Text("Website / Social") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Multi-Photo Batch Gallery Upload (Requirement 5)
        Text("Venue Photo Gallery (Up to 3 Photos) *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text("Uploaded photos render in a swipeable horizontal gallery on your venue profile.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("Photo 1 (Main)", photo1) { v: String -> photo1 = v },
                Triple("Photo 2", photo2) { v: String -> photo2 = v },
                Triple("Photo 3", photo3) { v: String -> photo3 = v }
            ).forEachIndexed { idx, (label, value, onValChange) ->
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) {
                        if (value.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(value).crossfade(true).build(),
                                contentDescription = "Venue photo ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValChange,
                        placeholder = { Text("URL ${idx + 1}", fontSize = 10.sp) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Launch Offer / Discount (Attracts Followers)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = offerTitle,
            onValueChange = { offerTitle = it },
            label = { Text("Offer Headline") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = discountPercent,
                onValueChange = { discountPercent = it },
                label = { Text("Discount %") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = promoCode,
                onValueChange = { promoCode = it },
                label = { Text("Promo Code") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Verification Tier Selection
        VerificationTierSelectionSection(
            selectedTier = selectedVerificationTier,
            onSelectTier = { selectedVerificationTier = it }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Mandatory Annual Listing Fee & Payment Gate (Requirement 1 & 2)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isListingPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isListingPaid) Color(0xFF81C784) else Color(0xFFFFB300)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isListingPaid) "Listing Payment Verified ✅" else "Payment Gate: Annual Listing Fee",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isListingPaid) Color(0xFF1B5E20) else Color(0xFFE65100)
                        )
                        Text(
                            text = if (isListingPaid) "Txn ID: #$paymentTxnId" else "Mandatory ₹99/yr + Verification Tier",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "₹$totalPayableAmount",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = if (isListingPaid) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = if (isListingPaid) Color(0xFFA5D6A7) else Color(0xFFFFCC80))
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Base Annual Listing Fee (Mandatory):", fontSize = 11.sp)
                    Text("₹99/year", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Verification Tier Add-On (${VerificationTier.fromId(selectedVerificationTier).badgeLabel}):", fontSize = 11.sp)
                    Text("₹$tierAddOnFee/year", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• Included Bonus Points:", fontSize = 11.sp)
                    Text("$bonusPoints pts (₹${String.format(java.util.Locale.getDefault(), "%.2f", bonusPoints * 0.05)})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1B5E20))
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isListingPaid) {
                    Button(
                        onClick = { showPaymentGateway = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay ₹$totalPayableAmount via Payment Gateway 🔒", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "🔒 Venture submission is locked until the mandatory payment transaction is successfully completed via payment gateway.",
                        fontSize = 10.sp,
                        color = Color(0xFFD84315),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFC8E6C9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Payment completed successfully! Submission unlocked.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                if (businessName.isNotBlank() && address.isNotBlank() && isListingPaid) {
                    viewModel.createBusinessProfile(
                        name = businessName,
                        tagline = tagline.ifBlank { "Top rated spot for memorable dates" },
                        category = "$parentCategory • $subCategory",
                        description = description.ifBlank { "Exciting partner venue featuring top cuisine, artisan brews and couple vibes." },
                        address = address,
                        city = city,
                        distanceKm = distanceKm.toDoubleOrNull() ?: 0.8,
                        bannerUrl = photo1.ifBlank { photo2.ifBlank { photo3 } },
                        logoEmoji = logoEmoji,
                        phoneNumber = phoneNumber,
                        websiteUrl = websiteUrl,
                        initialOfferTitle = offerTitle,
                        initialDiscountPercent = discountPercent.toIntOrNull() ?: 20,
                        initialPromoCode = promoCode,
                        verificationTier = selectedVerificationTier,
                        walletPoints = bonusPoints,
                        isListingPaid = true,
                        listingPaymentTxnId = paymentTxnId,
                        latitude = detectedLatitude,
                        longitude = detectedLongitude,
                        photoGallery = listOf(photo1, photo2, photo3).filter { it.isNotBlank() },
                        onSuccess = onSuccess
                    )
                }
            },
            enabled = businessName.isNotBlank() && address.isNotBlank() && isListingPaid,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_publish_business"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListingPaid) Color(0xFFE91E63) else Color(0xFF9E9E9E)
            )
        ) {
            Text(
                text = if (isListingPaid) "Publish Business Profile ✨" else "Pay ₹$totalPayableAmount Listing Fee to Unlock 🔒",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun MyBusinessesTab(
    businesses: List<BusinessEntity>,
    viewModel: DatingViewModel,
    onSelect: (BusinessEntity) -> Unit,
    onViewAnalytics: (BusinessEntity) -> Unit,
    onAddNew: () -> Unit,
    onRestore: () -> Unit = {}
) {
    var activeWalletBiz by remember { mutableStateOf<BusinessEntity?>(null) }
    var activeBroadcastBiz by remember { mutableStateOf<BusinessEntity?>(null) }
    var activeDirectApiBiz by remember { mutableStateOf<BusinessEntity?>(null) }
    var activeUpgradeBiz by remember { mutableStateOf<BusinessEntity?>(null) }

    activeWalletBiz?.let { biz ->
        val currentBiz = businesses.firstOrNull { it.id == biz.id } ?: biz
        BusinessWalletDialog(
            business = currentBiz,
            viewModel = viewModel,
            onDismiss = { activeWalletBiz = null }
        )
    }

    activeBroadcastBiz?.let { biz ->
        val currentBiz = businesses.firstOrNull { it.id == biz.id } ?: biz
        SendFollowerBroadcastDialog(
            business = currentBiz,
            viewModel = viewModel,
            onDismiss = { activeBroadcastBiz = null }
        )
    }

    activeDirectApiBiz?.let { biz ->
        val currentBiz = businesses.firstOrNull { it.id == biz.id } ?: biz
        VibeSyncCloudApiConfigDialog(
            business = currentBiz,
            viewModel = viewModel,
            onDismiss = { activeDirectApiBiz = null }
        )
    }

    activeUpgradeBiz?.let { biz ->
        val currentBiz = businesses.firstOrNull { it.id == biz.id } ?: biz
        UpgradeTierDialog(
            business = currentBiz,
            viewModel = viewModel,
            onDismiss = { activeUpgradeBiz = null }
        )
    }

    if (businesses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏢", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You haven't added a business yet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap 'Add with Us' to list your cafe, lounge, activity or brand on VibeSync and broadcast deals!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAddNew,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Your Business Now", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRestore,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Paid Business from Cloud", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(businesses, key = { it.id }) { biz ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(biz) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(biz.logoEmoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(biz.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${biz.category} • ${biz.city}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                VerificationBadgeView(tier = biz.tierEnum, showRank = true)
                                Spacer(modifier = Modifier.height(3.dp))
                                Surface(
                                    color = Color(0xFF2E7D32),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "LIVE (₹99)",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Business Points Wallet Row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💳", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            "Wallet: ${biz.walletPoints} pts (₹${String.format(java.util.Locale.getDefault(), "%.2f", biz.walletPoints * 0.05)})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF1B5E20)
                                        )
                                        Text(
                                            "Can broadcast to ${biz.walletPoints} followers",
                                            fontSize = 9.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                                Button(
                                    onClick = { activeWalletBiz = biz },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("+ Reload", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats & Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👥 ${biz.followerCount} followers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { activeBroadcastBiz = biz },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("📢 Broadcast", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { activeDirectApiBiz = biz },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("💬 Direct API", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                if (biz.verificationTier != "GOLD") {
                                    OutlinedButton(
                                        onClick = { activeUpgradeBiz = biz },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("⭐ Upgrade", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onViewAnalytics(biz) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("📊", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { onSelect(biz) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Manage ↗", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onAddNew,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Another Business", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
