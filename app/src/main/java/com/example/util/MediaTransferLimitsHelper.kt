package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.text.DecimalFormat

/**
 * MediaTransferLimitsHelper
 *
 * Implements Instant Messaging Media Transfer Restrictions and Zero-Cost Infrastructure File Limits:
 * - Photos / Images: 16 MB max limit (Auto-compressed for zero-cost R2 bandwidth)
 * - Video Clips / Video Notes: 16 MB max limit (Standard limit to protect connectivity)
 * - Voice Notes / Audio Memos: 16 MB max limit
 * - Documents / PDF / Files: 100 MB max limit
 * - Restricted Formats: Rejects dangerous executable binaries (.apk, .exe, .bat, .sh)
 */
object MediaTransferLimitsHelper {

    private const val TAG = "MediaTransferLimits"

    // Standard Messaging & Zero-Cost Infrastructure File Limits
    const val MAX_IMAGE_SIZE_BYTES = 16L * 1024 * 1024 // 16 MB Image limit
    const val MAX_VIDEO_SIZE_BYTES = 16L * 1024 * 1024 // 16 MB Video limit
    const val MAX_AUDIO_SIZE_BYTES = 16L * 1024 * 1024 // 16 MB Audio limit
    const val MAX_DOCUMENT_SIZE_BYTES = 100L * 1024 * 1024 // 100 MB Document limit

    val RESTRICTED_EXTENSIONS = setOf("exe", "apk", "bat", "sh", "msi", "cmd", "vbs", "jar", "dex")

    data class MediaValidationResult(
        val isValid: Boolean,
        val fileName: String,
        val fileSizeFormatted: String,
        val fileSizeBytes: Long,
        val mimeType: String,
        val errorMessage: String? = null,
        val isOversized: Boolean = false,
        val isUnsupportedType: Boolean = false,
        val maxAllowedFormatted: String = "16 MB"
    )

    /**
     * Inspects a picked Uri from Gallery or File Manager, checks file size, extension, and MIME type against standard media limits.
     */
    fun validateMediaFile(context: Context, uri: Uri, expectedMediaType: String = "AUTO"): MediaValidationResult {
        var fileName = "Selected Media"
        var fileSize = 0L
        var mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving file metadata: ${e.message}")
        }

        val extension = fileName.substringAfterLast(".", "").lowercase()
        val formattedSize = formatFileSize(fileSize)

        // 1. Check Restricted Executable Formats
        if (RESTRICTED_EXTENSIONS.contains(extension)) {
            return MediaValidationResult(
                isValid = false,
                fileName = fileName,
                fileSizeFormatted = formattedSize,
                fileSizeBytes = fileSize,
                mimeType = mimeType,
                errorMessage = "Security Notice: '.${extension.uppercase()}' files are unsupported on VibeSync for user protection and security compliance.",
                isUnsupportedType = true,
                maxAllowedFormatted = "N/A"
            )
        }

        // 2. Evaluate Size Limits by Category (Standard Messaging & Zero-Cost Limits)
        val isVideo = expectedMediaType == "VIDEO" || mimeType.startsWith("video/") || extension in listOf("mp4", "mov", "mkv", "3gp", "webm")
        val isImage = expectedMediaType == "IMAGE" || mimeType.startsWith("image/") || extension in listOf("jpg", "jpeg", "png", "webp", "heic")
        val isAudio = expectedMediaType == "AUDIO" || mimeType.startsWith("audio/") || extension in listOf("mp3", "m4a", "aac", "ogg", "wav")

        val maxAllowed = when {
            isVideo -> MAX_VIDEO_SIZE_BYTES
            isImage -> MAX_IMAGE_SIZE_BYTES
            isAudio -> MAX_AUDIO_SIZE_BYTES
            else -> MAX_DOCUMENT_SIZE_BYTES
        }

        val limitFormatted = formatFileSize(maxAllowed)

        if (fileSize > maxAllowed) {
            val categoryLabel = when {
                isVideo -> "video file"
                isImage -> "photo"
                isAudio -> "voice/audio memo"
                else -> "document"
            }
            return MediaValidationResult(
                isValid = false,
                fileName = fileName,
                fileSizeFormatted = formattedSize,
                fileSizeBytes = fileSize,
                mimeType = mimeType,
                errorMessage = "File Size Large: Selected $categoryLabel ($formattedSize) exceeds the $limitFormatted transfer limit.",
                isOversized = true,
                maxAllowedFormatted = limitFormatted
            )
        }

        return MediaValidationResult(
            isValid = true,
            fileName = fileName,
            fileSizeFormatted = formattedSize,
            fileSizeBytes = fileSize,
            mimeType = mimeType,
            maxAllowedFormatted = limitFormatted
        )
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[digitGroups.coerceAtMost(units.size - 1)]
    }
}
