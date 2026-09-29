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
}
