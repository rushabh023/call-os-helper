package com.example.helper_application.recording

import android.content.Context
import com.example.helper_application.util.AppLog

object RecordingPreferences {

    private const val PREFS = "call_recording_prefs"
    private const val KEY_AUTO_RECORD = "auto_record_enabled"
    private const val KEY_LAST_ERROR = "last_error"
    private const val KEY_RECENT_FILES = "recent_files"
    private const val KEY_APP_CONNECTOR = "app_connector_enabled"
    private const val KEY_UNSENT_COUNT = "unsent_count"
    private const val KEY_FOLDER_READY = "folder_ready"
    private const val KEY_PREFERRED_ENGINE = "preferred_engine"
    private const val KEY_LAST_STATUS = "last_recording_status"
    private const val KEY_LAST_ENGINE = "last_recording_engine"
    private const val KEY_LAST_FILE = "last_recording_file"
    private const val KEY_LAST_TIMESTAMP = "last_recording_timestamp"
    private const val KEY_LAST_AUDIO_SOURCE = "last_audio_source"
    private const val KEY_DUAL_CAPTURE_ENABLED = "dual_capture_enabled"
    private const val KEY_SHIZUKU_ENABLED = "shizuku_recording_enabled"
    private const val KEY_ACR_STYLE = "acr_style_recording"
    private const val KEY_SPEAKER_BOOST = "speaker_boost_during_call"
    private const val KEY_SHIZUKU_OPT_IN = "shizuku_opt_in"
    private const val KEY_STRICT_SHIZUKU_TEST = "strict_shizuku_test_mode"
    private const val KEY_ENGINE_PENALTY_PREFIX = "engine_penalty_"
    private const val KEY_ZERO_SIGNAL_STREAK = "zero_signal_streak"
    private const val KEY_INCALL_CAPTURE_BLOCKED = "incall_capture_blocked"
    private const val KEY_LAST_CALL_HEADSET = "last_call_headset_connected"

    fun isAutoRecordEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_RECORD, false)
    }

    fun setAutoRecordEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_RECORD, enabled)
            .apply()
    }

    fun getLastError(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_ERROR, null)
    }

    fun setLastError(context: Context, message: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_ERROR, message)
            .apply()
        if (message != null) {
            AppLog.Recording.w("Last error set: $message")
        } else {
            AppLog.Recording.d("Last error cleared")
        }
    }

    fun addSavedRecording(context: Context, fileName: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_RECENT_FILES, "") ?: ""
        val list = if (current.isEmpty()) mutableListOf() else current.split("\n").toMutableList()
        list.add(0, fileName)
        while (list.size > 20) list.removeAt(list.lastIndex)
        prefs.edit().putString(KEY_RECENT_FILES, list.joinToString("\n")).apply()
    }

    fun getRecentRecordings(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_RECENT_FILES, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("\n")
    }

    fun setAppConnectorEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_APP_CONNECTOR, enabled)
            .apply()
    }

    fun isAppConnectorEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_APP_CONNECTOR, false)
    }

    fun getUnsentRecordCount(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_UNSENT_COUNT, 0)
    }

    fun setUnsentRecordCount(context: Context, count: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_UNSENT_COUNT, count.coerceAtLeast(0))
            .apply()
    }

    fun isFolderReady(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_FOLDER_READY, false)
    }

    fun setFolderReady(context: Context, ready: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FOLDER_READY, ready)
            .apply()
    }

    fun getPreferredEngine(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PREFERRED_ENGINE, null)
    }

    fun setPreferredEngine(context: Context, engineId: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFERRED_ENGINE, engineId)
            .apply()
        if (engineId != null) {
            AppLog.Engine.i("Preferred engine saved: $engineId")
        }
    }

    fun setLastRecordingStatus(
        context: Context,
        status: LastRecordingStatus,
        engineId: String? = null,
        fileName: String? = null
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_STATUS, status.name)
            .putLong(KEY_LAST_TIMESTAMP, System.currentTimeMillis())
            .apply {
                if (engineId != null) putString(KEY_LAST_ENGINE, engineId)
                if (fileName != null) putString(KEY_LAST_FILE, fileName)
            }
            .apply()
        val pathNote = if (fileName != null && status == LastRecordingStatus.SAVED) {
            ", fileManagerPath=Internal storage/Documents/MesValidationCallRecorder/$fileName"
        } else ""
        AppLog.Recording.i(
            "Status updated: ${status.name}" +
                (engineId?.let { ", engine=$it" } ?: "") +
                (fileName?.let { ", file=$it" } ?: "") +
                pathNote
        )
    }

    fun getLastRecordingStatus(context: Context): LastRecordingStatus? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_STATUS, null) ?: return null
        return runCatching { LastRecordingStatus.valueOf(raw) }.getOrNull()
    }

    fun getLastRecordingEngine(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_ENGINE, null)
    }

    fun getLastRecordingFile(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_FILE, null)
    }

    fun getLastRecordingTimestamp(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_TIMESTAMP, 0L)
    }

    fun setLastAudioSource(context: Context, sourceLabel: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_AUDIO_SOURCE, sourceLabel)
            .apply()
    }

    fun getLastAudioSource(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_AUDIO_SOURCE, null)
    }

    fun isDualCaptureEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DUAL_CAPTURE_ENABLED, false) &&
            MediaProjectionHolder.isReady()
    }

    fun setDualCaptureEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DUAL_CAPTURE_ENABLED, enabled)
            .apply()
    }

    fun isShizukuRecordingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHIZUKU_ENABLED, false)
    }

    fun setShizukuRecordingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHIZUKU_ENABLED, enabled)
            .apply()
    }

    /** Default false — global recommendation is Shizuku/advanced when available. */
    fun isAcrStyleRecording(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACR_STYLE, false)
    }

    /**
     * Cube/ACR compatibility path when Shizuku is blocked or user opted into ACR style.
     * Uses AMR + VOICE_RECOGNITION + in-communication mode (not MODE_IN_CALL).
     */
    fun isCubeCompatibilityMode(context: Context): Boolean {
        return isAcrStyleRecording(context) ||
            com.example.helper_application.shizuku.ShizukuManager.readinessReason() == "service_bind_blocked"
    }

    /** Cube FAQ recommends ~5s auto-delay before starting capture on some Samsung builds. */
    fun recordingStartDelayMs(context: Context): Long {
        return if (isCubeCompatibilityMode(context)) 5_000L else 900L
    }

    fun setAcrStyleRecording(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACR_STYLE, enabled)
            .apply()
    }

    /** Turns on speakerphone during calls so the mic hears the other party (ACR behavior). */
    fun isSpeakerBoostEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SPEAKER_BOOST, true)
    }

    fun setSpeakerBoostEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SPEAKER_BOOST, enabled)
            .apply()
    }

    fun isShizukuOptIn(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHIZUKU_OPT_IN, true)
    }

    fun setShizukuOptIn(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHIZUKU_OPT_IN, enabled)
            .apply()
    }

    /**
     * Strict test mode: do not fallback to non-Shizuku engines in advanced mode.
     * Enabled by default so two-way capability can be validated with a clear yes/no result.
     */
    fun isStrictShizukuTestMode(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_STRICT_SHIZUKU_TEST, false)
    }

    fun setStrictShizukuTestMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_STRICT_SHIZUKU_TEST, enabled)
            .apply()
    }

    fun getEnginePenalty(context: Context, engineId: String): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("$KEY_ENGINE_PENALTY_PREFIX$engineId", 0)
    }

    fun incrementEnginePenalty(context: Context, engineId: String) {
        val current = getEnginePenalty(context, engineId)
        val next = (current + 1).coerceAtMost(10)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt("$KEY_ENGINE_PENALTY_PREFIX$engineId", next)
            .apply()
        AppLog.Engine.w("Engine penalty incremented: $engineId $current->$next")
    }

    fun reduceEnginePenalty(context: Context, engineId: String) {
        val current = getEnginePenalty(context, engineId)
        val next = (current - 1).coerceAtLeast(0)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt("$KEY_ENGINE_PENALTY_PREFIX$engineId", next)
            .apply()
        AppLog.Engine.i("Engine penalty reduced: $engineId $current->$next")
    }

    fun getZeroSignalStreak(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_ZERO_SIGNAL_STREAK, 0)
    }

    fun isInCallCaptureBlocked(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_INCALL_CAPTURE_BLOCKED, false)
    }

    fun observeLikelySilentCapture(context: Context, reason: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = (prefs.getInt(KEY_ZERO_SIGNAL_STREAK, 0) + 1).coerceAtMost(20)
        val markBlocked = next >= 3 && isZeroSignalReason(reason)
        prefs.edit()
            .putInt(KEY_ZERO_SIGNAL_STREAK, next)
            .apply {
                if (markBlocked) putBoolean(KEY_INCALL_CAPTURE_BLOCKED, true)
            }
            .apply()
        AppLog.Engine.w(
            "silent_streak_update: streak=$next reason=$reason blocked=${if (markBlocked) "true" else "false"}"
        )
    }

    fun observeAudibleCapture(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ZERO_SIGNAL_STREAK, 0)
            .putBoolean(KEY_INCALL_CAPTURE_BLOCKED, false)
            .apply()
        AppLog.Engine.i("silent_streak_reset: audible capture detected")
    }

    private fun isZeroSignalReason(reason: String): Boolean {
        return reason.startsWith("wav_peak_low:0") ||
            reason.startsWith("pcm_peak_low:0") ||
            reason.startsWith("m4a_peak_zero:0")
    }

    fun setLastCallHeadsetConnected(context: Context, connected: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LAST_CALL_HEADSET, connected)
            .apply()
    }

    fun wasLastCallHeadsetConnected(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LAST_CALL_HEADSET, false)
    }
}

enum class LastRecordingStatus {
    RECORDING,
    SAVED,
    FAILED_START,
    FAILED_EMPTY,
    FAILED_SAVE
}

fun engineDisplayName(engineId: String?): String = when (engineId) {
    "shizuku_voice_call" -> "Shizuku two-way (M4A)"
    "dual_playback_mic_wav" -> "Two-way (playback + mic WAV)"
    "media_recorder_amr" -> "MediaRecorder (AMR, Cube-style)"
    "audio_record_8k_voice" -> "AudioRecord 8kHz (voice fallback)"
    "media_recorder_mic", "media_recorder_m4a" -> "MediaRecorder (M4A)"
    "audio_record_mic_wav" -> "AudioRecord (WAV)"
    null, "" -> "Not set yet"
    else -> engineId
}
