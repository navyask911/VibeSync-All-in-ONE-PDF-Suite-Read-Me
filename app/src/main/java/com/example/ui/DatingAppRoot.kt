package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.ui.theme.MyApplicationTheme
import com.example.util.BiometricPromptHelper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.MatchEntity
import com.example.ui.components.AccountRecoveryDialog
import com.example.ui.components.ActiveCallOverlay
import com.example.ui.components.DuplicateProfileDetectedDialog
import com.example.ui.components.FriendsListDialog
import com.example.ui.components.FriendshipRequestDialog
import com.example.ui.components.GoogleCloudBackupSetupDialog
import com.example.ui.components.InviteToVibeSyncDialog
import com.example.ui.components.MandatoryPermissionsDialog
import com.example.ui.components.MatchCelebrationDialog
import com.example.ui.components.MpinSetupDialog
import com.example.ui.components.NetworkConnectivityOverlay
import com.example.ui.components.NetworkSyncStatusBanner
import com.example.ui.components.OutgoingFriendshipDialog
import com.example.ui.components.OutgoingLikeDialog
import com.example.ui.components.RestoreBackupPromptDialog
import com.example.ui.components.InteractionsAndRequestsModal
import com.example.ui.components.VibeSyncInAppNotificationBanner
import com.example.ui.screens.AccountReportScreen
import com.example.ui.screens.AdminBackendScreen
import com.example.ui.screens.AuthLoginScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.FaceVerificationScreen
import com.example.ui.screens.LikedYouScreen
import com.example.ui.screens.MandatoryMobileVerificationScreen
import com.example.ui.screens.MatchesChatScreen
import com.example.ui.screens.MpinScreen
import com.example.ui.screens.PhonebookContactsScreen
import com.example.ui.screens.ProfileDetailDialog
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RegistrationProfileScreen
import com.example.ui.screens.StatusStoriesScreen
import com.example.ui.screens.VibeSyncSplashScreen
import com.example.ui.theme.VibeSyncLightGreen
import com.example.ui.theme.VibeSyncTeal
import com.example.ui.theme.VibeSyncUnreadBadge
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.Explore

enum class DatingTab(val title: String) {
    CHATS("Chats"),
    STATUS("Stories"),
    FRIENDS("Friends"),
    FIND("Find"),
    PROFILE("Profile")
}

@Composable
fun DatingAppRoot(
    viewModel: DatingViewModel = viewModel()
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val candidates by viewModel.candidateProfiles.collectAsStateWithLifecycle()
    val likedMeProfiles by viewModel.likedMeProfiles.collectAsStateWithLifecycle()
    val matches by viewModel.rawMatches.collectAsStateWithLifecycle()
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val isSessionChecking by viewModel.isSessionChecking.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    val activeMatchDialog by viewModel.activeMatchDialog.collectAsStateWithLifecycle()
    val activeChat by viewModel.activeChat.collectAsStateWithLifecycle()
    val selectedDetail by viewModel.selectedProfileDetail.collectAsStateWithLifecycle()
    val activeReportProfile by viewModel.activeReportProfile.collectAsStateWithLifecycle()
    val networkState by viewModel.networkState.collectAsStateWithLifecycle()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    viewModel.disconnectRealtime()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    viewModel.reconnectRealtime()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val chatMessages by viewModel.currentChatMessages.collectAsStateWithLifecycle()

    val showAdminPortal by viewModel.showAdminPortal.collectAsStateWithLifecycle()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsStateWithLifecycle()
    val allAdminProfiles by viewModel.allProfilesForAdmin.collectAsStateWithLifecycle()
    val supabaseDiagnostic by viewModel.supabaseDiagnostic.collectAsStateWithLifecycle()
    val simulatedOtp by viewModel.simulatedMobileOtp.collectAsStateWithLifecycle()
    val showFaceVerification by viewModel.showFaceVerification.collectAsStateWithLifecycle()

    val adminRoles by viewModel.adminRoles.collectAsStateWithLifecycle()
    val adminEmployees by viewModel.adminEmployees.collectAsStateWithLifecycle()
    val isMpinUnlocked by viewModel.isMpinUnlocked.collectAsStateWithLifecycle()

    val superLikesCount by viewModel.superLikesCount.collectAsStateWithLifecycle()
    val isSpotlightActive by viewModel.isSpotlightActive.collectAsStateWithLifecycle()

    val activeDatingProfile by viewModel.activeDatingMatchProfile.collectAsStateWithLifecycle()
    val demographicAnalytics by viewModel.demographicAnalytics.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()

    // Clean Phonebook Contact Gated Chat & Invites States
    val phonebookContacts by viewModel.phonebookContacts.collectAsStateWithLifecycle()
    val isPhonebookSyncing by viewModel.isPhonebookSyncing.collectAsStateWithLifecycle()
    val showPhonebookScreen by viewModel.showPhonebookScreen.collectAsStateWithLifecycle()
    val selectedContactToInvite by viewModel.selectedContactToInvite.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Zero Profile Duplication & Recovery States
    val duplicateProfileDetected by viewModel.duplicateProfileDetected.collectAsStateWithLifecycle()
    val showAccountRecoveryDialog by viewModel.showAccountRecoveryDialog.collectAsStateWithLifecycle()
    val showMandatoryPermissionsDialog by viewModel.showMandatoryPermissionsDialog.collectAsStateWithLifecycle()
    val showPostRegistrationBackupDialog by viewModel.showPostRegistrationBackupDialog.collectAsStateWithLifecycle()
    val detectedDriveBackup by viewModel.detectedDriveBackup.collectAsStateWithLifecycle()
    val showRestorePromptDialog by viewModel.showRestorePromptDialog.collectAsStateWithLifecycle()
    val isBackupInProgress by viewModel.isBackupInProgress.collectAsStateWithLifecycle()
    val isRestoreInProgress by viewModel.isRestoreInProgress.collectAsStateWithLifecycle()
    val showMpinSetupPrompt by viewModel.showMpinSetupPrompt.collectAsStateWithLifecycle()

    // Friendship System States (Swipe Down in Connect Tab, Request Review, Mutual Friends)
    val acceptedFriends by viewModel.acceptedFriends.collectAsStateWithLifecycle()
    val pendingFriendRequests by viewModel.pendingFriendRequests.collectAsStateWithLifecycle()
    val pendingOutgoingRequests by viewModel.pendingOutgoingRequests.collectAsStateWithLifecycle()
    val mutualFriendsMap by viewModel.mutualFriendsMap.collectAsStateWithLifecycle()
    val pendingRequestToReview by viewModel.pendingRequestToReview.collectAsStateWithLifecycle()
    val outgoingFriendRequestNotification by viewModel.outgoingFriendRequestNotification.collectAsStateWithLifecycle()
    val outgoingLikeNotification by viewModel.outgoingLikeNotification.collectAsStateWithLifecycle()
    val sharedBreakupCounts by viewModel.sharedBreakupCounts.collectAsStateWithLifecycle()
    val showFriendsListDialog by viewModel.showFriendsListDialog.collectAsStateWithLifecycle()
    val allSwipes by viewModel.allSwipes.collectAsStateWithLifecycle()
    val pendingBreakupRequests by viewModel.pendingBreakupRequests.collectAsStateWithLifecycle()
    val selectedBusiness by viewModel.selectedBusiness.collectAsStateWithLifecycle()
    val allBusinesses by viewModel.allBusinesses.collectAsStateWithLifecycle()
    val allApiRequests by viewModel.allApiRequests.collectAsStateWithLifecycle()
    val pendingDeepLinkBizId by com.example.util.DeepLinkManager.pendingBusinessId.collectAsStateWithLifecycle()
    var showInteractionsModal by remember { mutableStateOf(false) }

    LaunchedEffect(pendingDeepLinkBizId) {
        pendingDeepLinkBizId?.let { bizId ->
            viewModel.openBusinessById(bizId)
            com.example.util.DeepLinkManager.setPendingBusinessId(null)
        }
    }

    val systemPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.onPermissionsResult(allGranted)
        if (results[android.Manifest.permission.READ_CONTACTS] == true) {
            com.example.util.ContactResolver.reload(context)
            viewModel.loadPhonebookContacts()
        }
    }

    // Initialize ContactResolver on startup
    LaunchedEffect(Unit) {
        com.example.util.ContactResolver.init(context)
    }

    // Listen for toast/snackbar events
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Auto-prompt mandatory permissions only after login and profile creation are completed
    LaunchedEffect(preferences?.isLoggedIn, preferences?.isProfileCompleted, preferences?.permissionsGranted) {
        if (preferences?.isLoggedIn == true && preferences?.isProfileCompleted == true && preferences?.permissionsGranted != true) {
            val permissionsToRequest = mutableListOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_CONTACTS
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            try {
                systemPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            } catch (e: Exception) {
                viewModel.onPermissionsResult(true)
            }
        }
    }

    // Authentication & Guard State Variables
    val isLoggedIn = preferences?.isLoggedIn ?: false
    val loginTimestamp = preferences?.loginTimestamp ?: 0L
    val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L
    val isSessionExpired = (System.currentTimeMillis() - loginTimestamp) > thirtyDaysMillis
    val isMpinSet = preferences?.isMpinSet ?: false
    val savedMpin = preferences?.userMpin ?: ""
    val isProfileCompleted = preferences?.isProfileCompleted ?: false

    // Auto-reset navigation tab to 0 whenever user logs out or session is wiped
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            try {
                pagerState.scrollToPage(0)
            } catch (_: Exception) {}
        }
    }

    // Root Exit Debounce State
    var lastRootBackPressTime by remember { mutableLongStateOf(0L) }

    // Check if any overlay, sheet, or dialog is currently showing
    val isAnyOverlayOpen = activeChat != null ||
            activeCall != null ||
            activeMatchDialog != null ||
            selectedDetail != null ||
            pendingRequestToReview != null ||
            outgoingFriendRequestNotification != null ||
            outgoingLikeNotification != null ||
            showFriendsListDialog ||
            showMandatoryPermissionsDialog ||
            showPostRegistrationBackupDialog ||
            showMpinSetupPrompt ||
            showPhonebookScreen ||
            showInteractionsModal ||
            selectedContactToInvite != null

    // Automatically fetch and update Stories when user opens the Stories tab
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            viewModel.fetchMutualFriendsStories()
        }
    }

    // System Back Handler for Main Bottom Navigation Tabs (Tab 1/2/3 -> Tab 0 Chats)
    BackHandler(
        enabled = isLoggedIn && !isSessionExpired && isMpinUnlocked && isProfileCompleted && !isAnyOverlayOpen && !showAdminPortal && !showFaceVerification && pagerState.currentPage != 0
    ) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    // System Back Handler for Main Root Screen (Tab 0 Chats) - double tap back to exit safeguard
    BackHandler(
        enabled = isLoggedIn && !isSessionExpired && isMpinUnlocked && isProfileCompleted && !isAnyOverlayOpen && !showAdminPortal && !showFaceVerification && pagerState.currentPage == 0
    ) {
        val now = System.currentTimeMillis()
        if (now - lastRootBackPressTime < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastRootBackPressTime = now
            Toast.makeText(context, "Press back again to exit VibeSync", Toast.LENGTH_SHORT).show()
        }
    }

    // High Priority Back Handlers for Overlays & Sub-Screens
    BackHandler(enabled = showAdminPortal) {
        viewModel.closeAdminPortal()
    }

    BackHandler(enabled = showFaceVerification) {
        if (showAccountRecoveryDialog) {
            viewModel.closeAccountRecovery()
        } else if (duplicateProfileDetected != null) {
            viewModel.dismissDuplicateProfileDialog()
        } else {
            viewModel.skipOrCancelFaceVerification()
        }
    }

    BackHandler(enabled = (!isLoggedIn || isSessionExpired) && showAccountRecoveryDialog) {
        viewModel.closeAccountRecovery()
    }

    BackHandler(enabled = (!isLoggedIn || isSessionExpired) && duplicateProfileDetected != null) {
        viewModel.dismissDuplicateProfileDialog()
    }

    BackHandler(enabled = selectedContactToInvite != null) {
        viewModel.closeInviteContactDialog()
    }

    BackHandler(enabled = showPhonebookScreen) {
        viewModel.closePhonebookScreen()
    }

    BackHandler(enabled = showMpinSetupPrompt) {
        viewModel.dismissMpinSetupPrompt()
    }

    BackHandler(enabled = showPostRegistrationBackupDialog) {
        viewModel.dismissPostRegistrationBackupDialog()
    }

    BackHandler(enabled = showMandatoryPermissionsDialog) {
        viewModel.dismissMandatoryPermissionsDialog()
    }

    BackHandler(enabled = showFriendsListDialog) {
        viewModel.closeFriendsList()
    }

    BackHandler(enabled = showInteractionsModal) {
        showInteractionsModal = false
    }

    BackHandler(enabled = outgoingLikeNotification != null) {
        viewModel.dismissOutgoingLikeNotification()
    }

    BackHandler(enabled = outgoingFriendRequestNotification != null) {
        viewModel.dismissOutgoingNotification()
    }

    BackHandler(enabled = pendingRequestToReview != null) {
        viewModel.dismissFriendRequestReview()
    }

    BackHandler(enabled = selectedDetail != null) {
        viewModel.closeProfileDetail()
    }

    BackHandler(enabled = activeMatchDialog != null) {
        viewModel.dismissMatchDialog()
    }

    BackHandler(enabled = activeCall != null) {
        viewModel.endCall()
    }

    BackHandler(enabled = activeChat != null) {
        viewModel.closeChat()
    }

    MyApplicationTheme(presetId = preferences?.selectedThemePreset ?: "PURE_LIGHT") {
        // Secure AuthState Root Navigator (Eliminates Infinite Splash Loading Loop)
        if (authState == AuthState.INITIALIZING || isSessionChecking) {
        VibeSyncSplashScreen()
    } else if (showAdminPortal) {
        AdminBackendScreen(
            isAdminLoggedIn = isAdminLoggedIn,
            onLoginAdmin = { u, p -> viewModel.loginAdmin(u, p) },
            onLogoutAdmin = { viewModel.logoutAdmin() },
            currentAdminUsername = preferences?.adminUsername ?: "creator",
            onUpdateAdminCredentials = { u, p -> viewModel.updateAdminCredentials(u, p) },
            roles = adminRoles,
            employees = adminEmployees,
            onCreateRole = { role -> viewModel.createAdminRole(role) },
            onAssignEmployee = { emp -> viewModel.assignAdminEmployee(emp) },
            onToggleEmployeeStatus = { id -> viewModel.toggleEmployeeStatus(id) },
            onDeleteEmployee = { id -> viewModel.deleteAdminEmployee(id) },
            profiles = allAdminProfiles,
            demographicAnalytics = demographicAnalytics,
            onBanProfile = { id, note -> viewModel.banProfile(id, note) },
            onUnbanProfile = { id -> viewModel.unbanProfile(id) },
            onToggleSpam = { id, cur -> viewModel.toggleSpamFlag(id, cur) },
            onToggleVerification = { profile -> viewModel.toggleProfileVerification(profile) },
            onDeleteProfile = { id -> viewModel.deleteProfile(id) },
            onAddCustomProfile = { profile -> viewModel.addCustomProfile(profile) },
            onRunAiSpamScan = { viewModel.runAiAntiSpamScan() },
            onExportUserData = { userId, callback -> viewModel.exportLawEnforcementUserData(userId, callback) },
            onExportActivityLogs = { callback -> viewModel.exportSystemActivityLogs(callback) },
            onExportUserActivityLogs = { userId, callback -> viewModel.exportUserActivityLogs(userId, callback) },
            onCleanDatabaseForTwoPhones = { viewModel.cleanDatabaseForTwoPhonesInstallation() },
            onForceWipeAllBackendData = { viewModel.clearAllProfilesAndBackendData() },
            supabaseDiagnostic = supabaseDiagnostic,
            onRunDiagnosticTest = { viewModel.runSupabaseDiagnosticTest() },
            apiRequests = allApiRequests,
            onApproveApiRequest = { id, key, hook, notes -> viewModel.updateApiRequestApproval(id, "APPROVED", key, hook, notes) },
            onRejectApiRequest = { id, notes -> viewModel.updateApiRequestApproval(id, "REJECTED", notes = notes) },
            onClose = { viewModel.closeAdminPortal() }
        )
    } else if (authState == AuthState.UNAUTHENTICATED) {
        AuthLoginScreen(
            onVerifyPhoneDirect = { phone -> viewModel.verifyPhoneDirect(phone) },
            onOpenAdminPortal = { viewModel.openAdminPortal() }
        )
    } else if (!isProfileCompleted) {
        RegistrationProfileScreen(
            initialName = preferences?.userName ?: "",
            initialOccupation = preferences?.userOccupation ?: "",
            initialAvatarUrl = preferences?.avatarUrl ?: "",
            onBackToLogin = { viewModel.logout() },
            onCompleteRegistration = { name, age, dob, gender, interestedIn, goal, country, countryFlag, place, qualification, occupation, bio, interests, maritalStatus, isOpenForDating, address, city, coords, avatar, photos ->
                val (parsedLat, parsedLon) = try {
                    val parts = coords.split(",")
                    val latVal = parts.getOrNull(0)?.replace("[^0-9.-]".toRegex(), "")?.toDoubleOrNull() ?: 12.9716
                    val lonVal = parts.getOrNull(1)?.replace("[^0-9.-]".toRegex(), "")?.toDoubleOrNull() ?: 77.5946
                    Pair(latVal, lonVal)
                } catch (_: Exception) {
                    Pair(12.9716, 77.5946)
                }
                viewModel.registerUserProfile(
                    name = name,
                    age = age,
                    dob = dob,
                    gender = gender,
                    address = address,
                    city = city,
                    latitude = parsedLat,
                    longitude = parsedLon,
                    avatarUrl = avatar,
                    interestedIn = interestedIn,
                    goal = goal,
                    country = country,
                    countryFlag = countryFlag,
                    place = place,
                    qualification = qualification,
                    occupation = occupation,
                    bio = bio,
                    interests = interests,
                    maritalStatus = maritalStatus,
                    isOpenForDating = isOpenForDating,
                    photos = photos
                )
            }
        )
    } else if (isMpinSet && !isMpinUnlocked) {
        MpinScreen(
            isMpinSet = isMpinSet,
            savedMpin = savedMpin,
            onMpinSuccess = { viewModel.unlockMpin() },
            onSetMpin = { mpin -> viewModel.setUserMpin(mpin) },
            onForgotPasswordOrRelogin = { viewModel.logout() }
        )
    } else {
        Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (activeChat == null) {
                val currentTab = pagerState.currentPage
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("dating_bottom_nav")
                ) {
                    // Chats Tab (with unread badge)
                    val unreadCount = matches.count { it.hasUnread }
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { coroutineScope.launch { pagerState.scrollToPage(0) } },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(containerColor = VibeSyncUnreadBadge) {
                                            Text("$unreadCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentTab == 0) Icons.Default.Chat else Icons.Outlined.Chat,
                                    contentDescription = "Chats",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { Text("Chats", fontSize = 11.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VibeSyncTeal,
                            selectedTextColor = VibeSyncTeal,
                            indicatorColor = VibeSyncTeal.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("tab_chats")
                    )

                    // Stories Tab
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { coroutineScope.launch { pagerState.scrollToPage(1) } },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 1) Icons.Default.DonutLarge else Icons.Outlined.DonutLarge,
                                contentDescription = "Stories",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Stories", fontSize = 11.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VibeSyncTeal,
                            selectedTextColor = VibeSyncTeal,
                            indicatorColor = VibeSyncTeal.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("tab_status")
                    )

                    // Friends Tab (Consolidated Contacts & Friends)
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { coroutineScope.launch { pagerState.scrollToPage(2) } },
                        icon = {
                            BadgedBox(
                                badge = {
                                    val totalFriendsNotifications = likedMeProfiles.size + pendingFriendRequests.size
                                    if (totalFriendsNotifications > 0) {
                                        Badge(containerColor = VibeSyncTeal) {
                                            Text("$totalFriendsNotifications")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentTab == 2) Icons.Default.Groups else Icons.Outlined.Groups,
                                    contentDescription = "Friends",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { Text("Friends", fontSize = 11.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VibeSyncTeal,
                            selectedTextColor = VibeSyncTeal,
                            indicatorColor = VibeSyncTeal.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("tab_friends")
                    )

                    // Find Tab (Nearest Businesses & Local Deals Discovery)
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { coroutineScope.launch { pagerState.scrollToPage(3) } },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 3) Icons.Default.Explore else Icons.Outlined.Explore,
                                contentDescription = "Find",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Find", fontSize = 11.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VibeSyncTeal,
                            selectedTextColor = VibeSyncTeal,
                            indicatorColor = VibeSyncTeal.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("tab_find")
                    )

                    // Profile Tab
                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { coroutineScope.launch { pagerState.scrollToPage(4) } },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 4) Icons.Default.Person else Icons.Outlined.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Profile", fontSize = 11.sp, fontWeight = if (currentTab == 4) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VibeSyncTeal,
                            selectedTextColor = VibeSyncTeal,
                            indicatorColor = VibeSyncTeal.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("tab_profile")
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NetworkSyncStatusBanner(
                networkState = networkState,
                onRetryCheck = { viewModel.retryNetworkCheck() }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                userScrollEnabled = true,
                beyondViewportPageCount = 2
            ) { page ->
            when (page) {
                0 -> MatchesChatScreen(
                    matches = matches,
                    phonebookContacts = phonebookContacts,
                    onOpenChat = { match, profile ->
                        BiometricPromptHelper.verifyBeforeEnteringChat(
                            context = context,
                            contextName = profile.name,
                            onVerified = { viewModel.openChat(match, profile) }
                        )
                    },
                    getProfileSync = { id -> viewModel.getProfileSync(id) },
                    onOpenPhonebook = { viewModel.openPhonebookScreen() },
                    onSelectChatContact = { contact -> viewModel.startChatWithContact(contact) },
                    onInviteContact = { contact -> viewModel.openInviteContactDialog(contact) },
                    onRefreshContacts = { viewModel.loadPhonebookContacts() },
                    onOpenAdmin = { viewModel.openAdminPortal() },
                    onOpenInteractionsModal = { showInteractionsModal = true }
                )
                1 -> StatusStoriesScreen(
                    viewModel = viewModel
                )
                2 -> DiscoverScreen(
                    candidates = candidates,
                    viewModel = viewModel,
                    activeDatingProfile = activeDatingProfile,
                    isOpenForDating = preferences?.isOpenForDating ?: true,
                    onEnableOpenForDating = { viewModel.enableOpenForDating() },
                    onOpenActiveChat = {
                        BiometricPromptHelper.verifyBeforeEnteringChat(
                            context = context,
                            contextName = activeDatingProfile?.name ?: "Dating Partner",
                            onVerified = { viewModel.openChatForActiveDatingMatch() }
                        )
                    },
                    onBreakupActiveMatch = { viewModel.breakupCurrentActiveMatch() },
                    onSwipe = { profileId, dir ->
                        if (dir == "LIKE" || dir == "SUPER_LIKE" || dir == "PROPOSAL") {
                            BiometricPromptHelper.verifyBeforeSwipeMatch(
                                context = context,
                                onVerified = { viewModel.swipe(profileId, dir) }
                            )
                        } else {
                            viewModel.swipe(profileId, dir)
                        }
                    },
                    onRewind = { viewModel.rewind() },
                    onOpenProfileDetail = { viewModel.openProfileDetail(it) },
                    onOpenFilters = { coroutineScope.launch { pagerState.scrollToPage(4) } },
                    onOpenAdmin = { viewModel.openAdminPortal() },
                    onResetData = { viewModel.resetAllData() },
                    superLikesCount = superLikesCount,
                    mutualFriendsMap = mutualFriendsMap,
                    onOpenInteractionsModal = { showInteractionsModal = true },
                    onOpenReviewRequest = { viewModel.openFriendRequestReview(it) },
                    likedMeProfiles = likedMeProfiles,
                    userPreferences = preferences,
                    onUpdatePreferences = { viewModel.updatePreferences(it) },
                    sharedBreakupCounts = sharedBreakupCounts,
                    pendingBreakupRequests = pendingBreakupRequests,
                    onRequestBreakupShare = { profile -> viewModel.requestBreakupCountShare(profile.id, profile.name) }
                )
                3 -> com.example.ui.components.BusinessHubScreen(
                    viewModel = viewModel,
                    initialTab = 0
                )
                4 -> ProfileScreen(
                    preferences = preferences,
                    onUpdatePreferences = { viewModel.updatePreferences(it) },
                    onToggleProfileLock = { viewModel.toggleProfileLock(it) },
                    onResetData = { viewModel.resetAllData() },
                    onDeleteAccount = { viewModel.deleteAccount() },
                    onOpenAdminPortal = { viewModel.openAdminPortal() },
                    onLogout = { viewModel.logout() },
                    superLikesCount = superLikesCount,
                    isSpotlightActive = isSpotlightActive,
                    onSyncCloudBackup = { viewModel.syncGoogleCloudBackupNow() },
                    onRestoreCloudBackup = { viewModel.restoreFromGoogleCloudBackup() },
                    onUpdateSchedule = { freq, wifiOnly -> viewModel.updateBackupScheduleSettings(freq, wifiOnly) },
                    isBackupInProgress = isBackupInProgress,
                    isRestoreInProgress = isRestoreInProgress,
                    onOpenMpinSetup = { viewModel.openMpinSetupPrompt() },
                    onSetMpin = { pin -> viewModel.updateMpin(pin) },
                    onTriggerFaceVerification = { viewModel.triggerFaceVerification() },
                    onOpenFriendsList = { viewModel.openFriendsList() },
                    onOpenChat = { targetId -> viewModel.startChatWithProfile(targetId) },
                    acceptedFriends = acceptedFriends,
                    mutualFriendsMap = mutualFriendsMap,
                    allProfiles = allAdminProfiles,
                    onUnbanProfile = { id -> viewModel.unbanProfile(id) },
                    onToggleHideFriends = { hide -> viewModel.toggleHideFriendsList(hide) },
                    onRefreshLiveLocation = { viewModel.refreshLiveLocation() },
                    onUpdatePhoneNumber = { verifiedPhone -> viewModel.updateVerifiedPhoneNumber(verifiedPhone) },
                    onSyncFrontLoginToBackend = { viewModel.syncFrontLoginToBackendProfile(context) },
                    onRefreshAllSettings = { viewModel.refreshAllSettingsAndBackend() }
                )
            }
        }
        }
    }

    // Full Screen Active Chat Overlay
    AnimatedVisibility(
        visible = activeChat != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        activeChat?.let { (match, profile) ->
            ChatDetailScreen(
                profile = profile,
                match = match,
                messages = chatMessages,
                currentUserId = preferences?.verifiedMobileNumber?.ifBlank { preferences?.googleEmail } ?: "USER",
                onSendMessage = { text -> viewModel.sendMessage(text) },
                onSendMedia = { mediaType, text, mediaUrl, voiceSec ->
                    viewModel.sendMediaMessage(mediaType, text, mediaUrl, voiceSec)
                },
                onStartCall = { isVideo ->
                    viewModel.startCall(profile, isVideo)
                },
                onProposeRelationship = {
                    viewModel.proposeRelationship(match.matchId)
                },
                onRespondToProposal = { accept ->
                    viewModel.respondToRelationshipProposal(match.matchId, accept)
                },
                onRequestBreakup = {
                    viewModel.requestBreakup(match.matchId)
                },
                onRespondToBreakup = { accept ->
                    viewModel.respondToBreakupRequest(match.matchId, accept)
                },
                onBreakup = {
                    viewModel.requestBreakup(match.matchId)
                },
                onBack = { viewModel.closeChat() },
                onViewProfile = { viewModel.openProfileDetail(profile) },
                onBlockAndReport = { reason, details ->
                    viewModel.emergencyBlockAndReportUser(profile.id, match.matchId, reason, details)
                },
                onOpenReport = { targetProf ->
                    viewModel.openAccountReport(targetProf)
                },
                hasActiveDatingPartner = (preferences?.activeDatingMatchId != null && preferences?.activeDatingMatchId != match.matchId),
                activeDatingPartnerName = activeDatingProfile?.name,
                onEditMessage = { messageId, newText ->
                    viewModel.editMessage(messageId, newText)
                },
                onDeleteForMe = { messageId ->
                    viewModel.deleteMessageForMe(messageId)
                },
                onDeleteForEveryone = { messageId ->
                    viewModel.deleteMessageForEveryone(messageId)
                },
                onSendMessageWithReply = { text, replyToId, replyToText, replyToSender ->
                    viewModel.sendMessageWithReply(text, replyToId, replyToText, replyToSender)
                },
                onForwardMessage = { targetMatchId, message ->
                    viewModel.forwardMessage(targetMatchId, message)
                },
                onToggleStarMessage = { messageId ->
                    viewModel.toggleStarMessage(messageId)
                },
                otherMatches = matches.filter { it.matchId != match.matchId }.mapNotNull { m ->
                    viewModel.getProfile(m.profileId)?.let { p -> Pair(m, p) }
                },
                isBreakupShared = profile.id in sharedBreakupCounts,
                isBreakupPending = profile.id in pendingBreakupRequests,
                onRequestBreakupShare = { viewModel.requestBreakupCountShare(profile.id, profile.name) }
            )
        }
    }

    // Full Screen Active Call Overlay (VibeSync E2EE Protocol E2EE)
    activeCall?.let { call ->
        ActiveCallOverlay(
            callState = call,
            onEndCall = { viewModel.endCall() },
            onToggleMute = { viewModel.toggleMuteCall() },
            onToggleVideo = { viewModel.toggleVideoCall() }
        )
    }

    // Mutual Match Celebration Dialog
    activeMatchDialog?.let { mutualMatch ->
        val comp = viewModel.calculateCompatibility(mutualMatch.profile)
        MatchCelebrationDialog(
            matchedProfile = mutualMatch.profile,
            matchId = mutualMatch.matchId,
            compatibility = comp,
            onSendMessage = {
                viewModel.openChatFromMatchDialog(mutualMatch.profile, mutualMatch.matchId)
            },
            onKeepSwiping = {
                viewModel.dismissMatchDialog()
            }
        )
    }

    // Detailed Profile Bottom Sheet / Dialog
    selectedDetail?.let { profile ->
        val comp = viewModel.calculateCompatibility(profile)
        ProfileDetailDialog(
            profile = profile,
            mutualFriends = mutualFriendsMap[profile.id] ?: emptyList(),
            compatibility = comp,
            onDismiss = { viewModel.closeProfileDetail() },
            onLike = { viewModel.swipe(profile.id, "LIKE") },
            onPass = { viewModel.swipe(profile.id, "PASS") },
            onAskFriendship = { viewModel.swipe(profile.id, "FRIEND_REQUEST") },
            onBlockAndReport = { reason -> viewModel.emergencyBlockAndReportUser(profile.id, null, reason) },
            onOpenReport = { prof -> viewModel.openAccountReport(prof) },
            isBreakupShared = profile.id in sharedBreakupCounts,
            isBreakupPending = profile.id in pendingBreakupRequests,
            onRequestBreakupShare = { viewModel.requestBreakupCountShare(profile.id, profile.name) },
            onOpenChat = { profileId -> viewModel.startChatWithProfile(profileId) }
        )
    }

    // Review Incoming Friendship Request Dialog (Notified to confirm friendship by viewing sender profile & bio)
    pendingRequestToReview?.let { request ->
        val mutuals = mutualFriendsMap[request.senderId] ?: emptyList()
        FriendshipRequestDialog(
            request = request,
            mutualFriends = mutuals,
            onConfirm = { reqId -> viewModel.acceptFriendship(reqId) },
            onDecline = { reqId -> viewModel.declineFriendship(reqId) },
            onDismiss = { viewModel.dismissFriendRequestReview() }
        )
    }

    // Outgoing Friendship Request Notification Dialog (When User A swiped down in Connect)
    outgoingFriendRequestNotification?.let { (targetProfile, req) ->
        OutgoingFriendshipDialog(
            profile = targetProfile,
            request = req,
            onSimulateConfirm = {
                viewModel.simulatePartnerConfirmFriendship(targetProfile, req)
            },
            onDismiss = { viewModel.dismissOutgoingNotification() }
        )
    }

    // Outgoing Like/SuperLike Request Simulation Dialog (When User A liked User B)
    outgoingLikeNotification?.let { (profile, direction) ->
        val comp = viewModel.calculateCompatibility(profile)
        OutgoingLikeDialog(
            profile = profile,
            direction = direction,
            compatibility = comp,
            onSimulateReceive = {
                viewModel.acceptMutualLike(profile)
            },
            onSimulateDeny = {
                viewModel.denyMutualLike(profile)
            },
            onDismiss = {
                viewModel.dismissOutgoingLikeNotification()
            }
        )
    }

    // Business Profile Detail Dialog (from Deep-Links, QR scans, or Global Selection)
    selectedBusiness?.let { biz ->
        val currentBiz = allBusinesses.firstOrNull { it.id == biz.id } ?: biz
        com.example.ui.components.BusinessProfileDialog(
            business = currentBiz,
            viewModel = viewModel,
            onDismissRequest = { viewModel.selectBusiness(null) }
        )
    }

    // Requests & Interactions Dialog (Friend Requests, Sent Requests, Likes & Superlikes)
    if (showInteractionsModal) {
        InteractionsAndRequestsModal(
            pendingReceivedRequests = pendingFriendRequests,
            pendingSentRequests = pendingOutgoingRequests,
            receivedLikes = likedMeProfiles,
            allSwipes = allSwipes,
            allProfiles = allAdminProfiles,
            onAcceptFriendRequest = { reqId -> viewModel.acceptFriendship(reqId) },
            onCancelFriendRequest = { reqId -> viewModel.cancelFriendshipRequest(reqId) },
            onLikeBack = { profile ->
                viewModel.swipe(profile.id, "LIKE")
            },
            onOpenChat = { profileId ->
                showInteractionsModal = false
                viewModel.startChatWithProfile(profileId)
            },
            onDismiss = { showInteractionsModal = false }
        )
    }

    // Friends List & User Security Privacy Dialog (Shows real friends count, hide/show friends in user security)
    if (showFriendsListDialog) {
        FriendsListDialog(
            friends = acceptedFriends,
            pendingOutgoingRequests = pendingOutgoingRequests,
            allProfiles = allAdminProfiles,
            hideFriendsList = preferences?.hideFriendsList ?: false,
            onToggleHideFriends = { viewModel.toggleHideFriendsList(it) },
            onCancelPendingRequest = { requestId -> viewModel.cancelPendingRequest(requestId) },
            onOpenChat = { profileId ->
                val prof = viewModel.getProfile(profileId)
                if (prof != null) {
                    val match = MatchEntity(
                        matchId = "match_$profileId",
                        profileId = profileId,
                        matchedAt = System.currentTimeMillis(),
                        lastMessage = "Friendship connected! 🤝",
                        lastMessageTime = System.currentTimeMillis(),
                        hasUnread = false,
                        isDatingMatch = false,
                        relationshipStatus = "FRIENDS"
                    )
                    viewModel.openChat(match, prof)
                }
            },
            onDismiss = { viewModel.closeFriendsList() }
        )
    }

    // Google Cloud Backup Onboarding Dialog (Shown Post-Registration)
    if (showPostRegistrationBackupDialog) {
        GoogleCloudBackupSetupDialog(
            userEmail = preferences?.cloudBackupAccount?.ifBlank { preferences?.googleEmail } ?: "",
            onEnableBackup = { email, freq, wifiOnly -> viewModel.enableGoogleCloudBackup(email, freq, wifiOnly) },
            onDismiss = { viewModel.dismissPostRegistrationBackupDialog() }
        )
    }

    // Google Drive Recovery / Restore Prompt Dialog (Shown on reinstall/login when backup found)
    if (showRestorePromptDialog) {
        RestoreBackupPromptDialog(
            backupMetadata = detectedDriveBackup,
            accountEmail = preferences?.cloudBackupAccount?.ifBlank { preferences?.googleEmail } ?: "",
            isRestoring = isRestoreInProgress,
            onRestore = { fileId ->
                val email = preferences?.cloudBackupAccount?.ifBlank { preferences?.googleEmail } ?: ""
                viewModel.restoreFromGoogleCloudBackup(fileId = fileId, email = email)
            },
            onSkip = { viewModel.skipDriveRestore() }
        )
    }

    // 4-Digit MPIN Setup Prompt (Presented after profile registration by user interest)
    if (showMpinSetupPrompt) {
        MpinSetupDialog(
            onSetMpin = { newPin ->
                viewModel.setUserMpin(newPin)
                viewModel.dismissMpinSetupPrompt()
            },
            onDismiss = { viewModel.dismissMpinSetupPrompt() }
        )
    }

    // Full Screen Clean Phonebook / Select Contact Overlay
    AnimatedVisibility(
        visible = showPhonebookScreen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        PhonebookContactsScreen(
            contacts = phonebookContacts,
            isSyncing = isPhonebookSyncing,
            onSelectChatContact = { contact -> viewModel.startChatWithContact(contact) },
            onInviteContact = { contact -> viewModel.openInviteContactDialog(contact) },
            onRefreshContacts = { viewModel.loadPhonebookContacts(forceRefresh = true) },
            onBack = { viewModel.closePhonebookScreen() },
            onStartChatWithNumber = { num -> viewModel.startChatWithNumber(num) }
        )
    }

    // Messaging / Carrier SMS Invite Dialog with Short Message & Transparency Notice
    selectedContactToInvite?.let { contact ->
        InviteToVibeSyncDialog(
            contact = contact,
            onInviteMessaging = { msg ->
                viewModel.sendInstantMessagingInvite(contact, msg, context)
            },
            onInviteSms = { msg ->
                viewModel.sendSmsInvite(contact, msg, context)
            },
            onDismiss = { viewModel.closeInviteContactDialog() }
        )
    }

    // Account Report & Anti-Spam Evaluation Screen
    activeReportProfile?.let { reportTarget ->
        AccountReportScreen(
            targetProfile = reportTarget,
            onDismiss = { viewModel.closeAccountReport() },
            onSubmitReport = { category, notes, evidence, alsoBlock, onComplete ->
                viewModel.submitAccountReport(category, notes, evidence, alsoBlock, onComplete)
            }
        )
    }

    // Real-Time Non-Intrusive Bottom Overlay for Network Disconnection & Sync Pause
    NetworkConnectivityOverlay(
        customNetworkState = networkState,
        onManualRetry = { viewModel.retryNetworkCheck() }
    )

    // Real-Time Heads-Up In-App Notification Dropdown Banner
    VibeSyncInAppNotificationBanner(
        onOpenChat = { targetId, senderName ->
            val profile = viewModel.getProfile(targetId)
            val matchId = if (targetId.startsWith("match_")) targetId else "match_$targetId"
            if (profile != null) {
                val match = MatchEntity(
                    matchId = matchId,
                    profileId = profile.id,
                    matchedAt = System.currentTimeMillis(),
                    lastMessage = "Hey! Let's chat 💬",
                    lastMessageTime = System.currentTimeMillis(),
                    hasUnread = false,
                    isDatingMatch = true
                )
                viewModel.openChat(match, profile)
            }
        },
        onOpenMatch = { matchId ->
            val cleanId = matchId.removePrefix("match_")
            val profile = viewModel.getProfile(cleanId)
            if (profile != null) {
                viewModel.openProfileDetail(profile)
            }
        }
    )
    }
}
}
