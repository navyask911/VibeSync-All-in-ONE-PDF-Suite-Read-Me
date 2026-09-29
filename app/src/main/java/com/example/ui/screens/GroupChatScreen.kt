package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.FriendshipRequestEntity
import com.example.data.model.ProfileEntity
import com.example.ui.theme.CoralPink
import com.example.ui.theme.LikeGreen
import com.example.ui.theme.PassRed
import com.example.ui.theme.RomanticViolet
import com.example.util.GroupMeshWebRTCManager
import com.example.util.GroupMessagingManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class GroupModel(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val adminName: String,
    val members: List<String>,
    var lastMessage: String,
    var lastMessageTime: Long,
    var unreadCount: Int = 0
)

data class GroupMessageModel(
    val id: String,
    val groupId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isMe: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    group: GroupModel,
    myUserName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var messageInput by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(
            listOf(
                GroupMessageModel("m1", group.id, "u1", "Aarav", "Hey everyone! Welcome to ${group.name} 👋", System.currentTimeMillis() - 120_000, false),
                GroupMessageModel("m2", group.id, "u2", "Ananya", "Great to connect! Shall we plan a group hangout this weekend?", System.currentTimeMillis() - 60_000, false),
                GroupMessageModel("m3", group.id, "me", myUserName.ifBlank { "You" }, "Sounds wonderful! Let's do it 🎉", System.currentTimeMillis() - 30_000, true)
            )
        )
    }

    var showGroupVoiceCall by remember { mutableStateOf(false) }
    var showGroupVideoCall by remember { mutableStateOf(false) }
    var showGroupInfoDialog by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showGroupInfoDialog = true }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RomanticViolet.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(group.emoji, fontSize = 22.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = group.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "${group.members.size} participants • Tap for info",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Group Voice Call Action
                    IconButton(
                        onClick = {
                            GroupMeshWebRTCManager.startOrJoinGroupMeshCall(context, group.id, "me", group.members)
                            showGroupVoiceCall = true
                        },
                        modifier = Modifier.testTag("btn_group_voice_call")
                    ) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = "Group Voice Call", tint = LikeGreen)
                    }

                    // Group Video Call Action
                    IconButton(
                        onClick = {
                            GroupMeshWebRTCManager.startOrJoinGroupMeshCall(context, group.id, "me", group.members)
                            showGroupVideoCall = true
                        },
                        modifier = Modifier.testTag("btn_group_video_call")
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "Group Video Call", tint = Color(0xFF00E5FF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Message group...", fontSize = 13.sp) },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_group_message"),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                val newMsg = GroupMessageModel(
                                    id = UUID.randomUUID().toString(),
                                    groupId = group.id,
                                    senderId = "me",
                                    senderName = myUserName.ifBlank { "You" },
                                    text = messageInput.trim(),
                                    timestamp = System.currentTimeMillis(),
                                    isMe = true
                                )
                                messages = messages + newMsg
                                group.lastMessage = messageInput.trim()
                                group.lastMessageTime = System.currentTimeMillis()
                                messageInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(RomanticViolet)
                            .testTag("btn_send_group_message")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.isMe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        ),
                        color = if (isMe) RomanticViolet else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            if (!isMe) {
                                Text(
                                    text = msg.senderName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralPink
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = msg.text,
                                fontSize = 13.5.sp,
                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                                fontSize = 9.5.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }

    // Group Voice Call Modal
    if (showGroupVoiceCall) {
        GroupVoiceCallDialog(
            group = group,
            onEndCall = { showGroupVoiceCall = false }
        )
    }

    // Group Video Call Modal
    if (showGroupVideoCall) {
        GroupVideoCallDialog(
            group = group,
            onEndCall = { showGroupVideoCall = false }
        )
    }

    // Group Info Dialog
    if (showGroupInfoDialog) {
        AlertDialog(
            onDismissRequest = { showGroupInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(group.emoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(group.name, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(group.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    Text("Participants (${group.members.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    group.members.forEach { member ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = RomanticViolet)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(member, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupInfoDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun GroupVoiceCallDialog(
    group: GroupModel,
    onEndCall: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF10121A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LikeGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "📞 E2EE GROUP VOICE CALL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LikeGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(group.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${group.members.size} connected participants", fontSize = 13.sp, color = Color(0xFF8E9BAE))
                }

                // Grid of Connected Participants
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val participants = group.members.take(6)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        participants.take(3).forEach { member ->
                            ParticipantVoiceAvatar(name = member, isSpeaking = member.length % 2 == 0)
                        }
                    }
                    if (participants.size > 3) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            participants.drop(3).forEach { member ->
                                ParticipantVoiceAvatar(name = member, isSpeaking = member.length % 2 != 0)
                            }
                        }
                    }
                }

                // Control Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) PassRed else Color(0xFF262C3D))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }

                    // End Call
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PassRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) LikeGreen else Color(0xFF262C3D))
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Speaker",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantVoiceAvatar(name: String, isSpeaking: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF202738))
                .border(
                    width = if (isSpeaking) 3.dp else 1.dp,
                    color = if (isSpeaking) LikeGreen else Color(0xFF35405A),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (isSpeaking) {
                Surface(
                    shape = CircleShape,
                    color = LikeGreen,
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                ) {}
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(name, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GroupVideoCallDialog(
    group: GroupModel,
    onEndCall: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(group.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text("📹 E2EE Group Video (${group.members.size} active)", color = Color(0xFF00E5FF), fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = { isFrontCamera = !isFrontCamera },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2433))
                    ) {
                        Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                    }
                }

                // 2x2 or 2x3 Grid of Live Video Tiles
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val members = group.members.take(4)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoTile(name = "You", isVideoOff = isVideoOff, modifier = Modifier.weight(1f))
                        if (members.isNotEmpty()) {
                            VideoTile(name = members[0], isVideoOff = false, modifier = Modifier.weight(1f))
                        }
                    }
                    if (members.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoTile(name = members[1], isVideoOff = false, modifier = Modifier.weight(1f))
                            if (members.size > 2) {
                                VideoTile(name = members[2], isVideoOff = false, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Video Control Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) PassRed else Color(0xFF1E2433))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { isVideoOff = !isVideoOff },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isVideoOff) PassRed else Color(0xFF1E2433))
                    ) {
                        Icon(
                            imageVector = if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "Video",
                            tint = Color.White
                        )
                    }

                    // End Call
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(PassRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoTile(name: String, isVideoOff: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161B26))
            .border(1.dp, Color(0xFF263044), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isVideoOff) {
            Surface(
                shape = CircleShape,
                color = RomanticViolet,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        } else {
            // Simulated live video stream background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF1F2B3E), Color(0xFF0F1522))
                        )
                    )
            )
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(60.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun CreateGroupDialog(
    availableMembers: List<String>,
    onDismiss: () -> Unit,
    onCreateGroup: (name: String, desc: String, emoji: String, members: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("👥") }
    var selectedMembers by remember { mutableStateOf(setOf<String>()) }

    val emojis = listOf("👥", "🎉", "☕", "🚀", "🍸", "⚽", "🎶", "🎬")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Weekend Explorers") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_group_name")
                )

                // Select Emoji
                Text("Select Group Icon:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis) { em ->
                        Surface(
                            shape = CircleShape,
                            color = if (emoji == em) RomanticViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (emoji == em) RomanticViolet else Color.Transparent),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { emoji = em }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(em, fontSize = 18.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description / Topic") },
                    placeholder = { Text("What is this group about?") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Add Participants (${selectedMembers.size}):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyColumn(modifier = Modifier.height(130.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(availableMembers) { member ->
                        val isSelected = member in selectedMembers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedMembers = if (isSelected) selectedMembers - member else selectedMembers + member
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    selectedMembers = if (isSelected) selectedMembers - member else selectedMembers + member
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(member, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreateGroup(name.trim(), desc.trim().ifBlank { "Group conversation" }, emoji, selectedMembers.toList())
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RomanticViolet),
                modifier = Modifier.testTag("btn_confirm_create_group")
            ) {
                Text("Create Group", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
