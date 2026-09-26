package com.example.domain

import com.example.data.Message

interface CommunicationTransport {
    val name: String
    suspend fun sendMessage(message: Message): Boolean
    fun isAvailable(): Boolean
}
