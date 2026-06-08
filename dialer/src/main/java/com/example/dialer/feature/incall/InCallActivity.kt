package com.example.dialer.feature.incall

import android.media.AudioManager
import android.os.Bundle
import android.telecom.Call
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dialer.databinding.ActivityInCallBinding
import com.example.dialer.feature.settings.RecordingDisclaimerDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class InCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInCallBinding
    private val viewModel: InCallViewModel by viewModels()
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        audioManager = getSystemService(AudioManager::class.java)

        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME).orEmpty()
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER).orEmpty()
        binding.callerName.text = displayName.ifBlank { getString(com.example.dialer.R.string.unknown_caller) }
        binding.callerNumber.text = phoneNumber

        binding.btnEndCall.setOnClickListener { endPrimaryCall() }
        binding.btnMute.setOnClickListener { toggleMute() }
        binding.btnSpeaker.setOnClickListener { toggleSpeaker() }
        binding.btnHold.setOnClickListener { toggleHold() }
        binding.btnRecord.setOnClickListener { onRecordClicked(displayName, phoneNumber) }
        binding.btnKeypad.setOnClickListener { binding.dtmfPanel.visibility = View.VISIBLE }
        binding.btnCloseDtmf.setOnClickListener { binding.dtmfPanel.visibility = View.GONE }
        setupDtmfPad()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessionState.collect { state ->
                    val call = state.calls.firstOrNull() ?: return@collect
                    binding.callState.text = stateLabel(call.state)
                    if (call.state == Call.STATE_ACTIVE && !viewModel.recordingState.value.isRecording) {
                        viewModel.maybeAutoRecord(
                            displayName.takeIf { it.isNotBlank() },
                            phoneNumber.takeIf { it.isNotBlank() },
                            isUnknown = displayName.isBlank()
                        )
                    }
                    if (call.state == Call.STATE_DISCONNECTED) {
                        finish()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recordingState.collect { rec ->
                    binding.btnRecord.text = if (rec.isRecording) {
                        formatElapsed(rec.startedAtMs)
                    } else {
                        getString(com.example.dialer.R.string.record)
                    }
                    binding.recordingDot.visibility = if (rec.isRecording) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun onRecordClicked(displayName: String, phoneNumber: String) {
        RecordingDisclaimerDialog.showIfNeeded(this) {
            viewModel.toggleRecording(
                displayName.takeIf { it.isNotBlank() },
                phoneNumber.takeIf { it.isNotBlank() }
            )
        }
    }

    private fun toggleMute() {
        val call = CallSessionController.primaryCall() ?: return
        val muted = !binding.btnMute.isSelected
        CallSessionController.setMute(call, muted)
        binding.btnMute.isSelected = muted
    }

    private fun toggleSpeaker() {
        val on = !binding.btnSpeaker.isSelected
        audioManager.isSpeakerphoneOn = on
        binding.btnSpeaker.isSelected = on
        CallSessionController.primaryCall()?.let { CallSessionController.setSpeaker(it, on) }
    }

    private fun toggleHold() {
        val call = CallSessionController.primaryCall() ?: return
        if (call.state == Call.STATE_HOLDING) call.unhold() else call.hold()
    }

    private fun endPrimaryCall() {
        CallSessionController.primaryCall()?.disconnect()
        finish()
    }

    private fun setupDtmfPad() {
        val buttons = listOf(
            binding.dtmf0 to '0', binding.dtmf1 to '1', binding.dtmf2 to '2',
            binding.dtmf3 to '3', binding.dtmf4 to '4', binding.dtmf5 to '5',
            binding.dtmf6 to '6', binding.dtmf7 to '7', binding.dtmf8 to '8',
            binding.dtmf9 to '9', binding.dtmfStar to '*', binding.dtmfHash to '#'
        )
        buttons.forEach { (view, tone) ->
            view.setOnClickListener {
                CallSessionController.primaryCall()?.playDtmfTone(tone)
            }
        }
    }

    private fun stateLabel(state: Int): String = when (state) {
        Call.STATE_DIALING -> getString(com.example.dialer.R.string.call_state_dialing)
        Call.STATE_RINGING -> getString(com.example.dialer.R.string.call_state_ringing)
        Call.STATE_ACTIVE -> getString(com.example.dialer.R.string.call_state_active)
        Call.STATE_HOLDING -> getString(com.example.dialer.R.string.call_state_hold)
        Call.STATE_DISCONNECTED -> getString(com.example.dialer.R.string.call_state_disconnected)
        else -> getString(com.example.dialer.R.string.call_state_connecting)
    }

    private fun formatElapsed(startedAtMs: Long): String {
        val elapsed = System.currentTimeMillis() - startedAtMs
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60
        return String.format(Locale.getDefault(), "REC %d:%02d", minutes, seconds)
    }

    companion object {
        const val EXTRA_DISPLAY_NAME = "display_name"
        const val EXTRA_PHONE_NUMBER = "phone_number"
    }
}
