package com.example.dialer.feature.incall

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import com.example.dialer.core.data.repository.BlocklistRepository
import com.example.dialer.feature.incoming.IncomingCallActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MyInCallService : InCallService() {

    @Inject lateinit var blocklistRepository: BlocklistRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        val name = call.details?.callerDisplayName?.toString().orEmpty().ifBlank { number }
        scope.launch {
            if (blocklistRepository.isBlocked(number)) {
                Timber.i("Blocked incoming/outgoing call: %s", number)
                call.disconnect()
                return@launch
            }
            CallSessionController.onCallAdded(call, name, number)
            launchInCallUi(call, name, number)
        }
    }

    override fun onCallRemoved(call: Call) {
        CallSessionController.onCallRemoved(call)
        if (CallSessionController.state.value.calls.isEmpty()) {
            CallSessionController.clear()
        }
        super.onCallRemoved(call)
    }

    private fun launchInCallUi(call: Call, name: String, number: String) {
        val ringing = call.state == Call.STATE_RINGING
        val activityClass = if (ringing) IncomingCallActivity::class.java else InCallActivity::class.java
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(InCallActivity.EXTRA_DISPLAY_NAME, name)
            putExtra(InCallActivity.EXTRA_PHONE_NUMBER, number)
        }
        startActivity(intent)
    }
}
