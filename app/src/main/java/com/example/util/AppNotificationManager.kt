package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.service.NotificationActionReceiver
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Payload data class for in-app alert broadcasting.
 */
data class AppAlertPayload(
    val type: String, // "MATCH", "MESSAGE", "LIKE", "SYSTEM"
    val title: String,
    val body: String,
    val senderOrMatchId: String,
    val senderName: String,
    val photoUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AppNotificationManager
 * VibeSync-style Real-time Push Notification Manager
 * Features:
 * - Real-time Heads-up Popups (Priority MAX, VibeSync sound & vibration)
 * - NotificationCompat.MessagingStyle with conversation message stacking
 * - Direct Inline Reply via RemoteInput (reply right from the notification bar!)
 * - Quick Action Buttons (Mark as Read, Chat Now, Say Hello)
 * - Firebase Cloud Messaging (FCM) integration
 * - In-app animated floating banner broadcaster
 */
object AppNotificationManager {

    private const val TAG = "AppNotificationManager"

    // Channel IDs
    const val CHANNEL_ID_MATCHES = "channel_vibe_matches_v3"
    const val CHANNEL_ID_MESSAGES = "channel_vibe_messages_v3"
    const val CHANNEL_ID_ALERTS = "channel_vibe_alerts_v3"
    const val CHANNEL_ID_OTP = "channel_vibe_otp_v3"

    // Intent Actions & Extras
    const val ACTION_DIRECT_REPLY = "com.example.action.DIRECT_REPLY"
    const val ACTION_MARK_AS_READ = "com.example.action.MARK_AS_READ"
    const val KEY_TEXT_REPLY = "key_text_reply"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
    const val EXTRA_TARGET_ID = "extra_target_id"
    const val EXTRA_TARGET_NAME = "extra_target_name"

    // VibeSync Signature Brand Color
    const val VIBESYNC_TEAL_COLOR = 0xFF00A884.toInt()
    const val MATCH_PINK_COLOR = 0xFFFF4081.toInt()

    // Global in-app event stream for floating banner
    private val _inAppAlertEvents = MutableSharedFlow<AppAlertPayload>(extraBufferCapacity = 64)
    val inAppAlertEvents = _inAppAlertEvents.asSharedFlow()

    private var currentFcmToken: String? = null
    var appContext: Context? = null

    // App Presence & Active Conversation State Tracking
    @Volatile
    var isAppInForeground: Boolean = true

    @Volatile
    var activeChatPartnerId: String? = null

    @Volatile
    var activeChatMatchId: String? = null

    fun setAppForegroundState(inForeground: Boolean) {
        isAppInForeground = inForeground
        Log.d(TAG, "App foreground state updated: $inForeground")
    }

    fun setActiveChatSession(partnerId: String?, matchId: String?) {
        activeChatPartnerId = partnerId
        activeChatMatchId = matchId
        Log.d(TAG, "Active chat session set -> partner: $partnerId, match: $matchId")
    }

    fun clearActiveChatSession() {
        activeChatPartnerId = null
        activeChatMatchId = null
        Log.d(TAG, "Active chat session cleared")
    }

    fun isUserActiveInChatWith(senderOrMatchId: String): Boolean {
        // Only suppress if the app is currently active in the foreground
        if (!isAppInForeground) return false

        val currentPartner = activeChatPartnerId?.trim()
        val currentMatch = activeChatMatchId?.trim()
        if (currentPartner.isNullOrBlank() && currentMatch.isNullOrBlank()) return false

        val cleanTarget = senderOrMatchId.trim()
        if (cleanTarget.isBlank()) return false

        val targetDigits = cleanTarget.filter { it.isDigit() }
        val targetDigits10 = if (targetDigits.length >= 10) targetDigits.takeLast(10) else targetDigits

        // 1. Check partner id matching
        if (!currentPartner.isNullOrBlank()) {
            val partnerDigits = currentPartner.filter { it.isDigit() }
            val partnerDigits10 = if (partnerDigits.length >= 10) partnerDigits.takeLast(10) else partnerDigits

            if (currentPartner.equals(cleanTarget, ignoreCase = true) ||
                cleanTarget.equals("match_$currentPartner", ignoreCase = true) ||
                currentPartner.equals(cleanTarget.removePrefix("match_"), ignoreCase = true) ||
                (partnerDigits10.length >= 7 && targetDigits10.length >= 7 && partnerDigits10 == targetDigits10) ||
                (cleanTarget.contains(currentPartner, ignoreCase = true)) ||
                (currentPartner.contains(cleanTarget, ignoreCase = true))
            ) {
                return true
            }
        }

        // 2. Check match id matching
        if (!currentMatch.isNullOrBlank()) {
            val matchDigits = currentMatch.filter { it.isDigit() }

            if (currentMatch.equals(cleanTarget, ignoreCase = true) ||
                currentMatch.equals("match_$cleanTarget", ignoreCase = true) ||
                cleanTarget.equals("match_$currentMatch", ignoreCase = true) ||
                currentMatch.removePrefix("match_").equals(cleanTarget.removePrefix("match_"), ignoreCase = true) ||
                (targetDigits10.length >= 7 && currentMatch.contains(targetDigits10)) ||
                (!currentPartner.isNullOrBlank() && cleanTarget.contains(currentPartner, ignoreCase = true)) ||
                (cleanTarget.removePrefix("match_").contains(currentMatch.removePrefix("match_"), ignoreCase = true))
            ) {
                return true
            }
        }

        return false
    }

    // Message History for VibeSync-style MessagingStyle conversation threads
    private val conversationHistory = ConcurrentHashMap<String, MutableList<NotificationCompat.MessagingStyle.Message>>()

    /**
     * Initializes the notification manager and Android 8.0+ notification channels.
     */
    fun initChannels(context: Context) {
        appContext = context.applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            // Instant Chat Messages Channel (High Importance, Sound & Vibration)
            val messageChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Instant Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming chat messages, voice notes, media, and direct inline replies"
                enableLights(true)
                lightColor = VIBESYNC_TEAL_COLOR
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 250)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }

            // 2. High Priority Matches & Mutual Likes Channel
            val matchChannel = NotificationChannel(
                CHANNEL_ID_MATCHES,
                "Mutual Matches & Likes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant real-time match celebrations and mutual attraction alerts"
                enableLights(true)
                lightColor = MATCH_PINK_COLOR
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // 3. System & Security Channel
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "App Announcements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Account security updates and announcements"
                enableLights(false)
                setShowBadge(false)
            }

            // 4. Instant Mobile OTP & Verification Codes Channel
            val otpChannel = NotificationChannel(
                CHANNEL_ID_OTP,
                "Mobile OTP Verification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Push notification receive system for instant mobile verification codes"
                enableLights(true)
                lightColor = VIBESYNC_TEAL_COLOR
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(listOf(messageChannel, matchChannel, alertChannel, otpChannel))
            Log.d(TAG, "VibeSync-style notification channels initialized successfully")
        }
    }

    /**
     * Instant Mobile number OTP Push Notification & Heads-up system alert.
     * Dispatches high-priority system notification and floating in-app banner with the 6-digit code.
     */
    @SuppressLint("MissingPermission")
    fun showOtpNotification(
        context: Context,
        mobileNumber: String,
        otpCode: String
    ) {
        val now = System.currentTimeMillis()
        val title = "VibeSync Security Code: $otpCode"
        val message = "$otpCode is your VibeSync verification code. Enter this code to sign in."

        // 1. Emit in-app floating push banner
        _inAppAlertEvents.tryEmit(
            AppAlertPayload(
                type = "OTP",
                title = "VibeSync Verification Code 💬",
                body = "$otpCode is your 6-digit code. Tap to auto-fill.",
                senderOrMatchId = "otp_service",
                senderName = "VibeSync Security",
                photoUrl = "",
                timestamp = now
            )
        )

        // 2. High Priority Heads-up system notification
        if (!hasNotificationPermission(context)) return

        try {
            val intent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_VERIFY_OTP", otpCode)
                putExtra("EXTRA_PHONE_NUMBER", mobileNumber)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_OTP)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(VIBESYNC_TEAL_COLOR)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            NotificationManagerCompat.from(context).notify(99001, notification)
            Log.d(TAG, "Dispatched OTP push notification for $mobileNumber: $otpCode")
        } catch (e: Exception) {
            Log.w(TAG, "OTP notification dispatch notice: ${e.message}")
        }
    }

    /**
     * Retrieves or refreshes the device FCM registration token if Google Play Services is available.
     */
    fun fetchFcmToken(context: Context? = null, onTokenReceived: ((String?) -> Unit)? = null) {
        try {
            if (context != null) {
                val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                if (availability != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                    Log.d(TAG, "Google Play Services not active (code: $availability). Deferring FCM token fetch.")
                    onTokenReceived?.invoke(null)
                    return
                }
            }

            val fcmInstance = FirebaseMessaging.getInstance()
            fcmInstance.isAutoInitEnabled = false
            fcmInstance.token
                .addOnCompleteListener { task ->
                    try {
                        if (!task.isSuccessful) {
                            Log.d(TAG, "FCM token retrieval deferred: ${task.exception?.message}")
                            onTokenReceived?.invoke(null)
                            return@addOnCompleteListener
                        }

                        val token = task.result
                        currentFcmToken = token
                        Log.d(TAG, "FCM Registration Token: $token")
                        onTokenReceived?.invoke(token)
                    } catch (e: Exception) {
                        Log.d(TAG, "FCM token result safely handled: ${e.message}")
                        onTokenReceived?.invoke(null)
                    }
                }
        } catch (e: Throwable) {
            Log.d(TAG, "Firebase messaging startup token check safely deferred: ${e.message}")
            onTokenReceived?.invoke(null)
        }
    }

    fun getCachedFcmToken(): String? = currentFcmToken

    /**
     * Shows a VibeSync-style Real-Time Chat Message Notification with:
     * - MessagingStyle multi-message conversation thread
     * - RemoteInput Direct Inline Reply
     * - "Mark as Read" quick action
     * - Heads-up banner display
     */
    @SuppressLint("MissingPermission")
    fun showMessageNotification(
        context: Context,
        senderId: String,
        senderName: String,
        messageText: String,
        senderPhotoUrl: String = "",
        mediaType: String = "TEXT"
    ) {
        if (!hasNotificationPermission(context)) return

        // Critical Check: If the user is currently online and actively in live chat with the same person,
        // do NOT trigger any notification (system popup or in-app banner dropdown) since they see it live on screen.
        if (isUserActiveInChatWith(senderId)) {
            Log.d(TAG, "Notification suppressed: user is actively chatting with $senderName ($senderId) live.")
            return
        }

        val displayBody = when (mediaType) {
            "IMAGE" -> "📸 Photo"
            "AI_IMAGE" -> "✨ AI Art: $messageText"
            "VOICE" -> "🎤 Voice message"
            "LOCATION", "LIVE_LOCATION" -> "📍 Location"
            "DOCUMENT" -> "📄 Document"
            "POLL" -> "📊 Poll"
            "PAYMENT" -> "💳 Payment request"
            "EVENT" -> "📅 Event invite"
            else -> messageText.ifBlank { "New message" }
        }

        val notificationId = ("msg_$senderId").hashCode()
        val now = System.currentTimeMillis()

        // 1. Trigger open-source sound and vibration alert
        AlertSoundAndVibrationManager.triggerChatAlert(context)

        // 2. Emit in-app event for floating banner
        _inAppAlertEvents.tryEmit(
            AppAlertPayload(
                type = "MESSAGE",
                title = senderName,
                body = displayBody,
                senderOrMatchId = senderId,
                senderName = senderName,
                photoUrl = senderPhotoUrl,
                timestamp = now
            )
        )

        // 2. Build VibeSync-style MessagingStyle
        CoroutineScope(Dispatchers.IO).launch {
            val senderBitmap = if (senderPhotoUrl.isNotBlank()) downloadBitmap(senderPhotoUrl) else null
            val senderIcon = senderBitmap?.let { IconCompat.createWithBitmap(it) }

            val senderPerson = Person.Builder()
                .setName(senderName)
                .setKey(senderId)
                .apply { if (senderIcon != null) setIcon(senderIcon) }
                .build()

            val userPerson = Person.Builder()
                .setName("You")
                .setKey("current_user")
                .build()

            // Append to conversation history for rich multi-message bubbles
            val msgList = conversationHistory.getOrPut(senderId) { mutableListOf() }
            msgList.add(NotificationCompat.MessagingStyle.Message(displayBody, now, senderPerson))
            if (msgList.size > 8) {
                msgList.removeAt(0)
            }

            val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
                .setConversationTitle(senderName)
                .setGroupConversation(false)

            msgList.forEach { msg ->
                messagingStyle.addMessage(msg)
            }

            // Deep-link intent to open chat
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NOTIFICATION_TYPE, "MESSAGE")
                putExtra(EXTRA_TARGET_ID, senderId)
                putExtra(EXTRA_TARGET_NAME, senderName)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Direct Inline Reply Action (RemoteInput)
            val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel("Reply to $senderName...")
                .build()

            val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_DIRECT_REPLY
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_TARGET_ID, senderId)
                putExtra(EXTRA_TARGET_NAME, senderName)
            }

            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            )

            val replyAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                "Reply",
                replyPendingIntent
            ).addRemoteInput(remoteInput).build()

            // "Mark as Read" Quick Action
            val markAsReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_MARK_AS_READ
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_TARGET_ID, senderId)
            }
            val markAsReadPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2,
                markAsReadIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val markAsReadAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_view,
                "Mark as read",
                markAsReadPendingIntent
            ).build()

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setStyle(messagingStyle)
                .setContentTitle(senderName)
                .setContentText(displayBody)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0, 200, 100, 250))
                .setLights(VIBESYNC_TEAL_COLOR, 500, 1000)
                .setColor(VIBESYNC_TEAL_COLOR)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(replyAction)
                .addAction(markAsReadAction)
                .apply {
                    if (senderBitmap != null) {
                        setLargeIcon(senderBitmap)
                    }
                }
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(notificationId, notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission missing: ${e.message}")
            }
        }
    }

    /**
     * Shows a VibeSync-style Real-Time New Match Alert Notification with:
     * - Heads-up pop-over alert
     * - "Chat Now" direct action
     * - "View Profile" quick action
     */
    @SuppressLint("MissingPermission")
    fun showMatchNotification(
        context: Context,
        matchId: String,
        matchName: String,
        matchPhotoUrl: String = "",
        age: Int = 0,
        city: String = "",
        score: Int = 94
    ) {
        if (!hasNotificationPermission(context)) return

        val title = "✨ New Match: $matchName 💕"
        val subtitle = if (age > 0 && city.isNotBlank()) {
            "You and $matchName ($age, $city) liked each other! Compatibility: $score%"
        } else {
            "You and $matchName liked each other! Start chatting now on VibeSync."
        }

        val notificationId = ("match_$matchId").hashCode()
        val now = System.currentTimeMillis()

        // 1. In-app floating banner event
        _inAppAlertEvents.tryEmit(
            AppAlertPayload(
                type = "MATCH",
                title = "✨ It's a Mutual Match!",
                body = "You and $matchName liked each other ($score% match)!",
                senderOrMatchId = matchId,
                senderName = matchName,
                photoUrl = matchPhotoUrl,
                timestamp = now
            )
        )

        // 2. Deep link to open chat
        val chatIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, "MATCH")
            putExtra(EXTRA_TARGET_ID, matchId)
            putExtra(EXTRA_TARGET_NAME, matchName)
        }
        val chatPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            chatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val chatAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "💬 Chat Now",
            chatPendingIntent
        ).build()

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        CoroutineScope(Dispatchers.IO).launch {
            val matchBitmap = if (matchPhotoUrl.isNotBlank()) downloadBitmap(matchPhotoUrl) else null

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_MATCHES)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(defaultSoundUri)
                .setVibrate(longArrayOf(0, 300, 150, 300))
                .setLights(MATCH_PINK_COLOR, 500, 1000)
                .setColor(MATCH_PINK_COLOR)
                .setAutoCancel(true)
                .setContentIntent(chatPendingIntent)
                .addAction(chatAction)

            if (matchBitmap != null) {
                notificationBuilder.setLargeIcon(matchBitmap)
            }

            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(notificationId, notificationBuilder.build())
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission missing: ${e.message}")
            }
        }
    }

    /**
     * Shows a real-time Notification alert for a New Incoming Like.
     */
    @SuppressLint("MissingPermission")
    fun showLikeNotification(
        context: Context,
        likerId: String,
        likerName: String,
        likerPhotoUrl: String = "",
        likerCity: String = ""
    ) {
        if (!hasNotificationPermission(context)) return

        val title = "💖 New Profile Like!"
        val subtitle = if (likerCity.isNotBlank()) {
            "$likerName from $likerCity liked your profile! Open VibeSync to match back."
        } else {
            "$likerName liked your profile! Open VibeSync to match back."
        }

        val notificationId = ("like_$likerId").hashCode()

        _inAppAlertEvents.tryEmit(
            AppAlertPayload(
                type = "LIKE",
                title = title,
                body = subtitle,
                senderOrMatchId = likerId,
                senderName = likerName,
                photoUrl = likerPhotoUrl
            )
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, "LIKE")
            putExtra(EXTRA_TARGET_ID, likerId)
            putExtra(EXTRA_TARGET_NAME, likerName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        CoroutineScope(Dispatchers.IO).launch {
            val largeBitmap = if (likerPhotoUrl.isNotBlank()) downloadBitmap(likerPhotoUrl) else null

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_MATCHES)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setSound(defaultSoundUri)
                .setColor(0xFFFF5252.toInt())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (largeBitmap != null) {
                notificationBuilder.setLargeIcon(largeBitmap)
            }

            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(notificationId, notificationBuilder.build())
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission missing: ${e.message}")
            }
        }
    }

    /**
     * Shows a real-time Notification alert for a Super Like.
     */
    @SuppressLint("MissingPermission")
    fun showSuperLikeNotification(
        context: Context,
        likerId: String,
        likerName: String,
        likerPhotoUrl: String = "",
        likerCity: String = ""
    ) {
        if (!hasNotificationPermission(context)) return

        val title = "⭐ You received a Super Like!"
        val subtitle = if (likerCity.isNotBlank()) {
            "$likerName from $likerCity super liked you! Match instantly and chat."
        } else {
            "$likerName super liked your profile! Match instantly and chat."
        }

        val notificationId = ("superlike_$likerId").hashCode()

        _inAppAlertEvents.tryEmit(
            AppAlertPayload(
                type = "SUPER_LIKE",
                title = title,
                body = subtitle,
                senderOrMatchId = likerId,
                senderName = likerName,
                photoUrl = likerPhotoUrl
            )
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, "SUPER_LIKE")
            putExtra(EXTRA_TARGET_ID, likerId)
            putExtra(EXTRA_TARGET_NAME, likerName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        CoroutineScope(Dispatchers.IO).launch {
            val largeBitmap = if (likerPhotoUrl.isNotBlank()) downloadBitmap(likerPhotoUrl) else null

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_MATCHES)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setSound(defaultSoundUri)
                .setColor(0xFF2196F3.toInt())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (largeBitmap != null) {
                notificationBuilder.setLargeIcon(largeBitmap)
            }

            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(notificationId, notificationBuilder.build())
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission missing: ${e.message}")
            }
        }
    }

    /**
     * Shows a real-time Notification alert for a Friendship Request.
     */
    @SuppressLint("MissingPermission")
    fun showFriendRequestNotification(
        context: Context,
        senderId: String,
        senderName: String,
        senderPhotoUrl: String = "",
        senderCity: String = ""
    ) {
        if (!hasNotificationPermission(context)) return

        val title = "🤝 New Friendship Request"
        val subtitle = if (senderCity.isNotBlank()) {
            "$senderName from $senderCity sent you a friend request to connect!"
        } else {
            "$senderName sent you a friend request to connect on VibeSync!"
        }

        val notificationId = ("friendreq_$senderId").hashCode()

        _inAppAlertEvents.tryEmit(
            AppAlertPayload(
                type = "FRIEND_REQUEST",
                title = title,
                body = subtitle,
                senderOrMatchId = senderId,
                senderName = senderName,
                photoUrl = senderPhotoUrl
            )
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, "FRIEND_REQUEST")
            putExtra(EXTRA_TARGET_ID, senderId)
            putExtra(EXTRA_TARGET_NAME, senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        CoroutineScope(Dispatchers.IO).launch {
            val largeBitmap = if (senderPhotoUrl.isNotBlank()) downloadBitmap(senderPhotoUrl) else null

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_MATCHES)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setSound(defaultSoundUri)
                .setColor(0xFFFF9800.toInt())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            if (largeBitmap != null) {
                notificationBuilder.setLargeIcon(largeBitmap)
            }

            val notificationManager = NotificationManagerCompat.from(context)
            try {
                notificationManager.notify(notificationId, notificationBuilder.build())
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification permission missing: ${e.message}")
            }
        }
    }

    /**
     * Updates the notification in place after an inline reply is typed by the user.
     */
    @SuppressLint("MissingPermission")
    fun onInlineReplyHandled(
        context: Context,
        notificationId: Int,
        senderId: String,
        senderName: String,
        replyText: String
    ) {
        if (!hasNotificationPermission(context)) return

        val userPerson = Person.Builder().setName("You").build()
        val senderPerson = Person.Builder().setName(senderName).setKey(senderId).build()

        val msgList = conversationHistory.getOrPut(senderId) { mutableListOf() }
        msgList.add(NotificationCompat.MessagingStyle.Message(replyText, System.currentTimeMillis(), userPerson))

        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(senderName)
            .setGroupConversation(false)

        msgList.forEach { messagingStyle.addMessage(it) }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, "MESSAGE")
            putExtra(EXTRA_TARGET_ID, senderId)
            putExtra(EXTRA_TARGET_NAME, senderName)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val updatedNotification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(messagingStyle)
            .setContentTitle(senderName)
            .setContentText("Sent: $replyText")
            .setColor(VIBESYNC_TEAL_COLOR)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, updatedNotification)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to update notification safely: ${e.message}")
        }
    }

    /**
     * Cancels a notification by ID and clears conversation history cache for that contact.
     */
    fun clearNotification(context: Context, notificationId: Int) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(notificationId)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to cancel notification: ${e.message}")
        }
    }

    fun clearConversationHistory(senderId: String) {
        conversationHistory.remove(senderId)
    }

    /**
     * Checks whether POST_NOTIFICATIONS permission is granted (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private suspend fun downloadBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (_: Exception) {
            null
        }
    }
}
