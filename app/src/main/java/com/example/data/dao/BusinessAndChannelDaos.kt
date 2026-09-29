package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BusinessEntity
import com.example.data.model.BusinessPostEntity
import com.example.data.model.BusinessReviewEntity
import com.example.data.model.ChannelBroadcastEntity
import com.example.data.model.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses ORDER BY distanceKm ASC")
    fun getAllBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE isFollowed = 1 ORDER BY distanceKm ASC")
    fun getFollowedBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE isUserCreated = 1 ORDER BY createdAt DESC")
    fun getMyCreatedBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    fun getBusinessById(id: String): Flow<BusinessEntity?>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessByIdSync(id: String): BusinessEntity?

    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinesses(businesses: List<BusinessEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity)

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("UPDATE businesses SET isFollowed = :isFollowed, followerCount = followerCount + :delta WHERE id = :id")
    suspend fun updateFollowStatus(id: String, isFollowed: Boolean, delta: Int)

    // Posts & Offers
    @Query("SELECT * FROM business_posts WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun getPostsForBusiness(businessId: String): Flow<List<BusinessPostEntity>>

    @Query("SELECT * FROM business_posts ORDER BY timestamp DESC")
    fun getAllBusinessPosts(): Flow<List<BusinessPostEntity>>

    @Query("SELECT COUNT(*) FROM business_posts")
    suspend fun getPostsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<BusinessPostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: BusinessPostEntity)

    @Query("UPDATE business_posts SET isLiked = :isLiked, likesCount = likesCount + :delta WHERE id = :postId")
    suspend fun togglePostLike(postId: String, isLiked: Boolean, delta: Int)

    @Query("UPDATE businesses SET walletPoints = :newPoints WHERE id = :id")
    suspend fun updateWalletPoints(id: String, newPoints: Int)

    @Query("UPDATE businesses SET verificationTier = :tier, isVerified = 1 WHERE id = :id")
    suspend fun updateVerificationTier(id: String, tier: String)

    @Query("UPDATE businesses SET whatsappApiEnabled = :enabled, whatsappWabaId = :wabaId, whatsappBusinessNumber = :phone, whatsappApiKey = :apiKey WHERE id = :id")
    suspend fun updateWhatsAppApiConfig(id: String, enabled: Boolean, wabaId: String, phone: String, apiKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: BusinessReviewEntity)

    @Query("SELECT * FROM business_reviews WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getReviewsForBusiness(businessId: String): Flow<List<BusinessReviewEntity>>

    @Query("UPDATE businesses SET rating = :newRating, reviewCount = :newCount WHERE id = :id")
    suspend fun updateBusinessRating(id: String, newRating: Double, newCount: Int)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY followerCount DESC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFollowed = 1 ORDER BY followerCount DESC")
    fun getFollowedChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    fun getChannelById(id: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelByIdSync(id: String): ChannelEntity?

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("UPDATE channels SET isFollowed = :isFollowed, followerCount = followerCount + :delta WHERE id = :channelId")
    suspend fun updateFollowStatus(channelId: String, isFollowed: Boolean, delta: Int)

    // Broadcasts
    @Query("SELECT * FROM channel_broadcasts WHERE channelId = :channelId ORDER BY timestamp DESC")
    fun getBroadcastsForChannel(channelId: String): Flow<List<ChannelBroadcastEntity>>

    @Query("SELECT COUNT(*) FROM channel_broadcasts")
    suspend fun getBroadcastsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcasts(broadcasts: List<ChannelBroadcastEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcast(broadcast: ChannelBroadcastEntity)

    @Query("UPDATE channel_broadcasts SET isReacted = :isReacted, reactionsCount = reactionsCount + :delta WHERE id = :broadcastId")
    suspend fun toggleReaction(broadcastId: String, isReacted: Boolean, delta: Int)
}
