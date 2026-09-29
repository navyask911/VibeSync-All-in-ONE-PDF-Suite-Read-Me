package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.ui.theme.CoralPink
import com.example.ui.theme.SuperLikeBlue
import com.example.ui.theme.VibeSyncTeal
import com.example.util.AppUpdateHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AppUpdateCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isWifiServerActive by remember { mutableStateOf(AppUpdateHelper.isLocalServerRunning()) }
    var wifiServerUrl by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadStatusText by remember { mutableStateOf("") }
    var updateUrlInput by remember { mutableStateOf("https://github.com/aistudio/vibesync/releases/latest/download/app-release.apk") }
    var hasInstallPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                true
            }
        )
    }

    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    ProfileSectionDrawer(
        title = "In-App Update & Phone Transfer",
        icon = Icons.Default.SystemUpdate,
        iconTint = Color(0xFF00897B),
        iconBackground = Color(0xFF00897B).copy(alpha = 0.15f),
        statusBadgeText = "v$currentVersionName • No USB Needed",
        statusBadgeColor = Color(0xFF00897B),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        testTag = "drawer_app_update"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VibeSyncTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = VibeSyncTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Current App Version: v$currentVersionName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Send APK between phones or update over the internet.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // SECTION 1: Phone-to-Phone Transfer
            Text(
                text = "📱 1. SEND APP DIRECTLY TO ANOTHER PHONE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VibeSyncTeal
            )

            // Option A: Share APK via VibeSync / Quick Share / Bluetooth
            Button(
                onClick = {
                    val shared = AppUpdateHelper.shareInstalledApk(context)
                    if (shared) {
                        Toast.makeText(context, "Select VibeSync, Quick Share, or Bluetooth to send APK", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to extract APK", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_share_apk_phone"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share APK (VibeSync / Quick Share)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Option B: Direct Wi-Fi / Hotspot APK Server
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wi-Fi / Hotspot Direct APK Server",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isWifiServerActive) "Server Running! Ready on other phone." else "Send over Wi-Fi without internet or cables",
                                fontSize = 11.sp,
                                color = if (isWifiServerActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isWifiServerActive,
                            onCheckedChange = { active ->
                                if (active) {
                                    coroutineScope.launch {
                                        AppUpdateHelper.startLocalApkServer(
                                            context = context,
                                            onStarted = { url ->
                                                isWifiServerActive = true
                                                wifiServerUrl = url
                                                Toast.makeText(context, "Wi-Fi Server Started!", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                isWifiServerActive = false
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                } else {
                                    AppUpdateHelper.stopLocalApkServer()
                                    isWifiServerActive = false
                                    wifiServerUrl = ""
                                    Toast.makeText(context, "Wi-Fi Server Stopped", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("switch_wifi_server")
                        )
                    }

                    AnimatedVisibility(visible = isWifiServerActive && wifiServerUrl.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "📥 On Phone 2, open Chrome browser and enter:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1B5E20)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = wifiServerUrl,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("APK Link", wifiServerUrl))
                                        Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 11.sp)
                                }
                            }
                            Text(
                                text = "The APK will immediately download and install on Phone 2!",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // SECTION 2: Update from Internet
            Text(
                text = "🌐 2. UPDATE APP FROM INTERNET",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SuperLikeBlue
            )

            OutlinedTextField(
                valUpdate = updateUrlInput,
                onValueChange = { updateUrlInput = it },
                label = "Cloud APK Download URL",
                placeholder = "https://example.com/app.apk",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (isDownloading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Downloading update...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$downloadProgress%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuperLikeBlue)
                    }
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = SuperLikeBlue,
                    )
                    if (downloadStatusText.isNotBlank()) {
                        Text(downloadStatusText, fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (updateUrlInput.isBlank()) {
                            Toast.makeText(context, "Please enter a valid APK download URL", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isDownloading = true
                        downloadProgress = 0
                        downloadStatusText = ""
                        coroutineScope.launch {
                            AppUpdateHelper.downloadApkFromUrl(
                                context = context,
                                downloadUrl = updateUrlInput.trim(),
                                onProgress = { progress -> downloadProgress = progress },
                                onSuccess = { file ->
                                    isDownloading = false
                                    Toast.makeText(context, "Download complete! Opening installer...", Toast.LENGTH_SHORT).show()
                                    AppUpdateHelper.installApk(context, file)
                                },
                                onError = { error ->
                                    isDownloading = false
                                    downloadStatusText = error
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_download_update_internet"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuperLikeBlue)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download & Install Update", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Unknown Sources Permission Helper
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasInstallPermission) "✅ Install Permission Granted" else "⚠️ Install Permission Required",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasInstallPermission) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                    if (!hasInstallPermission) {
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Enable in Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlinedTextField(
    valUpdate: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    androidx.compose.material3.OutlinedTextField(
        value = valUpdate,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 12.sp) },
        modifier = modifier,
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SuperLikeBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}
