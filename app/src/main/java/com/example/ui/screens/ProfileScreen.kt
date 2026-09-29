package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FriendshipRequestEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.UserPreferencesEntity
import com.example.ui.components.AdvertiseWithUsDialog
import com.example.ui.components.AppSoundsNotificationCard
import com.example.ui.components.InviteToVibeSyncDialog
import com.example.ui.screens.profile.*
import com.example.ui.theme.CoralPink
import com.example.ui.theme.PassRed
import com.example.ui.theme.RomanticViolet
import com.example.ui.theme.VibeSyncTeal
import com.example.util.AppUpdateHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    preferences: UserPreferencesEntity?,
    onUpdatePreferences: (UserPreferencesEntity) -> Unit,
    onResetData: () -> Unit,
    onDeleteAccount: () -> Unit = {},
    onOpenAdminPortal: () -> Unit = {},
    onLogout: () -> Unit = {},
    superLikesCount: Int = 5,
    isSpotlightActive: Boolean = false,
    onSyncCloudBackup: () -> Unit = {},
    onRestoreCloudBackup: () -> Unit = {},
    onUpdateSchedule: (frequency: String, wifiOnly: Boolean) -> Unit = { _, _ -> },
    isBackupInProgress: Boolean = false,
    isRestoreInProgress: Boolean = false,
    onToggleProfileLock: (Boolean) -> Unit = {},
    onOpenMpinSetup: () -> Unit = {},
    onSetMpin: (String) -> Unit = {},
    onTriggerFaceVerification: () -> Unit = {},
    onOpenFriendsList: () -> Unit = {},
    onOpenChat: (String) -> Unit = {},
    acceptedFriends: List<FriendshipRequestEntity> = emptyList(),
    mutualFriendsMap: Map<String, List<ProfileEntity>> = emptyMap(),
    allProfiles: List<ProfileEntity> = emptyList(),
    onUnbanProfile: (String) -> Unit = {},
    onToggleHideFriends: (Boolean) -> Unit = {},
    onRefreshLiveLocation: () -> Unit = {},
    onUpdatePhoneNumber: (String) -> Unit = {},
    onSyncFrontLoginToBackend: () -> Unit = {},
    onRefreshAllSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val prefs = preferences ?: UserPreferencesEntity()
    val context = LocalContext.current

    // Dialog & Navigation States
    var showEditDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showAdvertiseDialog by remember { mutableStateOf(false) }
    var showUpdatePhoneDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var fullPhotoViewerUrl by remember { mutableStateOf<String?>(null) }
    var fullPhotoViewerTitle by remember { mutableStateOf("Profile Photo") }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // Collapsible Accordion Sections State
    var expandedSection by remember { mutableStateOf<String?>(null) }

    fun toggleSection(sectionKey: String) {
        expandedSection = if (expandedSection == sectionKey) null else sectionKey
    }

    // Intercept phone back button to dismiss subpages or dialogs
    BackHandler(enabled = fullPhotoViewerUrl != null || showEditDialog || showWallpaperDialog || showUpdatePhoneDialog || showInviteDialog || showQrCodeDialog || showHelpDialog || showDeleteAccountConfirmDialog || showLogoutConfirmDialog) {
        if (fullPhotoViewerUrl != null) {
            fullPhotoViewerUrl = null
        } else if (showEditDialog) {
            showEditDialog = false
        } else if (showWallpaperDialog) {
            showWallpaperDialog = false
        } else if (showUpdatePhoneDialog) {
            showUpdatePhoneDialog = false
        } else if (showInviteDialog) {
            showInviteDialog = false
        } else if (showQrCodeDialog) {
            showQrCodeDialog = false
        } else if (showHelpDialog) {
            showHelpDialog = false
        } else if (showDeleteAccountConfirmDialog) {
            showDeleteAccountConfirmDialog = false
        } else if (showLogoutConfirmDialog) {
            showLogoutConfirmDialog = false
        }
    }

    val displayName = if (prefs.userName.isNotBlank() && prefs.userName != "Registered Member" && prefs.userName != "VibeSync User") prefs.userName else "VibeSync Member"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_profile")
    ) {
        // CLEAN OFFICIAL TOP BAR
        TopAppBar(
            title = {
                Text(
                    text = "Profile & Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            actions = {
                IconButton(onClick = { showQrCodeDialog = true }, modifier = Modifier.testTag("btn_qr_code")) {
                    Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { showEditDialog = true }, modifier = Modifier.testTag("btn_edit_profile_top")) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = VibeSyncTeal)
                }
                IconButton(onClick = onRefreshAllSettings, modifier = Modifier.testTag("btn_refresh_settings")) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", tint = VibeSyncTeal)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

        // MAIN SCROLLABLE SETTINGS CONTENT (ACCORDION STYLE)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==========================================
            // 1. TOP PROFILE HEADER (ONLY BOTTOM 3-PHOTO SLIDABLE PICKER)
            // ==========================================
            ProfileHeaderSection(
                preferences = prefs,
                onUpdatePreferences = onUpdatePreferences,
                onOpenEditProfile = { showEditDialog = true },
                onOpenWallpaperDialog = { showWallpaperDialog = true },
                onViewFullPhoto = { url, title ->
                    fullPhotoViewerUrl = url
                    fullPhotoViewerTitle = title
                },
                onRefreshLiveLocation = onRefreshLiveLocation,
                onLogout = { showLogoutConfirmDialog = true }
            )

            // ==========================================
            // 2. COLLAPSIBLE ACCORDION SECTIONS
            // ==========================================

            // Section 1: Privacy & Identity Shield
            ProfileSectionDrawer(
                title = "Privacy & Discovery",
                icon = Icons.Default.Lock,
                iconTint = Color(0xFF1565C0),
                iconBackground = Color(0xFF1565C0).copy(alpha = 0.12f),
                statusBadgeText = if (prefs.isFaceVerified) "Verified 🛡️" else "Protection Active",
                statusBadgeColor = Color(0xFF1565C0),
                isExpanded = expandedSection == "PRIVACY",
                onToggleExpand = { toggleSection("PRIVACY") },
                testTag = "drawer_privacy"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Open for Connect Toggle
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Open for Connect other People inside Vibesync 🤝",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Make your profile discoverable to other verified members in Connect",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.isOpenForDating,
                                onCheckedChange = { checked ->
                                    onUpdatePreferences(prefs.copy(isOpenForDating = checked))
                                },
                                modifier = Modifier.testTag("switch_open_for_connect")
                            )
                        }
                    }

                    // Profile Lock Privacy Shield
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Profile Lock Privacy Shield 🔒",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Restrict personal occupation and details to matched connections only",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.isProfileLocked,
                                onCheckedChange = { locked ->
                                    onToggleProfileLock(locked)
                                },
                                modifier = Modifier.testTag("switch_profile_lock")
                            )
                        }
                    }

                    // Genuine Biometric Face Verification
                    GenuineVerificationCard(
                        preferences = prefs,
                        onTriggerFaceVerification = onTriggerFaceVerification,
                        onOpenUpdatePhoneDialog = { showUpdatePhoneDialog = true },
                        onSyncFrontLoginToBackend = onSyncFrontLoginToBackend,
                        isExpanded = true,
                        onToggleExpand = {}
                    )

                    // Discovery Preferences
                    DiscoveryPreferencesCard(
                        preferences = prefs,
                        onUpdatePreferences = onUpdatePreferences,
                        isExpanded = true,
                        onToggleExpand = {}
                    )
                }
            }

            // Section 3: Chats & Cloud Backup
            ProfileSectionDrawer(
                title = "Chats & Cloud Backup",
                icon = Icons.Default.Chat,
                iconTint = VibeSyncTeal,
                iconBackground = VibeSyncTeal.copy(alpha = 0.12f),
                statusBadgeText = if (prefs.isGoogleCloudBackupEnabled) "Encrypted Backup ON" else "Local Only",
                statusBadgeColor = VibeSyncTeal,
                isExpanded = expandedSection == "CHATS",
                onToggleExpand = { toggleSection("CHATS") },
                testTag = "drawer_chats"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CloudBackupCard(
                        preferences = prefs,
                        onUpdatePreferences = onUpdatePreferences,
                        onSyncCloudBackup = onSyncCloudBackup,
                        onRestoreCloudBackup = onRestoreCloudBackup,
                        onUpdateSchedule = onUpdateSchedule,
                        isBackupInProgress = isBackupInProgress,
                        isRestoreInProgress = isRestoreInProgress,
                        isExpanded = true,
                        onToggleExpand = {}
                    )

                    OutlinedButton(
                        onClick = { showWallpaperDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat Wallpaper & Bubble Theme", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Section 4: Appearance & Theme
            ProfileSectionDrawer(
                title = "Appearance & Theme",
                icon = Icons.Default.Palette,
                iconTint = RomanticViolet,
                iconBackground = RomanticViolet.copy(alpha = 0.12f),
                statusBadgeText = "Custom Styling",
                statusBadgeColor = RomanticViolet,
                isExpanded = expandedSection == "APPEARANCE",
                onToggleExpand = { toggleSection("APPEARANCE") },
                testTag = "drawer_appearance"
            ) {
                AppThemeCard(
                    preferences = prefs,
                    onUpdatePreferences = onUpdatePreferences,
                    isExpanded = true,
                    onToggleExpand = {}
                )
            }

            // Section 5: Notifications & Sound Alerts
            ProfileSectionDrawer(
                title = "Notifications & Sounds",
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFFF57C00),
                iconBackground = Color(0xFFF57C00).copy(alpha = 0.12f),
                statusBadgeText = "Sound & Vibration",
                statusBadgeColor = Color(0xFFF57C00),
                isExpanded = expandedSection == "NOTIFICATIONS",
                onToggleExpand = { toggleSection("NOTIFICATIONS") },
                testTag = "drawer_notifications"
            ) {
                AppSoundsNotificationCard(
                    preferences = prefs,
                    onUpdatePreferences = onUpdatePreferences,
                    isExpanded = true,
                    onToggleExpand = {}
                )
            }

            // Section 6: Storage & Network Data
            ProfileSectionDrawer(
                title = "Storage & Data",
                icon = Icons.Default.Storage,
                iconTint = Color(0xFF00796B),
                iconBackground = Color(0xFF00796B).copy(alpha = 0.12f),
                statusBadgeText = "Cache Cleaner",
                statusBadgeColor = Color(0xFF00796B),
                isExpanded = expandedSection == "STORAGE",
                onToggleExpand = { toggleSection("STORAGE") },
                testTag = "drawer_storage"
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Local Device Storage", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Encrypted Local Room DB", fontSize = 13.sp)
                            Text("Active & Synced", fontWeight = FontWeight.Bold, color = VibeSyncTeal)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Zero-Leak E2EE Memory Footprint", fontSize = 13.sp)
                            Text("Minimal", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Button(
                            onClick = onResetData,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_clear_cache_profile")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Media Cache & Reset Local DB")
                        }
                    }
                }
            }

            // Section 7: Share APK & Invite Friends (REQUEST 5)
            ProfileSectionDrawer(
                title = "Share VibeSync APK",
                icon = Icons.Default.Share,
                iconTint = Color(0xFF2E7D32),
                iconBackground = Color(0xFF2E7D32).copy(alpha = 0.12f),
                statusBadgeText = "Instant Bluetooth / Share",
                statusBadgeColor = Color(0xFF2E7D32),
                isExpanded = expandedSection == "SHARE",
                onToggleExpand = { toggleSection("SHARE") },
                testTag = "drawer_share_apk"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Share the official VibeSync APK installation file directly with nearby friends and family via Quick Share, Bluetooth, or messaging apps.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    // SHARE APK BUTTON
                    Surface(
                        onClick = {
                            AppUpdateHelper.shareInstalledApk(context)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.10f),
                        border = BorderStroke(1.2.dp, Color(0xFF2E7D32)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_share_apk_profile")
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
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Share APK with Friends & Family",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }

                    // Option to invite via SMS or contacts
                    OutlinedButton(
                        onClick = { showInviteDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_invite_contacts_profile")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Invite Contacts via SMS & Link", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Section 8: Safety, Legal Compliance & About
            ProfileSectionDrawer(
                title = "Legal Compliance & Safety",
                icon = Icons.Default.Gavel,
                iconTint = Color(0xFF455A64),
                iconBackground = Color(0xFF455A64).copy(alpha = 0.12f),
                statusBadgeText = "Indian IT Rules 2021",
                statusBadgeColor = Color(0xFF455A64),
                isExpanded = expandedSection == "LEGAL",
                onToggleExpand = { toggleSection("LEGAL") },
                testTag = "drawer_legal_compliance"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("1. Cryptographic E2EE Mandate", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VibeSyncTeal)
                            Text("All chats and media files are encrypted client-side on your device prior to network routing using symmetric AES-256-GCM. VibeSync physical servers do not hold decryption keys.", fontSize = 11.5.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("2. Indian IT Rules 2021 & Rule 4(2)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VibeSyncTeal)
                            Text("In compliance with Indian IT Rules, VibeSync implements an anonymous First Originator Traceability Signature model, generating irreversible hashes of message payloads without reading chat content.", fontSize = 11.5.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("3. Section 91 CrPC / Section 94 BNSS Compliance", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VibeSyncTeal)
                            Text("When presented with authorized lawful orders, VibeSync provides lawful non-E2EE metadata assistance to law enforcement agencies without compromising personal social graphs.", fontSize = 11.5.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Section 9: Account & System Settings (Placed below Legal Compliance & Safety)
            AccountManagementCard(
                onDeleteAccount = { showDeleteAccountConfirmDialog = true },
                onResetData = onResetData,
                onLogout = { showLogoutConfirmDialog = true },
                isExpanded = expandedSection == "ACCOUNT",
                onToggleExpand = { toggleSection("ACCOUNT") }
            )

            // Developer Admin Portal Launch Button
            Surface(
                onClick = onOpenAdminPortal,
                shape = RoundedCornerShape(14.dp),
                color = VibeSyncTeal.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, VibeSyncTeal.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_open_admin_portal_profile")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = VibeSyncTeal, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("⚡ Admin & Operations Portal", fontWeight = FontWeight.Bold, color = VibeSyncTeal, fontSize = 13.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // MODAL DIALOGS

    // 1. Delete Account Dialog
    if (showDeleteAccountConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirmDialog = false },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = PassRed, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Delete VibeSync Account?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Deleting your account will permanently wipe your profile, matches, chat messages, and cloud session.", fontSize = 13.sp)
                    Text("You will be logged out and returned to the initial login screen immediately.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountConfirmDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PassRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Permanently Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Log out of VibeSync?", fontWeight = FontWeight.Bold) },
            text = { Text("You can log back in anytime with your registered phone number.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibeSyncTeal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. QR Code Dialog
    if (showQrCodeDialog) {
        val userQrPayload = "https://vibesync.app/user/${prefs.id}?phone=${prefs.verifiedMobileNumber}"
        val userQrBitmap = remember(userQrPayload) {
            com.example.util.QrCodeGeneratorHelper.generateQrBitmap(userQrPayload, 600, android.graphics.Color.BLACK, android.graphics.Color.WHITE)
        }
        AlertDialog(
            onDismissRequest = { showQrCodeDialog = false },
            title = { Text("My VibeSync Profile QR", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userQrBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = userQrBitmap.asImageBitmap(),
                                contentDescription = "User Profile QR",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = VibeSyncTeal, modifier = Modifier.size(140.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(prefs.verifiedMobileNumber.ifBlank { "VibeSync Verified ID" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Scan with any smartphone camera to connect instantly.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { showQrCodeDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = VibeSyncTeal)) {
                    Text("Close")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    userQrBitmap?.let { bmp ->
                        com.example.util.QrCodeGeneratorHelper.shareBitmap(context, bmp, "$displayName's VibeSync Profile QR")
                    }
                }) {
                    Text("Share QR")
                }
            }
        )
    }

    // Full Screen Photo Viewer Dialog
    fullPhotoViewerUrl?.let { photoUrl ->
        FullScreenPhotoViewerDialog(
            photoUrl = photoUrl,
            title = fullPhotoViewerTitle,
            onDismiss = { fullPhotoViewerUrl = null }
        )
    }

    // Update Phone Number with OTP Dialog
    if (showUpdatePhoneDialog) {
        UpdatePhoneNumberDialog(
            preferences = prefs,
            onDismiss = { showUpdatePhoneDialog = false },
            onPhoneVerified = { verifiedPhone ->
                onUpdatePhoneNumber(verifiedPhone)
                showUpdatePhoneDialog = false
            }
        )
    }

    // Edit Profile Modal Dialog
    if (showEditDialog) {
        EditProfileDialog(
            preferences = prefs,
            onUpdatePreferences = onUpdatePreferences,
            onDismiss = { showEditDialog = false }
        )
    }

    // Cover Wallpaper Dialog
    if (showWallpaperDialog) {
        ProfileWallpaperDialog(
            preferences = prefs,
            onUpdatePreferences = onUpdatePreferences,
            onDismiss = { showWallpaperDialog = false }
        )
    }

    // Advertise With Us Dialog
    if (showAdvertiseDialog) {
        AdvertiseWithUsDialog(
            onDismissRequest = { showAdvertiseDialog = false }
        )
    }

    // Invite Dialog
    if (showInviteDialog) {
        val dummyContact = remember { com.example.data.model.PhoneContact(id = "invite", name = "Friends & Contacts", phoneNumber = "") }
        InviteToVibeSyncDialog(
            contact = dummyContact,
            onInviteMessaging = { msg ->
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, msg)
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Invitation"))
                showInviteDialog = false
            },
            onInviteSms = { msg ->
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, msg)
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "Send via SMS"))
                showInviteDialog = false
            },
            onDismiss = { showInviteDialog = false }
        )
    }
}
