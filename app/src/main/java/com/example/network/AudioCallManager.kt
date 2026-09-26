package com.example.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioCallManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var durationJob: Job? = null

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds

    sealed class CallState {
        object Idle : CallState()
        data class Outgoing(val peerName: String) : CallState()
        data class Incoming(val peerName: String) : CallState()
        data class Connected(val peerName: String) : CallState()
        data class Disconnected(val reason: String) : CallState()
    }

    fun startOutgoingCall(peerName: String) {
        _callState.value = CallState.Outgoing(peerName)
        _callDurationSeconds.value = 0
        _isMuted.value = false
        _isSpeakerOn.value = false

        // Simulate peer answering after 3 seconds
        scope.launch {
            delay(3000)
            if (_callState.value is CallState.Outgoing) {
                connectCall(peerName)
            }
        }
    }

    fun receiveIncomingCall(peerName: String) {
        _callState.value = CallState.Incoming(peerName)
        _callDurationSeconds.value = 0
        _isMuted.value = false
        _isSpeakerOn.value = false
    }

    fun acceptIncomingCall() {
        val state = _callState.value
        if (state is CallState.Incoming) {
            connectCall(state.peerName)
        }
    }

    private fun connectCall(peerName: String) {
        _callState.value = CallState.Connected(peerName)
        startDurationCounter()
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun endCall() {
        durationJob?.cancel()
        _callState.value = CallState.Disconnected("Call ended by user")
        scope.launch {
            delay(2000)
            _callState.value = CallState.Idle
        }
    }

    private fun startDurationCounter() {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (true) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }
}
