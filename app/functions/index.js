const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Cloud Function triggered on new message created in chats/{matchId}/messages/{messageId}
 * Resolves recipient's FCM token from 'fcm_tokens' collection and dispatches
 * a free, high-priority FCM data payload (type: "NEW_E2EE_MESSAGE").
 */
exports.onChatMessageCreated = onDocumentCreated(
  "chats/{matchId}/messages/{messageId}",
  async (event) => {
    const snap = event.data;
    if (!snap) {
      console.log("No snapshot data available.");
      return null;
    }

    const messageData = snap.data();
    const matchId = event.params.matchId;
    const messageId = event.params.messageId;

    const senderPhone = messageData.senderPhone || messageData.senderId || "";
    const receiverPhone = messageData.receiverPhone || messageData.receiverId || "";
    const ciphertext = messageData.ciphertext || messageData.text || "";
    const mediaType = messageData.mediaType || "TEXT";
    const mediaUrl = messageData.mediaUrl || "";
    const timestamp = String(messageData.timestamp || Date.now());

    if (!receiverPhone) {
      console.log(`No receiverPhone specified for message ${messageId} in match ${matchId}`);
      return null;
    }

    // Clean phone number formats for lookup
    const cleanReceiver = receiverPhone.replace(/[^0-9]/g, "");
    const last10Digits = cleanReceiver.slice(-10);

    const db = admin.firestore();

    // 1. Try finding FCM token by exact receiverPhone, cleanReceiver, or last10Digits
    let token = null;
    const tokenDocCandidates = [receiverPhone, cleanReceiver, `+${cleanReceiver}`, last10Digits];

    for (const docId of tokenDocCandidates) {
      if (!docId) continue;
      try {
        const tokenDoc = await db.collection("fcm_tokens").document(docId).get();
        if (tokenDoc.exists && tokenDoc.data() && tokenDoc.data().fcmToken) {
          token = tokenDoc.data().fcmToken;
          console.log(`Found FCM token via document key: ${docId}`);
          break;
        }
      } catch (err) {
        console.warn(`Error reading fcm_tokens doc ${docId}:`, err.message);
      }
    }

    // Fallback: Query by userId in fcm_tokens collection
    if (!token && last10Digits) {
      try {
        const querySnap = await db.collection("fcm_tokens")
          .where("userId", ">=", last10Digits)
          .limit(5)
          .get();

        for (const doc of querySnap.docs) {
          const d = doc.data();
          if (d.userId && d.userId.includes(last10Digits) && d.fcmToken) {
            token = d.fcmToken;
            console.log(`Found FCM token via query on userId: ${d.userId}`);
            break;
          }
        }
      } catch (err) {
        console.warn("Fallback query error on fcm_tokens:", err.message);
      }
    }

    if (!token) {
      console.log(`No active FCM token found for recipient: ${receiverPhone}`);
      return null;
    }

    // Construct high-priority FCM data payload matching AppFirebaseMessagingService.kt
    const payload = {
      token: token,
      data: {
        type: "NEW_E2EE_MESSAGE",
        message_id: messageId,
        match_id: matchId,
        sender_phone: senderPhone,
        receiver_phone: receiverPhone,
        ciphertext: ciphertext,
        media_type: mediaType,
        media_url: mediaUrl,
        timestamp: timestamp
      },
      android: {
        priority: "high",
        ttl: 86400 // 24 hours
      }
    };

    try {
      const response = await admin.messaging().send(payload);
      console.log(`Successfully dispatched free FCM push message ${messageId} to ${receiverPhone}:`, response);
      return response;
    } catch (error) {
      console.error(`Error sending FCM message to ${receiverPhone}:`, error);
      // Remove stale token if invalid
      if (
        error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered"
      ) {
        console.log(`Cleaning up unregistered FCM token for ${receiverPhone}`);
        for (const docId of tokenDocCandidates) {
          if (docId) {
            await db.collection("fcm_tokens").document(docId).delete().catch(() => {});
          }
        }
      }
      return null;
    }
  }
);

/**
 * Cloud Function triggered on new match alert in matches/{matchId}
 */
exports.onUserMatchCreated = onDocumentCreated(
  "matches/{matchId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return null;
    const matchData = snap.data();
    const recipientPhone = matchData.userB_phone || matchData.profileId || "";
    if (!recipientPhone) return null;

    const cleanReceiver = recipientPhone.replace(/[^0-9]/g, "");
    const last10Digits = cleanReceiver.slice(-10);

    const db = admin.firestore();
    let token = null;
    const tokenDocCandidates = [recipientPhone, cleanReceiver, last10Digits];

    for (const docId of tokenDocCandidates) {
      if (!docId) continue;
      const tokenDoc = await db.collection("fcm_tokens").document(docId).get();
      if (tokenDoc.exists && tokenDoc.data() && tokenDoc.data().fcmToken) {
        token = tokenDoc.data().fcmToken;
        break;
      }
    }

    if (!token) return null;

    const payload = {
      token: token,
      data: {
        type: "MATCH",
        match_id: event.params.matchId,
        sender_id: matchData.userA_phone || "",
        sender_name: matchData.userA_name || "Someone",
        photo_url: matchData.userA_photo || "",
        score: String(matchData.score || 95),
        city: matchData.userA_city || "Bengaluru"
      },
      android: {
        priority: "high"
      }
    };

    try {
      return await admin.messaging().send(payload);
    } catch (err) {
      console.error("Match notification push error:", err);
      return null;
    }
  }
);
