package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseMessengerRepository
import com.example.data.MessengerConversation
import com.example.data.MessengerMessage
import com.example.data.MessengerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    val repository = FirebaseMessengerRepository(application)
    val profile = repository.authState().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val conversations = profile.flatMapLatest { if (it == null) flowOf(emptyList()) else repository.conversations() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedConversation = MutableStateFlow<MessengerConversation?>(null)
    val selectedConversation = _selectedConversation.asStateFlow()
    val messages = _selectedConversation.flatMapLatest {
        if (it == null) flowOf(emptyList()) else repository.messages(it.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _contactResult = MutableStateFlow<MessengerProfile?>(null)
    val contactResult = _contactResult.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) { _error.value = "Email and password are required."; return }
        viewModelScope.launch {
            _busy.value = true
            repository.signIn(email, password).onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    fun signUp(email: String, password: String, name: String, username: String) {
        viewModelScope.launch {
            _busy.value = true
            repository.signUp(email, password, name, username).onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    fun signOut() { repository.signOut(); _selectedConversation.value = null }
    fun clearError() { _error.value = null }
    fun clearContact() { _contactResult.value = null }

    fun findContact(value: String) {
        if (value.isBlank()) return
        viewModelScope.launch {
            _busy.value = true
            repository.findUserByUsername(value)
                .onSuccess { _contactResult.value = it }
                .onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    fun startConversation(profile: MessengerProfile) {
        viewModelScope.launch {
            _busy.value = true
            repository.openConversation(profile).onSuccess { id ->
                _selectedConversation.value = conversations.value.firstOrNull { it.id == id }
                    ?: MessengerConversation(id, profile.uid, profile.username, profile.displayName, "", 0L)
                _contactResult.value = null
            }.onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    fun openConversation(conversation: MessengerConversation) { _selectedConversation.value = conversation }
    fun closeConversation() { _selectedConversation.value = null }

    fun sendMessage(text: String) {
        val conversation = _selectedConversation.value ?: return
        viewModelScope.launch {
            repository.sendMessage(conversation.id, text).onFailure { _error.value = it.message }
        }
    }
}
