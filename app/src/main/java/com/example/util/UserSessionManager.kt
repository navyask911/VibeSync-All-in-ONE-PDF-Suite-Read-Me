package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

object UserSessionManager {
    private const val TAG = "UserSessionManager"
    private const val PREFS_AUTH_SESSION = "vibesync_auth_session"

    fun clearStaleAuthSessions(context: Context) {
        try {
            Log.i(TAG, "Purging stale / corrupted auth sessions and pending tokens before OTP verification")
            val sp: SharedPreferences = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            sp.edit().clear().apply()

            // Also clear default shared preferences auth keys if any exist
            val defaultSp = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
            defaultSp.edit()
                .remove("pending_auth_token")
                .remove("stale_session_token")
                .remove("cached_otp_session")
                .remove("temp_login_state")
                .apply()

            // Sign out any lingering partial firebase credentials
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseAuth signOut notice: ${e.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error purging auth sessions: ${e.message}")
        }
    }

    fun sanitizeCorruptedStorageAndCache(context: Context) {
        try {
            Log.w(TAG, "Sanitizing corrupted local storage & app cache keys...")
            clearStaleAuthSessions(context)
            val defaultSp = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
            defaultSp.edit().clear().apply()

            // Delete corrupted database file if unreadable/locked to prevent cold-launch hangs
            try {
                val dbFile = context.getDatabasePath("dating_app.db")
                if (dbFile.exists()) {
                    context.deleteDatabase("dating_app.db")
                    Log.i(TAG, "Successfully purged corrupted dating_app.db database file.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error purging corrupted database file: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sanitizing corrupted storage: ${e.message}", e)
        }
    }

    fun saveVerifiedSession(context: Context, phoneNumber: String) {
        try {
            val publicKeyset = TinkCryptoManager.getMyPublicKeysetJson(context)
            val sp: SharedPreferences = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            sp.edit()
                .putString("verified_phone_number", phoneNumber)
                .putLong("verified_timestamp", System.currentTimeMillis())
                .putBoolean("is_authenticated", true)
                .apply()
            if (publicKeyset.isNotBlank()) {
                sp.edit().putString("public_identity_key", publicKeyset).apply()
            }
            Log.i(TAG, "Successfully saved verified session and Tink keyset for phone: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving verified session: ${e.message}", e)
        }
    }

    fun getVerifiedPhoneNumber(context: Context): String? {
        return try {
            val sp: SharedPreferences = context.getSharedPreferences(PREFS_AUTH_SESSION, Context.MODE_PRIVATE)
            sp.getString("verified_phone_number", null)
        } catch (e: Exception) {
            null
        }
    }
}
