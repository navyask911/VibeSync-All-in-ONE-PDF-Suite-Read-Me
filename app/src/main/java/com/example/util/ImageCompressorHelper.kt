package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

object ImageCompressorHelper {

    private const val TAG = "ImageCompressorHelper"

    /**
     * Reads image from Uri, downscales to max 1024px dimension, compresses to 75% quality JPEG,
     * and encodes to Base64 string prefixed with "IMG_B64:".
     */
    fun compressAndEncodeImageUriToBase64(
        context: Context,
        uriString: String,
        maxDimension: Int = 800,
        initialQuality: Int = 70
    ): String? {
        if (uriString.isBlank()) return null
        if (uriString.startsWith("IMG_B64:") || uriString.startsWith("IMG_URL:") || uriString.startsWith("http")) {
            return uriString
        }

        return try {
            val uri = Uri.parse(uriString)
            val inputStream1 = context.contentResolver.openInputStream(uri) ?: return uriString
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream1, null, boundsOptions)
            inputStream1.close()

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return uriString

            var sampleSize = 1
            while ((origWidth / sampleSize) > maxDimension || (origHeight / sampleSize) > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return uriString
            var bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()

            var activeBitmap: Bitmap = bitmap ?: return uriString

            // Ensure exact max dimension scaling if still above maxDimension
            if (activeBitmap.width > maxDimension || activeBitmap.height > maxDimension) {
                val scale = maxDimension.toFloat() / Math.max(activeBitmap.width, activeBitmap.height)
                val newWidth = (activeBitmap.width * scale).toInt()
                val newHeight = (activeBitmap.height * scale).toInt()
                activeBitmap = Bitmap.createScaledBitmap(activeBitmap, newWidth, newHeight, true)
            }

            // Adaptive compression loop to enforce upper limit of ~80 KB (81920 bytes)
            var quality = initialQuality
            val maxSizeBytes = 80 * 1024
            var bytes: ByteArray
            var baos: ByteArrayOutputStream

            do {
                baos = ByteArrayOutputStream()
                activeBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                bytes = baos.toByteArray()
                if (bytes.size <= maxSizeBytes || quality <= 20) break
                quality -= 10
            } while (quality > 10)

            // If still over 80 KB after quality reduction, scale down further
            while (bytes.size > maxSizeBytes && activeBitmap.width > 200 && activeBitmap.height > 200) {
                val scale = 0.8f
                val newW = (activeBitmap.width * scale).toInt()
                val newH = (activeBitmap.height * scale).toInt()
                activeBitmap = Bitmap.createScaledBitmap(activeBitmap, newW, newH, true)
                baos = ByteArrayOutputStream()
                activeBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                bytes = baos.toByteArray()
            }

            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "IMG_B64:$b64"
        } catch (e: Exception) {
            Log.w(TAG, "Notice compressing image Uri '$uriString': ${e.message}")
            uriString
        }
    }

    /**
     * Decodes Base64 data string (from "IMG_B64:[data]") into Android Bitmap for rendering.
     */
    fun decodeBase64ToBitmap(b64Data: String): Bitmap? {
        return try {
            val cleanData = b64Data.removePrefix("IMG_B64:")
            val bytes = Base64.decode(cleanData, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.w(TAG, "Notice decoding Base64 image: ${e.message}")
            null
        }
    }
}
