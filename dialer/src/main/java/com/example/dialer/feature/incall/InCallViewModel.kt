package com.example.dialer.feature.incall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dialer.core.data.local.entity.CallRecordingEntity
import com.example.dialer.core.data.repository.RecordingRepository
import com.example.dialer.feature.recordings.RecordingManager
import com.example.dialer.feature.settings.RecordingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class InCallViewModel @Inject constructor(
    private val recordingManager: RecordingManager,
    private val recordingRepository: RecordingRepository,
    private val recordingPreferences: RecordingPreferences
) : ViewModel() {

    val sessionState = CallSessionController.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CallSessionController.state.value
    )

    val recordingState = recordingManager.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        recordingManager.state.value
    )

    private var timerJob: Job? = null

    fun toggleRecording(contactName: String?, phoneNumber: String?) {
        if (recordingManager.isRecording()) {
            stopRecording(contactName, phoneNumber)
        } else {
            if (!recordingPreferences.isDisclaimerAccepted()) return
            if (recordingManager.start(contactName, phoneNumber)) {
                startTimer()
                CallSessionController.setRecording(true)
            }
        }
    }

    fun maybeAutoRecord(contactName: String?, phoneNumber: String?, isUnknown: Boolean) {
        if (!recordingPreferences.isDisclaimerAccepted()) return
        if (recordingManager.isRecording()) return
        val shouldRecord = when {
            recordingPreferences.isAutoRecordAll() -> true
            recordingPreferences.isAutoRecordUnknownOnly() && isUnknown -> true
            else -> false
        }
        if (shouldRecord && recordingManager.start(contactName, phoneNumber)) {
            startTimer()
            CallSessionController.setRecording(true)
        }
    }

    private fun stopRecording(contactName: String?, phoneNumber: String?) {
        timerJob?.cancel()
        val result = recordingManager.stop() ?: return
        CallSessionController.setRecording(false)
        viewModelScope.launch {
            recordingRepository.save(
                CallRecordingEntity(
                    filePath = result.filePath,
                    displayName = result.displayName,
                    phoneNumber = phoneNumber,
                    contactName = contactName,
                    createdAt = Date(),
                    durationMs = result.durationMs,
                    fileSizeBytes = result.fileSizeBytes,
                    audioSourceLabel = result.audioSourceLabel
                )
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val started = System.currentTimeMillis()
            while (isActive) {
                CallSessionController.updateRecordingElapsed(System.currentTimeMillis() - started)
                delay(500)
            }
        }
    }

    override fun onCleared() {
        if (recordingManager.isRecording()) {
            recordingManager.stop()
            CallSessionController.setRecording(false)
        }
        timerJob?.cancel()
        super.onCleared()
    }
}
