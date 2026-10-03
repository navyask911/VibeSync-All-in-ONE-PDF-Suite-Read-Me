# Implementation Plan: Free Firebase Cloud Messaging (FCM) Integration for VibeSync

Configure and operationalize 100% free push notifications using Firebase Cloud Messaging (FCM) and Cloud Functions triggered by Firestore chat messages (`chats/{matchId}/messages/{messageId}`).

---

## 1. Architecture Overview & Free Tier Economics

* **Firebase Cloud Messaging (FCM) Pricing:**
  * FCM is **100% free and unlimited** across all Firebase plans (Spark and Blaze). Google charges zero message delivery fees for push notifications and data payloads sent to Android devices.
* **Delivery Flow:**
  1. **Message Dispatch:** Sender posts message to Firestore collection `chats/{matchId}/messages/{messageId}` with encrypted ciphertext and recipient phone.
  2. **Event Trigger:** A Firebase Cloud Function (`onMessageCreated`) listens to `chats/{matchId}/messages/{messageId}`.
  3. **Token Resolution:** The function resolves the recipient's phone/UID and fetches the target device registration token from the `fcm_tokens/{userId}` collection (persisted by `FirebaseBackendSyncManager`).
  4. **FCM Payload Dispatch:** The Cloud Function invokes Firebase Admin SDK (`admin.messaging().send()`) with a high-priority data payload (`type: "NEW_E2EE_MESSAGE"`).
  5. **Client Reception & ACK:** The recipient's `AppFirebaseMessagingService` catches `onMessageReceived`, decrypts via Google Tink, commits to Room DB, and calls delivery ACK back to Supabase/Firestore.

---

## 2. Step-by-Step Setup Guide

### Step 1: Firebase Console Setup & `google-services.json`
1. Navigate to [Firebase Console](https://console.firebase.google.com/) and select or create your project.
2. In Project Settings, add an Android App with package name matching `com.aistudio.vibesync.*` (or `com.example`).
3. Download the generated `google-services.json` and place it in the `app/` directory of the project.
4. Enable **Cloud Firestore** in production or test mode.
5. In Project Settings > Cloud Messaging, verify that **Firebase Cloud Messaging API (V1)** is enabled.

### Step 2: Client-Side Android Configuration (VibeSync Verification)
* **Token Registration:**
  * `AppFirebaseMessagingService.onNewToken()` automatically pushes the device FCM token to Firestore under `fcm_tokens/{cleanPhone}` via `FirebaseBackendSyncManager.registerDeviceFcmToken()`.
  * Ensure runtime permission `POST_NOTIFICATIONS` is granted on Android 13+ (API 33+), handled via `AppNotificationManager`.
* **Background Reception:**
  * High-priority data messages (`type == "NEW_E2EE_MESSAGE"`) are intercepted in `AppFirebaseMessagingService.onMessageReceived()`, decrypted with Google Tink, and immediately persisted to Room DB with delivery receipts.

### Step 3: Firebase Cloud Function Implementation (`functions/index.js`)
* Create a dedicated Firebase Functions project (`functions/`) containing:
  * Node.js script using `firebase-admin` and `firebase-functions/v2`.
  * Firestore trigger: `onDocumentCreated("chats/{matchId}/messages/{messageId}", ...)`
  * Logic to determine the recipient ID from `matchId` or message payload.
  * Query `fcm_tokens` collection to get `fcmToken`.
  * Send high-priority Android FCM message with fields:
    * `message_id`, `ciphertext`, `sender_phone`, `receiver_phone`, `type: "NEW_E2EE_MESSAGE"`, `timestamp`.

### Step 4: Verification & End-to-End Testing
1. Send a chat message from Account A to Account B.
2. Verify Firestore write at `chats/{matchId}/messages/{messageId}`.
3. Observe Cloud Function execution in Firebase Console logs.
4. Verify Account B receives the background notification, renders the bubble, and sends back the double-tick delivery ACK.

---

## 3. Proposed Changes & Deliverables

### Firebase Functions Scripts
* **`functions/package.json`**: Declare `firebase-admin` (v12+) and `firebase-functions` (v5+).
* **`functions/index.js`**: Implement `onMessageCreated` with recipient token lookup and FCM HTTP v1 dispatch.

### Android Client Adjustments
* **`DatingRepository.kt`**: Verify Firestore write in `sendMessage` writes the recipient's phone/UID into the message document so the Cloud Function can look up the recipient without additional index queries.
* **`AppFirebaseMessagingService.kt`**: Ensure complete handshake with token refreshing and notification channels.

---

## 4. Verification Plan

* **Local Compilation:** Run `compile_applet` to ensure Android project compiles cleanly.
* **Cloud Function Linting:** Validate JavaScript/Node.js syntax for the Cloud Function.
* **FCM Payload Compatibility:** Ensure payload keys match `AppFirebaseMessagingService.kt` (`ciphertext`, `sender_phone`, `receiver_phone`, `message_id`, `type`).
