package com.example.helper_application.recording

import android.content.Context
import com.example.helper_application.bridge.MesValidationConnectionBridge
import com.example.helper_application.util.AppLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingDiagnosticsSnapshot(
    val mesValidationInstalled: Boolean,
    val localRecordingMode: Boolean,
    val preferredEngine: String?,
    val preferredEngineLabel: String,
    val lastStatus: LastRecordingStatus?,
    val lastStatusLabel: String,
    val lastEngine: String?,
    val lastEngineLabel: String,
    val lastFile: String?,
    val lastError: String?,
    val lastAudioSource: String?,
    val captureVerdictLabel: String,
    val lastTimestampLabel: String,
    val recentFiles: List<String>
)

object RecordingDiagnostics {

    fun snapshot(context: Context): RecordingDiagnosticsSnapshot {
        reconcileStaleRecordingStatus(context)
        val mesInstalled = MesValidationConnectionBridge.isMesValidationInstalled(context)
        val preferred = RecordingPreferences.getPreferredEngine(context)
        val lastStatus = RecordingPreferences.getLastRecordingStatus(context)
        val lastEngine = RecordingPreferences.getLastRecordingEngine(context)
        val ts = RecordingPreferences.getLastRecordingTimestamp(context)
        val timeLabel = if (ts > 0L) {
            SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(ts))
        } else {
            "Never"
        }

        val snapshot = RecordingDiagnosticsSnapshot(
            mesValidationInstalled = mesInstalled,
            localRecordingMode = !mesInstalled,
            preferredEngine = preferred,
            preferredEngineLabel = engineDisplayName(preferred),
            lastStatus = lastStatus,
            lastStatusLabel = statusLabel(context, lastStatus, mesInstalled),
            lastEngine = lastEngine,
            lastEngineLabel = engineDisplayName(lastEngine),
            lastFile = RecordingPreferences.getLastRecordingFile(context),
            lastError = RecordingPreferences.getLastError(context),
            lastAudioSource = RecordingPreferences.getLastAudioSource(context),
            captureVerdictLabel = captureVerdictLabel(context, mesInstalled),
            lastTimestampLabel = timeLabel,
            recentFiles = RecordingPreferences.getRecentRecordings(context).take(3)
        )
        AppLog.Dashboard.d(
            "Snapshot: mode=${if (snapshot.localRecordingMode) "local" else "mes"}, " +
                "preferred=${snapshot.preferredEngine}, status=${snapshot.lastStatus}, " +
                "lastFile=${snapshot.lastFile}, error=${snapshot.lastError}"
        )
        return snapshot
    }

    /** After a save crash, status can stay RECORDING while lastError is set — fix for display. */
    private fun reconcileStaleRecordingStatus(context: Context) {
        val status = RecordingPreferences.getLastRecordingStatus(context) ?: return
        if (status != LastRecordingStatus.RECORDING) return
        val hasError = !RecordingPreferences.getLastError(context).isNullOrBlank()
        val staleMs = System.currentTimeMillis() - RecordingPreferences.getLastRecordingTimestamp(context)
        if (hasError || staleMs > 120_000L) {
            RecordingPreferences.setLastRecordingStatus(context, LastRecordingStatus.FAILED_SAVE)
            AppLog.Dashboard.d("Reconciled stale RECORDING status → FAILED_SAVE")
        }
    }

    private fun statusLabel(context: Context, status: LastRecordingStatus?, mesInstalled: Boolean): String {
        if (mesInstalled) return "Delegated to Mes Validation app"
        if (RecordingPreferences.isInCallCaptureBlocked(context)) {
            return "Device blocks in-call capture on this build"
        }
        return when (status) {
            LastRecordingStatus.RECORDING -> "Recording in progress…"
            LastRecordingStatus.SAVED -> if (RecordingPreferences.wasLastCallHeadsetConnected(context)) {
                "Saved — your voice captured; unplug headset for two-way"
            } else {
                "Last call saved successfully"
            }
            LastRecordingStatus.FAILED_START -> "Could not start recording"
            LastRecordingStatus.FAILED_EMPTY -> "No audio captured"
            LastRecordingStatus.FAILED_SAVE -> "Could not save file"
            null -> "No call recorded yet on this device"
        }
    }

    private fun captureVerdictLabel(context: Context, mesInstalled: Boolean): String {
        if (mesInstalled) return "Handled by Mes Validation app"
        return if (RecordingPreferences.isInCallCaptureBlocked(context)) {
            "Unsupported on this device build (repeated zero-signal call captures)"
        } else {
            "Not blocked yet (adaptive engine testing active)"
        }
    }
}
