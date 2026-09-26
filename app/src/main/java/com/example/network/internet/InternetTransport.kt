package com.example.network.internet

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import com.example.data.Message
import com.example.domain.CommunicationTransport
import com.example.security.EncryptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface InternetMessageApi {
    @POST("api/v1/messages/send")
    suspend fun sendMessage(
        @Header("Authorization") authToken: String,
        @Body messagePayload: MessagePayload
    ): ResponseBody
}

data class MessagePayload(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val encryptedText: String,
    val timestamp: Long,
    val status: String,
    val attachmentType: String?,
    val attachmentPath: String?
)

class InternetTransport(private val context: Context) : CommunicationTransport {
    private val tag = "InternetTransport"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Configurable secure backend base URL (self-hosted or production API)
    private val backendBaseUrl = BuildConfig.MESSAGE_BACKEND_URL.trimEnd('/')
    private val authToken = BuildConfig.MESSAGE_BACKEND_TOKEN

    private val retrofit = Retrofit.Builder()
        .baseUrl(if (backendBaseUrl.isBlank()) "https://invalid.local/" else "$backendBaseUrl/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val api = retrofit.create(InternetMessageApi::class.java)

    override val name: String = "Internet"

    override fun isAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return backendBaseUrl.isNotBlank() && authToken.isNotBlank() && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override suspend fun sendMessage(message: Message): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) {
            Log.d(tag, "Internet is unavailable for sending message.")
            return@withContext false
        }

        try {
            val secretKey = EncryptionHelper.deriveKey("AI_MESSENGER_INTERNET_KEY")
            val cipherText = EncryptionHelper.encrypt(message.text, secretKey)

            val payload = MessagePayload(
                id = message.id,
                conversationId = message.conversationId,
                senderId = message.senderId,
                senderName = message.senderName,
                encryptedText = cipherText,
                timestamp = message.timestamp,
                status = message.status,
                attachmentType = message.attachmentType,
                attachmentPath = message.attachmentPath
            )

            // Secure authenticated request to messaging backend API
            val response = api.sendMessage("Bearer $authToken", payload)
            val success = response.string().isNotEmpty()
            Log.d(tag, "Internet secure messaging API transaction success: $success")
            true
        } catch (e: Exception) {
            Log.e(tag, "Internet transport transmission failed: ${e.message}")
            false
        }
    }
}
