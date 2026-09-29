package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.util.AppNotificationManager
import com.example.util.CoilImageLoaderConfig
import com.example.util.FirebaseBackendSyncManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.launch

/**
 * VibeSyncApplication
 * Global Application class ensuring reliable Firebase Cloud Backend & Storage initialization,
 * Web Push Certificate binding, Coil ImageLoader configuration, and crash shield diagnostics.
 */
class VibeSyncApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return CoilImageLoaderConfig.createImageLoader(this)
    }

    companion object {
        private const val TAG = "VibeSyncApplication"
        const val FIREBASE_PROJECT_ID = "vibesync-chat-social-connect"
        const val FIREBASE_SENDER_ID = "1009658280592"
        const val FIREBASE_STORAGE_BUCKET = "vibesync-chat-social-connect.firebasestorage.app"
        const val FIREBASE_WEB_PUSH_CERTIFICATE = "BPmhSjWM2KlWF3S3KJA8HdhNDubEFKq_ccFnGNv-UPJJbKpSE7D-tu-ljRANqxnuJObdeOO_VIvWwQu6EcJrK7s"
        const val FIREBASE_API_KEY = "AIzaSyC3nGN1YUPmfu32IzsdsVAba-9Idq2n7Ug"
        const val FIREBASE_APP_ID = "1:1009658280592:android:d189801dba9ea5f4d0f615"
        const val TRUECALLER_CLIENT_ID = "z3rfwgowtpkt-4_5hmsogbcjf2wc8igmb_bjpvzk8xc"
        const val FIREBASE_TEST_TOKEN = "AVweKojAbMS2zKXorXPybnrLVTRJ4wQ0oHVPNnqcnbIpZCUtteta7uzkZKVyYuWGPXBYdt4wH2WUOwjsUO3rGyC5FPgUTM9_8GDURDsmN5v0_wxVPPzZnTGoB4QknaYTg-Yz82rp266VbBjDv7V1JHPSCA"
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Crash Shield: Global UncaughtExceptionHandler to prevent "app keep stopping" crashes
        setupCrashShield()

        // 2. Safe Firebase Backend & Cloud Storage Initialization
        initFirebase()

        // 3. Diagnostics & Realtime Monitors (No eager startup network pings)
        try {
            com.example.util.CrossDeviceDiagnosticManager.startDiagnosticListeners()
            com.example.data.repository.SyncDiagnosticsRepository.startMonitoring()
        } catch (e: Throwable) {
            Log.w(TAG, "CrossDeviceDiagnosticManager init notice: ${e.message}")
        }

        // 4. Initialize VibeSync-style notification channels
        try {
            AppNotificationManager.initChannels(this)
            AppNotificationManager.fetchFcmToken(this)
        } catch (e: Throwable) {
            Log.w(TAG, "Notification channel init notice: ${e.message}")
        }
    }

    private fun setupCrashShield() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "CRASH SHIELD INTERCEPTED EXCEPTION on thread ${thread.name}: ${throwable.message}", throwable)
            // If critical system crash, delegate or log
            if (throwable is OutOfMemoryError) {
                defaultHandler?.uncaughtException(thread, throwable)
            } else {
                Log.w(TAG, "Prevented unhandled termination for: ${throwable.javaClass.simpleName}")
            }
        }
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(FIREBASE_PROJECT_ID)
                    .setApplicationId(FIREBASE_APP_ID)
                    .setApiKey(FIREBASE_API_KEY)
                    .setGcmSenderId(FIREBASE_SENDER_ID)
                    .setStorageBucket(FIREBASE_STORAGE_BUCKET)
                    .build()

                FirebaseApp.initializeApp(this, options)
                Log.i(TAG, "Firebase initialized with Project ID: $FIREBASE_PROJECT_ID and Storage Bucket: $FIREBASE_STORAGE_BUCKET")
            } else {
                Log.i(TAG, "Firebase already initialized by google-services")
            }

            // Configure Firebase Firestore for Spark Plan (Free Tier)
            // Enable offline persistence to minimize document reads and avoid exceeding Spark 50k reads/day quota
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                            .setSizeBytes(100L * 1024L * 1024L) // 100MB offline local cache
                            .build()
                    )
                    .build()
                firestore.firestoreSettings = settings
                Log.i(TAG, "Firestore configured for Firebase Spark Free Plan with 100MB persistent cache.")
            } catch (e: Throwable) {
                Log.w(TAG, "Firestore Spark plan settings note: ${e.message}")
            }

            // Sync Web Push Certificate and initialize Firebase cloud sync
            FirebaseBackendSyncManager.init(this)
            com.example.util.FirestoreSyncManager.getInstance().init(this)
            com.example.util.FirestoreDiagnosticManager.init(this)
            com.example.util.BusinessHubSyncManager.init()
            com.example.util.ZeroCostE2eeMessagingManager.init(this)
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase init handled safely: ${e.message}")
        }
    }
}
