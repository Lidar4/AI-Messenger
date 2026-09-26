package com.example.network.internet

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.Message
import com.example.domain.CommunicationTransport
import com.example.security.EncryptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface InternetMessageApi {
    @POST("post") // Hitting a standard endpoint like httpbin.org to echo the response
    suspend fun sendMessageEcho(@Body messagePayload: MessagePayload): ResponseBody
}

data class MessagePayload(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val encryptedText: String,
    val timestamp: Long
)

class InternetTransport(private val context: Context) : CommunicationTransport {
    private val tag = "InternetTransport"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://httpbin.org/")
        .client(okHttpClient)
        .build()

    private val api = retrofit.create(InternetMessageApi::class.java)

    override val name: String = "Internet"

    override fun isAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override suspend fun sendMessage(message: Message): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) {
            Log.d(tag, "Internet is unavailable for sending message.")
            return@withContext false
        }

        try {
            // Encrypt using standard AES before transmission
            val secretKey = EncryptionHelper.deriveKey("AI_MESSENGER_INTERNET_KEY")
            val cipherText = EncryptionHelper.encrypt(message.text, secretKey)

            val payload = MessagePayload(
                id = message.id,
                conversationId = message.conversationId,
                senderId = message.senderId,
                senderName = message.senderName,
                encryptedText = cipherText,
                timestamp = message.timestamp
            )

            // Echo message payload to httpbin.org as a real network transaction!
            val response = api.sendMessageEcho(payload)
            val success = response.string().isNotEmpty()
            Log.d(tag, "Internet message transaction success: $success")
            true
        } catch (e: Exception) {
            Log.e(tag, "Internet transport transmission failed: ${e.message}")
            false
        }
    }
}
