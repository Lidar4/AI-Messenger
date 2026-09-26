package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val displayName: String,
    val username: String,
    val profilePhotoUri: String? = null,
    val isMe: Boolean = false
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val title: String,
    val isGroup: Boolean,
    val participantsJson: String, // comma-separated or JSON list of user IDs
    val lastMessageText: String? = null,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, SENT, DELIVERED, READ
    val replyToId: String? = null,
    val replyToText: String? = null,
    val attachmentPath: String? = null,
    val attachmentType: String? = null, // IMAGE, VIDEO, DOCUMENT, VOICE, NONE
    val isIncoming: Boolean = false,
    val transportUsed: String = "INTERNET" // INTERNET, NEARBY
)

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val isIncoming: Boolean,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String // MISSED, COMPLETED, REJECTED, FAILED
)

@Entity(tableName = "ai_workspace_sessions")
data class AIWorkspaceSession(
    @PrimaryKey val id: String,
    val title: String,
    val toolMode: String, // GENERAL, CODING, DEBUGGING, WRITING, RESEARCH, STUDY, DATA, APP_BUILDING, LANGUAGE
    val timestamp: Long = System.currentTimeMillis()
)
