package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage

/**
 * Utility for listening to SMS messages and auto-extracting 4 to 6-digit verification OTP codes.
 */
object SmsOtpReceiverHelper {

    fun registerSmsReceiver(
        context: Context,
        onOtpReceived: (otpCode: String) -> Unit
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    for (sms in messages) {
                        val body = sms.displayMessageBody ?: continue
                        val extracted = extractOtp(body)
                        if (extracted != null) {
                            onOtpReceived(extracted)
                            break
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        try {
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Exception) {
            // Gracefully ignore if device restrictions or permissions prevent receiver registration
        }
        return receiver
    }

    fun unregisterSmsReceiver(context: Context, receiver: BroadcastReceiver?) {
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Extracts a 6-digit (or 4-digit) numerical OTP code from an SMS message string.
     */
    fun extractOtp(messageBody: String): String? {
        val regex = Regex("\\b(\\d{6})\\b")
        val match = regex.find(messageBody)
        if (match != null) {
            return match.groupValues[1]
        }
        val fourDigitRegex = Regex("\\b(\\d{4})\\b")
        return fourDigitRegex.find(messageBody)?.groupValues?.get(1)
    }
}
