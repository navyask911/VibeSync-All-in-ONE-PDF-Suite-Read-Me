package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.BusinessEntity
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VenueReportExporter {
    private const val TAG = "VenueReportExporter"

    /**
     * Generates a CSV export of the venue's analytics and performance metrics.
     */
    fun exportCsvReport(context: Context, venue: BusinessEntity, analytics: VenueAnalyticsData): File? {
        return try {
            val exportDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeVenueName = venue.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val file = File(exportDir, "${safeVenueName}_PerformanceReport_${timestamp}.csv")

            FileWriter(file).use { writer ->
                // Header / Overview
                writer.append("=== VIBESYNC BUSINESS INTELLIGENCE REPORT ===\n")
                writer.append("Venue Name,${escapeCsv(venue.name)}\n")
                writer.append("Category,${escapeCsv(venue.category)}\n")
                writer.append("City & Address,${escapeCsv("${venue.address}, ${venue.city}")}\n")
                writer.append("Export Date,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")

                // KPI Summary
                writer.append("=== KEY PERFORMANCE METRICS ===\n")
                writer.append("Metric,Value\n")
                writer.append("Total Views (Impressions),${analytics.totalImpressions}\n")
                writer.append("Total Footfall Visitors,${analytics.totalVisitors}\n")
                writer.append("Live Browsing Users,${analytics.activeBrowsingNow}\n")
                writer.append("Map Navigation Requests,${analytics.directionRequests}\n")
                writer.append("Direct Call Inquiries,${analytics.callInquiries}\n")
                writer.append("Discounts & Coupons Claimed,${analytics.couponRedemptions}\n")
                writer.append("Story / Timeline Views,${analytics.storyViews}\n")
                writer.append("Average Dwell Time (Minutes),${analytics.avgDwellMinutes}\n")
                writer.append("Growth Rate Percentage,${analytics.growthRatePercentage}%\n\n")

                // Hourly Footfall Table
                writer.append("=== HOURLY TRAFFIC & FOOTFALL ===\n")
                writer.append("Time Slot,Total Visitors,Date Couples\n")
                analytics.hourlyTraffic.forEach { pt ->
                    writer.append("${escapeCsv(pt.label)},${pt.count},${pt.secondaryCount}\n")
                }
                writer.append("\n")

                // Weekly Engagement Table
                writer.append("=== WEEKLY ENGAGEMENT & CONVERSION ===\n")
                writer.append("Day,Profile Views,Actions Taken,Bookings\n")
                analytics.dailyEngagement.forEach { day ->
                    writer.append("${escapeCsv(day.day)},${day.views},${day.interactions},${day.bookings}\n")
                }
                writer.append("\n")

                // Menu Performance Table
                writer.append("=== TOP MENU ITEMS & REVENUE SHARE ===\n")
                writer.append("Item Name,Category,Price (INR),Orders Count,Revenue Generated (INR),Share Percentage\n")
                var totalRevenue = 0.0
                analytics.popularMenuItems.forEach { item ->
                    totalRevenue += item.revenue
                    writer.append("${escapeCsv(item.itemName)},${escapeCsv(item.category)},${item.price},${item.ordersCount},${item.revenue},${item.sharePercentage}%\n")
                }
                writer.append("TOTAL ESTIMATED REVENUE,,,,${totalRevenue},\n")
            }

            file
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting CSV: ${e.message}", e)
            null
        }
    }

    /**
     * Generates a beautifully formatted PDF document for offline records and printing.
     */
    fun exportPdfReport(context: Context, venue: BusinessEntity, analytics: VenueAnalyticsData): File? {
        val pdfDoc = PdfDocument()
        return try {
            val pageWidth = 595 // Standard A4 width in PostScript points (72 dpi)
            val pageHeight = 842 // Standard A4 height in PostScript points
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val formatter = NumberFormat.getNumberInstance(Locale.US)
            val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

            // 1. Top Header Banner (Dark Navy Theme)
            paint.color = AndroidColor.rgb(16, 20, 30)
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

            // Header Accent Line (Cyan)
            paint.color = AndroidColor.rgb(0, 229, 255)
            canvas.drawRect(0f, 92f, pageWidth.toFloat(), 95f, paint)

            // Header Texts
            paint.color = AndroidColor.WHITE
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("VIBESYNC BUSINESS REPORT", 30f, 40f, paint)

            paint.color = AndroidColor.rgb(0, 229, 255)
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("CONFIDENTIAL PERFORMANCE AUDIT", 30f, 56f, paint)

            paint.color = AndroidColor.rgb(180, 195, 210)
            paint.textSize = 9f
            paint.isFakeBoldText = false
            canvas.drawText("Generated: $dateStr", 30f, 74f, paint)

            // Venue Name & Info on Header Right
            paint.color = AndroidColor.WHITE
            paint.textSize = 14f
            paint.isFakeBoldText = true
            val venueTitle = venue.name
            val textWidth = paint.measureText(venueTitle)
            canvas.drawText(venueTitle, (pageWidth - 30f - textWidth), 40f, paint)

            paint.color = AndroidColor.rgb(180, 195, 210)
            paint.textSize = 9f
            paint.isFakeBoldText = false
            val subText = "${venue.category} • ${venue.city}"
            val subWidth = paint.measureText(subText)
            canvas.drawText(subText, (pageWidth - 30f - subWidth), 56f, paint)

            var currentY = 120f

            // 2. Executive KPI Cards Section (4 boxes)
            paint.color = AndroidColor.rgb(20, 30, 45)
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("EXECUTIVE PERFORMANCE SUMMARY", 30f, currentY, paint)
            currentY += 14f

            val boxWidth = (pageWidth - 60f - 30f) / 4f
            val boxHeight = 55f

            val kpis = listOf(
                Pair("Total Views", formatter.format(analytics.totalImpressions)),
                Pair("Footfall Visitors", formatter.format(analytics.totalVisitors)),
                Pair("Map Directions", "${analytics.directionRequests}"),
                Pair("Direct Leads", "${analytics.callInquiries + analytics.couponRedemptions}")
            )

            kpis.forEachIndexed { i, kpi ->
                val left = 30f + i * (boxWidth + 10f)
                val rect = RectF(left, currentY, left + boxWidth, currentY + boxHeight)

                // Box background
                paint.color = AndroidColor.rgb(245, 247, 250)
                canvas.drawRoundRect(rect, 6f, 6f, paint)

                // Box Border
                paint.color = AndroidColor.rgb(220, 226, 235)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(rect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                // Title
                paint.color = AndroidColor.rgb(100, 115, 130)
                paint.textSize = 8.5f
                paint.isFakeBoldText = false
                canvas.drawText(kpi.first, left + 8f, currentY + 18f, paint)

                // Value
                paint.color = AndroidColor.rgb(16, 20, 30)
                paint.textSize = 13f
                paint.isFakeBoldText = true
                canvas.drawText(kpi.second, left + 8f, currentY + 40f, paint)
            }

            currentY += boxHeight + 24f

            // 3. Hourly Traffic & Visitor Peaks Table
            paint.color = AndroidColor.rgb(20, 30, 45)
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("HOURLY TRAFFIC & VISITOR PEAKS", 30f, currentY, paint)
            currentY += 12f

            // Table Header
            paint.color = AndroidColor.rgb(230, 235, 245)
            canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 18f, paint)

            paint.color = AndroidColor.rgb(40, 50, 70)
            paint.textSize = 8.5f
            paint.isFakeBoldText = true
            canvas.drawText("Time Slot", 38f, currentY + 12f, paint)
            canvas.drawText("Active Visitors", 180f, currentY + 12f, paint)
            canvas.drawText("Date Couples (Target)", 340f, currentY + 12f, paint)
            currentY += 18f

            paint.isFakeBoldText = false
            paint.textSize = 8f
            analytics.hourlyTraffic.forEachIndexed { index, pt ->
                if (index % 2 == 1) {
                    paint.color = AndroidColor.rgb(250, 252, 255)
                    canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 16f, paint)
                }
                paint.color = AndroidColor.rgb(30, 40, 50)
                canvas.drawText(pt.label, 38f, currentY + 11f, paint)
                canvas.drawText("${pt.count} visitors", 180f, currentY + 11f, paint)
                canvas.drawText("${pt.secondaryCount} couples", 340f, currentY + 11f, paint)
                currentY += 16f
            }

            currentY += 20f

            // 4. Weekly Engagement Table
            paint.color = AndroidColor.rgb(20, 30, 45)
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("WEEKLY ENGAGEMENT & CONVERSION", 30f, currentY, paint)
            currentY += 12f

            // Table Header
            paint.color = AndroidColor.rgb(230, 235, 245)
            canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 18f, paint)

            paint.color = AndroidColor.rgb(40, 50, 70)
            paint.textSize = 8.5f
            paint.isFakeBoldText = true
            canvas.drawText("Day", 38f, currentY + 12f, paint)
            canvas.drawText("Profile Views", 140f, currentY + 12f, paint)
            canvas.drawText("Interactions / Leads", 260f, currentY + 12f, paint)
            canvas.drawText("Date Bookings", 400f, currentY + 12f, paint)
            currentY += 18f

            paint.isFakeBoldText = false
            paint.textSize = 8f
            analytics.dailyEngagement.forEachIndexed { index, day ->
                if (index % 2 == 1) {
                    paint.color = AndroidColor.rgb(250, 252, 255)
                    canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 16f, paint)
                }
                paint.color = AndroidColor.rgb(30, 40, 50)
                canvas.drawText(day.day, 38f, currentY + 11f, paint)
                canvas.drawText("${day.views}", 140f, currentY + 11f, paint)
                canvas.drawText("${day.interactions}", 260f, currentY + 11f, paint)
                canvas.drawText("${day.bookings} bookings", 400f, currentY + 11f, paint)
                currentY += 16f
            }

            currentY += 20f

            // 5. Popular Menu Items & Revenue Share Table
            paint.color = AndroidColor.rgb(20, 30, 45)
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("POPULAR DISHES & MENU REVENUE BREAKDOWN", 30f, currentY, paint)
            currentY += 12f

            // Table Header
            paint.color = AndroidColor.rgb(230, 235, 245)
            canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 18f, paint)

            paint.color = AndroidColor.rgb(40, 50, 70)
            paint.textSize = 8.5f
            paint.isFakeBoldText = true
            canvas.drawText("Item Name", 38f, currentY + 12f, paint)
            canvas.drawText("Category", 210f, currentY + 12f, paint)
            canvas.drawText("Price", 320f, currentY + 12f, paint)
            canvas.drawText("Orders", 380f, currentY + 12f, paint)
            canvas.drawText("Total Revenue (INR)", 450f, currentY + 12f, paint)
            currentY += 18f

            paint.isFakeBoldText = false
            paint.textSize = 8f
            var totalMenuRevenue = 0.0
            analytics.popularMenuItems.forEachIndexed { index, item ->
                totalMenuRevenue += item.revenue
                if (index % 2 == 1) {
                    paint.color = AndroidColor.rgb(250, 252, 255)
                    canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 16f, paint)
                }
                paint.color = AndroidColor.rgb(30, 40, 50)
                canvas.drawText(item.itemName, 38f, currentY + 11f, paint)
                canvas.drawText(item.category, 210f, currentY + 11f, paint)
                canvas.drawText("₹${item.price.toInt()}", 320f, currentY + 11f, paint)
                canvas.drawText("${item.ordersCount}", 380f, currentY + 11f, paint)
                canvas.drawText("₹${formatter.format(item.revenue.toInt())}", 450f, currentY + 11f, paint)
                currentY += 16f
            }

            // Total revenue row
            paint.color = AndroidColor.rgb(235, 245, 255)
            canvas.drawRect(30f, currentY, pageWidth - 30f, currentY + 18f, paint)
            paint.color = AndroidColor.rgb(10, 80, 160)
            paint.textSize = 8.5f
            paint.isFakeBoldText = true
            canvas.drawText("Total Estimated Menu Revenue", 38f, currentY + 12f, paint)
            canvas.drawText("₹${formatter.format(totalMenuRevenue.toInt())}", 450f, currentY + 12f, paint)

            // 6. Page Footer
            paint.color = AndroidColor.rgb(150, 160, 175)
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText("VibeSync Platform • Offline Verified Business Analytics Record", 30f, pageHeight - 20f, paint)
            canvas.drawText("Page 1 of 1", pageWidth - 70f, pageHeight - 20f, paint)

            pdfDoc.finishPage(page)

            // Save PDF to cache
            val exportDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeVenueName = venue.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val file = File(exportDir, "${safeVenueName}_AnalyticsReport_${timestamp}.pdf")

            FileOutputStream(file).use { out ->
                pdfDoc.writeTo(out)
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF report: ${e.message}", e)
            null
        } finally {
            pdfDoc.close()
        }
    }

    /**
     * Triggers the Android Native Share Sheet to export / share / save the generated file.
     */
    fun shareReportFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Here is the offline performance and analytics report for your venue.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Export / Save Performance Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share report file: ${e.message}", e)
            Toast.makeText(context, "Could not open share dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeCsv(value: String): String {
        val containsSpecialChars = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (containsSpecialChars) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
