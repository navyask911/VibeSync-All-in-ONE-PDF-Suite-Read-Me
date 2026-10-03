package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ApiCredentialRequestEntity
import com.example.data.model.LocalBusinessAnalyticsEntity
import com.example.data.model.LocalBusinessAnalyticsEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalBusinessAnalyticsDao {
    @Query("SELECT * FROM local_business_analytics WHERE business_id = :businessId LIMIT 1")
    fun getAnalyticsByBusinessIdFlow(businessId: String): Flow<LocalBusinessAnalyticsEntity?>

    @Query("SELECT * FROM local_business_analytics WHERE business_id = :businessId LIMIT 1")
    suspend fun getAnalyticsByBusinessId(businessId: String): LocalBusinessAnalyticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAnalytics(analytics: LocalBusinessAnalyticsEntity)

    @Query("SELECT * FROM local_business_analytics")
    fun getAllAnalyticsFlow(): Flow<List<LocalBusinessAnalyticsEntity>>

    // Event Logging for Time-Range Filtering (Today, This Week, This Month, All Time)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: LocalBusinessAnalyticsEventEntity)

    @Query("SELECT COUNT(*) FROM local_business_analytics_events WHERE businessId = :businessId AND eventType = :type AND timestamp >= :sinceTimestamp")
    suspend fun countEventsSince(businessId: String, type: String, sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM local_business_analytics_events WHERE businessId = :businessId AND eventType = :type")
    suspend fun countAllEvents(businessId: String, type: String): Int

    @Query("DELETE FROM local_business_analytics_events WHERE businessId = :businessId")
    suspend fun clearEventsForBusiness(businessId: String)
}

@Dao
interface ApiCredentialRequestDao {
    @Query("SELECT * FROM api_credential_requests ORDER BY requestedAt DESC")
    fun getAllRequestsFlow(): Flow<List<ApiCredentialRequestEntity>>

    @Query("SELECT * FROM api_credential_requests WHERE businessId = :businessId ORDER BY requestedAt DESC")
    fun getRequestsByBusinessFlow(businessId: String): Flow<List<ApiCredentialRequestEntity>>

    @Query("SELECT * FROM api_credential_requests WHERE id = :requestId LIMIT 1")
    suspend fun getRequestById(requestId: String): ApiCredentialRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: ApiCredentialRequestEntity)

    @Update
    suspend fun updateRequest(request: ApiCredentialRequestEntity)

    @Query("UPDATE api_credential_requests SET status = :status, assignedApiKey = :apiKey, assignedWebhookEndpoint = :webhook, reviewedAt = :reviewedAt, adminNotes = :notes WHERE id = :requestId")
    suspend fun updateRequestApproval(requestId: String, status: String, apiKey: String, webhook: String, notes: String, reviewedAt: Long)

    @Query("DELETE FROM api_credential_requests WHERE id = :requestId")
    suspend fun deleteRequest(requestId: String)
}
