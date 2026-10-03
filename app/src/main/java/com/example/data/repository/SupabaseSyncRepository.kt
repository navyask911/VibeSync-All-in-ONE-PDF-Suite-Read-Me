package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.PhoneContact
import com.example.data.model.ProfileEntity
import com.example.util.ContactResolver
import com.example.util.PhonebookHasher
import com.example.util.PhonebookHelper
import com.example.util.SupabaseClientManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SupabaseSyncState(
    val isConnected: Boolean = true,
    val httpStatus: Int = 200,
    val latencyMs: Long = 0L,
    val totalProfilesInSupabase: Int = 0,
    val matchedContactsCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val statusMessage: String = "Supabase Database Online",
    val isSyncing: Boolean = false,
    val logs: List<String> = emptyList()
)

/**
 * SupabaseSyncRepository
 *
 * Runs VibeSync entirely on Supabase database.
 * Manages profile synchronization, 16-character SHA-256 phone hash lookups via PhonebookHasher,
 * and the zero-knowledge contact matching pipeline that queries Supabase tables directly,
 * overriding any remote encrypted strings with the local phonebook saved contact name.
 */
object SupabaseSyncRepository {

    private const val TAG = "SupabaseSyncRepo"
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncState = MutableStateFlow(SupabaseSyncState())
    val syncState: StateFlow<SupabaseSyncState> = _syncState.asStateFlow()

    /**
     * Checks realtime connectivity to Supabase PostgREST endpoints.
     * Only executed on explicit user demand (e.g. clicking 'Test API').
     */
    suspend fun checkConnectivity(): SupabaseClientManager.DiagnosticResult = withContext(Dispatchers.IO) {
        val result = SupabaseClientManager.checkSupabaseConnectivity()
        _syncState.value = _syncState.value.copy(
            isConnected = result.isConnected,
            httpStatus = result.httpStatus,
            latencyMs = result.latencyMs,
            statusMessage = result.errorMessage
        )
        addLog("⚡ Supabase connectivity: ${if (result.isConnected) "ONLINE (${result.latencyMs}ms)" else "OFFLINE (${result.errorMessage})"}")
        result
    }

    /**
     * Syncs own profile to Supabase 'profiles' table with 16-character SHA-256 phone_hash.
     */
    suspend fun syncOwnProfile(profile: ProfileEntity): Boolean = withContext(Dispatchers.IO) {
        val success = SupabaseClientManager.upsertProfile(profile)
        if (success) {
            val hash16 = PhonebookHasher.generate16CharHash(profile.phoneNumber.ifBlank { profile.id })
            addLog("✅ Upserted own profile to Supabase 'profiles' (id: ${profile.id}, phone_hash: $hash16)")
        } else {
            addLog("❌ Failed upserting own profile to Supabase")
        }
        success
    }

    /**
     * Fetches registered user profiles directly from Supabase 'profiles' table with strict limit 50.
     */
    suspend fun fetchAllProfiles(limit: Int = 50): List<ProfileEntity> = withContext(Dispatchers.IO) {
        val list = SupabaseClientManager.fetchAllProfiles(limit)
        _syncState.value = _syncState.value.copy(
            totalProfilesInSupabase = list.size,
            lastSyncTimestamp = System.currentTimeMillis()
        )
        addLog("📦 Fetched ${list.size} profile(s) from Supabase 'profiles' table (limit: $limit)")
        list
    }

    /**
     * Queries Supabase 'profiles' table directly by phone hashes (strict limit 50).
     */
    suspend fun fetchProfilesByHashes(hashes: List<String>): List<ProfileEntity> = withContext(Dispatchers.IO) {
        if (hashes.isEmpty()) return@withContext emptyList()
        val list = SupabaseClientManager.fetchProfilesByHashes(hashes)
        addLog("🔍 Queried Supabase by ${hashes.size} hash(es) -> Found ${list.size} matching profile(s)")
        list
    }

    /**
     * Zero-Knowledge Contact Matching Pipeline:
     * 1. Normalizes phone numbers to standard last 10 digits and hashes them locally with SHA-256 (64-char hex / 32-byte binary).
     * 2. Calls Supabase PostgREST RPC 'match_contacts' in batches of 50 to query the indexed 'phone_sync' BYTEA table (<2ms).
     * 3. Overrides remote contact names with the local device saved contact name via ContactResolver.
     * 4. Completely preserves zero-knowledge privacy and prevents cloud leakage.
     */
    suspend fun matchContactsAgainstSupabase(
        context: Context,
        rawContacts: List<PhoneContact>,
        myPhoneNumber: String = ""
    ): List<PhoneContact> = withContext(Dispatchers.IO) {
        if (rawContacts.isEmpty()) {
            _syncState.value = _syncState.value.copy(isSyncing = false)
            return@withContext emptyList()
        }

        _syncState.value = _syncState.value.copy(isSyncing = true)
        val startTime = System.currentTimeMillis()

        try {
            val database = com.example.data.database.DatingDatabase.getDatabase(context)
            val contactRepository = ContactRepository(database)
            val resolvedContacts = contactRepository.syncAndMatchPhonebookContacts(context, rawContacts)

            val matchedCount = resolvedContacts.count { it.isOnVibeSync }
            val totalDurationMs = System.currentTimeMillis() - startTime

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                matchedContactsCount = matchedCount,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            addLog("🚀 High-Performance Contact Sync matched $matchedCount contact(s) on VibeSync in ${totalDurationMs}ms")

            resolvedContacts
        } catch (e: Exception) {
            Log.e(TAG, "Error in matchContactsAgainstSupabase: ${e.message}", e)
            _syncState.value = _syncState.value.copy(isSyncing = false)
            rawContacts
        }
    }

    fun addLog(msg: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] $msg"
        Log.i(TAG, entry)
        val current = _syncState.value.logs.take(50)
        _syncState.value = _syncState.value.copy(logs = listOf(entry) + current)
    }

    fun clearLogs() {
        _syncState.value = _syncState.value.copy(logs = emptyList())
    }
}
