package com.example.util

import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object GeminiImageHelper {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Enhances a dating/chat image prompt with Gemini and generates a high-quality AI art image URL.
     */
    suspend fun generateAiImage(userPrompt: String): AiImageResult = withContext(Dispatchers.IO) {
        val cleanPrompt = userPrompt.trim().ifBlank { "Romantic coffee date in a cozy ambient cafe" }
        var enhancedPrompt = cleanPrompt

        // Try Gemini 3.5 Flash prompt refinement if API key exists
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val jsonPayload = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "You are an AI image art prompter. Expand this chat idea into a vivid, aesthetic, photorealistic 1-sentence prompt for an image generator (no preamble, no quotes): '$cleanPrompt'")
                                })
                            })
                        })
                    }
                    put("contents", contentsArray)
                }

                val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val responseJson = JSONObject(body)
                    val candidateText = responseJson.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                    if (!candidateText.isNullOrBlank()) {
                        enhancedPrompt = candidateText.trim().replace("\"", "")
                    }
                }
            } catch (_: Exception) {
                // Fallback to cleanPrompt on network error
            }
        }

        // Generate reliable high-definition AI art URL with seed & aesthetic tags
        val encodedPrompt = URLEncoder.encode(
            "$enhancedPrompt, highly detailed, photorealistic, 8k resolution, romantic warm lighting",
            "UTF-8"
        )
        val seed = (System.currentTimeMillis() % 100000).toInt()
        val generatedUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=1024&height=1024&nologo=true&seed=$seed&model=flux"

        AiImageResult(
            originalPrompt = cleanPrompt,
            enhancedPrompt = enhancedPrompt,
            imageUrl = generatedUrl
        )
    }

    data class AiImageResult(
        val originalPrompt: String,
        val enhancedPrompt: String,
        val imageUrl: String
    )
}
