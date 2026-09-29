package com.example.util.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Dynamic scheduler for background WorkManager chat backups to Google Drive AppFolder.
 * Supports Daily (1 day), Weekly (7 days), and Bi-Weekly (15 days) intervals,
 * as well as Wi-Fi Only constraints and immediate on-demand backups.
 */
object BackupScheduler {
    const val UNIQUE_PERIODIC_WORK_NAME = "secure_chat_backup_job"
    const val UNIQUE_ONE_TIME_WORK_NAME = "secure_chat_backup_immediate"

    /**
     * Schedules periodic encrypted backup to Google Drive based on user preferences.
     * @param intervalDays 1 (Daily), 7 (Weekly), or 15 (Bi-Weekly). Pass <= 0 to cancel.
     * @param wifiOnly When true, requires an UNMETERED (Wi-Fi) connection.
     */
    fun scheduleBackup(context: Context, intervalDays: Int, wifiOnly: Boolean = false) {
        if (intervalDays <= 0) {
            cancelBackup(context)
            return
        }

        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            intervalDays.toLong(), TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            backupRequest
        )

        android.util.Log.i("BackupScheduler", "Enqueued backup schedule: every $intervalDays days (WiFi only: $wifiOnly)")
    }

    /**
     * Cancels any active periodic backup jobs.
     */
    fun cancelBackup(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        android.util.Log.i("BackupScheduler", "Cancelled periodic backup work.")
    }

    /**
     * Triggers an immediate one-time background backup job to Google Drive.
     */
    fun triggerImmediateBackup(context: Context, wifiOnly: Boolean = false) {
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )

        android.util.Log.i("BackupScheduler", "Triggered immediate one-time backup to Google Drive.")
    }
}
