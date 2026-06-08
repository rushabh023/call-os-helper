package com.example.helper_application.dialer.feature.incall

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helper_application.bridge.MesValidationConnectionBridge
import com.example.helper_application.dialer.core.data.local.entity.CallRecordingEntity
import com.example.helper_application.dialer.core.data.repository.RecordingRepository
import com.example.helper_application.dialer.feature.recordings.RecordingForegroundService
import com.example.helper_application.dialer.feature.recordings.RecordingSessionState
import com.example.helper_application.dialer.feature.settings.DialerRecordingPreferences
import com.example.helper_application.recording.CallDirection
import com.example.helper_application.recording.CallRecorder
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.recording.RecordingStorage
import com.example.helper_application.util.AppLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class InCallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callRecorder: CallRecorder,
    private val recordingRepository: RecordingRepository,
    private val recordingPreferences: DialerRecordingPreferences
) : ViewModel() {

    val sessionState = CallSessionController.state.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        CallSessionController.state.value
    )

    private val _recordingState = MutableStateFlow(RecordingSessionState())
    val recordingState = _recordingState.asStateFlow()

    private var timerJob: Job? = null
    private var callDirection: CallDirection = CallDirection.OUTGOING

    fun setCallDirection(incoming: Boolean) {
        callDirection = if (incoming) CallDirection.INCOMING else CallDirection.OUTGOING
    }

    fun toggleRecording(contactName: String?, phoneNumber: String?): Boolean {
        return if (callRecorder.isRecording) {
            stopRecording(contactName, phoneNumber)
            true
        } else {
            startRecording(contactName, phoneNumber)
        }
    }

    fun stopRecordingIfActive(contactName: String?, phoneNumber: String?) {
        if (callRecorder.isRecording) {
            stopRecording(contactName, phoneNumber)
        }
    }

    fun maybeAutoRecord(contactName: String?, phoneNumber: String?, isUnknown: Boolean) {
        if (!recordingPreferences.isDisclaimerAccepted()) return
        if (callRecorder.isRecording) return
        if (!CallSessionController.isEffectivelyConnected()) return
        val shouldRecord = when {
            recordingPreferences.isAutoRecordAll() -> true
            recordingPreferences.isAutoRecordUnknownOnly() && isUnknown -> true
            else -> false
        }
        if (shouldRecord) startRecording(contactName, phoneNumber)
    }

    private fun startRecording(contactName: String?, phoneNumber: String?): Boolean {
        if (!recordingPreferences.isDisclaimerAccepted()) return false
        if (!CallSessionController.hasActiveCall()) {
            AppLog.w("Recording skipped: no active call")
            return false
        }
        val label = contactName?.takeIf { it.isNotBlank() } ?: phoneNumber.orEmpty().ifBlank { "call" }
        RecordingForegroundService.start(context, label)
        val tempFile = callRecorder.start()
        if (tempFile == null) {
            RecordingForegroundService.stop(context)
            AppLog.w("Recording failed: all engines rejected")
            return false
        }
        notifyMesValidationStart(phoneNumber)
        val startedAt = System.currentTimeMillis()
        _recordingState.value = RecordingSessionState(
            isRecording = true,
            startedAtMs = startedAt,
            displayLabel = label,
            outputPath = tempFile.absolutePath,
            audioSourceLabel = callRecorder.activeEngineId.orEmpty()
        )
        startTimer(startedAt)
        CallSessionController.setRecording(true)
        AppLog.i("In-call recording started: ${tempFile.absolutePath}")
        return true
    }

    private fun notifyMesValidationStart(phoneNumber: String?) {
        if (MesValidationConnectionBridge.isMesValidationInstalled(context)) {
            MesValidationConnectionBridge.startRecording(context, callDirection, phoneNumber)
        }
    }

    private fun stopRecording(contactName: String?, phoneNumber: String?) {
        timerJob?.cancel()
        RecordingForegroundService.stop(context)
        val temp = callRecorder.stop()
        CallSessionController.setRecording(false)
        _recordingState.value = RecordingSessionState()
        if (MesValidationConnectionBridge.isMesValidationInstalled(context)) {
            MesValidationConnectionBridge.stopRecording(context)
        }
        if (temp == null || !temp.exists() || temp.length() <= 0L) {
            temp?.delete()
            AppLog.w("Recording stop: empty or missing file")
            return
        }
        val summary = callRecorder.lastStopSummary
        val extension = summary?.outputExtension ?: temp.extension.ifBlank { "amr" }
        val fileName = RecordingStorage.buildFileName(callDirection, phoneNumber, extension)
        RecordingStorage.ensureFolderExists(context)
        val savedUri = RecordingStorage.saveRecording(context, temp, fileName)
        val destFile = File(RecordingStorage.getPublicFolderFile(), fileName)
        val finalPath = when {
            destFile.exists() && destFile.length() > 0L -> destFile.absolutePath
            temp.exists() && temp.length() > 0L -> temp.absolutePath
            else -> {
                AppLog.w("Recording save failed for $fileName (uri=$savedUri)")
                return
            }
        }
        RecordingPreferences.addSavedRecording(context, File(finalPath).name)
        MesValidationConnectionBridge.notifyRecordingComplete(
            context,
            File(finalPath).name,
            finalPath
        )
        viewModelScope.launch {
            recordingRepository.save(
                CallRecordingEntity(
                    filePath = finalPath,
                    displayName = File(finalPath).nameWithoutExtension,
                    phoneNumber = phoneNumber,
                    contactName = contactName,
                    createdAt = Date(),
                    durationMs = summary?.durationMs ?: 0L,
                    fileSizeBytes = File(finalPath).length(),
                    audioSourceLabel = summary?.audioSource ?: callRecorder.activeEngineId.orEmpty()
                )
            )
        }
        AppLog.i("Recording saved: $finalPath (${File(finalPath).length()} bytes)")
    }

    private fun startTimer(startedAtMs: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startedAtMs
                CallSessionController.updateRecordingElapsed(elapsed)
                _recordingState.value = _recordingState.value.copy(startedAtMs = startedAtMs)
                delay(1_000)
            }
        }
    }

    override fun onCleared() {
        if (callRecorder.isRecording) {
            val uiCall = CallSessionController.primaryUiCall()
            stopRecording(uiCall?.displayName, uiCall?.phoneNumber)
        }
        timerJob?.cancel()
        super.onCleared()
    }
}
