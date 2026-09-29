package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkUnreadChatAlt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.DatingAvatar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchEntity
import com.example.data.model.PhoneContact
import com.example.data.model.ProfileEntity
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.RomanticViolet
import com.example.ui.theme.VibeSyncBlueTick
import com.example.ui.theme.VibeSyncLightGreen
import com.example.ui.theme.VibeSyncTeal
import com.example.ui.theme.VibeSyncUnreadBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchesChatScreen(
    matches: List<MatchEntity>,
    phonebookContacts: List<PhoneContact> = emptyList(),
    onOpenChat: (MatchEntity, ProfileEntity) -> Unit,
    getProfileSync: suspend (String) -> ProfileEntity?,
    onOpenPhonebook: () -> Unit = {},
    onSelectChatContact: (PhoneContact) -> Unit = {},
    onInviteContact: (PhoneContact) -> Unit = {},
    onRefreshContacts: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    onOpenInteractionsModal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Cache profile models for matches
    val profileMap = remember { mutableStateMapOf<String, ProfileEntity>() }
    val syncedLocalPhotos = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    var searchQuery by remember { mutableStateOf("") }
    // Requested tab order: All, Unread, NEXT TO COME Contacts (Phonebook contacts & invitations here), Dating, Friends
    var selectedFilter by remember { mutableStateOf("All") }

    var groups by remember {
        mutableStateOf(
            listOf(
                GroupModel(
                    id = "grp_1",
                    name = "VibeSync Community ✨",
                    description = "Official community hub for connecting, socializing & dating",
                    emoji = "✨",
                    adminName = "VibeSync Admin",
                    members = listOf("You", "Aarav", "Priya", "Vikram", "Ananya"),
                    lastMessage = "Welcome everyone to VibeSync group hub!",
                    lastMessageTime = System.currentTimeMillis() - 3600_000,
                    unreadCount = 2
                ),
                GroupModel(
                    id = "grp_2",
                    name = "Weekend Trips & Treks 🏕️",
                    description = "Planning hikes, road trips and adventures together",
                    emoji = "🏕️",
                    adminName = "Vikram",
                    members = listOf("You", "Vikram", "Rohan", "Sneha", "Kavya"),
                    lastMessage = "Who's in for sunrise trek this Sunday?",
                    lastMessageTime = System.currentTimeMillis() - 7200_000,
                    unreadCount = 0
                ),
                GroupModel(
                    id = "grp_3",
                    name = "Foodies & Coffee Club ☕",
                    description = "Café hopping and gourmet food adventures",
                    emoji = "☕",
                    adminName = "Ananya",
                    members = listOf("You", "Ananya", "Priya", "Rahul"),
                    lastMessage = "Found an amazing rooftop café in town!",
                    lastMessageTime = System.currentTimeMillis() - 14400_000,
                    unreadCount = 1
                )
            )
        )
    }
    var activeGroup by remember { mutableStateOf<GroupModel?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    if (activeGroup != null) {
        GroupChatScreen(
            group = activeGroup!!,
            myUserName = "You",
            onBack = { activeGroup = null }
        )
        return
    }

    // Intercept phone back button to clear search query first if user is filtering
    BackHandler(enabled = searchQuery.isNotBlank()) {
        searchQuery = ""
    }

    val latestMessageMap = remember { mutableStateMapOf<String, com.example.data.model.ChatMessageEntity>() }

    LaunchedEffect(matches) {
        matches.forEach { match ->
            if (!profileMap.containsKey(match.profileId)) {
                val profile = getProfileSync(match.profileId)
                if (profile != null) {
                    profileMap[match.profileId] = profile
                }
            }
            if (!latestMessageMap.containsKey(match.matchId)) {
                try {
                    val db = com.example.data.database.DatingDatabase.getDatabase(context)
                    val msgs = db.chatMessageDao().getMessagesForMatchSync(match.matchId)
                    msgs.lastOrNull()?.let { latestMessageMap[match.matchId] = it }
                } catch (_: Throwable) {}
            }
        }
    }

    val filteredMatches = remember(matches, searchQuery, selectedFilter, profileMap) {
        val searchDigits = searchQuery.filter { it.isDigit() }
        val filtered = matches.filter { match ->
            val profile = profileMap[match.profileId]
            val matchesSearch = searchQuery.isBlank() ||
                    (profile?.name?.contains(searchQuery, ignoreCase = true) == true) ||
                    (profile?.phoneNumber?.contains(searchQuery, ignoreCase = true) == true) ||
                    (searchDigits.isNotBlank() && profile?.phoneNumber?.filter { it.isDigit() }?.contains(searchDigits) == true) ||
                    (searchDigits.isNotBlank() && profile?.id?.filter { it.isDigit() }?.contains(searchDigits) == true) ||
                    match.lastMessage.contains(searchQuery, ignoreCase = true)

            val isConnectTabPerson = match.lastMessage.isBlank() || match.lastMessage.startsWith("Connected with ")
            val matchesFilter = when (selectedFilter) {
                "All" -> !isConnectTabPerson
                "Unread" -> match.hasUnread && !isConnectTabPerson
                "Connect" -> match.isDatingMatch && !isConnectTabPerson
                "Friends" -> !match.isDatingMatch // Connect tab people are visible here!
                else -> true
            }

            matchesSearch && matchesFilter
        }

        // Additional STRICT deduplication block in UI using 16-character phone_hash as canonical key
        val map = mutableMapOf<String, MatchEntity>()
        for (m in filtered) {
            val profId = com.example.util.ContactResolver.sanitizePhone(m.profileId)
            val profile = profileMap[m.profileId]
            val partnerPhone = profile?.phoneNumber ?: profId
            val key = com.example.util.PhonebookHasher.generate16CharHash(partnerPhone)
            val existing = map[key]
            if (existing == null || m.lastMessageTime > existing.lastMessageTime) {
                map[key] = m
            }
        }
        map.values.sortedByDescending { it.lastMessageTime }
    }

    val filteredContacts = remember(phonebookContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            phonebookContacts
        } else {
            val searchDigits = searchQuery.filter { it.isDigit() }
            phonebookContacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                        (searchDigits.isNotBlank() && it.phoneNumber.filter { d -> d.isDigit() }.contains(searchDigits))
            }
        }
    }

    val contactsOnVibeSync = remember(filteredContacts) {
        filteredContacts.filter { it.isOnVibeSync }
    }

    val contactsToInvite = remember(filteredContacts) {
        filteredContacts.filter { !it.isOnVibeSync }
    }

    val unreadCount = remember(matches) {
        matches.count { it.hasUnread }
    }

    val onVibeSyncTotal = remember(phonebookContacts) {
        phonebookContacts.count { it.isOnVibeSync }
    }

    var showSyncDiagDialog by remember { mutableStateOf(false) }

    if (showSyncDiagDialog) {
        com.example.ui.components.SyncDiagnosticsDialog(
            onDismiss = { showSyncDiagDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
        ) {
            // 1. Top Brand Bar (VibeSync Logo matching Connect tab)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gradient VibeSync Brand Logo & Verified Genuine Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VibeSync",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                brush = Brush.linearGradient(listOf(CoralPink, RomanticViolet, Color(0xFF00F2FE))),
                                letterSpacing = (-0.5).sp
                            ),
                            modifier = Modifier.testTag("app_logo_chats")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LikeGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "100% GENUINE",
                                color = LikeGreen,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Top Action Icons: New Chat / Contacts, Interactions & Sync Diagnostics
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenPhonebook,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_open_contacts_top")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "New Chat / Contacts",
                                tint = VibeSyncTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }


                        IconButton(
                            onClick = { showSyncDiagDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_sync_diagnostics_top")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Diagnostics",
                                tint = VibeSyncTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenInteractionsModal,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_open_interactions_chat")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Requests & Interactions",
                                tint = VibeSyncTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = onRefreshContacts,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_refresh_contacts_top")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Contacts",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Search Box
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search chats, names or messages...",
                            fontSize = 13.5.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibeSyncTeal,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("input_search_matches")
                )
            }

            // 3. Main Chat Tabs: All, Unread, Groups, Connect, Friends
            item {
                data class ChatTabModel(
                    val name: String,
                    val count: Int,
                    val icon: ImageVector
                )

                val tabList = listOf(
                    ChatTabModel("All", matches.size, Icons.Default.Forum),
                    ChatTabModel("Unread", unreadCount, Icons.Default.MarkUnreadChatAlt),
                    ChatTabModel("Groups", groups.size, Icons.Default.GroupAdd),
                    ChatTabModel("Connect", matches.count { it.isDatingMatch }, Icons.Default.Favorite),
                    ChatTabModel("Friends", matches.count { !it.isDatingMatch }, Icons.Default.Group)
                )

                Surface(
                    color = Color(0xFF000000),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF262626)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(tabList) { tab ->
                            val isSelected = selectedFilter == tab.name
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = if (isSelected) Color(0xFF005C4B) else Color(0xFF141414),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00E676) else Color(0xFF2C2C2C)
                                ),
                                shadowElevation = if (isSelected) 4.dp else 0.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { selectedFilter = tab.name }
                                    .testTag("filter_chip_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFFE0E0E0),
                                        modifier = Modifier.size(16.dp)
                                    )

                                    Text(
                                        text = tab.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )

                                    if (tab.count > 0 && tab.name != "All") {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (tab.name == "Unread") Color(0xFF00E676) else if (isSelected) Color.White else Color(0xFF005C4B),
                                            modifier = Modifier.height(20.dp).widthIn(min = 20.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (tab.count > 99) "99+" else "${tab.count}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (tab.name == "Unread") Color.Black else if (isSelected) Color(0xFF005C4B) else Color.White
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

            if (selectedFilter == "Groups") {
                // Header: Create New Group
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { showCreateGroupDialog = true }
                            .testTag("card_create_new_group"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RomanticViolet.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, RomanticViolet.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RomanticViolet,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = "Create Group", tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Create New Group", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RomanticViolet)
                                Text("Add friends & contacts for group chat, voice & video calls", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = RomanticViolet)
                        }
                    }
                }

                // Groups list items
                items(groups, key = { it.id }) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { activeGroup = group }
                            .testTag("group_item_${group.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RomanticViolet.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(group.emoji, fontSize = 24.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(group.lastMessageTime)),
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = group.lastMessage,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (group.unreadCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF00E676),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${group.unreadCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${group.members.size} members • Voice & Video call ready 📞📹",
                                    fontSize = 10.sp,
                                    color = LikeGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                // Chats Filter Lists (All, Unread, Dating, Friends)
                if (selectedFilter == "Friends") {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)),
                            border = BorderStroke(1.dp, RomanticViolet.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RomanticViolet.copy(alpha = 0.25f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🔒", fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Anonymous Friends Mode Active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Exclusive connect tab accepted friends only. All phonebook contacts, mobile numbers, and personal details are hidden here for your privacy & safety.",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
                if (filteredMatches.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(text = "💬", fontSize = 42.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No chats match \"$searchQuery\"" else "No $selectedFilter Conversations Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Chat directly with contacts from your phonebook who are on VibeSync, or invite your friends via messaging app / SMS.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onOpenPhonebook,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VibeSyncTeal,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("btn_empty_open_contacts")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "New Chat / Contacts",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(filteredMatches, key = { it.matchId }) { match ->
                        val baseProfile = profileMap[match.profileId] ?: ProfileEntity(
                            id = match.profileId,
                            name = if (match.profileId.length >= 10 && match.profileId.all { it.isDigit() || it == '+' }) match.profileId else "VibeSync Match",
                            age = 24,
                            occupation = "Member",
                            city = "Bengaluru",
                            distanceMiles = 1,
                            bio = "VibeSync Connection",
                            interests = "Chat",
                            relationshipGoal = if (match.isDatingMatch) "Connect" else "Friends",
                            promptQuestion = "",
                            promptAnswer = "",
                            gradientColorStart = 0xFFFF5E62,
                            gradientColorEnd = 0xFFFF9966,
                            avatarEmoji = "✨",
                            phoneNumber = if (match.profileId.length >= 10 && match.profileId.all { it.isDigit() || it == '+' }) match.profileId else "",
                            isVerified = true
                        )

                        // Match contact name from ContactsContract against chat participant, overriding remote strings
                        val profile = com.example.util.ContactResolver.matchChatSessionParticipant(context, baseProfile)

                        VibeSyncConversationItem(
                            match = match,
                            profile = profile,
                            latestMessage = latestMessageMap[match.matchId],
                            onClick = { onOpenChat(match, profile) },
                            syncedLocalPhotos = syncedLocalPhotos
                        )
                    }
                }

                // Security & Privacy Footer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Personal messages are end-to-end encrypted with VibeSync E2EE Protocol",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }

        // Floating Action Button for New Chat / Contacts
        FloatingActionButton(
            onClick = {
                if (selectedFilter == "Groups") {
                    showCreateGroupDialog = true
                } else {
                    onOpenPhonebook()
                }
            },
            containerColor = VibeSyncTeal,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
                .size(56.dp)
                .testTag("fab_new_chat")
        ) {
            Icon(
                imageVector = if (selectedFilter == "Groups") Icons.Default.GroupAdd else Icons.Default.PersonAdd,
                contentDescription = "New Chat / Contacts",
                modifier = Modifier.size(24.dp)
            )
        }

        if (showCreateGroupDialog) {
            val availableMembers = remember(phonebookContacts, matches) {
                val names = mutableListOf("Aarav", "Priya", "Vikram", "Ananya", "Rohan", "Sneha")
                names.addAll(phonebookContacts.map { it.name })
                names.distinct()
            }
            CreateGroupDialog(
                availableMembers = availableMembers,
                onDismiss = { showCreateGroupDialog = false },
                onCreateGroup = { name, desc, emoji, members ->
                    val newGroup = GroupModel(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        description = desc,
                        emoji = emoji,
                        adminName = "You",
                        members = listOf("You") + members,
                        lastMessage = "Group created",
                        lastMessageTime = System.currentTimeMillis(),
                        unreadCount = 0
                    )
                    groups = listOf(newGroup) + groups
                    showCreateGroupDialog = false
                    activeGroup = newGroup
                }
            )
        }
    }
}

@Composable
private fun PhonebookContactsInlineHeader(
    totalContacts: Int,
    onVibeSyncCount: Int,
    toInviteCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VibeSyncTeal.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, VibeSyncTeal.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Phonebook Contacts & Invitations",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = VibeSyncTeal
                    )
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = VibeSyncTeal
                ) {
                    Text(
                        text = "$totalContacts TOTAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 9.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chat exclusively with verified contacts on VibeSync. Send invites with personalized messages via messaging apps or carrier SMS (at your own plan rates).",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LikeGreen.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✅", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$onVibeSyncCount on VibeSync",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = LikeGreen,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✉️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$toInviteCount Invitable",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactOnVibeSyncRow(
    contact: PhoneContact,
    onClick: () -> Unit,
    syncedLocalPhotos: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val contactDisplayName = remember(contact.id, contact.phoneNumber, contact.name) {
        com.example.util.ContactResolver.resolveParticipantDisplayName(context, contact.phoneNumber.ifBlank { contact.id }, contact.name)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("vibesync_contact_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clickable {
                    val phone = contact.phoneNumber.ifBlank { contact.id }
                    val photoUri = com.example.util.ContactResolver.fetchContactPhotoByPhoneNumber(context, phone)
                    if (!photoUri.isNullOrBlank()) {
                        syncedLocalPhotos[phone] = photoUri
                        android.widget.Toast.makeText(context, "🔄 Profile picture successfully synced from your local device contacts!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "ℹ️ No photo set for this contact in your device's address book.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val contactPhoto = syncedLocalPhotos[contact.phoneNumber.ifBlank { contact.id }] ?: contact.photoUrl.ifBlank { contact.vibeSyncUser?.avatarUrl ?: "" }
            if (contactPhoto.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = contactPhoto,
                    contentDescription = contactDisplayName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(CoralPink, RomanticViolet)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = contact.avatarEmoji.ifBlank { "👤" }, fontSize = 20.sp)
                }
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(VibeSyncUnreadBadge)
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contactDisplayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified User",
                    tint = VibeSyncTeal,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = contact.statusTagline.ifBlank { contact.phoneNumber },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VibeSyncTeal,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Chat", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ContactToInviteRow(
    contact: PhoneContact,
    onInvite: () -> Unit,
    syncedLocalPhotos: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onInvite)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag("invite_contact_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable {
                    val phone = contact.phoneNumber.ifBlank { contact.id }
                    val photoUri = com.example.util.ContactResolver.fetchContactPhotoByPhoneNumber(context, phone)
                    if (!photoUri.isNullOrBlank()) {
                        syncedLocalPhotos[phone] = photoUri
                        android.widget.Toast.makeText(context, "🔄 Profile picture successfully synced from your local device contacts!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "ℹ️ No photo set for this contact in your device's address book.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val contactPhoto = syncedLocalPhotos[contact.phoneNumber.ifBlank { contact.id }] ?: ""
            if (contactPhoto.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = contactPhoto,
                    contentDescription = contact.name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(text = contact.avatarEmoji.ifBlank { "👤" }, fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${contact.phoneNumber} • Invite candidate",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onInvite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, VibeSyncTeal),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VibeSyncTeal),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, tint = VibeSyncTeal, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Invite", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyContactsState(
    searchQuery: String,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "👥", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isNotBlank()) "No contacts found for \"$searchQuery\"" else "No phonebook contacts available",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ensure contacts permission is granted or refresh the address book.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onRefresh,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Refresh Contacts")
        }
    }
}

@Composable
private fun VibeSyncConversationItem(
    match: MatchEntity,
    profile: ProfileEntity,
    latestMessage: com.example.data.model.ChatMessageEntity?,
    onClick: () -> Unit,
    syncedLocalPhotos: androidx.compose.runtime.snapshots.SnapshotStateMap<String, String>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val timeFormatted = remember(match.lastMessageTime) {
        val diff = System.currentTimeMillis() - match.lastMessageTime
        val minutes = diff / (60 * 1000)
        val hours = minutes / 60
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(match.lastMessageTime))
            else -> "Yesterday"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp)
            .testTag("chat_item_${match.matchId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with real photo or stylized emoji
        DatingAvatar(
            name = profile.name,
            emoji = profile.avatarEmoji,
            colorStart = profile.gradientColorStart,
            colorEnd = profile.gradientColorEnd,
            size = 48.dp,
            isVerified = profile.isVerified,
            isOnline = profile.activityStatus.contains("Online", ignoreCase = true) || profile.activityStatus.contains("Active", ignoreCase = true),
            avatarUrl = syncedLocalPhotos[profile.phoneNumber.ifBlank { profile.id }] ?: profile.avatarUrl,
            modifier = Modifier.clickable {
                val phone = profile.phoneNumber.ifBlank { profile.id }
                val photoUri = com.example.util.ContactResolver.fetchContactPhotoByPhoneNumber(context, phone)
                if (!photoUri.isNullOrBlank()) {
                    syncedLocalPhotos[phone] = photoUri
                    android.widget.Toast.makeText(context, "🔄 Profile picture successfully synced from your local device contacts!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "ℹ️ No photo set for this contact in your device's address book.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.width(13.dp))

        // Name, snippet and timestamp
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (match.hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Human",
                            tint = VibeSyncTeal,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (match.hasUnread) VibeSyncTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (match.hasUnread) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Relationship Badge / Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (match.status == "BROKEN_UP" || match.relationshipStatus == "MUTUAL_BROKEN_UP") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE53935).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "💔 Mutual Breakup (+1 Recorded)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else if (match.relationshipStatus == "COMPLICATED" || match.relationshipStatus == "NOT_MUTUAL_BREAKUP") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFF9800).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "⚠️ Complicated (Not Mutual Breakup)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else if (match.relationshipStatus == "IN_RELATIONSHIP") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = VibeSyncTeal.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "💖 Official Date Mates (Mutual)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibeSyncTeal,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "🤝 Social & Friend",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "• 👥 ${profile.friendsCount} friends",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Last message snippet with dynamic delivery ticks
            val isOutgoing = latestMessage?.isSentByMe == true || match.lastMessage.startsWith("You: ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOutgoing && latestMessage != null) {
                    val (tickIcon, tickTint) = when {
                        latestMessage.isRead -> Icons.Default.DoneAll to VibeSyncBlueTick
                        latestMessage.isDelivered -> Icons.Default.DoneAll to Color(0xFF64748B)
                        else -> Icons.Default.Check to Color(0xFF64748B)
                    }
                    Icon(
                        imageVector = tickIcon,
                        contentDescription = if (latestMessage.isRead) "Read" else if (latestMessage.isDelivered) "Delivered" else "Sent",
                        tint = tickTint,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = match.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (match.hasUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (match.hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        // Unread Count Green Badge
        if (match.hasUnread) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(VibeSyncUnreadBadge),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "1",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
