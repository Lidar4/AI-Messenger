package com.example.data

import android.content.Context
import android.util.Log
import com.example.domain.CommunicationTransport
import com.example.network.internet.InternetTransport
import com.example.network.nearby.NearbyTransport
import com.example.network.nearby.P2PManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val tag = "ChatRepository"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val userDao = database.userDao()
    val conversationDao = database.conversationDao()
    val messageDao = database.messageDao()
    val callLogDao = database.callLogDao()
    val aiWorkspaceSessionDao = database.aiWorkspaceSessionDao()

    // P2P Manager to run local TCP socket server
    val p2pManager = P2PManager(
        myUserId = "user_me",
        onMessageReceived = { incomingMessage ->
            scope.launch {
                handleIncomingMessage(incomingMessage)
            }
        }
    )

    // Communication transports
    private val internetTransport = InternetTransport(context)
    private val nearbyTransport = NearbyTransport(p2pManager)

    init {
        // Automatically populate initial users/conversations if empty
        scope.launch {
            prepopulateDataIfNeeded()
        }
    }

    private suspend fun handleIncomingMessage(message: Message) {
        // Register message for duplicate prevention / replay protection
        if (!com.example.network.nearby.MeshRouter.registerMessageAndCheckDuplicate(message.id)) {
            return
        }

        // Insert into database (source of truth)
        messageDao.insertMessage(message)

        // Update conversation state
        val existingConv = conversationDao.getConversationById(message.conversationId)
        if (existingConv != null) {
            conversationDao.updateLastMessage(
                id = message.conversationId,
                text = message.text,
                time = message.timestamp,
                unreadIncrement = 1
            )
        } else {
            val newConv = Conversation(
                id = message.conversationId,
                title = message.senderName,
                isGroup = false,
                participantsJson = "user_me,${message.senderId}",
                lastMessageText = message.text,
                lastMessageTime = message.timestamp,
                unreadCount = 1
            )
            conversationDao.insertConversation(newConv)
        }
    }

    suspend fun sendMessage(conversationId: String, text: String, replyToId: String? = null, replyToText: String? = null, attachmentPath: String? = null, attachmentType: String? = null): Message {
        val me = userDao.getMe() ?: User("user_me", "Me", "me_handle", null, true)
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = me.id,
            senderName = me.displayName,
            text = text,
            timestamp = System.currentTimeMillis(),
            status = "PENDING",
            replyToId = replyToId,
            replyToText = replyToText,
            attachmentPath = attachmentPath,
            attachmentType = attachmentType,
            isIncoming = false,
            transportUsed = "INTERNET"
        )

        // Store locally first so it works offline!
        messageDao.insertMessage(message)
        conversationDao.updateLastMessage(conversationId, text, message.timestamp, 0)

        scope.launch {
            transmitMessage(message)
        }

        return message
    }

    private suspend fun transmitMessage(message: Message) {
        // Dual-path transport routing
        var success = false
        var transportUsed = "INTERNET"

        if (internetTransport.isAvailable()) {
            Log.d(tag, "Attempting transmission over Internet")
            success = internetTransport.sendMessage(message)
            transportUsed = "INTERNET"
        }

        if (!success && nearbyTransport.isAvailable()) {
            Log.d(tag, "Internet failed/unavailable. Falling back to Nearby Mesh")
            success = nearbyTransport.sendMessage(message)
            transportUsed = "NEARBY"
        }

        if (success) {
            messageDao.updateMessageStatus(message.id, "SENT")
            // Update transport used in DB for transparency
            val updatedMsg = message.copy(status = "SENT", transportUsed = transportUsed)
            messageDao.insertMessage(updatedMsg)
        } else {
            messageDao.updateMessageStatus(message.id, "PENDING")
            Log.d(tag, "Message transmission failed. Queued for auto-retry.")
        }
    }

    suspend fun clearConversationUnread(conversationId: String) {
        conversationDao.clearUnreadCount(conversationId)
    }

    private suspend fun prepopulateDataIfNeeded() {
        val me = userDao.getMe()
        if (me == null) {
            userDao.insertUser(User("user_me", "Me", "me_handle", null, true))
            userDao.insertUser(User("peer_alice", "Alice", "alice_gcm", null, false))
            userDao.insertUser(User("peer_bob", "Bob", "bob_mesh", null, false))

            // Populate mock conversations
            conversationDao.insertConversation(
                Conversation(
                    id = "conv_alice",
                    title = "Alice",
                    isGroup = false,
                    participantsJson = "user_me,peer_alice",
                    lastMessageText = "Welcome to AI Messenger!",
                    lastMessageTime = System.currentTimeMillis() - 3600000,
                    unreadCount = 0
                )
            )

            messageDao.insertMessage(
                Message(
                    id = "msg_init_1",
                    conversationId = "conv_alice",
                    senderId = "peer_alice",
                    senderName = "Alice",
                    text = "Welcome to AI Messenger! Let's chat securely.",
                    timestamp = System.currentTimeMillis() - 3600000,
                    status = "READ",
                    isIncoming = true,
                    transportUsed = "INTERNET"
                )
            )

            // Setup default AI assistant workspace session
            aiWorkspaceSessionDao.insertSession(
                AIWorkspaceSession(
                    id = "ai_default",
                    title = "AI Assistant Workspace",
                    toolMode = "GENERAL"
                )
            )
        }
    }
}
