package com.example.util.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.DatingDatabase
import com.example.data.repository.SocialConnectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Background WorkManager Worker for periodic or on-demand encrypted chat backup to Google Drive AppFolder.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val appContext = applicationContext
        val db = DatingDatabase.getDatabase(appContext)
        val repository = SocialConnectRepository.getInstance(appContext)

        val prefs = repository.userPreferences.first() ?: return@withContext Result.failure()

        // Check if user turned off backup or skipped
        if (!prefs.isGoogleCloudBackupEnabled || prefs.backupFrequency == "OFF") {
            android.util.Log.d("BackupWorker", "Backup disabled by user preferences. Skipping.")
            return@withContext Result.success()
        }

        // 1. Identify Google Account
        val account = GoogleDriveAuthHolder.getSilentSignInAccount(appContext)
        val targetEmail = account?.email ?: prefs.cloudBackupAccount.ifBlank { prefs.googleEmail }

        if (targetEmail.isBlank()) {
            android.util.Log.w("BackupWorker", "No Google account linked for background backup.")
            return@withContext Result.retry()
        }

        try {
            // 2. Query Room database for all chat data
            val messages = db.chatMessageDao().getAllMessagesSync()
            val matches = db.matchDao().getAllMatchesSync()
            val friendRequests = db.friendshipRequestDao().getAllRequestsSync()
            val contacts = db.userContactDao().getAllContactsSync()
            val profile = db.profileDao().getProfileByIdSync("current_user")

            val payload = ChatBackupPayload(
                version = 1,
                backupTimestamp = System.currentTimeMillis(),
                accountEmail = targetEmail,
                totalMessages = messages.size,
                totalMatches = matches.size,
                totalContacts = contacts.size,
                messages = messages,
                matches = matches,
                friendshipRequests = friendRequests,
                userContacts = contacts,
                myProfileSnapshot = profile
            )

            val jsonString = payload.toJsonString()

            // 3. Encrypt payload locally
            val localBackupFile = File(appContext.cacheDir, "current_backup.enc")
            val isEncrypted = ChatBackupCryptoHelper.encryptJsonToFile(
                plainJson = jsonString,
                targetFile = localBackupFile,
                userSeed = targetEmail.ifBlank { "VIBESYNC_MASTER_KEY" }
            )

            if (!isEncrypted || !localBackupFile.exists()) {
                android.util.Log.e("BackupWorker", "Failed to encrypt backup file locally.")
                return@withContext Result.retry()
            }

            // 4. Upload to Google Drive AppFolder
            val backupManager = GoogleDriveBackupManager(appContext)
            val fileId = backupManager.uploadBackup(localBackupFile, targetEmail)

            if (fileId != null) {
                val fileSizeStr = formatSize(localBackupFile.length())
                repository.updatePreferences(
                    prefs.copy(
                        lastCloudBackupTimestamp = System.currentTimeMillis(),
                        cloudBackupAccount = targetEmail,
                        lastBackupFileSize = fileSizeStr
                    )
                )
                android.util.Log.i("BackupWorker", "Backup completed successfully to Google Drive. Size: $fileSizeStr")
                Result.success()
            } else {
                android.util.Log.w("BackupWorker", "Drive upload returned null file ID.")
                Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("BackupWorker", "Backup execution failed: ${e.message}", e)
            Result.retry()
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes <= 0 -> "0 KB"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
