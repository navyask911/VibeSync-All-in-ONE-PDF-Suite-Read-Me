package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class VoiceRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    var activeAudioFile: File? = null
        private set

    fun startRecording(): Boolean {
        return try {
            val audioFile = File(context.cacheDir, "voice_memo_${System.currentTimeMillis()}.mp3")
            activeAudioFile = audioFile

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Error starting voice recording", e)
            recorder?.release()
            recorder = null
            false
        }
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            activeAudioFile
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Error stopping voice recording", e)
            recorder?.release()
            recorder = null
            activeAudioFile
        }
    }

    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            activeAudioFile?.delete()
            activeAudioFile = null
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Error canceling voice recording", e)
            recorder?.release()
            recorder = null
        }
    }

    fun playAudio(filePath: String, onComplete: () -> Unit = {}) {
        try {
            stopAudio()
            if (filePath.isBlank()) return
            val file = File(filePath)
            if (!file.exists()) return

            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    onComplete()
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Error playing audio file", e)
        }
    }

    fun stopAudio() {
        try {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
            player = null
        } catch (e: Exception) {
            player = null
        }
    }
}
