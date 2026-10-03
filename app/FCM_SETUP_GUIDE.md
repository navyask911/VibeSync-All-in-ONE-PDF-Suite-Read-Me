# VibeSync: Free Firebase Cloud Messaging (FCM) Setup Guide

This guide walks you through deploying and utilizing **100% free push notifications** for VibeSync using Firebase Cloud Messaging (FCM) and Cloud Functions.

---

## 1. Why Firebase Cloud Messaging is Free
* **Zero Per-Message Cost:** Firebase Cloud Messaging (FCM) is completely free on both the Spark and Blaze plans with no limit on the number of messages sent to Android devices.
* **Cloud Functions Free Tier:** The Firebase Blaze plan includes **2,000,000 Cloud Function invocations per month for free**, which is more than enough for thousands of daily chat messages.

---

## 2. Prerequisites & Console Setup

### Step A: Download and Place `google-services.json`
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Select your Firebase project (or create one named `VibeSync`).
3. Click the gear icon ⚙️ > **Project settings**.
4. In the **Your apps** section, click **Add app** > **Android**.
   - Package name: `com.aistudio.vibesync.*` (or your app's `applicationId` in `app/build.gradle.kts`).
   - App nickname: `VibeSync`.
5. Download `google-services.json`.
6. Place `google-services.json` into the `app/` directory of this project:
   ```bash
   /app/app/google-services.json
   ```

### Step B: Enable Cloud Firestore
1. In Firebase Console, go to **Build** > **Firestore Database**.
2. Click **Create database**.
3. Select a location close to your users (e.g., `asia-south1` or `us-central1`).
4. Start in **Production mode** (or test mode). The repository's `firestore.rules` file handles security rules.

### Step C: Verify Cloud Messaging API (V1)
1. In Firebase Console, go to **Project settings** > **Cloud Messaging**.
2. Ensure **Firebase Cloud Messaging API (V1)** is **Enabled**. If not, click the three dots and enable it in Google Cloud Console.

---

## 3. Deploying the Cloud Functions

The Cloud Function code is already created in the `functions/` directory:
- `functions/index.js` (listens to `chats/{matchId}/messages/{messageId}` and sends FCM high-priority payloads)
- `functions/package.json`
- `firebase.json`
- `firestore.rules`

### To Deploy:
1. Install Firebase CLI (if not already installed):
   ```bash
   npm install -g firebase-tools
   ```
2. Log in to Firebase:
   ```bash
   firebase login
   ```
3. Associate your project:
   ```bash
   firebase use <your-firebase-project-id>
   ```
4. Deploy the functions and Firestore rules:
   ```bash
   firebase deploy --only functions,firestore
   ```

---

## 4. How the Flow Works Under the Hood

```text
[Device A (Sender)]
      │
      ├─ 1. Inserts message locally in Room DB (Single Tick ✓)
      ├─ 2. Writes message to Firestore: chats/{matchId}/messages/{messageId}
      │
      ▼
[Firebase Cloud Function: onChatMessageCreated]
      │
      ├─ 3. Detects new message document
      ├─ 4. Looks up recipient FCM token in: fcm_tokens/{receiverPhone}
      ├─ 5. Dispatches FCM HTTP v1 payload: type = "NEW_E2EE_MESSAGE"
      │
      ▼
[Device B (Recipient AppFirebaseMessagingService)]
      │
      ├─ 6. Receives high-priority data payload in background
      ├─ 7. Decrypts ciphertext via Google Tink HybridDecrypt
      ├─ 8. Saves plaintext to local Room DB
      ├─ 9. Fires delivery ACK (UPDATE is_delivered = true) -> Double Grey Ticks (✓✓)
      └─ 10. Displays system heads-up notification (if not currently inside active chat)
```

---

## 5. Testing & Verification

1. Launch VibeSync on Device B and grant Notification permission when prompted.
2. Verify Device B's FCM token is stored in Firestore under the `fcm_tokens` collection.
3. On Device A, send a message to Device B.
4. Check Firebase Console > Functions > Logs:
   - You should see: `Successfully dispatched free FCM push message ... to ...`
5. Device B will receive the push notification and automatically execute the delivery ACK, changing Device A's checkmark to double ticks (✓✓).
