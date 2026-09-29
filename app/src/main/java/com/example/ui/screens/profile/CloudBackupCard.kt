package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserPreferencesEntity
import com.example.ui.theme.VibeSyncTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudBackupCard(
    preferences: UserPreferencesEntity,
    onUpdatePreferences: (UserPreferencesEntity) -> Unit,
    onSyncCloudBackup: () -> Unit,
    onRestoreCloudBackup: () -> Unit,
    onUpdateSchedule: (frequency: String, wifiOnly: Boolean) -> Unit = { _, _ -> },
    isBackupInProgress: Boolean = false,
    isRestoreInProgress: Boolean = false,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val backupAccount = preferences.cloudBackupAccount.ifBlank { preferences.googleEmail }
    val formattedDate = remember(preferences.lastCloudBackupTimestamp) {
        if (preferences.lastCloudBackupTimestamp > 0) {
            SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                .format(Date(preferences.lastCloudBackupTimestamp))
        } else {
            "Never synced"
        }
    }

    val statusText = when {
        !preferences.isGoogleCloudBackupEnabled || preferences.backupFrequency == "OFF" -> "Backup Disabled"
        preferences.backupFrequency == "DAILY" -> "Daily (24h) Active ☁️"
        preferences.backupFrequency == "WEEKLY" -> "Weekly Active ☁️"
        preferences.backupFrequency == "BI_WEEKLY" -> "15 Days Active ☁️"
        else -> "Cloud Active ☁️"
    }

    val statusColor = if (preferences.isGoogleCloudBackupEnabled && preferences.backupFrequency != "OFF") {
        Color(0xFF1976D2)
    } else {
        Color(0xFF757575)
    }

    ProfileSectionDrawer(
        title = "Cloud Backup & Drive Sync",
        icon = Icons.Default.Cloud,
        iconTint = Color(0xFF1976D2),
        iconBackground = Color(0xFF1976D2).copy(alpha = 0.15f),
        statusBadgeText = statusText,
        statusBadgeColor = statusColor,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        testTag = "drawer_cloud_backup"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Backup Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1976D2).copy(alpha = 0.08f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1976D2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Drive AppFolder Backup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target Account: ${backupAccount.ifBlank { "Personal Google Account" }}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Last synced: $formattedDate",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (preferences.lastBackupFileSize.isNotBlank()) {
                                Text(
                                    text = " • ${preferences.lastBackupFileSize}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VibeSyncTeal
                                )
                            }
                        }
                    }
                }
            }

            // Sync Now & Restore Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSyncCloudBackup,
                    enabled = !isBackupInProgress && !isRestoreInProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_sync_cloud_now"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    if (isBackupInProgress) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backing up...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup Now", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }

                OutlinedButton(
                    onClick = onRestoreCloudBackup,
                    enabled = !isBackupInProgress && !isRestoreInProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_restore_cloud_backup"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isRestoreInProgress) {
                        CircularProgressIndicator(
                            color = Color(0xFF1976D2),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restoring...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                    }
                }
            }

            // Backup Frequency Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Auto-Backup Interval (WorkManager)",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Scheduled in background without interrupting chats",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val intervals = listOf(
                    "DAILY" to "Daily (24h)",
                    "WEEKLY" to "Weekly (7d)",
                    "BI_WEEKLY" to "15 Days",
                    "OFF" to "Off"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    intervals.forEach { (freqKey, label) ->
                        val isSelected = preferences.backupFrequency == freqKey
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onUpdateSchedule(freqKey, preferences.backupOverWifiOnly)
                            },
                            label = {
                                Text(
                                    label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1976D2),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Wi-Fi Only Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (preferences.backupOverWifiOnly) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Back Up Over Wi-Fi Only",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Save mobile data plan quota",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = preferences.backupOverWifiOnly,
                    onCheckedChange = { wifiOnly ->
                        onUpdateSchedule(preferences.backupFrequency, wifiOnly)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF1976D2)
                    ),
                    modifier = Modifier.testTag("switch_wifi_only")
                )
            }

            // Security Notice Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = VibeSyncTeal.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VibeSyncTeal.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = VibeSyncTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hidden from visible Google Drive bin/files. Stored in isolated AppData space with AES-256 GCM encryption.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
