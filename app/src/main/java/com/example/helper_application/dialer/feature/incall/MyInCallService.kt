package com.example.helper_application.dialer.feature.incall

import android.provider.CallLog
import android.telecom.Call
import android.telecom.InCallService
import com.example.helper_application.dialer.core.data.repository.BlocklistRepository
import com.example.helper_application.dialer.core.data.repository.CallLogRepository
import com.example.helper_application.dialer.core.utils.DialerCallPlacer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.helper_application.util.AppLog
import javax.inject.Inject

@AndroidEntryPoint
class MyInCallService : InCallService() {

    @Inject lateinit var blocklistRepository: BlocklistRepository
    @Inject lateinit var callLogRepository: CallLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        DialerCallPlacer.clearPlacingInFlight()
        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        val name = call.details?.callerDisplayName?.toString().orEmpty().ifBlank { number }

        CallSessionController.onCallAdded(call, name, number) { updatedCall ->
            if (InCallUiLauncher.shouldShowForState(updatedCall.state)) {
                InCallUiLauncher.show(this@MyInCallService, updatedCall, name, number)
            }
        }
        InCallUiLauncher.show(this, call, name, number)

        val incoming = call.details?.callDirection == Call.Details.DIRECTION_INCOMING ||
            call.state == Call.STATE_RINGING
        if (incoming) {
            scope.launch(Dispatchers.IO) {
                if (blocklistRepository.isBlocked(number)) {
                    AppLog.i("Blocked incoming call disconnected: $number")
                    call.disconnect()
                }
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        val cause = call.details?.disconnectCause
        AppLog.i(
            "Call removed: number=${call.details?.handle?.schemeSpecificPart} " +
                "state=${call.state} cause=${cause?.code} label=${cause?.label}"
        )
        val meta = CallSessionController.onCallRemoved(call)
        meta?.let { tracked ->
            val duration = CallSessionController.callDurationSec(tracked)
            val type = CallSessionController.buildCallLogType(tracked)
            val shouldLog = duration > 0L ||
                tracked.connectedAtMs != null ||
                type == CallLog.Calls.MISSED_TYPE
            if (shouldLog) {
                callLogRepository.logCompletedCall(
                    number = tracked.number,
                    displayName = tracked.displayName,
                    type = type,
                    startedAtMs = tracked.startedAtMs,
                    durationSec = duration
                )
            } else {
                AppLog.w("Skipped call log for failed/zero-duration call: ${tracked.number}")
            }
        }
        if (CallSessionController.state.value.calls.isEmpty()) {
            CallSessionController.clear()
            DialerCallPlacer.clearPlacingInFlight()
        }
        super.onCallRemoved(call)
    }

    companion object {
        @Volatile
        private var instance: MyInCallService? = null

        fun setMicrophoneMuted(muted: Boolean) {
            instance?.setMuted(muted)
        }
    }
}
