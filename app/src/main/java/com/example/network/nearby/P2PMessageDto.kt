package com.example.network.nearby

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class P2PMessageDto(
    val protocolVersion: Int = 1,
    val messageId: String,
    val senderDeviceId: String,
    val senderName: String,
    val conversationId: String,
    val timestamp: Long,
    val ttl: Int = 3,
    val messageType: String = "TEXT",
    val encryptedPayload: String,
    val attachmentMetadata: String? = null
)
