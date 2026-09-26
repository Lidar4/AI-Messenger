package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.AudioCallManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "ChatViewModel"
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    val isBackendConfigured: Boolean get() = auth != null && db != null

    val callManager = AudioCallManager()

    private val _currentUser = MutableStateFlow(auth?.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Navigation: AUTH, MAIN, CHAT_CONVERSATION, ACTIVE_CALL
    private val _currentScreen = MutableStateFlow(if (auth?.currentUser != null) "MAIN" else "AUTH")
    val currentScreen: StateFlow<String> = _currentScreen

    // Navigation tabs: CHATS, UPDATES, CALLS, SETTINGS
    private val _activeTab = MutableStateFlow("CHATS")
    val activeTab: StateFlow<String> = _activeTab

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation

    private val _activeMessages = MutableStateFlow<List<Message>>(emptyList())
    val activeMessages: StateFlow<List<Message>> = _activeMessages

    private val _searchedUser = MutableStateFlow<User?>(null)
    val searchedUser: StateFlow<User?> = _searchedUser

    // Status updates
    private val _statuses = MutableStateFlow<List<StatusUpdate>>(emptyList())
    val statuses: StateFlow<List<StatusUpdate>> = _statuses

    // Settings state
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    private val _readReceiptsEnabled = MutableStateFlow(true)
    val readReceiptsEnabled: StateFlow<Boolean> = _readReceiptsEnabled

    private val _onlineStatusVisible = MutableStateFlow(true)
    val onlineStatusVisible: StateFlow<Boolean> = _onlineStatusVisible

    private var convListener: ListenerRegistration? = null
    private var msgListener: ListenerRegistration? = null
    private var statusListener: ListenerRegistration? = null

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                _currentScreen.value = "MAIN"
                fetchUserProfile(user.uid)
                startConversationsListener(user.uid)
                startStatusListener()
                updateFcmToken(user.uid)
            } else {
                _currentScreen.value = "AUTH"
                _userProfile.value = null
                stopListeners()
            }
        }
        auth?.currentUser?.let { user ->
            fetchUserProfile(user.uid)
            startConversationsListener(user.uid)
            startStatusListener()
        }
    }

    fun login(email: String, pass: String) {
        if (!isBackendConfigured) {
            _authError.value = "Firebase is not configured. Add google-services.json to enable accounts and cloud messaging."
            return
        }
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Please fill in all fields"
            return
        }
        _isLoading.value = true
        _authError.value = null
        val firebaseAuth = auth ?: run {
            _authError.value = "Firebase is not configured. Add google-services.json to enable accounts and cloud messaging."
            return
        }
        viewModelScope.launch {
            try {
                firebaseAuth.signInWithEmailAndPassword(email.trim(), pass).await()
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _authError.value = e.localizedMessage ?: "Login failed"
            }
        }
    }

    fun register(email: String, pass: String, username: String, displayName: String) {
        if (!isBackendConfigured) {
            _authError.value = "Firebase is not configured. Add google-services.json to enable accounts and cloud messaging."
            return
        }
        val cleanUsername = username.trim().lowercase().removePrefix("@")
        if (email.isBlank() || pass.isBlank() || cleanUsername.isBlank() || displayName.isBlank()) {
            _authError.value = "All fields are required"
            return
        }
        if (cleanUsername.length < 3 || !cleanUsername.matches(Regex("^[a-z0-9_]+$"))) {
            _authError.value = "Username must be at least 3 chars (letters, numbers, underscores only)"
            return
        }

        _isLoading.value = true
        _authError.value = null
        val firestore = db ?: run {
            _authError.value = "Firebase is not configured. Add google-services.json to enable accounts and cloud messaging."
            return
        }
        val firebaseAuth = auth ?: run {
            _authError.value = "Firebase is not configured. Add google-services.json to enable accounts and cloud messaging."
            return
        }
        viewModelScope.launch {
            try {
                val usernameDoc = firestore.collection("usernames").document(cleanUsername).get().await()
                if (usernameDoc.exists()) {
                    _isLoading.value = false
                    _authError.value = "Username @$cleanUsername is already taken"
                    return@launch
                }

                val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
                val uid = authResult.user?.uid ?: throw Exception("User creation failed")

                firestore.collection("usernames").document(cleanUsername).set(mapOf("uid" to uid)).await()

                val newUser = User(
                    id = uid,
                    displayName = displayName.trim(),
                    username = cleanUsername,
                    isMe = true
                )
                firestore.collection("users").document(uid).set(newUser).await()
                _userProfile.value = newUser
                _isLoading.value = false
                _currentScreen.value = "MAIN"
                startConversationsListener(uid)
                startStatusListener()
            } catch (e: Exception) {
                _isLoading.value = false
                _authError.value = e.localizedMessage ?: "Registration failed"
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (!isBackendConfigured) {
            _authError.value = "Firebase is not configured. Add google-services.json to enable accounts and cloud messaging."
            return
        }
        if (email.isBlank()) {
            _authError.value = "Please enter your email address"
            return
        }
        _isLoading.value = true
        _authError.value = null
        _authSuccessMessage.value = null
        val firebaseAuth = auth ?: run {
            _authError.value = "Firebase is not configured. Add google-services.json to enable accounts and cloud messaging."
            return
        }
        viewModelScope.launch {
            try {
                firebaseAuth.sendPasswordResetEmail(email.trim()).await()
                _isLoading.value = false
                _authSuccessMessage.value = "Password reset email sent!"
            } catch (e: Exception) {
                _isLoading.value = false
                _authError.value = e.localizedMessage ?: "Failed to send reset email"
            }
        }
    }

    fun logout() {
        auth?.signOut()
        stopListeners()
        _currentScreen.value = "AUTH"
    }

    private fun fetchUserProfile(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db?.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    _userProfile.value = doc.toObject(User::class.java)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error fetching user profile", e)
            }
        }
    }

    fun updateProfile(newDisplayName: String, newBio: String) {
        val uid = auth?.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db?.collection("users").document(uid)
                    .update(mapOf("displayName" to newDisplayName, "bio" to newBio)).await()
                fetchUserProfile(uid)
            } catch (e: Exception) {
                Log.e(tag, "Error updating profile", e)
            }
        }
    }

    private fun updateFcmToken(uid: String) {
        runCatching { FirebaseMessaging.getInstance().token }.getOrNull()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                db?.collection("users").document(uid).update("fcmToken", token)
            }
        }
    }

    fun searchUserByUsername(usernameQuery: String) {
        if (!isBackendConfigured) {
            _searchedUser.value = null
            return
        }
        val query = usernameQuery.trim().lowercase().removePrefix("@")
        if (query.isBlank()) {
            _searchedUser.value = null
            return
        }
        viewModelScope.launch {
            try {
                val usernameDoc = db?.collection("usernames").document(query).get().await()
                if (usernameDoc.exists()) {
                    val uid = usernameDoc.getString("uid")
                    if (uid != null && uid != auth?.currentUser?.uid) {
                        val userDoc = firestore.collection("users").document(uid).get().await()
                        if (userDoc.exists()) {
                            _searchedUser.value = userDoc.toObject(User::class.java)
                        } else {
                            _searchedUser.value = null
                        }
                    } else {
                        _searchedUser.value = null
                    }
                } else {
                    _searchedUser.value = null
                }
            } catch (e: Exception) {
                Log.e(tag, "Error searching user", e)
                _searchedUser.value = null
            }
        }
    }

    fun startConversationWith(peer: User, onStarted: (Conversation) -> Unit) {
        if (!isBackendConfigured) return
        val myUid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        val participants = listOf(myUid, peer.id).sorted()
        val convId = participants.joinToString("_")

        viewModelScope.launch {
            try {
                val convRef = firestore.collection("conversations").document(convId)
                val doc = convRef.get().await()
                val conv: Conversation
                if (!doc.exists()) {
                    conv = Conversation(
                        id = convId,
                        title = peer.displayName,
                        isGroup = false,
                        participantsJson = participants.joinToString(","),
                        lastMessageText = "Started conversation",
                        lastMessageTime = System.currentTimeMillis(),
                        unreadCount = 0
                    )
                    convRef.set(conv).await()
                } else {
                    conv = doc.toObject(Conversation::class.java) ?: Conversation(convId, peer.displayName, false, participants.joinToString(","))
                }
                openConversation(conv)
                onStarted(conv)
            } catch (e: Exception) {
                Log.e(tag, "Error starting conversation", e)
            }
        }
    }

    private fun startConversationsListener(uid: String) {
        if (!isBackendConfigured) return
        convListener?.remove()
        convListener = db?.collection("conversations")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(tag, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Conversation::class.java) }
                    _conversations.value = list.sortedByDescending { it.lastMessageTime }
                }
            }
    }

    private fun startStatusListener() {
        if (!isBackendConfigured) return
        statusListener?.remove()
        statusListener = db?.collection("statuses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(StatusUpdate::class.java) }
                    _statuses.value = list
                }
            }
    }

    fun postStatus(text: String) {
        if (!isBackendConfigured) return
        val user = _userProfile.value ?: return
        if (text.isBlank()) return
        val statusId = UUID.randomUUID().toString()
        val status = StatusUpdate(
            id = statusId,
            userId = user.id,
            userName = user.displayName,
            userAvatar = user.profilePhotoUri,
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            try {
                db?.collection("statuses").document(statusId).set(status).await()
            } catch (e: Exception) {
                Log.e(tag, "Error posting status", e)
            }
        }
    }

    fun openConversation(conversation: Conversation) {
        _activeConversation.value = conversation
        _currentScreen.value = "CHAT_CONVERSATION"
        startMessagesListener(conversation.id)
    }

    private fun startMessagesListener(convId: String) {
        if (!isBackendConfigured) return
        msgListener?.remove()
        msgListener = db?.collection("conversations").document(convId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(tag, "Listen messages failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val msgs = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    val formatted = msgs.map { msg ->
                        msg.copy(isIncoming = msg.senderId != auth?.currentUser?.uid)
                    }
                    _activeMessages.value = formatted
                }
            }
    }

    fun closeConversation() {
        msgListener?.remove()
        _activeConversation.value = null
        _currentScreen.value = "MAIN"
    }

    fun sendMessage(text: String, replyToId: String? = null, replyToText: String? = null, attachmentType: String? = null, attachmentUrl: String? = null) {
        if (!isBackendConfigured) return
        val conv = _activeConversation.value ?: return
        val myUid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        val myName = _userProfile.value?.displayName ?: "User"

        if (text.isBlank() && attachmentType == null) return

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val message = Message(
            id = messageId,
            conversationId = conv.id,
            senderId = myUid,
            senderName = myName,
            text = text.trim(),
            timestamp = timestamp,
            status = "DELIVERED",
            replyToId = replyToId,
            replyToText = replyToText,
            attachmentType = attachmentType,
            attachmentUrl = attachmentUrl,
            isIncoming = false,
            transportUsed = "INTERNET"
        )

        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(conv.id)
                    .collection("messages").document(messageId).set(message).await()

                firestore.collection("conversations").document(conv.id)
                    .update(
                        mapOf(
                            "lastMessageText" to if (text.isNotBlank()) text.trim() else "Attachment ($attachmentType)",
                            "lastMessageTime" to timestamp
                        )
                    ).await()
            } catch (e: Exception) {
                Log.e(tag, "Error sending message", e)
            }
        }
    }

    fun toggleStarMessage(messageId: String, currentStarred: Boolean) {
        if (!isBackendConfigured) return
        val conv = _activeConversation.value ?: return
        val firestore = db ?: return
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(conv.id)
                    .collection("messages").document(messageId)
                    .update("starred", !currentStarred).await()
            } catch (e: Exception) {
                Log.e(tag, "Error starring message", e)
            }
        }
    }

    fun deleteMessage(messageId: String) {
        if (!isBackendConfigured) return
        val conv = _activeConversation.value ?: return
        val firestore = db ?: return
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(conv.id)
                    .collection("messages").document(messageId).delete().await()
            } catch (e: Exception) {
                Log.e(tag, "Error deleting message", e)
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun toggleReadReceipts(enabled: Boolean) {
        _readReceiptsEnabled.value = enabled
    }

    fun toggleOnlineStatus(enabled: Boolean) {
        _onlineStatusVisible.value = enabled
    }

    fun navigateToScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun initiateCall(peerName: String) {
        callManager.startOutgoingCall(peerName)
        _currentScreen.value = "ACTIVE_CALL"
    }

    private fun stopListeners() {
        convListener?.remove()
        msgListener?.remove()
        statusListener?.remove()
        convListener = null
        msgListener = null
        statusListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListeners()
    }
}
