package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BlockEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.FriendshipRequestEntity
import com.example.data.model.MatchEntity
import com.example.data.model.ProfileEntity
import com.example.data.model.RegisteredAccountEntity
import com.example.data.model.ReportEntity
import com.example.data.model.StatusStoryEntity
import com.example.data.model.SwipeEntity
import com.example.data.model.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfilesSync(): List<ProfileEntity>

    @Query("""
        SELECT * FROM profiles 
        WHERE isBanned = 0 AND isDeleted = 0 AND id NOT IN (SELECT profileId FROM swipes)
        ORDER BY likedMe DESC, id ASC
    """)
    fun getCandidateProfiles(): Flow<List<ProfileEntity>>

    @Query("""
        SELECT * FROM profiles 
        WHERE isBanned = 0 AND isDeleted = 0 AND likedMe = 1 AND id NOT IN (SELECT profileId FROM matches)
        ORDER BY isSuperLikedMe DESC, id ASC
    """)
    fun getLikedMeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY isBanned DESC, isFlaggedSpam DESC, trustScore ASC")
    fun getAllProfilesForAdmin(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfileById(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileByIdSync(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getProfileByPhone(phone: String): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET isBanned = :isBanned, moderationNote = :note WHERE id = :id")
    suspend fun updateBanStatus(id: String, isBanned: Boolean, note: String)

    @Query("UPDATE profiles SET isFlaggedSpam = :isFlagged, trustScore = :trustScore WHERE id = :id")
    suspend fun updateSpamStatus(id: String, isFlagged: Boolean, trustScore: Int)

    @Query("UPDATE profiles SET isVerified = :isVerified, isRealFaceVerified = :isFaceVerified WHERE id = :id")
    suspend fun updateVerification(id: String, isVerified: Boolean, isFaceVerified: Boolean)

    @Query("UPDATE profiles SET spamReportCount = spamReportCount + 1, trustScore = :trustScore, isFlaggedSpam = :isFlagged, moderationNote = :moderationNote WHERE id = :id")
    suspend fun recordReportOnProfile(id: String, trustScore: Int, isFlagged: Boolean, moderationNote: String)

    @Query("UPDATE profiles SET breakupCount = breakupCount + 1 WHERE id = :id")
    suspend fun incrementBreakupCount(id: String)

    @Query("UPDATE profiles SET friendsCount = friendsCount + 1 WHERE id = :id")
    suspend fun incrementFriendsCount(id: String)

    @Query("UPDATE profiles SET friendIds = :friendIds, friendsCount = :friendsCount WHERE id = :id")
    suspend fun updateFriendsData(id: String, friendIds: String, friendsCount: Int)

    @Query("SELECT * FROM profiles WHERE id IN (:ids)")
    fun getProfilesByIds(ids: List<String>): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id IN (:ids)")
    suspend fun getProfilesByIdsSync(ids: List<String>): List<ProfileEntity>

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("DELETE FROM profiles WHERE id LIKE 'p_%' OR id LIKE 'seed_%' OR id LIKE 'test_%' OR id LIKE 'demo_%'")
    suspend fun clearTestProfiles()

    @Query("""
        DELETE FROM profiles 
        WHERE id = 'null' OR LOWER(id) = 'null' OR TRIM(id) = ''
           OR name = 'null' OR LOWER(name) = 'null' OR LOWER(name) = 'null null' OR LOWER(name) = 'null • online' OR TRIM(name) = ''
           OR phoneNumber = 'null' OR LOWER(phoneNumber) = 'null' OR TRIM(phoneNumber) = ''
    """)
    suspend fun purgeGhostNullProfiles()

    @Query("DELETE FROM profiles")
    suspend fun clearProfiles()
}

@Dao
interface SwipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwipe(swipe: SwipeEntity)

    @Query("SELECT * FROM swipes ORDER BY timestamp DESC")
    fun getAllSwipes(): Flow<List<SwipeEntity>>

    @Query("SELECT * FROM swipes ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSwipe(): SwipeEntity?

    @Query("DELETE FROM swipes WHERE profileId = :profileId")
    suspend fun deleteSwipe(profileId: String)

    @Query("DELETE FROM swipes")
    suspend fun clearAllSwipes()
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY lastMessageTime DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches ORDER BY lastMessageTime DESC")
    suspend fun getAllMatchesSync(): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE matchId = :matchId LIMIT 1")
    fun getMatchById(matchId: String): Flow<MatchEntity?>

    @Query("SELECT * FROM matches WHERE matchId = :matchId LIMIT 1")
    suspend fun getMatchByIdSync(matchId: String): MatchEntity?

    @Query("SELECT * FROM matches WHERE profileId = :profileId LIMIT 1")
    suspend fun getMatchByProfileId(profileId: String): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Query("UPDATE matches SET lastMessage = :text, lastMessageTime = :timestamp, hasUnread = :hasUnread WHERE matchId = :matchId")
    suspend fun updateLastMessage(matchId: String, text: String, timestamp: Long, hasUnread: Boolean)

    @Query("UPDATE matches SET hasUnread = 0 WHERE matchId = :matchId")
    suspend fun markAsRead(matchId: String)

    @Query("UPDATE matches SET status = :status WHERE matchId = :matchId")
    suspend fun updateMatchStatus(matchId: String, status: String)

    @Query("UPDATE matches SET relationshipStatus = :relStatus, isDatingMatch = :isDating WHERE matchId = :matchId")
    suspend fun updateRelationshipStatus(matchId: String, relStatus: String, isDating: Boolean)

    @Query("DELETE FROM matches WHERE matchId = :matchId")
    suspend fun deleteMatch(matchId: String)

    @Query("DELETE FROM matches WHERE profileId LIKE 'p_%' OR matchId LIKE 'match_p_%' OR profileId LIKE 'seed_%' OR profileId LIKE 'test_%' OR profileId LIKE 'demo_%'")
    suspend fun clearTestMatches()

    @Query("DELETE FROM matches WHERE matchId = 'null' OR LOWER(matchId) = 'null' OR profileId = 'null' OR LOWER(profileId) = 'null' OR lastMessage = 'null' OR LOWER(lastMessage) = 'null' OR TRIM(matchId) = '' OR TRIM(profileId) = '' OR profileId = 'current_user'")
    suspend fun purgeGhostNullMatches()

    @Query("DELETE FROM matches")
    suspend fun clearAllMatches()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSync(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE matchId = :matchId ORDER BY timestamp ASC")
    fun getMessagesForMatch(matchId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE matchId = :matchId ORDER BY timestamp ASC")
    suspend fun getMessagesForMatchSync(matchId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?

    @Query("SELECT COUNT(*) FROM chat_messages WHERE messageId = :messageId")
    suspend fun hasMessage(messageId: String): Int

    @Query("DELETE FROM chat_messages WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM chat_messages WHERE matchId = :matchId")
    suspend fun clearMessagesForMatch(matchId: String)

    @Query("DELETE FROM chat_messages WHERE matchId LIKE 'match_p_%' OR senderId LIKE 'p_%' OR matchId LIKE 'match_seed_%' OR matchId LIKE 'match_test_%'")
    suspend fun clearTestMessages()

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages WHERE matchId = 'null' OR LOWER(matchId) = 'null' OR senderId = 'null' OR LOWER(senderId) = 'null' OR text = 'null' OR LOWER(text) = 'null' OR TRIM(matchId) = '' OR TRIM(senderId) = ''")
    suspend fun purgeGhostNullMessages()

    @Query("DELETE FROM chat_messages")
    suspend fun nukeTableOnLogout()
}

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getPreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferencesSync(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(preferences: UserPreferencesEntity)
}

@Dao
interface RegisteredAccountDao {
    @Query("SELECT * FROM registered_accounts")
    fun getAllAccounts(): Flow<List<RegisteredAccountEntity>>

    @Query("SELECT * FROM registered_accounts")
    suspend fun getAllAccountsSync(): List<RegisteredAccountEntity>

    @Query("SELECT * FROM registered_accounts WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getAccountByPhone(phoneNumber: String): RegisteredAccountEntity?

    @Query("SELECT * FROM registered_accounts WHERE googleEmail = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): RegisteredAccountEntity?

    @Query("SELECT * FROM registered_accounts WHERE biometricHash = :hash AND biometricHash != '' LIMIT 1")
    suspend fun getAccountByBiometricHash(hash: String): RegisteredAccountEntity?

    @Query("SELECT * FROM registered_accounts LIMIT 1")
    suspend fun getPrimaryAccount(): RegisteredAccountEntity?

    @Query("SELECT COUNT(*) FROM registered_accounts")
    suspend fun getAccountsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: RegisteredAccountEntity)

    @Query("DELETE FROM registered_accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Query("DELETE FROM registered_accounts WHERE phoneNumber = :phoneNumber")
    suspend fun deleteAccountByPhone(phoneNumber: String)

    @Query("DELETE FROM registered_accounts WHERE googleEmail = :email")
    suspend fun deleteAccountByEmail(email: String)

    @Query("DELETE FROM registered_accounts WHERE id LIKE 'acc_%' OR biometricHash = 'BIO_HUMAN_PRIMARY_911' OR phoneNumber = '+1 555-0199'")
    suspend fun deleteTestAccounts()

    @Query("DELETE FROM registered_accounts")
    suspend fun clearAllAccounts()
}

@Dao
interface StatusStoryDao {
    @Query("SELECT * FROM status_stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<StatusStoryEntity>>

    @Query("SELECT * FROM status_stories WHERE isMyStatus = 1 ORDER BY timestamp DESC")
    fun getMyStories(): Flow<List<StatusStoryEntity>>

    @Query("SELECT * FROM status_stories WHERE isMyStatus = 0 ORDER BY timestamp DESC")
    fun getFriendsStories(): Flow<List<StatusStoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StatusStoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StatusStoryEntity>)

    @Query("UPDATE status_stories SET isViewed = 1, viewsCount = viewsCount + 1 WHERE id = :storyId")
    suspend fun markStoryViewed(storyId: String)

    @Query("UPDATE status_stories SET likesCount = CASE WHEN isLikedByMe = 1 THEN MAX(0, likesCount - 1) ELSE likesCount + 1 END, isLikedByMe = CASE WHEN isLikedByMe = 1 THEN 0 ELSE 1 END WHERE id = :storyId")
    suspend fun toggleLikeStory(storyId: String)

    @Query("SELECT * FROM status_stories WHERE id = :storyId LIMIT 1")
    suspend fun getStoryById(storyId: String): StatusStoryEntity?

    @Query("DELETE FROM status_stories WHERE id = :storyId")
    suspend fun deleteStory(storyId: String)

    @Query("DELETE FROM status_stories WHERE userId LIKE 'p_%' OR id LIKE 'story_%' OR userId LIKE 'seed_%' OR userId LIKE 'test_%'")
    suspend fun clearTestStories()

    @Query("DELETE FROM status_stories")
    suspend fun clearAllStories()
}

@Dao
interface FriendshipRequestDao {
    @Query("SELECT * FROM friendship_requests ORDER BY timestamp DESC")
    fun getAllRequestsFlow(): Flow<List<FriendshipRequestEntity>>

    @Query("SELECT * FROM friendship_requests ORDER BY timestamp DESC")
    suspend fun getAllRequestsSync(): List<FriendshipRequestEntity>

    @Query("SELECT * FROM friendship_requests WHERE senderId = :userId AND status = 'PENDING' ORDER BY timestamp ASC")
    fun getPendingOutgoingRequestsForUser(userId: String = "current_user"): Flow<List<FriendshipRequestEntity>>

    @Query("SELECT * FROM friendship_requests WHERE senderId = :userId AND status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingOutgoingRequestsSync(userId: String = "current_user"): List<FriendshipRequestEntity>

    @Query("SELECT COUNT(*) FROM friendship_requests WHERE senderId = :userId AND status = 'PENDING'")
    suspend fun getPendingOutgoingCountSync(userId: String = "current_user"): Int

    @Query("SELECT * FROM friendship_requests WHERE receiverId = :userId AND status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingRequestsForUser(userId: String = "current_user"): Flow<List<FriendshipRequestEntity>>

    @Query("SELECT * FROM friendship_requests WHERE (senderId = :userId OR receiverId = :userId) AND status = 'ACCEPTED' ORDER BY acceptedTimestamp DESC")
    fun getAcceptedFriendships(userId: String = "current_user"): Flow<List<FriendshipRequestEntity>>

    @Query("SELECT * FROM friendship_requests WHERE (senderId = :userId OR receiverId = :userId) AND status = 'ACCEPTED' ORDER BY acceptedTimestamp DESC")
    suspend fun getAcceptedFriendshipsSync(userId: String = "current_user"): List<FriendshipRequestEntity>

    @Query("SELECT COUNT(*) FROM friendship_requests WHERE (senderId = :userId OR receiverId = :userId) AND status = 'ACCEPTED'")
    fun getAcceptedFriendsCount(userId: String = "current_user"): Flow<Int>

    @Query("SELECT COUNT(*) FROM friendship_requests WHERE (senderId = :userId OR receiverId = :userId) AND status = 'ACCEPTED'")
    suspend fun getAcceptedFriendsCountSync(userId: String = "current_user"): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: FriendshipRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(requests: List<FriendshipRequestEntity>)

    @Update
    suspend fun updateRequest(request: FriendshipRequestEntity)

    @Query("UPDATE friendship_requests SET status = 'ACCEPTED', acceptedTimestamp = :acceptedTime WHERE id = :requestId")
    suspend fun acceptRequest(requestId: String, acceptedTime: Long = System.currentTimeMillis())

    @Query("UPDATE friendship_requests SET status = 'DECLINED' WHERE id = :requestId")
    suspend fun declineRequest(requestId: String)

    @Query("DELETE FROM friendship_requests WHERE id = :requestId")
    suspend fun deleteRequest(requestId: String)

    @Query("SELECT * FROM friendship_requests WHERE id = :requestId")
    suspend fun getRequestById(requestId: String): FriendshipRequestEntity?

    @Query("SELECT * FROM friendship_requests WHERE ((senderId = :userA AND receiverId = :userB) OR (senderId = :userB AND receiverId = :userA)) LIMIT 1")
    suspend fun findExistingFriendship(userA: String, userB: String): FriendshipRequestEntity?

    @Query("DELETE FROM friendship_requests WHERE senderId LIKE 'p_%' OR receiverId LIKE 'p_%' OR id LIKE 'req_%' OR senderId LIKE 'test_%' OR receiverId LIKE 'test_%'")
    suspend fun clearTestFriendships()

    @Query("DELETE FROM friendship_requests")
    suspend fun clearAllFriendships()
}

@Dao
interface UserContactDao {
    @Query("SELECT * FROM user_contacts WHERE userId = :userId ORDER BY contactName ASC")
    fun getContactsForUser(userId: String = "current_user"): kotlinx.coroutines.flow.Flow<List<com.example.data.model.UserContactEntity>>

    @Query("SELECT * FROM user_contacts ORDER BY contactName ASC")
    suspend fun getAllContactsSync(): List<com.example.data.model.UserContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<com.example.data.model.UserContactEntity>)

    @Query("DELETE FROM user_contacts WHERE userId = :userId")
    suspend fun clearContactsForUser(userId: String = "current_user")

    @Query("DELETE FROM user_contacts")
    suspend fun clearAllContacts()
}

@Dao
interface BlockDao {
    @Query("SELECT * FROM blocked_users")
    fun getAllBlockedUsers(): kotlinx.coroutines.flow.Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocked_users")
    suspend fun getAllBlockedUsersSync(): List<BlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: BlockEntity)

    @Query("DELETE FROM blocked_users WHERE profileId = :profileId")
    suspend fun unblockUser(profileId: String)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reported_users ORDER BY reportedAt DESC")
    fun getAllReports(): kotlinx.coroutines.flow.Flow<List<ReportEntity>>

    @Query("SELECT * FROM reported_users ORDER BY reportedAt DESC")
    suspend fun getAllReportsSync(): List<ReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE reported_users SET status = :status WHERE reportId = :reportId")
    suspend fun updateReportStatus(reportId: String, status: String)

    @Query("DELETE FROM reported_users WHERE reportId = :reportId")
    suspend fun deleteReport(reportId: String)
}


