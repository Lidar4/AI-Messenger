package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "",
    val displayName: String = "",
    val username: String = "",
    val profilePhotoUri: String? = null,
    val bio: String = "Hey there! I am using AI Messenger.",
    val isOnline: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis(),
    val isMe: Boolean = false
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val isGroup: Boolean = false,
    val participantsJson: String = "",
    val lastMessageText: String? = null,
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT", // SENT, DELIVERED, READ
    val replyToId: String? = null,
    val replyToText: String? = null,
    val attachmentPath: String? = null,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null, // IMAGE, VIDEO, DOCUMENT, VOICE, NONE
    val isStarred: Boolean = false,
    val isIncoming: Boolean = false,
    val transportUsed: String = "INTERNET"
)

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val isIncoming: Boolean = false,
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED"
)

@Entity(tableName = "ai_workspace_sessions")
data class AIWorkspaceSession(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val toolMode: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class StatusUpdate(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String? = null,
    val text: String? = null,
    val mediaUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false
)
