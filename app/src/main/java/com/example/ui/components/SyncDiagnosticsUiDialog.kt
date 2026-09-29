package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.SupabaseSyncRepository
import com.example.data.repository.SyncDiagnosticsRepository
import kotlinx.coroutines.launch

private val VibeSyncTeal = Color(0xFF00BFA5)
private val VibeSyncDark = Color(0xFF1E293B)
private val ConsoleDark = Color(0xFF0F172A)
private val OffRed = Color(0xFFE53935)
private val SoftGreen = Color(0xFF10B981)
private val AmberOrange = Color(0xFFF59E0B)
private val SupabaseGreen = Color(0xFF3ECF8E)

@Composable
fun SyncDiagnosticsDialog(
    onDismiss: () -> Unit
) {
    val diagState by SyncDiagnosticsRepository.state.collectAsState()
    val sbState by SupabaseSyncRepository.syncState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("dialog_sync_diagnostics"),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SupabaseGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = SupabaseGreen)
                        }
                        Column {
                            Text(
                                "Supabase DB & Sync Diagnostics",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = VibeSyncDark
                            )
                            Text(
                                "16-Char SHA-256 E.164 Hash Matching & Profile Tables",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_sync_diag")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Database Card: Supabase PostgREST
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (sbState.isConnected) SupabaseGreen.copy(alpha = 0.08f) else OffRed.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, if (sbState.isConnected) SupabaseGreen.copy(alpha = 0.35f) else OffRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (sbState.isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (sbState.isConnected) SupabaseGreen else OffRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (sbState.isConnected) "SUPABASE DATABASE: ONLINE" else "SUPABASE DATABASE: OFFLINE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (sbState.isConnected) SupabaseGreen else OffRed
                                )
                            }
                            Text(
                                text = "HTTP ${sbState.httpStatus} • ${sbState.latencyMs} ms",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sbState.statusMessage,
                            fontSize = 11.sp,
                            color = VibeSyncDark
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Profiles in Supabase: ${sbState.totalProfilesInSupabase}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VibeSyncDark
                            )
                            Text(
                                "Matched VibeSync Users: ${sbState.matchedContactsCount}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SupabaseGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Discovered Test Devices & Hash Inspection Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = VibeSyncTeal, modifier = Modifier.size(16.dp))
                        Text(
                            "Discovered Test Devices (${diagState.deviceSnapshots.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = VibeSyncDark
                        )
                    }

                    if (diagState.isMigrating || sbState.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = VibeSyncTeal)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Device Snapshots List
                if (diagState.deviceSnapshots.isEmpty()) {
                    Text(
                        "No test devices registered yet. Profiles will appear here automatically upon registration.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 130.dp)
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(diagState.deviceSnapshots) { dev ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${dev.userName} (${dev.phoneNumber})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = VibeSyncDark
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (dev.hasValid16CharHash) SoftGreen.copy(alpha = 0.15f) else AmberOrange.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "hash: ${dev.phoneHash.take(16)}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (dev.hasValid16CharHash) SoftGreen else AmberOrange,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = dev.diagnosticNote,
                                            fontSize = 10.sp,
                                            color = if (dev.hasValid16CharHash) Color.Gray else AmberOrange
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                SupabaseSyncRepository.checkConnectivity()
                                SupabaseSyncRepository.fetchAllProfiles()
                            }
                        },
                        enabled = !sbState.isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_sync_supabase_now")
                    ) {
                        Text("⚡ Sync Supabase DB", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { SyncDiagnosticsRepository.runPhoneHashMigration() },
                        enabled = !diagState.isMigrating,
                        colors = ButtonDefaults.buttonColors(containerColor = VibeSyncTeal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_migrate_phone_hash")
                    ) {
                        Text("🔄 16-Char Hash Re-sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                SupabaseSyncRepository.checkConnectivity()
                                SyncDiagnosticsRepository.startMonitoring()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_refresh_sync_diag")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            SupabaseSyncRepository.clearLogs()
                            SyncDiagnosticsRepository.clearLogs()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_clear_sync_logs")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Combined Realtime Connectivity Logs Console
                val combinedLogs = (sbState.logs + diagState.logs).distinct().sortedDescending()
                Text(
                    "Realtime Database & Match Pipeline Logs (${combinedLogs.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = VibeSyncDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ConsoleDark)
                        .padding(8.dp)
                ) {
                    if (combinedLogs.isEmpty()) {
                        Text(
                            "Waiting for database events...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(combinedLogs) { logLine ->
                                val textColor = when {
                                    logLine.contains("❌") || logLine.contains("OFFLINE") || logLine.contains("FAILED") -> OffRed
                                    logLine.contains("✅") || logLine.contains("ONLINE") || logLine.contains("COMPLETE") -> SoftGreen
                                    logLine.contains("⚠️") || logLine.contains("WARNING") -> AmberOrange
                                    logLine.contains("📱") || logLine.contains("TEST DEVICES") -> Color(0xFF67E8F9)
                                    logLine.contains("🚀") || logLine.contains("⚡") -> SupabaseGreen
                                    else -> Color(0xFFE2E8F0)
                                }

                                Text(
                                    text = logLine,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = textColor,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
