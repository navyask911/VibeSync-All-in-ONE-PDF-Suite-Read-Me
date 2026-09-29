package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BusinessEntity
import com.example.ui.theme.LikeGreen
import com.example.util.BusinessHubSyncManager
import com.example.util.PlacesSearchService
import com.example.util.PlaceSearchResult
import kotlinx.coroutines.launch

data class MenuItemData(val id: String, val name: String, val price: String, val category: String)
data class EventData(val id: String, val title: String, val dateTime: String, val description: String)

@Composable
fun VenueManagerDialog(
    business: BusinessEntity,
    onSaveVenueDetails: (updatedBusiness: BusinessEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var venueName by remember { mutableStateOf(business.name) }
    var tagline by remember { mutableStateOf(business.tagline) }
    var address by remember { mutableStateOf(business.address) }
    var city by remember { mutableStateOf(business.city) }
    var latitude by remember { mutableStateOf(business.latitude.toString()) }
    var longitude by remember { mutableStateOf(business.longitude.toString()) }

    // Places Search state
    var placeSearchQuery by remember { mutableStateOf("") }
    var placeSearchResults by remember { mutableStateOf<List<PlaceSearchResult>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Photos (up to 5)
    val venuePhotos = remember { mutableStateListOf<String>() }
    if (venuePhotos.isEmpty() && business.bannerUrl.isNotBlank()) {
        venuePhotos.add(business.bannerUrl)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && venuePhotos.size < 5) {
            venuePhotos.add(uri.toString())
        }
    }

    // Timeline Post state
    var newPostCaption by remember { mutableStateOf("") }
    var newPostImageUri by remember { mutableStateOf<String?>(null) }
    val timelinePosts by BusinessHubSyncManager.realtimeTimeline.collectAsState()

    val timelinePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            newPostImageUri = uri.toString()
        }
    }

    // Menu items
    var newMenuName by remember { mutableStateOf("") }
    var newMenuPrice by remember { mutableStateOf("") }
    val menuItems = remember {
        mutableStateListOf(
            MenuItemData("1", "Signature Cappuccino", "₹240", "Beverages"),
            MenuItemData("2", "Woodfire Truffle Pizza", "₹650", "Main Course"),
            MenuItemData("3", "Belgian Chocolate Fondue", "₹480", "Desserts")
        )
    }

    // Events
    var newEventTitle by remember { mutableStateOf("") }
    var newEventTime by remember { mutableStateOf("Friday, 8:00 PM") }
    var newEventDesc by remember { mutableStateOf("") }
    val venueEvents = remember {
        mutableStateListOf(
            EventData("e1", "Acoustic Live Night", "Friday, 8:00 PM", "Unwind with soulful live guitar and cocktails.")
        )
    }

    var showDashboardScreen by remember { mutableStateOf(false) }

    if (showDashboardScreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDashboardScreen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BusinessDashboardScreen(
                venue = business,
                onNavigateBack = { showDashboardScreen = false }
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("venue_manager_dialog"),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color(0xFF12141C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
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
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Venue Manager Suite",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Photos, Menu, Events & Timeline Feed",
                            color = Color(0xFF8A99AD),
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1E2330), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 0: Live Intelligence & Traffic Analytics Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0E1A29),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDashboardScreen = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
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
                                            imageVector = Icons.Default.ShowChart,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Live Analytics & Traffic Dashboard",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Hourly graphs, conversion funnel & top menu items",
                                        color = Color(0xFF8A99AD),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { showDashboardScreen = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Open ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 1: Venue Photos (Up to 5)
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Venue Photos (${venuePhotos.size}/5)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (venuePhotos.size < 5) {
                                TextButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Photo", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(venuePhotos) { photoUrl ->
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E2330))
                                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Venue Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { venuePhotos.remove(photoUrl) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            if (venuePhotos.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1A1F2C)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: General Venue Info
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Venue Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedTextField(
                            value = venueName,
                            onValueChange = { venueName = it },
                            label = { Text("Venue Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF252D42),
                                focusedLabelColor = Color(0xFF00E5FF),
                                unfocusedLabelColor = Color(0xFF8A99AD),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = tagline,
                            onValueChange = { tagline = it },
                            label = { Text("Tagline / Special Vibe") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF252D42),
                                focusedLabelColor = Color(0xFF00E5FF),
                                unfocusedLabelColor = Color(0xFF8A99AD),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }

                // Section 3: Map Location & Coordinates with Places Search Integration
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            Text("Map Location & Pin (Places Search)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = placeSearchQuery,
                                onValueChange = { placeSearchQuery = it },
                                label = { Text("Search location (e.g. Connaught Place)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF252D42),
                                    focusedLabelColor = Color(0xFF00E5FF),
                                    unfocusedLabelColor = Color(0xFF8A99AD),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        placeSearchResults = PlacesSearchService.searchPlaces(placeSearchQuery)
                                    }
                                },
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                            ) {
                                Text("Search", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (placeSearchResults.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Select Location Result to Pin:", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                placeSearchResults.forEach { res ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1A1F2C),
                                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            address = res.address
                                            latitude = res.lat.toString()
                                            longitude = res.lon.toString()
                                            placeSearchResults = emptyList()
                                            placeSearchQuery = ""
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(res.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text("Lat: ${res.lat}, Lon: ${res.lon}", color = Color(0xFF8A99AD), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Street Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF252D42),
                                focusedLabelColor = Color(0xFF00E5FF),
                                unfocusedLabelColor = Color(0xFF8A99AD),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = latitude,
                                onValueChange = { latitude = it },
                                label = { Text("Latitude") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF252D42),
                                    focusedLabelColor = Color(0xFF00E5FF),
                                    unfocusedLabelColor = Color(0xFF8A99AD),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            OutlinedTextField(
                                value = longitude,
                                onValueChange = { longitude = it },
                                label = { Text("Longitude") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF252D42),
                                    focusedLabelColor = Color(0xFF00E5FF),
                                    unfocusedLabelColor = Color(0xFF8A99AD),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Section 4: Menu Items & Prices
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            Text("Menu & Pricing (${menuItems.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newMenuName,
                                onValueChange = { newMenuName = it },
                                label = { Text("Item Name") },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF252D42),
                                    focusedLabelColor = Color(0xFF00E5FF),
                                    unfocusedLabelColor = Color(0xFF8A99AD),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            OutlinedTextField(
                                value = newMenuPrice,
                                onValueChange = { newMenuPrice = it },
                                label = { Text("Price (₹)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color(0xFF252D42),
                                    focusedLabelColor = Color(0xFF00E5FF),
                                    unfocusedLabelColor = Color(0xFF8A99AD),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Button(
                                onClick = {
                                    if (newMenuName.isNotBlank() && newMenuPrice.isNotBlank()) {
                                        menuItems.add(MenuItemData(System.currentTimeMillis().toString(), newMenuName, if (newMenuPrice.startsWith("₹")) newMenuPrice else "₹$newMenuPrice", "Special"))
                                        newMenuName = ""
                                        newMenuPrice = ""
                                    }
                                },
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    }
                }

                items(menuItems, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF181D2A),
                        border = BorderStroke(1.dp, Color(0xFF252D42)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(item.category, color = Color(0xFF8A99AD), fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(item.price, color = LikeGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { menuItems.remove(item) }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Section 5: Events Management
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Event, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            Text("Venue Events & Parties (${venueEvents.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        OutlinedTextField(
                            value = newEventTitle,
                            onValueChange = { newEventTitle = it },
                            label = { Text("Event Title (e.g. Ladies Night)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF252D42),
                                focusedLabelColor = Color(0xFF00E5FF),
                                unfocusedLabelColor = Color(0xFF8A99AD),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = newEventDesc,
                            onValueChange = { newEventDesc = it },
                            label = { Text("Event Details & Offers") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF252D42),
                                focusedLabelColor = Color(0xFF00E5FF),
                                unfocusedLabelColor = Color(0xFF8A99AD),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                if (newEventTitle.isNotBlank()) {
                                    venueEvents.add(EventData(System.currentTimeMillis().toString(), newEventTitle, newEventTime, newEventDesc))
                                    newEventTitle = ""
                                    newEventDesc = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638), contentColor = Color(0xFF00E5FF))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Event to Venue Timeline", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                items(venueEvents, key = { it.id }) { ev ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181D2A)),
                        border = BorderStroke(1.dp, Color(0xFF252D42)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ev.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { venueEvents.remove(ev) }, modifier = Modifier.size(20.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(ev.dateTime, color = Color(0xFF00E5FF), fontSize = 11.sp)
                            if (ev.description.isNotBlank()) {
                                Text(ev.description, color = Color(0xFF8A99AD), fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Section 6: Timeline Feed & Announcements
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Article, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            Text("Timeline Stories & Announcements Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("Post live updates, stories, or announcements for your followers.", color = Color(0xFF8A99AD), fontSize = 12.sp)

                        OutlinedTextField(
                            value = newPostCaption,
                            onValueChange = { newPostCaption = it },
                            label = { Text("What's happening at your venue today?") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF252D42),
                                focusedLabelColor = Color(0xFF00E5FF),
                                unfocusedLabelColor = Color(0xFF8A99AD),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    timelinePhotoPicker.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (newPostImageUri != null) "Photo Attached ✓" else "Attach Photo", color = Color(0xFF00E5FF), fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (newPostCaption.isNotBlank()) {
                                        val postId = "post_" + System.currentTimeMillis()
                                        BusinessHubSyncManager.syncTimelinePost(
                                            postId = postId,
                                            venueId = business.id,
                                            venueName = venueName,
                                            caption = newPostCaption,
                                            imageUrl = newPostImageUri,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        newPostCaption = ""
                                        newPostImageUri = null
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                            ) {
                                Text("Publish to Feed", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                items(timelinePosts, key = { it["id"]?.toString() ?: "" }) { post ->
                    val postId = post["id"]?.toString() ?: ""
                    val postVenueId = post["venueId"]?.toString() ?: ""
                    val postVenueName = post["venueName"]?.toString() ?: "Venue"
                    val caption = post["caption"]?.toString() ?: ""
                    val imageUrl = post["imageUrl"]?.toString()

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181D2A)),
                        border = BorderStroke(1.dp, Color(0xFF252D42)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(postVenueName, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (postVenueId == business.id) {
                                    IconButton(
                                        onClick = { BusinessHubSyncManager.deleteTimelinePost(postId) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(caption, color = Color.White, fontSize = 13.sp)
                            if (!imageUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E2330))
                                ) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Timeline Story Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Changes Button
            Button(
                onClick = {
                    val updated = business.copy(
                        name = venueName,
                        tagline = tagline,
                        address = address,
                        city = city,
                        latitude = latitude.toDoubleOrNull() ?: business.latitude,
                        longitude = longitude.toDoubleOrNull() ?: business.longitude,
                        bannerUrl = venuePhotos.firstOrNull() ?: business.bannerUrl
                    )
                    onSaveVenueDetails(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_save_venue_manager"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LikeGreen, contentColor = Color.Black)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Venue Updates", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
