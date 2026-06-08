package com.example.helper_application.dialer.feature.incall

import android.provider.CallLog
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

data class TrackedCallMeta(
    val number: String,
    val displayName: String,
    val direction: Int,
    val startedAtMs: Long,
    var connectedAtMs: Long? = null
)

object CallSessionController {
    private val _state = MutableStateFlow(CallSessionState())
    val state: StateFlow<CallSessionState> = _state.asStateFlow()

    private val callbacks = mutableMapOf<Call, Call.Callback>()
    private val uiRelaunchHandlers = mutableMapOf<Call, (Call) -> Unit>()
    private val callMeta = mutableMapOf<Call, TrackedCallMeta>()

    fun onCallAdded(
        call: Call,
        displayName: String,
        phoneNumber: String,
        onStateChange: ((Call) -> Unit)? = null
    ) {
        if (onStateChange != null) {
            uiRelaunchHandlers[call] = onStateChange
        }
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                updateCall(call, displayName, phoneNumber)
                uiRelaunchHandlers[call]?.invoke(call)
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                updateCall(call, displayName, phoneNumber)
            }
        }
        callbacks[call] = callback
        call.registerCallback(callback)
        callMeta[call] = TrackedCallMeta(
            number = phoneNumber,
            displayName = displayName,
            direction = call.details?.callDirection ?: Call.Details.DIRECTION_UNKNOWN,
            startedAtMs = System.currentTimeMillis()
        )
        updateCall(call, displayName, phoneNumber)
    }

    fun onCallRemoved(call: Call): TrackedCallMeta? {
        callbacks.remove(call)?.let { call.unregisterCallback(it) }
        uiRelaunchHandlers.remove(call)
        val meta = callMeta.remove(call)
        meta?.let { finalizeMeta(it, call.state) }
        _state.update { current ->
            val remaining = current.calls.filterNot { it.telecomCall == call }
            current.copy(
                calls = remaining,
                primaryCallId = remaining.firstOrNull()?.telecomCall?.details?.handle?.toString()
            )
        }
        return meta
    }

    fun buildCallLogType(meta: TrackedCallMeta): Int {
        val incoming = meta.direction == Call.Details.DIRECTION_INCOMING
        return when {
            incoming && meta.connectedAtMs == null -> CallLog.Calls.MISSED_TYPE
            incoming -> CallLog.Calls.INCOMING_TYPE
            else -> CallLog.Calls.OUTGOING_TYPE
        }
    }

    fun callDurationSec(meta: TrackedCallMeta): Long {
        val connected = meta.connectedAtMs ?: return 0L
        return ((System.currentTimeMillis() - connected) / 1000L).coerceAtLeast(0L)
    }

    fun setRecording(active: Boolean, elapsedMs: Long = 0L) {
        _state.update { it.copy(isRecording = active, recordingElapsedMs = elapsedMs) }
    }

    fun updateRecordingElapsed(elapsedMs: Long) {
        _state.update { it.copy(recordingElapsedMs = elapsedMs) }
    }

    fun setMute(call: Call, muted: Boolean) {
        MyInCallService.setMicrophoneMuted(muted)
        patchCallFlags(call) { it.copy(isMuted = muted) }
    }

    fun setSpeaker(call: Call, on: Boolean) {
        patchCallFlags(call) { it.copy(isOnSpeaker = on) }
    }

    fun primaryCall(): Call? =
        _state.value.calls.firstOrNull()?.telecomCall

    fun primaryUiCall(): UiCall? = _state.value.calls.firstOrNull()

    fun primaryConnectedAtMs(): Long? {
        val call = primaryCall() ?: return null
        val telecomConnect = call.details?.connectTimeMillis ?: 0L
        if (telecomConnect > 0L) return telecomConnect
        return callMeta[call]?.connectedAtMs
    }

    fun isEffectivelyConnected(): Boolean {
        val call = primaryCall() ?: return false
        when (call.state) {
            Call.STATE_ACTIVE, Call.STATE_HOLDING -> return true
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> return false
        }
        val telecomConnect = call.details?.connectTimeMillis ?: 0L
        return telecomConnect > 0L
    }

    fun syncFromTelecom() {
        val call = primaryCall() ?: return
        val ui = primaryUiCall() ?: return
        updateCall(call, ui.displayName, ui.phoneNumber)
    }

    fun hasActiveCall(): Boolean =
        _state.value.calls.any { call ->
            call.state == Call.STATE_NEW ||
                call.state == Call.STATE_CONNECTING ||
                call.state == Call.STATE_SELECT_PHONE_ACCOUNT ||
                call.state == Call.STATE_DIALING ||
                call.state == Call.STATE_RINGING ||
                call.state == Call.STATE_ACTIVE ||
                call.state == Call.STATE_HOLDING
        }

    fun clear() {
        callbacks.keys.forEach { call ->
            callbacks[call]?.let { call.unregisterCallback(it) }
        }
        callbacks.clear()
        uiRelaunchHandlers.clear()
        callMeta.clear()
        _state.value = CallSessionState()
    }

    private fun finalizeMeta(meta: TrackedCallMeta, state: Int) {
        if (state == Call.STATE_ACTIVE || state == Call.STATE_HOLDING) {
            meta.connectedAtMs = meta.connectedAtMs ?: System.currentTimeMillis()
        }
    }

    private fun updateCall(call: Call, displayName: String, phoneNumber: String) {
        if (call.state == Call.STATE_ACTIVE || call.state == Call.STATE_HOLDING) {
            callMeta[call]?.connectedAtMs = callMeta[call]?.connectedAtMs ?: System.currentTimeMillis()
        }
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
