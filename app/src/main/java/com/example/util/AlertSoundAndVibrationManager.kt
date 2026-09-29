package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * AlertSoundAndVibrationManager
 * Provides open-source system sound, custom tone generation, ringtones, and haptic vibration alert mechanisms
 * for chat messages, mutual match celebrations, and voice/video calls.
 */
object AlertSoundAndVibrationManager {

    private const val TAG = "AlertSoundVibManager"
    private var activePreviewRingtone: Ringtone? = null

    /**
     * Plays a brief notification tone/ringtone and triggers haptic vibration.
     */
    fun triggerChatAlert(
        context: Context,
        isHighPriority: Boolean = false,
        soundTone: String = "DEFAULT",
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {
        if (soundEnabled) {
            playNotificationTone(context, isHighPriority, soundTone)
        }
        if (vibrationEnabled) {
            triggerVibration(context, isHighPriority)
        }
    }

    /**
     * Plays system notification sound, ringtone, or custom chime tone.
     */
    fun playNotificationTone(
        context: Context,
        isHighPriority: Boolean = false,
        soundTone: String = "DEFAULT"
    ) {
        if (soundTone.equals("SILENT", ignoreCase = true)) return

        try {
            when (soundTone.uppercase()) {
                "CLASSIC_CHIME" -> playToneSequence(ToneGenerator.TONE_PROP_BEEP, 120, 80)
                "CRYSTAL_DROP" -> playToneSequence(ToneGenerator.TONE_PROP_ACK, 100, 70)
                "WHISTLE_BREEZE" -> playToneSequence(ToneGenerator.TONE_PROP_PROMPT, 140, 90)
                "GENTLE_VIBE" -> playToneSequence(ToneGenerator.TONE_CDMA_PIP, 100, 60)
                "COSMIC_MATCH" -> playToneSequence(ToneGenerator.TONE_SUP_RINGTONE, 250, 100)
                "SWEET_BELL" -> playToneSequence(ToneGenerator.TONE_PROP_BEEP2, 180, 85)
                "DIGITAL_WAVE" -> playToneSequence(ToneGenerator.TONE_CDMA_HIGH_PBX_L, 200, 80)
                "MARIMBA" -> playToneSequence(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 220, 85)
                else -> {
                    // Default system notification or ringtone
                    val soundUri = RingtoneManager.getDefaultUri(
                        if (isHighPriority || soundTone.uppercase() == "STANDARD_RING") {
                            RingtoneManager.TYPE_RINGTONE
                        } else {
                            RingtoneManager.TYPE_NOTIFICATION
                        }
                    )
                    val ringtone = RingtoneManager.getRingtone(context.applicationContext, soundUri)
                    if (ringtone != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ringtone.isLooping = false
                        }
                        ringtone.play()
                    } else {
                        playToneSequence(ToneGenerator.TONE_PROP_BEEP, 150, 80)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error playing notification sound: ${e.message}")
            playToneSequence(ToneGenerator.TONE_CDMA_PIP, 120, 70)
        }
    }

    /**
     * Preview play chosen sound option from Settings inside the app.
     */
    fun playPreviewSound(context: Context, toneOptionKey: String) {
        stopPreviewSound()
        if (toneOptionKey.equals("SILENT", ignoreCase = true)) return

        try {
            playNotificationTone(
                context = context,
                isHighPriority = toneOptionKey in listOf("STANDARD_RING", "CELEBRATION", "COSMIC_MATCH"),
                soundTone = toneOptionKey
            )
            triggerVibration(context, isHighPriority = false)
        } catch (e: Exception) {
            Log.e(TAG, "Preview sound playback failed: ${e.message}")
        }
    }

    fun stopPreviewSound() {
        try {
            activePreviewRingtone?.stop()
            activePreviewRingtone = null
        } catch (_: Exception) {}
    }

    private fun playToneSequence(toneType: Int, durationMs: Int, volume: Int) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volume)
            toneGen.startTone(toneType, durationMs)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator execution failed: ${e.message}")
        }
    }

    /**
     * Triggers double-pulse vibration alert using Vibrator or VibratorManager.
     */
    fun triggerVibration(context: Context, isHighPriority: Boolean = false) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                val pattern = if (isHighPriority) {
                    longArrayOf(0, 200, 100, 300) // Double long pulse for matches/high priority
                } else {
                    longArrayOf(0, 120, 80, 150) // Double short pulse for chat messages
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(
                        pattern,
                        intArrayOf(0, if (isHighPriority) 255 else 180, 0, if (isHighPriority) 255 else 200),
                        -1
                    )
                    vibrator.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error triggering vibration alert: ${e.message}")
        }
    }
}
