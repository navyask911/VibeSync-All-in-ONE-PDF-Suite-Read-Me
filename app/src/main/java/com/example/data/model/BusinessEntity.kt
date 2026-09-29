package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VerificationTier(
    val id: String,
    val title: String,
    val annualFee: Int,
    val badgeLabel: String,
    val badgeEmoji: String,
    val priorityRank: Int
) {
    GOLD("GOLD", "Gold Verified Badge", 499, "Gold Verified", "⭐", 4),
    SILVER("SILVER", "Silver Verified Badge", 299, "Silver Verified", "🛡️", 3),
    BLUE_TICK("BLUE_TICK", "Blue Tick Verified", 199, "Blue Tick", "✓", 2),
    STANDARD("STANDARD", "Standard Listing", 0, "Unverified", "", 1);

    companion object {
        fun fromId(id: String): VerificationTier {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: STANDARD
        }
    }
}

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val tagline: String = "",
    val category: String = "Dining & Bistros",
    val description: String = "",
    val address: String = "",
    val city: String = "Bangalore",
    val latitude: Double = 12.9716,
    val longitude: Double = 77.5946,
    val distanceKm: Double = 1.2,
    val rating: Double = 4.8,
    val reviewCount: Int = 1240,
    val priceRange: String = "₹₹",
    val bannerUrl: String = "",
    val logoEmoji: String = "☕",
    val phoneNumber: String = "+91 98765 43210",
    val websiteUrl: String = "https://vibesync.app/biz",
    val socialHandle: String = "@vibesync_partner",
    val isVerified: Boolean = true,
    val isFollowed: Boolean = false,
    val followerCount: Int = 2450,
    val isUserCreated: Boolean = false,
    val ownerUserId: String = "system",
    val activeOfferSummary: String = "30% OFF First Date Combo",
    val verificationTier: String = "STANDARD", // "GOLD", "SILVER", "BLUE_TICK", "STANDARD"
    val walletPoints: Int = 500, // 5 points = ₹0.05
    val isListingPaid: Boolean = true,
    val listingPaymentTxnId: String = "",
    val photoGalleryJson: String = "", // Pipe-separated list of up to 3 photo URLs/URIs
    val whatsappApiEnabled: Boolean = false,
    val whatsappBusinessNumber: String = "",
    val whatsappWabaId: String = "",
    val whatsappApiKey: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

val BusinessEntity.tierEnum: VerificationTier
    get() = VerificationTier.fromId(verificationTier)

val BusinessEntity.tierPriorityRank: Int
    get() = tierEnum.priorityRank

val BusinessEntity.photosList: List<String>
    get() = if (photoGalleryJson.isNotBlank()) {
        photoGalleryJson.split("|").filter { it.isNotBlank() }
    } else if (bannerUrl.isNotBlank()) {
        listOf(bannerUrl)
    } else {
        listOf("https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80")
    }
