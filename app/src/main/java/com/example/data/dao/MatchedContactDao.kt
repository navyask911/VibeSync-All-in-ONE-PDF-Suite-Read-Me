package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MatchedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchedContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchedContacts(contacts: List<MatchedContactEntity>)

    @Query("SELECT * FROM matched_contacts ORDER BY isOnVibeSync DESC, contactName ASC")
    fun getAllMatchedContactsFlow(): Flow<List<MatchedContactEntity>>

    @Query("SELECT * FROM matched_contacts ORDER BY isOnVibeSync DESC, contactName ASC")
    suspend fun getAllMatchedContacts(): List<MatchedContactEntity>

    @Query("SELECT * FROM matched_contacts WHERE phoneHash IN (:hashes)")
    suspend fun getContactsByHashes(hashes: List<String>): List<MatchedContactEntity>

    @Query("DELETE FROM matched_contacts")
    suspend fun deleteAll()
}
