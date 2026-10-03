package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.DatingDatabase
import com.example.data.repository.ContactRepository
import com.example.data.repository.SocialConnectRepository
import com.example.util.PhonebookHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhonebookSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val deviceContacts = PhonebookHelper.fetchDeviceContacts(applicationContext)
            if (deviceContacts.isEmpty()) {
                return@withContext Result.success()
            }

            val database = DatingDatabase.getDatabase(applicationContext)
            val contactRepository = ContactRepository(database)
            val socialConnectRepository = SocialConnectRepository(database)

            val matchedContacts = contactRepository.syncAndMatchPhonebookContacts(applicationContext, deviceContacts)
            if (matchedContacts.isNotEmpty()) {
                socialConnectRepository.processPhonebookContactsForMatchesAndNotifications(matchedContacts)
            }

            Result.success()
        }.getOrElse { e ->
            android.util.Log.w("PhonebookSyncWorker", "Safe catch during phonebook sync worker: ${e.message}")
            Result.success()
        }
    }
}
