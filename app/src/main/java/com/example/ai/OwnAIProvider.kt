package com.example.ai

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OwnAIProvider implements AiProvider by communicating via HTTPS securely with your own
 * self-hosted AI Backend (which in turn connects to a local open-weight model runtime).
 * 
 * Configurable via build/environment or endpoint URL.
 */
class OwnAIProvider(
    private val backendBaseUrl: String = BuildConfig.AI_BACKEND_URL.trimEnd('/'),
    private val authToken: String = BuildConfig.AI_BACKEND_TOKEN
) : AiProvider {
    private val tag = "OwnAIProvider"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildRequest(path: String, jsonBody: JSONObject): Request {
        val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val builder = Request.Builder()
            .url("$backendBaseUrl$path")
            .post(body)
        if (authToken.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $authToken")
        }
        return builder.build()
    }

    override suspend fun chatCompletion(prompt: String, systemPrompt: String?): String {
        if (backendBaseUrl.isBlank()) return "AI backend is not configured. Set AI_BACKEND_URL and rebuild the app."
        return try {
            val jsonBody = JSONObject().apply {
                put("prompt", prompt)
                if (!systemPrompt.isNullOrEmpty()) {
                    put("system_prompt", systemPrompt)
                }
                put("personality", "helpful")
                put("response_length", "balanced")
            }

            val request = buildRequest("/v1/chat", jsonBody)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "AI backend error (HTTP ${response.code}). Please check if your self-hosted AI backend is running."
                }
                val respStr = response.body?.string() ?: return "Empty response from AI backend."
                val json = JSONObject(respStr)
                json.optString("response", respStr)
            }
        } catch (e: Exception) {
            Log.e(tag, "AI backend connection failed: ${e.message}")
            "AI backend unavailable. Please ensure your self-hosted AI backend is running at $backendBaseUrl."
        }
    }

    override suspend fun translateText(text: String, targetLanguage: String): String {
        if (backendBaseUrl.isBlank()) return "AI backend is not configured. Set AI_BACKEND_URL and rebuild the app."
        return try {
            val jsonBody = JSONObject().apply {
                put("text", text)
                put("target_language", targetLanguage)
            }
            val request = buildRequest("/v1/translate", jsonBody)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Translation failed (HTTP ${response.code})."
                val json = JSONObject(response.body?.string() ?: "")
                json.optString("translation", text)
            }
        } catch (e: Exception) {
            Log.e(tag, "Translation error: ${e.message}")
            "AI backend unavailable for translation."
        }
    }

    override suspend fun summarizeText(text: String): String {
        if (backendBaseUrl.isBlank()) return "AI backend is not configured. Set AI_BACKEND_URL and rebuild the app."
        return try {
            val jsonBody = JSONObject().apply {
                put("text", text)
            }
            val request = buildRequest("/v1/summarize", jsonBody)
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Summarization failed (HTTP ${response.code})."
                val json = JSONObject(response.body?.string() ?: "")
                json.optString("summary", text)
            }
        } catch (e: Exception) {
            Log.e(tag, "Summarization error: ${e.message}")
            "AI backend unavailable for summarization."
        }
    }
}
