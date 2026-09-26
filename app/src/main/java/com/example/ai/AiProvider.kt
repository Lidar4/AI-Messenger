package com.example.ai

interface AiProvider {
    suspend fun chatCompletion(prompt: String, systemPrompt: String? = null): String
    suspend fun translateText(text: String, targetLanguage: String): String
    suspend fun summarizeText(text: String): String
}
