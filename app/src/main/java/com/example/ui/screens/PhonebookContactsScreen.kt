package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PhoneContact
import com.example.ui.components.DatingAvatar
import com.example.ui.theme.VibeSyncEmerald
import com.example.ui.theme.VibeSyncTeal
import com.example.util.PhonebookHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonebookContactsScreen(
    contacts: List<PhoneContact>,
    onSelectChatContact: (PhoneContact) -> Unit,
    onInviteContact: (PhoneContact) -> Unit,
    onRefreshContacts: () -> Unit,
    onBack: () -> Unit,
    onStartChatWithNumber: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Intercept phone back button to dismiss search or exit phonebook
    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            onBack()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (contacts.isEmpty()) {
            onRefreshContacts()
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val onVibeSyncContacts = remember(filteredContacts) {
        filteredContacts.filter { it.isOnVibeSync }
    }

    val inviteContacts = remember(filteredContacts) {
        filteredContacts.filter { !it.isOnVibeSync }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // VibeSync Contacts Top Bar
        TopAppBar(
            title = {
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name or number...", fontSize = 14.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_contacts")
                    )
                } else {
                    Column {
                        Text(
                            text = "Select Contact",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${contacts.size} contacts • ${contacts.count { it.isOnVibeSync }} on VibeSync",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier.testTag("btn_back_contacts")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                if (isSearchActive) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search contacts",
                            tint = Color.White
                        )
                    }
                }
                IconButton(
                    onClick = onRefreshContacts,
                    modifier = Modifier.testTag("btn_refresh_contacts")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Phonebook",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = VibeSyncTeal
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (searchQuery.isNotBlank()) {
                item {
                    Card(
                        onClick = { onStartChatWithNumber(searchQuery) },
                        colors = CardDefaults.cardColors(containerColor = VibeSyncTeal.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("card_start_chat_with_number")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = VibeSyncTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Start Anonymous Chat with '$searchQuery'",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Chat instantly. Unsaved numbers have Anonymous Shield active with Block & Report options.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = VibeSyncTeal)
                        }
                    }
                }
            }

            // Explanatory Policy Banner Card
            item {
                Surface(
                    color = VibeSyncTeal.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, VibeSyncTeal.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VibeSyncTeal.copy(alpha = 0.18f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = VibeSyncTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "VibeSync Direct Phonebook Connections",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = VibeSyncTeal,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = "Directly chat with real contacts from your address book who are registered on VibeSync. Easily invite friends via messaging apps or standard carrier SMS.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            )
                        }
                    }
                }
            }

            // Quick Action: Share VibeSync App Link
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            PhonebookHelper.sendInstantMessagingInvite(
                                context = context,
                                phoneNumber = "",
                                message = PhonebookHelper.DEFAULT_INVITE_MESSAGE
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .testTag("action_share_invite_link"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = VibeSyncEmerald,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share VibeSync Invite Link",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Invite friends via messaging apps or SMS",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }

            // SECTION 1: CONTACTS ON VIBESYNC
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONTACTS ON VIBESYNC (${onVibeSyncContacts.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = VibeSyncTeal,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = VibeSyncEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Chat Ready 💬",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VibeSyncTeal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (onVibeSyncContacts.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No contacts on VibeSync matching \"$searchQuery\"" else "No phonebook contacts registered yet on VibeSync",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(onVibeSyncContacts, key = { "vibesync_${it.id}_${it.phoneNumber}" }) { contact ->
                    ContactOnVibeSyncItem(
                        contact = contact,
                        onChatClick = { onSelectChatContact(contact) }
                    )
                }
            }

            // SECTION 2: INVITE TO VIBESYNC
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INVITE TO VIBESYNC (${inviteContacts.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Messaging App / Own SMS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            if (inviteContacts.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No invite candidates matching \"$searchQuery\"" else "All contacts are already on VibeSync!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(inviteContacts, key = { "invite_${it.id}_${it.phoneNumber}" }) { contact ->
                    ContactToInviteItem(
                        contact = contact,
                        onInviteClick = { onInviteContact(contact) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactOnVibeSyncItem(
    contact: PhoneContact,
    onChatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onChatClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("contact_item_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with VibeSync Badge
        Box(
            modifier = Modifier.size(50.dp),
            contentAlignment = Alignment.Center
        ) {
            val contactPhoto = contact.photoUrl.ifBlank { contact.vibeSyncUser?.avatarUrl ?: "" }
            if (contactPhoto.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = contactPhoto,
                    contentDescription = contact.name,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = VibeSyncTeal.copy(alpha = 0.15f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = contact.avatarEmoji.ifBlank { "👤" },
                            fontSize = 22.sp
                        )
                    }
                }
            }

            // Green Online / VibeSync Verified indicator
            Surface(
                shape = CircleShape,
                color = VibeSyncEmerald,
                border = BorderStroke(2.dp, Color.White),
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
            ) {}
        }

        Spacer(modifier = Modifier.width(14.dp))

        val displayName = com.example.util.ContactResolver.resolveParticipantDisplayName(null, contact.phoneNumber.ifBlank { contact.id }, contact.name)
        val cleanPhone = com.example.util.ContactResolver.sanitizePhone(contact.phoneNumber)
        val formattedPhone = if (cleanPhone.isNotBlank() && cleanPhone != "null") com.example.util.ContactResolver.formatPhoneNumberForDisplay(cleanPhone) else com.example.util.ContactResolver.formatPhoneNumberForDisplay(contact.id)
        val cleanTagline = com.example.util.ContactResolver.sanitizeName(contact.statusTagline).ifBlank { "Connection • Online" }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified on VibeSync",
                    tint = VibeSyncTeal,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = cleanTagline,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formattedPhone,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = VibeSyncTeal,
                    fontSize = 11.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chat Button
        Button(
            onClick = onChatClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VibeSyncTeal,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_chat_contact_${contact.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Chat",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ContactToInviteItem(
    contact: PhoneContact,
    onInviteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInviteClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
            .testTag("invite_contact_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = contact.avatarEmoji.ifBlank { "👤" },
                    fontSize = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${contact.phoneNumber} • Invite candidate",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedButton(
            onClick = onInviteClick,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, VibeSyncTeal),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_invite_contact_${contact.id}")
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = VibeSyncTeal, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "INVITE",
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = VibeSyncTeal
            )
        }
    }
}
