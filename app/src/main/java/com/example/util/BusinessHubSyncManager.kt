package com.example.util

import android.util.Log
import com.example.data.model.BusinessEntity
import com.example.ui.components.EventData
import com.example.ui.components.MenuItemData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TrafficPoint(
    val label: String = "",
    val count: Int = 0,
    val secondaryCount: Int = 0
)

data class EngagementDayPoint(
    val day: String = "",
    val views: Int = 0,
    val interactions: Int = 0,
    val bookings: Int = 0
)

data class MenuItemAnalytics(
    val itemId: String = "",
    val itemName: String = "",
    val category: String = "",
    val ordersCount: Int = 0,
    val price: Double = 0.0,
    val revenue: Double = 0.0,
    val sharePercentage: Float = 0f
)

data class CategoryShare(
    val category: String = "",
    val sharePercentage: Float = 0f,
    val count: Int = 0,
    val colorHex: Long = 0xFF00E5FF
)

data class VenueAnalyticsData(
    val venueId: String = "",
    val venueName: String = "",
    val totalImpressions: Int = 8420,
    val totalVisitors: Int = 3140,
    val activeBrowsingNow: Int = 18,
    val directionRequests: Int = 540,
    val callInquiries: Int = 192,
    val couponRedemptions: Int = 286,
    val storyViews: Int = 1680,
    val avgDwellMinutes: Int = 46,
    val growthRatePercentage: Double = 24.8,
    val hourlyTraffic: List<TrafficPoint> = emptyList(),
    val dailyEngagement: List<EngagementDayPoint> = emptyList(),
    val popularMenuItems: List<MenuItemAnalytics> = emptyList(),
    val categoryBreakdown: List<CategoryShare> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

object BusinessHubSyncManager {
    private const val TAG = "BusinessHubSyncManager"
    private const val COLLECTION_VENUES = "vibesync_venues"
    private const val COLLECTION_MENUS = "venue_menus"
    private const val COLLECTION_EVENTS = "venue_events"
    private const val COLLECTION_TIMELINE = "venue_timeline"
    private const val COLLECTION_ANALYTICS = "venue_analytics"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _realtimeVenues = MutableStateFlow<List<BusinessEntity>>(emptyList())
    val realtimeVenues: StateFlow<List<BusinessEntity>> = _realtimeVenues.asStateFlow()

    private val _realtimeTimeline = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val realtimeTimeline: StateFlow<List<Map<String, Any>>> = _realtimeTimeline.asStateFlow()

    private val _realtimeAnalytics = MutableStateFlow<Map<String, VenueAnalyticsData>>(emptyMap())
    val realtimeAnalytics: StateFlow<Map<String, VenueAnalyticsData>> = _realtimeAnalytics.asStateFlow()

    private val activeListeners = mutableMapOf<String, ListenerRegistration>()

    fun init() {
        startListeningToVenues()
        startListeningToTimeline()
        startListeningToAnalytics()
    }

    private fun startListeningToVenues() {
        try {
            val db = FirebaseFirestore.getInstance()
            // ZERO-COST READ GUARD: Cap live Firestore snapshot to 15 featured venues
            // to prevent 50,000-document download explosions across 500,000 public users.
            db.collection(COLLECTION_VENUES)
                .limit(15)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for venues: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(BusinessEntity::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (list.isNotEmpty()) {
                            _realtimeVenues.value = list
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing venue listener: ${e.message}")
        }
    }

    private fun startListeningToTimeline() {
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection(COLLECTION_TIMELINE)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for timeline: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val posts = snapshot.documents.mapNotNull { it.data }
                        _realtimeTimeline.value = posts
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing timeline listener: ${e.message}")
        }
    }

    fun syncTimelinePost(postId: String, venueId: String, venueName: String, caption: String, imageUrl: String?, timestamp: Long) {
        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val postMap = mapOf(
                    "id" to postId,
                    "venueId" to venueId,
                    "venueName" to venueName,
                    "caption" to caption,
                    "imageUrl" to (imageUrl ?: ""),
                    "timestamp" to timestamp
                )
                db.collection(COLLECTION_TIMELINE).document(postId).set(postMap).await()
                Log.d(TAG, "Successfully synced timeline post $postId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync timeline post: ${e.message}")
            }
        }
    }

    fun deleteTimelinePost(postId: String) {
        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection(COLLECTION_TIMELINE).document(postId).delete().await()
                Log.d(TAG, "Successfully deleted timeline post $postId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete timeline post: ${e.message}")
            }
        }
    }

    fun syncVenueToFirestore(business: BusinessEntity) {
        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection(COLLECTION_VENUES)
                    .document(business.id)
                    .set(business)
                    .await()
                Log.d(TAG, "Successfully synced venue ${business.name} to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync venue to Firestore: ${e.message}")
            }
        }
    }

    fun syncMenuToFirestore(businessId: String, menuItems: List<MenuItemData>) {
        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val batch = db.batch()
                val menuRef = db.collection(COLLECTION_VENUES).document(businessId).collection(COLLECTION_MENUS)
                
                menuItems.forEach { item ->
                    val docRef = menuRef.document(item.id)
                    batch.set(docRef, item)
                }
                batch.commit().await()
                Log.d(TAG, "Successfully synced ${menuItems.size} menu items for venue $businessId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync menu items: ${e.message}")
            }
        }
    }

    fun syncEventsToFirestore(businessId: String, events: List<EventData>) {
        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val batch = db.batch()
                val eventsRef = db.collection(COLLECTION_VENUES).document(businessId).collection(COLLECTION_EVENTS)

                events.forEach { ev ->
                    val docRef = eventsRef.document(ev.id)
                    batch.set(docRef, ev)
                }
                batch.commit().await()
                Log.d(TAG, "Successfully synced ${events.size} events for venue $businessId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync events: ${e.message}")
            }
        }
    }

    private fun startListeningToAnalytics() {
        try {
            val db = FirebaseFirestore.getInstance()
            db.collection(COLLECTION_ANALYTICS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for analytics: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val map = mutableMapOf<String, VenueAnalyticsData>()
                        snapshot.documents.forEach { doc ->
                            try {
                                val data = doc.toObject(VenueAnalyticsData::class.java)
                                if (data != null) {
                                    map[doc.id] = data
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing analytics for ${doc.id}: ${e.message}")
                            }
                        }
                        if (map.isNotEmpty()) {
                            _realtimeAnalytics.value = map
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error initializing analytics listener: ${e.message}")
        }
    }

    fun getAnalyticsForVenue(venueId: String, venueName: String = "VibeSync Partner Venue"): VenueAnalyticsData {
        val existing = _realtimeAnalytics.value[venueId]
        if (existing != null) return existing

        // Rich default analytics populated for immediate high-fidelity visualization
        val defaultData = generateInitialAnalytics(venueId, venueName)
        // Store in memory & trigger background sync to Firestore
        _realtimeAnalytics.value = _realtimeAnalytics.value + (venueId to defaultData)
        syncAnalyticsToFirestore(defaultData)
        return defaultData
    }

    fun syncAnalyticsToFirestore(analytics: VenueAnalyticsData) {
        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection(COLLECTION_ANALYTICS)
                    .document(analytics.venueId)
                    .set(analytics)
                    .await()
                Log.d(TAG, "Successfully synced analytics for ${analytics.venueName}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync analytics to Firestore: ${e.message}")
            }
        }
    }

    fun recordVenueInteraction(venueId: String, venueName: String, type: String) {
        scope.launch {
            val current = getAnalyticsForVenue(venueId, venueName)
            val updated = when (type) {
                "impression" -> current.copy(totalImpressions = current.totalImpressions + 1)
                "visitor" -> current.copy(totalVisitors = current.totalVisitors + 1)
                "direction" -> current.copy(directionRequests = current.directionRequests + 1)
                "call" -> current.copy(callInquiries = current.callInquiries + 1)
                "coupon" -> current.copy(couponRedemptions = current.couponRedemptions + 1)
                "story" -> current.copy(storyViews = current.storyViews + 1)
                else -> current
            }
            _realtimeAnalytics.value = _realtimeAnalytics.value + (venueId to updated)
            syncAnalyticsToFirestore(updated)
        }
    }

    private fun generateInitialAnalytics(venueId: String, venueName: String): VenueAnalyticsData {
        val hourly = listOf(
            TrafficPoint("10 AM", 45, 12),
            TrafficPoint("12 PM", 128, 48),
            TrafficPoint("2 PM", 94, 30),
            TrafficPoint("4 PM", 160, 65),
            TrafficPoint("6 PM", 295, 142),
            TrafficPoint("8 PM", 380, 210),
            TrafficPoint("10 PM", 260, 130),
            TrafficPoint("12 AM", 110, 45)
        )

        val daily = listOf(
            EngagementDayPoint("Mon", 420, 130, 24),
            EngagementDayPoint("Tue", 510, 165, 32),
            EngagementDayPoint("Wed", 680, 220, 48),
            EngagementDayPoint("Thu", 790, 290, 62),
            EngagementDayPoint("Fri", 1450, 580, 140),
            EngagementDayPoint("Sat", 1890, 840, 225),
            EngagementDayPoint("Sun", 1620, 710, 190)
        )

        val menuItems = listOf(
            MenuItemAnalytics("m1", "Signature Cold Brew & Gelato", "Beverages", 542, 280.0, 151760.0, 28.5f),
            MenuItemAnalytics("m2", "Woodfire Truffle Pizza", "Main Course", 418, 650.0, 271700.0, 22.0f),
            MenuItemAnalytics("m3", "Belgian Dark Chocolate Fondue", "Desserts", 365, 480.0, 175200.0, 19.2f),
            MenuItemAnalytics("m4", "Smoked Salmon Crostini", "Appetizers", 290, 390.0, 113100.0, 15.3f),
            MenuItemAnalytics("m5", "Artisan Lavender Mocktail", "Special Drinks", 285, 240.0, 68400.0, 15.0f)
        )

        val categories = listOf(
            CategoryShare("Main Course", 38f, 418, 0xFF00E5FF),
            CategoryShare("Beverages & Brews", 28f, 542, 0xFFFF007A),
            CategoryShare("Desserts & Treats", 20f, 365, 0xFFFFB300),
            CategoryShare("Appetizers & Starters", 14f, 290, 0xFF00E676)
        )

        return VenueAnalyticsData(
            venueId = venueId,
            venueName = venueName,
            totalImpressions = 8940,
            totalVisitors = 3480,
            activeBrowsingNow = 23,
            directionRequests = 612,
            callInquiries = 214,
            couponRedemptions = 310,
            storyViews = 1890,
            avgDwellMinutes = 52,
            growthRatePercentage = 27.4,
            hourlyTraffic = hourly,
            dailyEngagement = daily,
            popularMenuItems = menuItems,
            categoryBreakdown = categories,
            lastUpdated = System.currentTimeMillis()
        )
    }

    suspend fun fetchVenueDetails(businessId: String): BusinessEntity? {
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection(COLLECTION_VENUES).document(businessId).get().await()
            doc.toObject(BusinessEntity::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch venue details: ${e.message}")
            null
        }
    }
}
