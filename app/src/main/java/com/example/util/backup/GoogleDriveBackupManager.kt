package com.example.util.backup

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

data class DriveBackupMetadata(
    val fileId: String,
    val fileName: String,
    val sizeBytes: Long,
    val createdTimestamp: Long,
    val formattedDate: String,
    val formattedSize: String
)

/**
 * Handles communication with Google Drive REST API using the isolated AppData space.
 * Files stored here are completely isolated to this app and cannot be read or deleted by regular users in Drive.
 */
class GoogleDriveBackupManager(private val context: Context) {

    private fun getDriveService(accountName: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context.applicationContext,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccountName = accountName
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("VibeSync-ChatBackup").build()
    }

    /**
     * Uploads an encrypted backup file payload to the hidden appDataFolder.
     */
    suspend fun uploadBackup(localBackupFile: java.io.File, accountName: String): String? = withContext(Dispatchers.IO) {
        if (!localBackupFile.exists()) {
            android.util.Log.e("GoogleDriveBackupManager", "Local backup file does not exist: ${localBackupFile.absolutePath}")
            return@withContext null
        }

        try {
            val driveService = getDriveService(accountName)

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileMetadata = File().apply {
                name = "chat_backup_${timestampStr}.enc"
                parents = Collections.singletonList("appDataFolder")
                description = "VibeSync AES-256 Encrypted Chat & Social Graph Backup"
            }

            val mediaContent = FileContent("application/octet-stream", localBackupFile)

            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size, createdTime")
                .execute()

            android.util.Log.i("GoogleDriveBackupManager", "Backup successfully uploaded. File ID: ${uploadedFile.id}")

            // Clean up old backups to prevent excessive storage
            try {
                deleteOldBackups(accountName, keepCount = 3)
            } catch (e: Exception) {
                android.util.Log.w("GoogleDriveBackupManager", "Cleanup warning: ${e.message}")
            }

            uploadedFile.id
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveBackupManager", "Error uploading backup to Drive: ${e.message}", e)
            null
        }
    }

    /**
     * Queries the hidden appDataFolder for the latest backup snapshot.
     */
    suspend fun checkExistingBackup(accountName: String): DriveBackupMetadata? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(accountName)

            val result: FileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, createdTime, size, modifiedTime)")
                .setOrderBy("createdTime desc")
                .setPageSize(5)
                .execute()

            val latest = result.files?.firstOrNull() ?: return@withContext null

            val timeVal = latest.createdTime?.value ?: latest.modifiedTime?.value ?: System.currentTimeMillis()
            val sizeVal = latest.getSize() ?: 0L

            val formattedDate = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(timeVal))
            val formattedSize = formatFileSize(sizeVal)

            DriveBackupMetadata(
                fileId = latest.id,
                fileName = latest.name ?: "chat_backup.enc",
                sizeBytes = sizeVal,
                createdTimestamp = timeVal,
                formattedDate = formattedDate,
                formattedSize = formattedSize
            )
        } catch (e: Exception) {
            android.util.Log.w("GoogleDriveBackupManager", "Error querying appDataFolder: ${e.message}")
            null
        }
    }

    /**
     * Downloads an encrypted backup file from Google Drive AppData folder.
     */
    suspend fun downloadBackupFile(accountName: String, fileId: String, targetFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(accountName)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            targetFile.parentFile?.mkdirs()

            FileOutputStream(targetFile).use { outputStream ->
                driveService.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream)
            }
            targetFile.exists() && targetFile.length() > 0
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveBackupManager", "Error downloading backup: ${e.message}", e)
            false
        }
    }

    /**
     * Keeps only the newest N backups in appDataFolder to prevent clutter.
     */
    suspend fun deleteOldBackups(accountName: String, keepCount: Int = 2): Unit = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(accountName)
            val result: FileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name, createdTime)")
                .setOrderBy("createdTime desc")
                .setPageSize(20)
                .execute()

            val files = result.files ?: return@withContext
            if (files.size > keepCount) {
                val toDelete = files.drop(keepCount)
                for (f in toDelete) {
                    try {
                        driveService.files().delete(f.id).execute()
                        android.util.Log.d("GoogleDriveBackupManager", "Deleted older backup: ${f.name} (${f.id})")
                    } catch (e: Exception) {
                        android.util.Log.w("GoogleDriveBackupManager", "Could not delete old backup ${f.id}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("GoogleDriveBackupManager", "Failed to clean old backups: ${e.message}")
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes <= 0 -> "0 KB"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
