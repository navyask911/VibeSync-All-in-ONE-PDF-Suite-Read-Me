package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.EnumMap

object QrCodeGeneratorHelper {

    fun generateQrBitmap(
        content: String,
        size: Int = 800,
        foregroundColor: Int = android.graphics.Color.BLACK,
        backgroundColor: Int = android.graphics.Color.WHITE
    ): Bitmap? {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("QrCodeHelper", "Error generating QR code", e)
            null
        }
    }

    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "VibeSync_QR_Sticker_${System.currentTimeMillis()}"
    ): Uri? {
        val fullFileName = "$fileName.png"
        var uri: Uri? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VibeSync_Stickers")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    uri = imageUri
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(imagesDir, "VibeSync_Stickers")
                if (!appDir.exists()) appDir.mkdirs()
                val imageFile = File(appDir, fullFileName)
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                uri = Uri.fromFile(imageFile)
            }
            Toast.makeText(context, "✅ High-Res Sticker saved to Pictures/VibeSync_Stickers", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("QrCodeHelper", "Failed to save sticker", e)
            Toast.makeText(context, "Failed to save sticker: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
        return uri
    }

    fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        title: String = "VibeSync Venue Sticker"
    ) {
        try {
            val cachePath = File(context.cacheDir, "shared_stickers")
            if (!cachePath.exists()) cachePath.mkdirs()
            val file = File(cachePath, "vibesync_counter_sticker_${System.currentTimeMillis()}.png")
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Here is our official VibeSync 4x4\" Printable Counter/Window Sticker! Scan to view offers and leave GPS reviews.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Printable Sticker"))
        } catch (e: Exception) {
            android.util.Log.e("QrCodeHelper", "Failed to share sticker", e)
            Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printBitmapDirectly(
        context: Context,
        bitmap: Bitmap,
        jobName: String = "VibeSync 4x4 Sticker"
    ) {
        try {
            val printHelper = PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
            }
            printHelper.printBitmap(jobName, bitmap)
        } catch (e: Exception) {
            android.util.Log.e("QrCodeHelper", "Direct print failed", e)
            Toast.makeText(context, "Print spooler error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
