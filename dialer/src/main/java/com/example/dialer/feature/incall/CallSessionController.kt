package com.example.dialer.feature.incall

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiCall(
    val telecomCall: Call,
    val displayName: String,
    val phoneNumber: String,
    val state: Int,
    val isMuted: Boolean = false,
    val isOnSpeaker: Boolean = false
)

data class CallSessionState(
    val calls: List<UiCall> = emptyList(),
    val primaryCallId: String? = null,
    val isRecording: Boolean = false,
    val recordingElapsedMs: Long = 0L
)

object CallSessionController {
    private val _state = MutableStateFlow(CallSessionState())
    val state: StateFlow<CallSessionState> = _state.asStateFlow()

    private val callbacks = mutableMapOf<Call, Call.Callback>()

    fun onCallAdded(call: Call, displayName: String, phoneNumber: String) {
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                updateCall(call, displayName, phoneNumber)
            }
            override fun onDetailsChanged(call: Call, details: Call.Details) {
                updateCall(call, displayName, phoneNumber)
            }
        }
        callbacks[call] = callback
        call.registerCallback(callback)
        updateCall(call, displayName, phoneNumber)
    }

    fun onCallRemoved(call: Call) {
        callbacks.remove(call)?.let { call.unregisterCallback(it) }
        _state.update { current ->
            val remaining = current.calls.filterNot { it.telecomCall == call }
            current.copy(
                calls = remaining,
                primaryCallId = remaining.firstOrNull()?.telecomCall?.details?.handle?.toString()
            )
        }
    }

    fun setRecording(active: Boolean, elapsedMs: Long = 0L) {
        _state.update { it.copy(isRecording = active, recordingElapsedMs = elapsedMs) }
    }

    fun updateRecordingElapsed(elapsedMs: Long) {
        _state.update { it.copy(recordingElapsedMs = elapsedMs) }
    }

    fun setMute(call: Call, muted: Boolean) {
        if (muted) call.mute() else call.unmute()
        patchCallFlags(call) { it.copy(isMuted = muted) }
    }

    fun setSpeaker(call: Call, on: Boolean) {
        // Audio route is managed in InCallActivity via AudioManager; store UI flag here.
        patchCallFlags(call) { it.copy(isOnSpeaker = on) }
    }

    fun primaryCall(): Call? =
        _state.value.calls.firstOrNull()?.telecomCall

    fun clear() {
        callbacks.keys.forEach { call ->
            callbacks[call]?.let { call.unregisterCallback(it) }
        }
        callbacks.clear()
        _state.value = CallSessionState()
    }

    private fun updateCall(call: Call, displayName: String, phoneNumber: String) {
        val ui = UiCall(
            telecomCall = call,
            displayName = displayName,
            phoneNumber = phoneNumber,
            state = call.state
        )
        _state.update { current ->
            val others = current.calls.filterNot { it.telecomCall == call }
            current.copy(
                calls = others + ui,
                primaryCallId = call.details?.handle?.toString()
            )
        }
    }

    private fun patchCallFlags(call: Call, patch: (UiCall) -> UiCall) {
        _state.update { current ->
            current.copy(
                calls = current.calls.map {
                    if (it.telecomCall == call) patch(it) else it
                }
            )
        }
    }
}
