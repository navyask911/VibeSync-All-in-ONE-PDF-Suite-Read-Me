package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.example.data.database.DatingDatabase
import com.example.data.model.ChatMessageEntity
import com.example.util.AppNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * NotificationActionReceiver
 * Receives VibeSync-style inline quick replies and "Mark as Read" actions directly
 * from the Android notification shade without opening the app.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val senderOrMatchId = intent.getStringExtra(AppNotificationManager.EXTRA_TARGET_ID) ?: return
        val senderName = intent.getStringExtra(AppNotificationManager.EXTRA_TARGET_NAME) ?: "Match"
        val notificationId = intent.getIntExtra(AppNotificationManager.EXTRA_NOTIFICATION_ID, senderOrMatchId.hashCode())

        Log.d("NotificationActionReceiver", "Received notification action: $action for target: $senderOrMatchId")

        val matchId = if (senderOrMatchId.startsWith("match_")) senderOrMatchId else "match_$senderOrMatchId"

        when (action) {
            AppNotificationManager.ACTION_DIRECT_REPLY -> {
                val results = RemoteInput.getResultsFromIntent(intent)
                val replyText = results?.getCharSequence(AppNotificationManager.KEY_TEXT_REPLY)?.toString()
                if (!replyText.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val database = DatingDatabase.getDatabase(context)
                            val replyMessage = ChatMessageEntity(
                                messageId = UUID.randomUUID().toString(),
                                matchId = matchId,
                                senderId = "USER",
                                text = replyText,
                                timestamp = System.currentTimeMillis(),
                                isEncrypted = true
                            )
                            database.chatMessageDao().insertMessage(replyMessage)
                            database.matchDao().updateLastMessage(
                                matchId = matchId,
                                text = replyText,
                                timestamp = replyMessage.timestamp,
                                hasUnread = false
                            )

                            // Update the notification in place like VibeSync
                            AppNotificationManager.onInlineReplyHandled(
                                context = context,
                                notificationId = notificationId,
                                senderId = senderOrMatchId,
                                senderName = senderName,
                                replyText = replyText
                            )
                        } catch (e: Exception) {
                            Log.e("NotificationActionReceiver", "Error saving direct reply: ${e.message}", e)
                        }
                    }
                }
            }
            AppNotificationManager.ACTION_MARK_AS_READ -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = DatingDatabase.getDatabase(context)
                        database.matchDao().markAsRead(matchId)
                        AppNotificationManager.clearNotification(context, notificationId)
                        AppNotificationManager.clearConversationHistory(senderOrMatchId)
                    } catch (e: Exception) {
                        Log.e("NotificationActionReceiver", "Error marking as read: ${e.message}", e)
                    }
                }
            }
        }
    }
}
