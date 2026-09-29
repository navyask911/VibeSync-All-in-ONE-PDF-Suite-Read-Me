package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.DatingDatabase
import com.example.data.model.MatchedContactEntity
import com.example.data.model.PhoneContact
import com.example.data.model.ProfileEntity
import com.example.data.model.UserContactEntity
import com.example.util.ContactResolver
import com.example.util.PhonebookHasher
import com.example.util.PhonebookHelper
import com.example.util.SupabaseClientManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ContactRepository
 *
 * Implements Zero-Knowledge contact matching against Supabase `clean_phone` column:
 * 1. Computes 16-character SHA-256 phone hashes locally using PhonebookHasher.
 * 2. Queries Supabase in chunks of 30: `profiles?select=id,name,clean_phone&clean_phone=in.(hash1,hash2,...)`.
 * 3. Never uploads or queries raw phone numbers.
 * 4. Saves matched users directly to local device storage (Room DB) with `isOnVibeSync = true`.
 * 5. Zero contact lists or address books held on server.
 */
class ContactRepository(
    private val database: DatingDatabase
) {
    private val matchedContactDao = database.matchedContactDao()
    private val userContactDao = database.userContactDao()
    private val profileDao = database.profileDao()

    companion object {
        private const val TAG = "ContactRepository"
    }

    /**
     * Performs Zero-Knowledge contact matching for a given list of device contacts.
     */
    suspend fun syncAndMatchPhonebookContacts(
        context: Context,
        deviceContacts: List<PhoneContact>
    ): List<PhoneContact> = withContext(Dispatchers.IO) {
        if (deviceContacts.isEmpty()) return@withContext emptyList()

        try {
            // 1. For every contact, normalize the number and compute its 16-character SHA-256 hash using PhonebookHasher
            val hashToContactMap = mutableMapOf<String, PhoneContact>()
            val contactHashes = mutableListOf<String>()

            for (contact in deviceContacts) {
                if (contact.phoneNumber.isNotBlank()) {
                    val h16 = PhonebookHasher.generate16CharHash(contact.phoneNumber)
                    if (h16.isNotBlank()) {
                        contactHashes.add(h16)
                        hashToContactMap[h16] = contact
                    }
                    val allHashes = PhonebookHasher.getAllMatchHashes(contact.phoneNumber)
                    for (h in allHashes) {
                        contactHashes.add(h)
                        hashToContactMap[h] = contact
                    }
                }
            }

            val distinctHashes = contactHashes.filter { it.isNotBlank() }.distinct()
            if (distinctHashes.isEmpty()) {
                Log.i(TAG, "No valid hashes generated, returning device contacts without Supabase query")
                return@withContext deviceContacts
            }
            Log.i(TAG, "Generated ${distinctHashes.size} Zero-Knowledge 16-char SHA-256 hashes from ${deviceContacts.size} contacts")

            // 2. Chunk contact hashes into batches of 30 to prevent query/URL length errors
            val matchedProfilesMap = mutableMapOf<String, ProfileEntity>()

            distinctHashes.chunked(30).forEach { batch ->
                if (batch.isNotEmpty()) {
                    runCatching {
                        val batchProfiles = SupabaseClientManager.fetchProfilesByHashes(batch)
                        for (profile in batchProfiles) {
                            matchedProfilesMap[profile.id] = profile
                            if (profile.phoneNumber.isNotBlank()) {
                                matchedProfilesMap[profile.phoneNumber] = profile
                                val digits = profile.phoneNumber.filter { it.isDigit() }
                                if (digits.isNotBlank()) matchedProfilesMap[digits] = profile
                                val pHash16 = PhonebookHasher.generate16CharHash(profile.phoneNumber)
                                if (pHash16.isNotBlank()) matchedProfilesMap[pHash16] = profile
                                for (h in PhonebookHasher.getAllMatchHashes(profile.phoneNumber)) {
                                    matchedProfilesMap[h] = profile
                                }
                            }
                        }
                    }.onFailure { e ->
                        Log.w(TAG, "Batch hash query notice: ${e.message}")
                    }
                }
            }

            // 3. Save matches directly to Device Storage (Room DB)
            val currentProfiles = profileDao.getAllProfilesSync()
            val resolvedList = PhonebookHelper.matchPhoneContactsWithProfiles(deviceContacts, currentProfiles, matchedProfilesMap)

            val matchedEntities = mutableListOf<MatchedContactEntity>()
            val userContactEntities = mutableListOf<UserContactEntity>()

            for (contact in resolvedList) {
                val cleanHash = PhonebookHasher.generate16CharHash(contact.phoneNumber)
                if (contact.isOnVibeSync) {
                    matchedEntities.add(
                        MatchedContactEntity(
                            id = contact.id,
                            contactName = contact.name,
                            phoneNumber = contact.phoneNumber,
                            phoneHash = cleanHash,
                            vibeSyncUserId = contact.vibeSyncProfileId ?: contact.id,
                            isOnVibeSync = true,
                            matchedAt = System.currentTimeMillis(),
                            avatarEmoji = contact.avatarEmoji,
                            photoUrl = contact.photoUrl,
                            statusTagline = contact.statusTagline
                        )
                    )
                    // Also ensure profile exists in local profileDao
                    contact.vibeSyncUser?.let { prof ->
                        val localOverrideProfile = ContactResolver.matchChatSessionParticipant(context, prof)
                        profileDao.insertProfile(localOverrideProfile)
                    }
                }

                userContactEntities.add(
                    UserContactEntity(
                        id = contact.id,
                        userId = "current_user",
                        contactName = contact.name,
                        phoneNumber = contact.phoneNumber,
                        isOnVibeSync = contact.isOnVibeSync,
                        photoUrl = contact.photoUrl,
                        statusTagline = contact.statusTagline,
                        syncedToFirestore = true,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            if (matchedEntities.isNotEmpty()) {
                matchedContactDao.insertMatchedContacts(matchedEntities)
            }
            if (userContactEntities.isNotEmpty()) {
                userContactDao.insertContacts(userContactEntities)
            }

            Log.i(TAG, "Zero-Knowledge matching complete: ${matchedEntities.size} contacts on VibeSync saved to Room DB")
            return@withContext resolvedList
        } catch (e: Exception) {
            Log.e(TAG, "Error in Zero-Knowledge contact matching: ${e.message}", e)
            return@withContext deviceContacts
        }
    }
}
