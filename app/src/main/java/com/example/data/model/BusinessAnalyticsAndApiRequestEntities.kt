package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room SQLite entity for pure on-device business analytics.
 * Zero Supabase network egress and zero cloud database weight.
 */
@Entity(tableName = "local_business_analytics")
data class LocalBusinessAnalyticsEntity(
    @PrimaryKey val business_id: String = "",
    val views_count: Int = 0,
    val visits_count: Int = 0,
    val navigations_count: Int = 0,
    val inquiries_count: Int = 0,
    val updated_at: Long = System.currentTimeMillis()
)

/**
 * Timestamped interaction events for time-range filtering (Today, This Week, This Month, All Time)
 * stored strictly in local SQLite.
 */
@Entity(tableName = "local_business_analytics_events")
data class LocalBusinessAnalyticsEventEntity(
    @PrimaryKey val id: String = "",
    val businessId: String = "",
    val eventType: String = "", // "VIEW", "VISIT", "NAVIGATION", "INQUIRY"
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Business API Credential Request entity stored in Room DB.
 * API credentials and webhook tokens are never exposed in frontend Compose screens.
 * Admin approval is required on a need basis.
 */
@Entity(tableName = "api_credential_requests")
data class ApiCredentialRequestEntity(
    @PrimaryKey val id: String = "",
    val businessId: String = "",
    val businessName: String = "",
    val ownerUserId: String = "current_user",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val intendedUseCase: String = "",
    val requestedIntegrationType: String = "MESSENGER_WEBHOOK", // "MESSENGER_WEBHOOK", "POS_ORDER_SYNC", "BOOKING_INTEGRATION"
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val assignedApiKey: String = "",
    val assignedWebhookEndpoint: String = "",
    val adminNotes: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long = 0L
)
