package com.example.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioCallManager {

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
        // No fake connection: real WebRTC transport is not implemented yet.
        _callState.value = CallState.Disconnected("Voice calls are not available yet")
        _callState.value = CallState.Idle
    }

    fun receiveIncomingCall(peerName: String) {
        _callState.value = CallState.Incoming(peerName)
    }

    fun acceptIncomingCall() {
        _callState.value = CallState.Disconnected("VoIP Calls (WebRTC) not yet implemented")
        _callState.value = CallState.Idle
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun endCall() {
        _callState.value = CallState.Disconnected("Call ended")
        scope.launch {
            delay(1000)
            _callState.value = CallState.Idle
        }
    }
}
