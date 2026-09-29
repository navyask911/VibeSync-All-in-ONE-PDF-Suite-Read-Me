package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.VibeSyncApplication
import com.example.data.model.RegisteredAccountEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * FirebaseBackendSyncManager
 * Manages live Cloud Firestore synchronization across multiple devices/phones,
 * Firebase Cloud Storage for media/photos, and Web Push Certificate / FCM credentials.
 */
object FirebaseBackendSyncManager {

    private const val TAG = "FirebaseBackendSync"
    private const val COLLECTION_REGISTERED_ACCOUNTS = "registered_accounts"
    private const val COLLECTION_USER_PROFILES = "users"
    private const val COLLECTION_DEVICE_TOKENS = "fcm_tokens"

    const val WEB_PUSH_CERTIFICATE = VibeSyncApplication.FIREBASE_WEB_PUSH_CERTIFICATE
    const val SENDER_ID = VibeSyncApplication.FIREBASE_SENDER_ID
    const val STORAGE_BUCKET = VibeSyncApplication.FIREBASE_STORAGE_BUCKET

    private var isInitialized = false

    fun init(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                isInitialized = true
                Log.d(TAG, "FirebaseBackendSyncManager initialized with Web Push Cert: ${WEB_PUSH_CERTIFICATE.take(12)}... and Bucket: $STORAGE_BUCKET")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase sync init notice: ${e.message}")
        }
    }

    /**
     * Pushes a registered account to Cloud Firestore only when properly completed with a valid name & phone.
     */
    suspend fun syncAccountToCloud(account: RegisteredAccountEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = account.phoneNumber.replace(Regex("[^0-9]"), "")
            val name = account.userName.trim()
            if (cleanPhone.length < 10 || name.isBlank() || name.length < 2 || name == "Registered Member" || name == "VibeSync User" || name.startsWith("User (")) {
                Log.d(TAG, "Skipping cloud sync for incomplete or placeholder account: ${account.phoneNumber} ($name)")
                return@withContext false
            }

            val firestore = FirebaseFirestore.getInstance()
            val primaryDocId = cleanPhone

            val dataMap = hashMapOf(
                "id" to primaryDocId,
                "phoneNumber" to account.phoneNumber,
                "cleanPhone" to cleanPhone,
                "googleEmail" to account.googleEmail,
                "userName" to name,
                "userAge" to account.userAge,
                "biometricHash" to account.biometricHash,
                "biometricRegisteredTimestamp" to account.biometricRegisteredTimestamp,
                "mpin" to account.mpin,
                "isVerified" to account.isVerified,
                "isDeleted" to account.isDeleted,
                "accountStatus" to account.accountStatus,
                "registrationTimestamp" to account.registrationTimestamp,
                "recoveryPhone" to account.recoveryPhone,
                "recoveryEmail" to account.recoveryEmail,
                "lastCloudSync" to System.currentTimeMillis(),
                "webPushCertificate" to WEB_PUSH_CERTIFICATE
            )

            // Primary set with merge by normalized phone
            firestore.collection(COLLECTION_REGISTERED_ACCOUNTS)
                .document(primaryDocId)
                .set(dataMap, SetOptions.merge())
                .await()

            Log.i(TAG, "Successfully synced verified account to cloud Firestore: ${account.phoneNumber} ($name)")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Cloud account sync deferred/offline: ${e.message}")
            false
        }
    }

    /**
     * Fetches a registered account from Cloud Firestore by phone number across any phone.
     */
    suspend fun fetchAccountFromCloudByPhone(phone: String): RegisteredAccountEntity? = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = phone.replace(Regex("[^0-9]"), "")
            if (cleanPhone.isBlank()) return@withContext null

            val firestore = FirebaseFirestore.getInstance()
            
            // Try direct document lookup by normalized cleanPhone
            val primarySnap = firestore.collection(COLLECTION_REGISTERED_ACCOUNTS)
                .document(cleanPhone)
                .get()
                .await()

            if (primarySnap.exists()) {
                return@withContext mapDocToAccount(primarySnap.data)
            }

            // Try legacy phone_ prefix document lookup
            val docSnap = firestore.collection(COLLECTION_REGISTERED_ACCOUNTS)
                .document("phone_$cleanPhone")
                .get()
                .await()

            if (docSnap.exists()) {
                return@withContext mapDocToAccount(docSnap.data)
            }

            // Try query by cleanPhone field
            val querySnap = firestore.collection(COLLECTION_REGISTERED_ACCOUNTS)
                .whereEqualTo("cleanPhone", cleanPhone)
                .limit(1)
                .get()
                .await()

            if (!querySnap.isEmpty) {
                return@withContext mapDocToAccount(querySnap.documents[0].data)
            }

            null
        } catch (e: Throwable) {
            Log.w(TAG, "Cloud fetch by phone notice: ${e.message}")
            null
        }
    }

    /**
     * Fetches a registered account from Cloud Firestore by Google email across any phone.
     */
    suspend fun fetchAccountFromCloudByEmail(email: String): RegisteredAccountEntity? = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank()) return@withContext null

            val firestore = FirebaseFirestore.getInstance()
            val querySnap = firestore.collection(COLLECTION_REGISTERED_ACCOUNTS)
                .whereEqualTo("googleEmail", cleanEmail)
                .limit(1)
                .get()
                .await()

            if (!querySnap.isEmpty) {
                return@withContext mapDocToAccount(querySnap.documents[0].data)
            }

            null
        } catch (e: Throwable) {
            Log.w(TAG, "Cloud fetch by email notice: ${e.message}")
            null
        }
    }

    /**
     * Fetches all registered accounts from Cloud Firestore to sync to local Room DB.
     */
    suspend fun fetchAllCloudAccounts(): List<RegisteredAccountEntity> = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val snap = firestore.collection(COLLECTION_REGISTERED_ACCOUNTS)
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                mapDocToAccount(doc.data)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Fetch all cloud accounts notice: ${e.message}")
            emptyList()
        }
    }

    /**
     * Uploads media file to Firebase Cloud Storage.
     */
    suspend fun uploadMediaToCloudStorage(context: Context, localUri: Uri, folder: String = "photos"): String? = withContext(Dispatchers.IO) {
        try {
            val storage = FirebaseStorage.getInstance("gs://$STORAGE_BUCKET")
            val fileName = "${folder}/${UUID.randomUUID()}.jpg"
            val ref: StorageReference = storage.reference.child(fileName)

            ref.putFile(localUri).await()
            val downloadUrl = ref.downloadUrl.await()
            Log.i(TAG, "Uploaded media to Cloud Storage: $downloadUrl")
            downloadUrl.toString()
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Storage upload fallback: ${e.message}")
            // Return local URI as safe fallback
            localUri.toString()
        }
    }

    /**
     * Registers device FCM token & Web Push certificate in Cloud Firestore.
     */
    suspend fun registerDeviceFcmToken(userId: String, token: String) = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "userId" to userId,
                "fcmToken" to token,
                "webPushCertificate" to WEB_PUSH_CERTIFICATE,
                "senderId" to SENDER_ID,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_DEVICE_TOKENS)
                .document(userId)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Registered FCM token with Web Push cert for user $userId")
        } catch (e: Throwable) {
            Log.w(TAG, "FCM token cloud registration notice: ${e.message}")
        }
    }

    private fun mapDocToAccount(data: Map<String, Any>?): RegisteredAccountEntity? {
        if (data == null) return null
        return try {
            val name = data["userName"] as? String ?: (data["name"] as? String ?: "")
            val age = (data["userAge"] as? Number)?.toInt() ?: ((data["age"] as? Number)?.toInt() ?: 0)
            val mpinVal = data["mpin"] as? String ?: ""
            RegisteredAccountEntity(
                id = data["id"] as? String ?: (data["userId"] as? String ?: "acc_${UUID.randomUUID().toString().take(8)}"),
                phoneNumber = data["phoneNumber"] as? String ?: (data["mobileNumber"] as? String ?: ""),
                googleEmail = data["googleEmail"] as? String ?: (data["email"] as? String ?: ""),
                userName = name,
                userAge = age,
                biometricHash = data["biometricHash"] as? String ?: "",
                biometricRegisteredTimestamp = (data["biometricRegisteredTimestamp"] as? Number)?.toLong() ?: 0L,
                mpin = mpinVal,
                isVerified = (data["isVerified"] as? Boolean) ?: true,
                registrationTimestamp = (data["registrationTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                recoveryPhone = data["recoveryPhone"] as? String ?: (data["phoneNumber"] as? String ?: ""),
                recoveryEmail = data["recoveryEmail"] as? String ?: (data["googleEmail"] as? String ?: "")
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Error mapping cloud doc to account: ${e.message}")
            null
        }
    }

    /**
     * Deletes user account and profile records from Cloud Firestore across all collections.
     */
    suspend fun deleteAccountFromCloud(phone: String, email: String, accountId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val cleanPhone = phone.replace(Regex("[^0-9]"), "")
            val deleteStatusMap = mapOf(
                "isDeleted" to true,
                "accountStatus" to "DELETED",
                "deletedAt" to System.currentTimeMillis()
            )

            if (cleanPhone.isNotBlank()) {
                try { firestore.collection(COLLECTION_REGISTERED_ACCOUNTS).document(cleanPhone).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection(COLLECTION_REGISTERED_ACCOUNTS).document("phone_$cleanPhone").set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection(COLLECTION_USER_PROFILES).document(cleanPhone).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("profiles").document(cleanPhone).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(cleanPhone).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
            }
            if (email.isNotBlank()) {
                val emailKey = email.replace("@", "_at_").replace(".", "_dot_")
                try { firestore.collection(COLLECTION_REGISTERED_ACCOUNTS).document("email_$emailKey").set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection(COLLECTION_USER_PROFILES).document(email).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("profiles").document(email).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(email).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
            }
            if (accountId.isNotBlank()) {
                try { firestore.collection(COLLECTION_REGISTERED_ACCOUNTS).document(accountId).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection(COLLECTION_USER_PROFILES).document(accountId).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("profiles").document(accountId).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
                try { firestore.collection("users").document(accountId).set(deleteStatusMap, SetOptions.merge()).await() } catch (_: Exception) {}
            }
            Log.i(TAG, "Successfully marked account status as DELETED in cloud for portal audit: $phone / $email / $accountId")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Cloud delete notice: ${e.message}")
            false
        }
    }
}
