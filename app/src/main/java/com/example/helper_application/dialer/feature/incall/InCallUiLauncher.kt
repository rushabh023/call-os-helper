package com.example.helper_application.dialer.feature.incall

import android.content.Context
import android.content.Intent
import android.telecom.Call
import com.example.helper_application.dialer.feature.incoming.IncomingCallActivity
import com.example.helper_application.util.AppLog

object InCallUiLauncher {

    fun show(
        context: Context,
        call: Call,
        name: String,
        number: String,
        answered: Boolean = false
    ) {
        val incoming = call.details?.callDirection == Call.Details.DIRECTION_INCOMING
        val showIncomingScreen = !answered && incoming && (
            call.state == Call.STATE_RINGING || call.state == Call.STATE_NEW
            )
        val activityClass = if (showIncomingScreen) {
            IncomingCallActivity::class.java
        } else {
            InCallActivity::class.java
        }
        val intent = Intent(context, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(InCallActivity.EXTRA_DISPLAY_NAME, name)
            putExtra(InCallActivity.EXTRA_PHONE_NUMBER, number)
            putExtra(InCallActivity.EXTRA_IS_INCOMING, incoming)
        }
        AppLog.i("Launching in-call UI: ${activityClass.simpleName} state=${call.stateLabel()}")
        context.startActivity(intent)
    }

    fun shouldShowForState(state: Int): Boolean =
        state == Call.STATE_NEW ||
            state == Call.STATE_CONNECTING ||
            state == Call.STATE_SELECT_PHONE_ACCOUNT ||
            state == Call.STATE_DIALING ||
            state == Call.STATE_RINGING ||
            state == Call.STATE_ACTIVE ||
            state == Call.STATE_HOLDING

    fun bringToFrontIfNeeded(context: Context) {
        if (context is InCallActivity || context is IncomingCallActivity) return
        val uiCall = CallSessionController.state.value.calls.firstOrNull() ?: return
        if (!CallSessionController.hasActiveCall()) return
        show(context, uiCall.telecomCall, uiCall.displayName, uiCall.phoneNumber)
    }

    private fun Call.stateLabel(): String = when (state) {
        Call.STATE_NEW -> "NEW"
        Call.STATE_CONNECTING -> "CONNECTING"
        Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_ACCOUNT"
        Call.STATE_DIALING -> "DIALING"
        Call.STATE_RINGING -> "RINGING"
        Call.STATE_HOLDING -> "HOLDING"
        Call.STATE_ACTIVE -> "ACTIVE"
        Call.STATE_DISCONNECTING -> "DISCONNECTING"
        Call.STATE_DISCONNECTED -> "DISCONNECTED"
        else -> "UNKNOWN($state)"
    }
}
