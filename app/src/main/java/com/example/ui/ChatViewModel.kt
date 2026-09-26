package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiProvider
import com.example.ai.OwnAIProvider
import com.example.data.*
import com.example.network.AudioCallManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "ChatViewModel"

    // Repository
    val repository: ChatRepository by lazy {
        val db = AppDatabase.getDatabase(application)
        ChatRepository(application, db)
    }

    // Audio VoIP Call Manager
    val callManager = AudioCallManager()

    // Decoupled AI Provider (Self-hosted / Local runtime)
    private val aiProvider: AiProvider = OwnAIProvider()

    // Active screen navigation
    // "MAIN", "CHAT_CONVERSATION", "ACTIVE_CALL"
    private val _currentScreen = MutableStateFlow("MAIN")
    val currentScreen: StateFlow<String> = _currentScreen

    // Selected navigation tab inside MAIN screen
    // "HOME", "CHATS", "GROUPS", "CALLS", "AI", "SETTINGS"
    private val _activeTab = MutableStateFlow("CHATS")
    val activeTab: StateFlow<String> = _activeTab

    // Active conversation being viewed
    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation

    // Messages in active conversation
    val activeMessages: StateFlow<List<Message>> = _activeConversation
        .flatMapLatest { conv ->
            if (conv != null) {
                repository.messageDao.getMessagesForConversation(conv.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All conversations flow
    val allConversations: StateFlow<List<Conversation>> = repository.conversationDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All call logs flow
    val callLogs: StateFlow<List<CallLog>> = repository.callLogDao.getAllCallLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Workspace sessions flow
    val aiSessions: StateFlow<List<AIWorkspaceSession>> = repository.aiWorkspaceSessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Discovered Mesh peers
    val discoveredPeers = repository.p2pManager.discoveredPeers
    val isMeshEnabled = repository.p2pManager.isMeshEnabled
    val meshConnectionStatus = repository.p2pManager.connectionStatus

    // Active AI State
    private val _aiWorkspaceMode = MutableStateFlow("GENERAL") // GENERAL, CODING, DEBUGGING, WRITING, RESEARCH, STUDY, DATA, APP_BUILDING, LANGUAGE
    val aiWorkspaceMode: StateFlow<String> = _aiWorkspaceMode

    private val _aiMessages = MutableStateFlow<List<AIMessage>>(listOf(
        AIMessage("assistant", "Hello! I am your integrated AI Workspace Assistant. Select a specialized tool mode below to begin.", System.currentTimeMillis())
    ))
    val aiMessages: StateFlow<List<AIMessage>> = _aiMessages

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading

    // Voice recording simulation
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice

    data class AIMessage(val role: String, val text: String, val timestamp: Long = System.currentTimeMillis())

    fun navigateToScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun openConversation(conversation: Conversation) {
        _activeConversation.value = conversation
        _currentScreen.value = "CHAT_CONVERSATION"
        viewModelScope.launch {
            repository.clearConversationUnread(conversation.id)
        }
    }

    fun closeConversation() {
        _activeConversation.value = null
        _currentScreen.value = "MAIN"
    }

    fun setAIWorkspaceMode(mode: String) {
        if (_aiWorkspaceMode.value == mode) return
        _aiWorkspaceMode.value = mode
        val welcomeMsg = when (mode) {
            "CODING" -> "Coding mode active. Ask me to write, review, or format code snippets."
            "DEBUGGING" -> "Debugging mode active. Send your stack traces or bug descriptions to analyze."
            "WRITING" -> "Writing assistant active. I can help with emails, essays, letters, and polite revisions."
            "RESEARCH" -> "Research mode active. Ask me to summarize articles or explain complex topics deeply."
            "STUDY" -> "Study coach active. Let's create quizzes, flashcards, or learn new core concepts."
            "DATA" -> "Data Sage active. I can format CSV, generate tables, or analyze custom lists."
            "APP_BUILDING" -> "App Builder active. Ask about layout structures, Compose components, or material design."
            "LANGUAGE" -> "Language companion active. I can translate, correct grammar, or practice conversations in multiple languages."
            else -> "Hello! I am your integrated AI Workspace Assistant. Select a specialized tool mode below to begin."
        }
        _aiMessages.value = listOf(AIMessage("assistant", welcomeMsg, System.currentTimeMillis()))
    }

    // Explicitly secure local message sending
    fun sendMessage(text: String, replyToId: String? = null, replyToText: String? = null, attachmentPath: String? = null, attachmentType: String? = null) {
        val conv = _activeConversation.value ?: return
        viewModelScope.launch {
            repository.sendMessage(conv.id, text, replyToId, replyToText, attachmentPath, attachmentType)
        }
    }

    // Trigger explicit call
    fun initiateCall(peerName: String) {
        callManager.startOutgoingCall(peerName)
        _currentScreen.value = "ACTIVE_CALL"
        viewModelScope.launch {
            repository.callLogDao.insertCallLog(
                CallLog(UUID.randomUUID().toString(), "peer_user", peerName, false, 0, System.currentTimeMillis(), "COMPLETED")
            )
        }
    }

    fun toggleMeshNetwork(enabled: Boolean) {
        repository.p2pManager.setMeshEnabled(enabled)
    }

    // AI Workspace prompt execution
    fun askAiWorkspace(prompt: String) {
        if (prompt.trim().isEmpty()) return
        val currentMsgs = _aiMessages.value.toMutableList()
        currentMsgs.add(AIMessage("user", prompt, System.currentTimeMillis()))
        _aiMessages.value = currentMsgs
        _isAiLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            // Build specialized system instructions based on selected AI Mode
            val systemPrompt = when (_aiWorkspaceMode.value) {
                "CODING" -> "You are an expert software engineer. Provide high-quality, formatted code blocks with minimal text."
                "DEBUGGING" -> "You are an expert debugger. Analyze errors, identify source bugs, and explain fixes step by step."
                "WRITING" -> "You are a professional copywriter and editor. Help refine drafts into clear, polished writing."
                "RESEARCH" -> "You are a research expert. Synthesize key concepts, provide clear summaries, and list reference structures."
                "STUDY" -> "You are an encouraging study tutor. Formulate explanations into accessible concepts, quizzes, or helpful summaries."
                "DATA" -> "You are a data extraction specialist. Structure responses as clean Markdown tables, JSON models, or lists."
                "APP_BUILDING" -> "You are a lead Android architect. Provide Jetpack Compose code snippets, and Material 3 layouts following canonical adaptive guides."
                "LANGUAGE" -> "You are a native linguist. Help practice language, explain grammatical patterns, and translate text naturally."
                else -> "You are a helpful and secure integrated AI Assistant inside a privacy-focused messenger."
            }

            val aiResponse = aiProvider.chatCompletion(prompt, systemPrompt)
            withContext(Dispatchers.Main) {
                _isAiLoading.value = false
                val updatedMsgs = _aiMessages.value.toMutableList()
                updatedMsgs.add(AIMessage("assistant", aiResponse, System.currentTimeMillis()))
                _aiMessages.value = updatedMsgs
            }
        }
    }

    // Long press action context triggers
    fun executeMessageAIContextAction(message: Message, actionType: String, onCompleted: (String) -> Unit) {
        _isAiLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val response = when (actionType) {
                "SUMMARIZE" -> aiProvider.summarizeText(message.text)
                "TRANSLATE" -> aiProvider.translateText(message.text, "English")
                else -> {
                    val systemPrompt = "You are an expert messaging assistant. You must perform the requested action strictly on the user message provided."
                    val finalPrompt = when (actionType) {
                        "REWRITE" -> "Politely and professionally rewrite this message so it's ready to send: \"${message.text}\""
                        "EXPLAIN" -> "Explain the key concept or context of this message thoroughly: \"${message.text}\""
                        "CONTINUE" -> "Draft a polite and relevant continue response to this message: \"${message.text}\""
                        "EXTRACT_POINTS" -> "Extract bullet-pointed action items from this message: \"${message.text}\""
                        else -> "Help me process this message: \"${message.text}\""
                    }
                    aiProvider.chatCompletion(finalPrompt, systemPrompt)
                }
            }
            withContext(Dispatchers.Main) {
                _isAiLoading.value = false
                onCompleted(response)
            }
        }
    }

    // Voice messaging simulation
    fun startVoiceRecording() {
        _isRecordingVoice.value = true
    }

    fun stopAndSendVoiceRecording() {
        if (!_isRecordingVoice.value) return
        _isRecordingVoice.value = false
        // Simulate sending a voice message with a mock attachment path
        val conv = _activeConversation.value ?: return
        sendMessage("[Voice Message]", attachmentPath = "voice_recording_mock.mp3", attachmentType = "VOICE")
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.messageDao.deleteMessage(messageId)
        }
    }
}
