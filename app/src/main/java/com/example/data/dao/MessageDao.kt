package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LocalMessage
import kotlinx.coroutines.flow.Flow

/**
 * MessageDao
 * Room DAO for LocalMessage queries supporting reactive Flow updates, insertion,
 * and complete instant local table wipe upon logout.
 */
@Dao
interface MessageDao {
    @Query("SELECT * FROM local_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<LocalMessage>>

    @Query("SELECT * FROM local_messages WHERE matchId = :matchId ORDER BY timestamp ASC")
    fun getMessagesForMatch(matchId: String): Flow<List<LocalMessage>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: LocalMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<LocalMessage>)

    @Query("DELETE FROM local_messages WHERE matchId = 'null' OR LOWER(matchId) = 'null' OR senderUid = 'null' OR LOWER(senderUid) = 'null' OR textContent = 'null' OR LOWER(textContent) = 'null' OR TRIM(matchId) = '' OR TRIM(senderUid) = ''")
    suspend fun purgeGhostNullLocalMessages()

    @Query("DELETE FROM local_messages")
    suspend fun nukeTableOnLogout()
}
