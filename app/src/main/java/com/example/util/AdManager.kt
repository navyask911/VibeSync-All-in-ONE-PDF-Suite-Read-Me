package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class AdPlacement(val displayName: String) {
    SWIPES("Swipe Cards Deck"),
    STORIES("User Stories"),
    BOTH("Both (Swipes & Stories)")
}

enum class AdActionType(val displayName: String) {
    BUSINESS_PROFILE("Open Business Profile"),
    WEBSITE_URL("Visit External Website"),
    DIRECT_CHAT("Start Direct Chat")
}

data class AdCampaign(
    val id: String = "ad_${UUID.randomUUID().toString().take(8)}",
    val campaignName: String,
    val businessName: String,
    val businessId: String = "",
    val headline: String,
    val description: String,
    val mediaType: String = "IMAGE", // "IMAGE" or "VIDEO"
    val bannerImageUrl: String = "",
    val videoUrl: String = "",
    val videoDurationSeconds: Int = 15, // Max 30s
    val placement: String = "BOTH", // "SWIPES", "STORIES", "BOTH"
    val actionType: String = "BUSINESS_PROFILE", // "BUSINESS_PROFILE", "WEBSITE_URL", "DIRECT_CHAT"
    val ctaText: String = "Claim Offer ✨",
    val targetLinkUrl: String = "https://vibesync.app/ad",
    val targetBusinessId: String = "",
    val targetCity: String = "All Cities",
    val targetRadiusKm: Int = 25,
    val maxViewCountLimit: Int = 5000,
    val currentViewCount: Int = 0,
    val swipesImpressions: Int = 0,
    val storiesImpressions: Int = 0,
    val clickCount: Int = 0,
    val contactEmail: String = "partner@vibesync.app",
    val budgetAmount: String = "₹750 INR",
    val paidAmountNum: Double = 750.0,
    val cpmRate: Double = 150.0, // ₹150 per 1,000 views
    val paymentId: String = "VS_TXN_${System.currentTimeMillis().toString().takeLast(8)}",
    val paymentStatus: String = "COMPLETED", // "COMPLETED", "PENDING_PAYMENT", "REFUNDED"
    val paymentMethod: String = "UPI (VibeSync Direct / Instant)", // "UPI", "CARD", "WALLET_POINTS", "RAZORPAY"
    val submitterUserId: String = "current_user",
    val durationType: String = "DAILY_24H", // "BLITZ_15", "BLITZ_30", "BLITZ_45", "BLITZ_60", "DAILY_24H", "WEEKLY_7D", "MONTHLY_30D"
    val durationExpiresAt: Long = System.currentTimeMillis() + (24 * 3600 * 1000),
    val isBlitz: Boolean = false,
    val status: String = "APPROVED", // "PENDING", "APPROVED", "REJECTED", "PAUSED", "COMPLETED"
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

data class RazorpayConfig(
    val keyId: String = "vs_live_merchant_98kL",
    val keySecret: String = "vs_sec_9a87d6f5e4c3b2a1",
    val merchantName: String = "VibeSync Business Media Pvt Ltd",
    val currency: String = "INR",
    val isLiveMode: Boolean = true,
    val autoWebhookUrl: String = "https://api.vibesync.app/v1/payments/vibesync-webhook",
    val isGatewayConfigured: Boolean = true
)

object AdManager {
    // Initial rich sample campaigns ready for Swipes & Stories injection
    private val initialCampaigns = listOf(
        AdCampaign(
            id = "ad_roastery_01",
            campaignName = "Artisan Cafe First Date Voucher",
            businessName = "The Roastery Lounge",
            businessId = "biz_001",
            headline = "Buy 1 Get 1 Free First Date Coffee ☕",
            description = "Enjoy cozy couch vibes, soft jazz, and artisan espresso. Show your VibeSync badge for 50% off sweet pastries!",
            mediaType = "IMAGE",
            bannerImageUrl = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80",
            placement = "BOTH",
            actionType = "BUSINESS_PROFILE",
            ctaText = "Claim Coffee Voucher 🎁",
            targetLinkUrl = "https://vibesync.app/partner/cafe",
            targetCity = "All Cities",
            targetRadiusKm = 25,
            maxViewCountLimit = 5000,
            currentViewCount = 1420,
            swipesImpressions = 890,
            storiesImpressions = 530,
            clickCount = 142,
            contactEmail = "partner@roastery.com",
            budgetAmount = "₹750 INR",
            paidAmountNum = 750.0,
            cpmRate = 150.0,
            status = "APPROVED"
        ),
        AdCampaign(
            id = "ad_festival_02",
            campaignName = "Sunset Music Festival 2026",
            businessName = "LiveNation Indie Events",
            businessId = "biz_002",
            headline = "Pair Up for VIP Festival Passes 🎟️",
            description = "Experience 3 stages of live indie electronic vibes! Match with fellow music lovers and get 2-for-1 pass discounts.",
            mediaType = "VIDEO",
            videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-concert-crowd-raising-hands-4148-large.mp4",
            videoDurationSeconds = 15,
            bannerImageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=800&q=80",
            placement = "BOTH",
            actionType = "WEBSITE_URL",
            ctaText = "Get 2-for-1 Tickets 🎶",
            targetLinkUrl = "https://vibesync.app/partner/festival",
            targetCity = "All Cities",
            targetRadiusKm = 50,
            maxViewCountLimit = 12000,
            currentViewCount = 3890,
            swipesImpressions = 2400,
            storiesImpressions = 1490,
            clickCount = 388,
            contactEmail = "events@livenation.com",
            budgetAmount = "₹2,400 INR",
            paidAmountNum = 2400.0,
            cpmRate = 200.0,
            status = "APPROVED"
        ),
        AdCampaign(
            id = "ad_gym_03",
            campaignName = "FitLife Gym Couple Pass",
            businessName = "FitLife Wellness Club",
            businessId = "biz_003",
            headline = "Free 7-Day Gym & Spa Guest Pass 🏋️",
            description = "Find workout partners nearby. Free sauna access, smoothie bar vouchers, and 1-on-1 personal training consultation.",
            mediaType = "IMAGE",
            bannerImageUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=800&q=80",
            placement = "SWIPES",
            actionType = "DIRECT_CHAT",
            ctaText = "Claim Free Guest Pass 🥤",
            targetLinkUrl = "https://vibesync.app/partner/fitlife",
            targetCity = "All Cities",
            targetRadiusKm = 15,
            maxViewCountLimit = 8000,
            currentViewCount = 2100,
            swipesImpressions = 2100,
            storiesImpressions = 0,
            clickCount = 189,
            contactEmail = "promo@fitlife.com",
            budgetAmount = "₹1,200 INR",
            paidAmountNum = 1200.0,
            cpmRate = 150.0,
            status = "APPROVED"
        ),
        AdCampaign(
            id = "ad_blitz_rooftop_04",
            campaignName = "Skyline Lounge Happy Hour Blitz",
            businessName = "Skyline Rooftop Bar & Grill",
            businessId = "biz_004",
            headline = "🔥 Blitz Deal: 40% Off Cocktails Next 60 Mins!",
            description = "Live DJ on deck! Walk in with your date right now and get complimentary appetizer nachos & 40% off artisan drinks.",
            mediaType = "IMAGE",
            bannerImageUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80",
            placement = "STORIES",
            actionType = "BUSINESS_PROFILE",
            ctaText = "Show Blitz Pass at Door 🍸",
            targetLinkUrl = "https://vibesync.app/partner/skyline",
            targetCity = "All Cities",
            targetRadiusKm = 10,
            maxViewCountLimit = 3000,
            currentViewCount = 980,
            swipesImpressions = 0,
            storiesImpressions = 980,
            clickCount = 165,
            contactEmail = "skyline@vibesync.app",
            budgetAmount = "₹600 INR",
            paidAmountNum = 600.0,
            cpmRate = 200.0,
            durationType = "BLITZ_60",
            isBlitz = true,
            status = "APPROVED"
        )
    )

    private val _campaigns = MutableStateFlow<List<AdCampaign>>(initialCampaigns)
    val campaigns: StateFlow<List<AdCampaign>> = _campaigns.asStateFlow()

    private val _razorpayConfig = MutableStateFlow(RazorpayConfig())
    val razorpayConfig: StateFlow<RazorpayConfig> = _razorpayConfig.asStateFlow()

    fun updateRazorpayConfig(config: RazorpayConfig) {
        _razorpayConfig.value = config
    }

    fun submitCampaign(campaign: AdCampaign) {
        _campaigns.value = listOf(campaign.copy(status = "APPROVED")) + _campaigns.value
    }

    fun approveCampaign(id: String) {
        _campaigns.value = _campaigns.value.map {
            if (it.id == id) it.copy(status = "APPROVED") else it
        }
    }

    fun rejectCampaign(id: String) {
        _campaigns.value = _campaigns.value.map {
            if (it.id == id) it.copy(status = "REJECTED") else it
        }
    }

    fun pauseCampaign(id: String) {
        _campaigns.value = _campaigns.value.map {
            if (it.id == id) {
                val newStatus = if (it.status == "APPROVED") "PAUSED" else "APPROVED"
                it.copy(status = newStatus)
            } else it
        }
    }

    fun deleteCampaign(id: String) {
        _campaigns.value = _campaigns.value.filter { it.id != id }
    }

    fun updateCampaignLimit(id: String, newMaxLimit: Int, targetCity: String) {
        _campaigns.value = _campaigns.value.map {
            if (it.id == id) it.copy(maxViewCountLimit = newMaxLimit, targetCity = targetCity) else it
        }
    }

    /**
     * Records an impression dynamically when ad enters visible viewport
     */
    fun recordAdImpression(id: String, placement: String = "SWIPES") {
        _campaigns.value = _campaigns.value.map { campaign ->
            if (campaign.id == id && campaign.status == "APPROVED") {
                val updatedCount = campaign.currentViewCount + 1
                val isExpired = updatedCount >= campaign.maxViewCountLimit
                val updatedSwipes = if (placement == "SWIPES") campaign.swipesImpressions + 1 else campaign.swipesImpressions
                val updatedStories = if (placement == "STORIES") campaign.storiesImpressions + 1 else campaign.storiesImpressions
                campaign.copy(
                    currentViewCount = updatedCount,
                    swipesImpressions = updatedSwipes,
                    storiesImpressions = updatedStories,
                    status = if (isExpired) "COMPLETED" else campaign.status
                )
            } else campaign
        }
    }

    fun recordAdClick(id: String) {
        _campaigns.value = _campaigns.value.map { campaign ->
            if (campaign.id == id) {
                campaign.copy(clickCount = campaign.clickCount + 1)
            } else campaign
        }
    }

    /**
     * Active approved ads for Swipe deck (Filter by placement and remaining quota)
     */
    fun getActiveApprovedAds(city: String? = null, placement: String = "SWIPES"): List<AdCampaign> {
        val now = System.currentTimeMillis()
        return _campaigns.value.filter { campaign ->
            val isPlacementMatch = campaign.placement == "BOTH" || campaign.placement == placement
            val isNotExpired = !campaign.isBlitz || campaign.durationExpiresAt > now
            campaign.status == "APPROVED" &&
                    isPlacementMatch &&
                    isNotExpired &&
                    campaign.currentViewCount < campaign.maxViewCountLimit &&
                    (city.isNullOrBlank() || campaign.targetCity == "All Cities" || campaign.targetCity.contains(city, ignoreCase = true))
        }
    }

    // --- In-Feed Swipe Injections ---
    private var cardSwipeCounter = 0
    private var activeSwipeAd: AdCampaign? = null

    /**
     * Called whenever a user swipes a profile card.
     * Rule: Automatically insert 1 responsive ad card after every 5 profile swipes.
     */
    fun recordCardSwipe(city: String? = null): AdCampaign? {
        cardSwipeCounter++
        if (cardSwipeCounter >= 5) {
            cardSwipeCounter = 0
            val activeAds = getActiveApprovedAds(city, "SWIPES")
            if (activeAds.isNotEmpty()) {
                val selected = activeAds.random()
                recordAdImpression(selected.id, "SWIPES")
                activeSwipeAd = selected
                return selected
            }
        }
        return null
    }

    fun getAdForDisplay(): AdDisplayItem? {
        val ad = activeSwipeAd ?: return null
        return AdDisplayItem(
            id = ad.id,
            title = ad.headline,
            description = ad.description,
            sponsorName = ad.businessName,
            actionText = ad.ctaText,
            targetUrl = ad.targetLinkUrl,
            imageUrl = ad.bannerImageUrl,
            videoUrl = ad.videoUrl,
            mediaType = ad.mediaType,
            businessId = ad.businessId,
            actionType = ad.actionType
        )
    }

    fun getActiveSwipeAdCampaign(): AdCampaign? = activeSwipeAd

    fun dismissAd() {
        activeSwipeAd = null
    }

    // --- Story Viewer Ad Injections ---
    private var storiesViewCounter = 0

    /**
     * Called whenever a user views a story.
     * Rule: Automatically play 1 ad story after every 5 user stories viewed.
     */
    fun recordStoryView(city: String? = null): AdCampaign? {
        storiesViewCounter++
        if (storiesViewCounter >= 5) {
            storiesViewCounter = 0
            val activeStoryAds = getActiveApprovedAds(city, "STORIES")
            if (activeStoryAds.isNotEmpty()) {
                val selected = activeStoryAds.random()
                recordAdImpression(selected.id, "STORIES")
                return selected
            }
        }
        return null
    }

    fun getStoryAdImmediate(city: String? = null): AdCampaign? {
        val activeStoryAds = getActiveApprovedAds(city, "STORIES")
        return if (activeStoryAds.isNotEmpty()) {
            val selected = activeStoryAds.random()
            recordAdImpression(selected.id, "STORIES")
            selected
        } else null
    }

    // --- Standard Market CPM Pricing Engine ---
    // Base CPM: ₹150 / 1,000 views ($1.80 CPM)
    // Video CPM: ₹200 / 1,000 views ($2.40 CPM)
    // Blitz (<60m surge): ₹250 / 1,000 views ($3.00 CPM)
    // Hyperlocal (<15km): +10%
    fun calculateCpmCost(
        impressions: Int,
        mediaType: String = "IMAGE",
        isBlitz: Boolean = false,
        radiusKm: Int = 25
    ): Double {
        var baseCpm = if (mediaType == "VIDEO") 200.0 else 150.0
        if (isBlitz) baseCpm += 50.0
        if (radiusKm <= 15) baseCpm += 25.0 // Hyperlocal targeting surcharge

        return (impressions / 1000.0) * baseCpm
    }

    // Wallet points conversion: 1 INR = 20 Wallet Points (1 Point = ₹0.05)
    fun convertInrToWalletPoints(inrAmount: Double): Int {
        return (inrAmount * 20).toInt()
    }
}

data class AdDisplayItem(
    val id: String,
    val title: String,
    val description: String,
    val sponsorName: String,
    val actionText: String = "Learn More ✨",
    val targetUrl: String = "https://vibesync.app/ad",
    val imageUrl: String = "",
    val videoUrl: String = "",
    val mediaType: String = "IMAGE",
    val businessId: String = "",
    val actionType: String = "BUSINESS_PROFILE"
)
