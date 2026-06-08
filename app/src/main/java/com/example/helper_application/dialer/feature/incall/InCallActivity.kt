package com.example.helper_application.dialer.feature.incall

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.AvatarHelper
import com.example.helper_application.dialer.feature.settings.RecordingDisclaimerDialog
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class InCallActivity : AppCompatActivity() {

    private val viewModel: InCallViewModel by viewModels()
    private lateinit var audioManager: AudioManager
    private lateinit var callerName: TextView
    private lateinit var callerNumber: TextView
    private lateinit var callState: TextView
    private lateinit var callDuration: TextView
    private lateinit var btnMute: MaterialButton
    private lateinit var btnSpeaker: MaterialButton
    private lateinit var btnRecord: MaterialButton
    private lateinit var recordingDot: View
    private lateinit var dtmfPanel: View

    private var displayName: String = ""
    private var phoneNumber: String = ""
    private var finishing = false
    private var recordingStartedAtMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyInCallWindowFlags()
        setContentView(R.layout.activity_in_call)
        audioManager = getSystemService(AudioManager::class.java)

        callerName = findViewById(R.id.callerName)
        callerNumber = findViewById(R.id.callerNumber)
        callState = findViewById(R.id.callState)
        callDuration = findViewById(R.id.callDuration)
        btnMute = findViewById(R.id.btnMute)
        btnSpeaker = findViewById(R.id.btnSpeaker)
        btnRecord = findViewById(R.id.btnRecord)
        recordingDot = findViewById(R.id.recordingDot)
        dtmfPanel = findViewById(R.id.dtmfPanel)

        bindCallerFromIntent(intent)

        findViewById<MaterialButton>(R.id.btnEndCall).setOnClickListener { endPrimaryCall() }
        btnMute.setOnClickListener { toggleMute() }
        btnSpeaker.setOnClickListener { toggleSpeaker() }
        findViewById<MaterialButton>(R.id.btnHold).setOnClickListener { toggleHold() }
        btnRecord.setOnClickListener { onRecordClicked() }
        findViewById<MaterialButton>(R.id.btnKeypad).setOnClickListener { dtmfPanel.visibility = View.VISIBLE }
        findViewById<MaterialButton>(R.id.btnCloseDtmf).setOnClickListener { dtmfPanel.visibility = View.GONE }
        setupDtmfPad()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessionState.collect { refreshCallUi() }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recordingState.collect { rec ->
                    recordingStartedAtMs = if (rec.isRecording) rec.startedAtMs else 0L
                    recordingDot.visibility = if (rec.isRecording) View.VISIBLE else View.GONE
                    btnRecord.isSelected = rec.isRecording
                    if (!rec.isRecording) {
                        btnRecord.text = getString(R.string.record)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    CallSessionController.syncFromTelecom()
                    refreshCallUi()
                    delay(1_000)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        bindCallerFromIntent(intent)
        finishing = false
    }

    override fun onResume() {
        super.onResume()
        applyInCallWindowFlags()
        CallSessionController.syncFromTelecom()
        refreshCallUi()
    }

    private fun bindCallerFromIntent(intent: android.content.Intent) {
        displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME).orEmpty()
        phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER).orEmpty()
        val isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, false)
        viewModel.setCallDirection(incoming = isIncoming)
        val shownName = displayName.ifBlank { getString(R.string.unknown_caller) }
        callerName.text = shownName
        callerNumber.text = phoneNumber
        AvatarHelper.bindInitials(findViewById(R.id.callerAvatar), shownName)
        callState.text = getString(R.string.call_state_connecting)
        callDuration.visibility = View.GONE
    }

    private fun refreshCallUi() {
        val uiCall = CallSessionController.primaryUiCall()
        val telecomCall = CallSessionController.primaryCall()
        val state = telecomCall?.state ?: uiCall?.state

        if (telecomCall == null && uiCall == null) {
            if (!finishing) {
                callState.text = getString(R.string.call_state_connecting)
                callDuration.visibility = View.GONE
            }
            return
        }

        if (state == Call.STATE_DISCONNECTED) {
            callState.text = getString(R.string.call_state_disconnected)
            callDuration.visibility = View.GONE
            viewModel.stopRecordingIfActive(
                displayName.takeIf { it.isNotBlank() },
                phoneNumber.takeIf { it.isNotBlank() }
            )
            scheduleFinishIfStillEnded()
            return
        }

        val connected = CallSessionController.isEffectivelyConnected()
        val connectedAt = CallSessionController.primaryConnectedAtMs()

        if (connected && connectedAt != null) {
            callDuration.visibility = View.VISIBLE
            callDuration.text = formatDuration(System.currentTimeMillis() - connectedAt)
            callState.text = when (state) {
                Call.STATE_HOLDING -> getString(R.string.call_state_hold)
                else -> getString(R.string.call_state_in_call)
            }
            if (!viewModel.recordingState.value.isRecording) {
                viewModel.maybeAutoRecord(
                    displayName.takeIf { it.isNotBlank() },
                    phoneNumber.takeIf { it.isNotBlank() },
                    isUnknown = displayName.isBlank()
                )
            }
        } else {
            callDuration.visibility = View.GONE
            callState.text = stateLabel(state)
        }

        if (recordingStartedAtMs > 0L) {
            btnRecord.text = formatRecElapsed(recordingStartedAtMs)
        }
    }

    private fun applyInCallWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun scheduleFinishIfStillEnded() {
        if (finishing) return
        finishing = true
        lifecycleScope.launch {
            delay(2_500)
            val stillEnded = !CallSessionController.hasActiveCall()
            if (stillEnded) finish()
            else finishing = false
        }
    }

    private fun onRecordClicked() {
        if (!CallSessionController.hasActiveCall()) {
            Toast.makeText(this, R.string.recording_requires_call, Toast.LENGTH_SHORT).show()
            return
        }
        RecordingDisclaimerDialog.showIfNeeded(this) {
            val ok = viewModel.toggleRecording(
                displayName.takeIf { it.isNotBlank() },
                phoneNumber.takeIf { it.isNotBlank() }
            )
            if (!ok) {
                Toast.makeText(this, R.string.recording_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleMute() {
        val call = CallSessionController.primaryCall() ?: return
        val muted = !btnMute.isSelected
        CallSessionController.setMute(call, muted)
        audioManager.isMicrophoneMute = muted
        btnMute.isSelected = muted
    }

    private fun toggleSpeaker() {
        val on = !btnSpeaker.isSelected
        audioManager.isSpeakerphoneOn = on
        btnSpeaker.isSelected = on
        CallSessionController.primaryCall()?.let { CallSessionController.setSpeaker(it, on) }
    }

    private fun toggleHold() {
        val call = CallSessionController.primaryCall() ?: return
        if (call.state == Call.STATE_HOLDING) call.unhold() else call.hold()
    }

    private fun endPrimaryCall() {
        viewModel.stopRecordingIfActive(
            displayName.takeIf { it.isNotBlank() },
            phoneNumber.takeIf { it.isNotBlank() }
        )
        CallSessionController.primaryCall()?.disconnect()
        finish()
    }

    private fun setupDtmfPad() {
        val buttons = listOf(
            findViewById<MaterialButton>(R.id.dtmf0) to '0',
            findViewById<MaterialButton>(R.id.dtmf1) to '1',
            findViewById<MaterialButton>(R.id.dtmf2) to '2',
            findViewById<MaterialButton>(R.id.dtmf3) to '3',
            findViewById<MaterialButton>(R.id.dtmf4) to '4',
            findViewById<MaterialButton>(R.id.dtmf5) to '5',
            findViewById<MaterialButton>(R.id.dtmf6) to '6',
            findViewById<MaterialButton>(R.id.dtmf7) to '7',
            findViewById<MaterialButton>(R.id.dtmf8) to '8',
            findViewById<MaterialButton>(R.id.dtmf9) to '9',
            findViewById<MaterialButton>(R.id.dtmfStar) to '*',
            findViewById<MaterialButton>(R.id.dtmfHash) to '#'
        )
        buttons.forEach { (view, tone) ->
            view.setOnClickListener {
                CallSessionController.primaryCall()?.playDtmfTone(tone)
            }
        }
    }

    private fun stateLabel(state: Int?): String = when (state) {
        Call.STATE_DIALING -> getString(R.string.call_state_dialing)
        Call.STATE_RINGING -> getString(R.string.call_state_ringing)
        Call.STATE_ACTIVE -> getString(R.string.call_state_active)
        Call.STATE_HOLDING -> getString(R.string.call_state_hold)
        Call.STATE_CONNECTING, Call.STATE_NEW, Call.STATE_SELECT_PHONE_ACCOUNT ->
            getString(R.string.call_state_connecting)
        else -> getString(R.string.call_state_connecting)
    }

    private fun formatDuration(elapsedMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun formatRecElapsed(startedAtMs: Long): String {
        val elapsed = System.currentTimeMillis() - startedAtMs
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60
        return String.format(Locale.getDefault(), "REC %d:%02d", minutes, seconds)
    }

    companion object {
        const val EXTRA_DISPLAY_NAME = "display_name"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_IS_INCOMING = "is_incoming"
    }
}
