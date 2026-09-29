package com.example.util.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages Google Sign-In with isolated App Folder (DRIVE_APPDATA) permissions.
 * Scope.DRIVE_APPDATA ensures that backup files are hidden from the user's regular Drive space
 * and can only be accessed by this specific application.
 */
object GoogleDriveAuthHolder {

    fun getSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            // Scope.DRIVE_APPDATA provides secure, sandboxed access to hidden appDataFolder
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
    }

    fun getClient(context: Context): GoogleSignInClient {
        return GoogleSignIn.getClient(context.applicationContext, getSignInOptions())
    }

    fun getSignInIntent(context: Context): Intent {
        return getClient(context).signInIntent
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context.applicationContext)
        return if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
            account
        } else {
            account
        }
    }

    suspend fun getSilentSignInAccount(context: Context): GoogleSignInAccount? = withContext(Dispatchers.IO) {
        val client = getClient(context)
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context.applicationContext)
        
        if (lastAccount != null && GoogleSignIn.hasPermissions(lastAccount, Scope(DriveScopes.DRIVE_APPDATA))) {
            return@withContext lastAccount
        }

        try {
            val task = client.silentSignIn()
            if (task.isSuccessful) {
                task.result
            } else {
                com.google.android.gms.tasks.Tasks.await(task)
            }
        } catch (e: Exception) {
            android.util.Log.w("GoogleDriveAuthHolder", "Silent sign-in failed: ${e.message}")
            lastAccount
        }
    }
}
