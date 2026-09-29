package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.model.BusinessEntity

enum class StickerShape(val label: String, val description: String) {
    ROUNDED_SQUARE("4x4\" Rounded Square", "Perfect for entrance doors, billing counters & windows"),
    ROUND_CIRCLE("4x4\" Circular Decal", "Ideal for round cafe tables, bar coasters & glass doors"),
    TABLE_TENT("Acrylic Table Stand Tent", "Tailored for dining table acrylic stands & displays")
}

enum class StickerTheme(
    val label: String,
    val primaryColor: Int,
    val secondaryColor: Int,
    val backgroundColor: Int,
    val textColor: Int,
    val accentColor: Int
) {
    VIBESYNC_CORAL(
        label = "VibeSync Sunset Signature",
        primaryColor = Color.parseColor("#E91E63"),
        secondaryColor = Color.parseColor("#FF5252"),
        backgroundColor = Color.parseColor("#FFFFFF"),
        textColor = Color.parseColor("#1A1A1A"),
        accentColor = Color.parseColor("#FF4081")
    ),
    LUXURY_GOLD(
        label = "Luxury Gold Partner",
        primaryColor = Color.parseColor("#D4AF37"),
        secondaryColor = Color.parseColor("#AA771C"),
        backgroundColor = Color.parseColor("#121212"),
        textColor = Color.parseColor("#F5F5F5"),
        accentColor = Color.parseColor("#FFD700")
    ),
    MIDNIGHT_ONYX(
        label = "Midnight Onyx Dark",
        primaryColor = Color.parseColor("#00E5FF"),
        secondaryColor = Color.parseColor("#7C4DFF"),
        backgroundColor = Color.parseColor("#1E1E2E"),
        textColor = Color.parseColor("#FFFFFF"),
        accentColor = Color.parseColor("#00E5FF")
    ),
    EMERALD_MINT(
        label = "Emerald Mint Fresh",
        primaryColor = Color.parseColor("#00897B"),
        secondaryColor = Color.parseColor("#43A047"),
        backgroundColor = Color.parseColor("#FFFFFF"),
        textColor = Color.parseColor("#1B5E20"),
        accentColor = Color.parseColor("#2E7D32")
    ),
    PURE_MONOCHROME(
        label = "High-Contrast B&W (Thermal/Laser)",
        primaryColor = Color.parseColor("#000000"),
        secondaryColor = Color.parseColor("#333333"),
        backgroundColor = Color.parseColor("#FFFFFF"),
        textColor = Color.parseColor("#000000"),
        accentColor = Color.parseColor("#000000")
    )
}

object BusinessStickerExporter {

    fun createPrintableStickerBitmap(
        business: BusinessEntity,
        shape: StickerShape = StickerShape.ROUNDED_SQUARE,
        theme: StickerTheme = StickerTheme.VIBESYNC_CORAL,
        tableLabel: String = "Table #01",
        customCta: String = "Scan with Camera or Tap NFC to Chat, View Offers & Drop a GPS-Verified Review",
        showLogo: Boolean = true,
        showOfferHighlight: Boolean = true,
        showNfcBadge: Boolean = true,
        size: Int = 1200
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        // 1. Draw Background & Outer Clip Shape
        val isDarkTheme = theme.backgroundColor != Color.WHITE
        paint.color = theme.backgroundColor
        paint.style = Paint.Style.FILL

        when (shape) {
            StickerShape.ROUND_CIRCLE -> {
                canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            }
            StickerShape.ROUNDED_SQUARE, StickerShape.TABLE_TENT -> {
                val cornerRadius = if (shape == StickerShape.TABLE_TENT) 48f else 96f
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            }
        }

        // 2. Draw Decorative Perimeter Border & Cut Line Guide
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 14f
        paint.color = theme.primaryColor
        val insetRect = RectF(24f, 24f, size - 24f, size - 24f)
        when (shape) {
            StickerShape.ROUND_CIRCLE -> {
                canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 24f, paint)
            }
            StickerShape.ROUNDED_SQUARE, StickerShape.TABLE_TENT -> {
                canvas.drawRoundRect(insetRect, 72f, 72f, paint)
            }
        }

        // Top Header Banner Pill: "SPOT VERIFIED ON VIBESYNC"
        val headerPillRect = RectF(120f, 60f, size - 120f, 130f)
        paint.style = Paint.Style.FILL
        paint.color = theme.primaryColor
        canvas.drawRoundRect(headerPillRect, 35f, 35f, paint)

        // Header Text
        paint.color = if (isDarkTheme && theme == StickerTheme.LUXURY_GOLD) Color.BLACK else Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val tierBadge = when (business.verificationTier.uppercase()) {
            "GOLD" -> "★ GOLD VERIFIED ON VIBESYNC ★"
            "SILVER" -> "★ SILVER VERIFIED ON VIBESYNC ★"
            "BLUE_TICK", "BLUE" -> "✓ VERIFIED PARTNER • VIBESYNC"
            else -> "✓ SPOT VERIFIED ON VIBESYNC"
        }
        canvas.drawText(tierBadge, size / 2f, 107f, paint)

        // Table / Counter Tag (if set)
        if (tableLabel.isNotBlank()) {
            paint.color = theme.accentColor
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("📍 $tableLabel", size / 2f, 165f, paint)
        }

        // Business Name
        paint.color = theme.textColor
        paint.textSize = 52f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val displayBizName = if (business.name.length > 28) business.name.take(26) + "..." else business.name
        canvas.drawText(displayBizName, size / 2f, if (tableLabel.isNotBlank()) 225f else 200f, paint)

        // Category & Subtext
        paint.color = if (isDarkTheme) Color.parseColor("#B0BEC5") else Color.parseColor("#616161")
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val categoryText = "${business.logoEmoji} ${business.category} • ${business.city}"
        canvas.drawText(categoryText, size / 2f, if (tableLabel.isNotBlank()) 265f else 240f, paint)

        // 3. Center QR Code Container
        val qrBoxSize = 510f
        val qrBoxLeft = (size - qrBoxSize) / 2f
        val qrBoxTop = if (tableLabel.isNotBlank()) 295f else 270f
        val qrBoxRect = RectF(qrBoxLeft, qrBoxTop, qrBoxLeft + qrBoxSize, qrBoxTop + qrBoxSize)

        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(qrBoxRect, 36f, 36f, paint)

        paint.color = Color.parseColor("#E0E0E0")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRoundRect(qrBoxRect, 36f, 36f, paint)

        val qrPayload = "https://vibesync.app/biz/${business.id}?src=table_sticker&lat=${business.latitude}&lng=${business.longitude}"
        val qrBitmap = QrCodeGeneratorHelper.generateQrBitmap(
            content = qrPayload,
            size = (qrBoxSize - 50).toInt(),
            foregroundColor = if (theme == StickerTheme.PURE_MONOCHROME) Color.BLACK else Color.parseColor("#111111"),
            backgroundColor = Color.WHITE
        )

        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, qrBoxLeft + 25f, qrBoxTop + 25f, null)

            if (showLogo) {
                val centerBadgeSize = 88f
                val centerLeft = (size - centerBadgeSize) / 2f
                val centerTop = qrBoxTop + (qrBoxSize - centerBadgeSize) / 2f
                val centerRect = RectF(centerLeft, centerTop, centerLeft + centerBadgeSize, centerTop + centerBadgeSize)

                paint.style = Paint.Style.FILL
                paint.color = theme.primaryColor
                canvas.drawRoundRect(centerRect, 22f, 22f, paint)

                paint.style = Paint.Style.STROKE
                paint.color = Color.WHITE
                paint.strokeWidth = 6f
                canvas.drawRoundRect(centerRect, 22f, 22f, paint)

                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                paint.textSize = 38f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("VS", size / 2f, centerTop + 58f, paint)
            }
        }

        // 4. Offer Highlight Ribbon (if enabled)
        val offerY = qrBoxTop + qrBoxSize + 48f
        if (showOfferHighlight && business.activeOfferSummary.isNotBlank()) {
            val offerPillRect = RectF(140f, offerY - 32f, size - 140f, offerY + 22f)
            paint.style = Paint.Style.FILL
            paint.color = if (isDarkTheme) Color.parseColor("#2E7D32") else Color.parseColor("#E8F5E9")
            canvas.drawRoundRect(offerPillRect, 25f, 25f, paint)

            paint.color = if (isDarkTheme) Color.WHITE else Color.parseColor("#1B5E20")
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val cleanOffer = "⚡ ${business.activeOfferSummary.take(45)}"
            canvas.drawText(cleanOffer, size / 2f, offerY + 6f, paint)
        }

        // 5. Call To Action Text
        val ctaY = if (showOfferHighlight && business.activeOfferSummary.isNotBlank()) offerY + 62f else qrBoxTop + qrBoxSize + 55f
        paint.color = theme.textColor
        paint.textSize = 29f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER

        val lines = splitTextToLines(customCta, 46)
        lines.forEachIndexed { i, line ->
            canvas.drawText(line, size / 2f, ctaY + (i * 38f), paint)
        }

        // 6. NFC Symbol & App Store Footer Badges
        val footerY = size - 75f

        if (showNfcBadge) {
            paint.color = theme.accentColor
            paint.textSize = 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("📱 TAP NFC CONTACTLESS OR SCAN QR", size / 2f, footerY - 48f, paint)
        }

        val badgePillLeft = RectF(160f, footerY - 26f, (size / 2f) - 15f, footerY + 28f)
        val badgePillRight = RectF((size / 2f) + 15f, footerY - 26f, size - 160f, footerY + 28f)

        paint.style = Paint.Style.FILL
        paint.color = if (isDarkTheme) Color.parseColor("#2A2A3C") else Color.parseColor("#1A1A1A")
        canvas.drawRoundRect(badgePillLeft, 14f, 14f, paint)
        canvas.drawRoundRect(badgePillRight, 14f, 14f, paint)

        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(" App Store", badgePillLeft.centerX(), badgePillLeft.centerY() + 7f, paint)
        canvas.drawText("▶ Google Play", badgePillRight.centerX(), badgePillRight.centerY() + 7f, paint)

        // UID Footnote
        paint.color = if (isDarkTheme) Color.parseColor("#78909C") else Color.parseColor("#9E9E9E")
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("VibeSync Verified ID: ${business.id.uppercase()} • 4x4\" Printable Decal", size / 2f, size - 16f, paint)

        return bitmap
    }

    private fun splitTextToLines(text: String, maxCharsPerLine: Int): List<String> {
        if (text.length <= maxCharsPerLine) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if ((currentLine + " " + word).trim().length <= maxCharsPerLine) {
                currentLine = (currentLine + " " + word).trim()
            } else {
                if (currentLine.isNotBlank()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotBlank()) lines.add(currentLine)
        return lines.take(2)
    }
}
