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

    // Cube ACR-style settings (Recording / Backup / Misc screens)
    private const val KEY_PHONE_AUDIO_SOURCE = "cube_phone_audio_source"
    private const val KEY_VOIP_AUDIO_SOURCE = "cube_voip_audio_source"
    private const val KEY_PHONE_DELAY_MS = "cube_phone_delay_ms"
    private const val KEY_VOIP_DELAY_MS = "cube_voip_delay_ms"
    private const val KEY_PHONE_CLARITY = "cube_phone_clarity"
    private const val KEY_VOIP_CLARITY = "cube_voip_clarity"
    private const val KEY_FORCE_IN_COMMUNICATION = "cube_force_in_communication"
    private const val KEY_FORCE_IN_CALL_VOIP = "cube_force_in_call_voip"
    private const val KEY_SKIP_HEADSET_CALLS = "cube_skip_headset_calls"
    private const val KEY_MAXIMIZE_INCALL_VOLUME = "cube_maximize_incall_volume"
    private const val KEY_RECORD_CELLULAR = "cube_record_cellular"
    private const val KEY_RECORD_MEET = "cube_record_meet"
    private const val KEY_AUTOSTART_RECORDING = "cube_autostart_recording"
    private const val KEY_BACKUP_GOOGLE_DRIVE = "cube_backup_gdrive"
    private const val KEY_BACKUP_DROPBOX = "cube_backup_dropbox"
    private const val KEY_BACKUP_ONEDRIVE = "cube_backup_onedrive"
    private const val KEY_BACKUP_ONEDRIVE_BUSINESS = "cube_backup_onedrive_biz"
    private const val KEY_BACKUP_FTP = "cube_backup_ftp"
    private const val KEY_BACKUP_EMAIL = "cube_backup_email"
    private const val KEY_BACKUP_CELLULAR = "cube_backup_cellular"
    private const val KEY_DELETE_AFTER_BACKUP = "cube_delete_after_backup"
    private const val KEY_TITLE_STARTS_WITH_DATE = "cube_title_starts_date"
    private const val KEY_HIDE_RECORDING_CONTROLS = "cube_hide_recording_controls"
    private const val KEY_DARK_THEME = "cube_dark_theme"
    private const val KEY_POST_CALL_ACTIONS = "cube_post_call_actions"
    private const val KEY_SHAKE_TO_MARK = "cube_shake_to_mark"
    private const val KEY_SHAKE_THRESHOLD = "cube_shake_threshold"
    private const val KEY_SHAKE_VIBRATION = "cube_shake_vibration"
    private const val KEY_GEO_TAGGING = "cube_geo_tagging"

    fun isAutoRecordEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_RECORD, true)
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

    /** Cube FAQ: delay after connect before MediaRecorder.start (phone vs VoIP). */
    fun recordingStartDelayMs(context: Context, voip: Boolean = false): Long {
        return if (voip) getVoipRecordingDelayMs(context) else getPhoneRecordingDelayMs(context)
    }

    fun getPhoneRecordingDelayMs(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_PHONE_DELAY_MS, 4_000L)
    }

    fun setPhoneRecordingDelayMs(context: Context, ms: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_PHONE_DELAY_MS, ms.coerceIn(0L, 15_000L)).apply()
    }

    fun getVoipRecordingDelayMs(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_VOIP_DELAY_MS, 1_000L)
    }

    fun setVoipRecordingDelayMs(context: Context, ms: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_VOIP_DELAY_MS, ms.coerceIn(0L, 15_000L)).apply()
    }

    fun getPhoneAudioSourceId(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PHONE_AUDIO_SOURCE, CubeAudioSourceOptions.DEFAULT_PHONE_ID)
            ?: CubeAudioSourceOptions.DEFAULT_PHONE_ID
    }

    fun setPhoneAudioSourceId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PHONE_AUDIO_SOURCE, id).apply()
    }

    fun getVoipAudioSourceId(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_VOIP_AUDIO_SOURCE, CubeAudioSourceOptions.DEFAULT_VOIP_ID)
            ?: CubeAudioSourceOptions.DEFAULT_VOIP_ID
    }

    fun setVoipAudioSourceId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_VOIP_AUDIO_SOURCE, id).apply()
    }

    fun getPhoneClarityLevel(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_PHONE_CLARITY, 100).coerceIn(0, 100)

    fun setPhoneClarityLevel(context: Context, level: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PHONE_CLARITY, level.coerceIn(0, 100)).apply()
    }

    fun getVoipClarityLevel(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_VOIP_CLARITY, 60).coerceIn(0, 100)

    fun setVoipClarityLevel(context: Context, level: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_VOIP_CLARITY, level.coerceIn(0, 100)).apply()
    }

    fun clarityGainMultiplier(level: Int): Float =
        if (level <= 0) 1f else 1f + (level / 100f) * 1.5f

    fun isForceInCommunicationMode(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_FORCE_IN_COMMUNICATION, true)

    fun setForceInCommunicationMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FORCE_IN_COMMUNICATION, enabled).apply()
    }

    fun isForceInCallModeVoip(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_FORCE_IN_CALL_VOIP, false)

    fun setForceInCallModeVoip(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FORCE_IN_CALL_VOIP, enabled).apply()
    }

    fun isSkipHeadsetCalls(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SKIP_HEADSET_CALLS, false)

    fun setSkipHeadsetCalls(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SKIP_HEADSET_CALLS, enabled).apply()
    }

    fun isMaximizeInCallVolume(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_MAXIMIZE_INCALL_VOLUME, true)

    fun setMaximizeInCallVolume(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_MAXIMIZE_INCALL_VOLUME, enabled).apply()
    }

    fun isRecordCellularEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_RECORD_CELLULAR, true)

    fun setRecordCellularEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_RECORD_CELLULAR, enabled).apply()
    }

    fun isRecordMeetEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_RECORD_MEET, false)

    fun setRecordMeetEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_RECORD_MEET, enabled).apply()
    }

    fun isAutostartRecording(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTOSTART_RECORDING, true)

    fun setAutostartRecording(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTOSTART_RECORDING, enabled).apply()
        setAutoRecordEnabled(context, enabled)
    }

    fun isRecordingMasterEnabled(context: Context): Boolean = isAutoRecordEnabled(context)

    fun setRecordingMasterEnabled(context: Context, enabled: Boolean) {
        setAutoRecordEnabled(context, enabled)
        setAutostartRecording(context, enabled)
    }

    fun isBackupGoogleDrive(context: Context): Boolean = prefBool(context, KEY_BACKUP_GOOGLE_DRIVE)
    fun setBackupGoogleDrive(context: Context, v: Boolean) = setPrefBool(context, KEY_BACKUP_GOOGLE_DRIVE, v)
    fun isBackupDropbox(context: Context): Boolean = prefBool(context, KEY_BACKUP_DROPBOX)
    fun setBackupDropbox(context: Context, v: Boolean) = setPrefBool(context, KEY_BACKUP_DROPBOX, v)
    fun isBackupOneDrive(context: Context): Boolean = prefBool(context, KEY_BACKUP_ONEDRIVE)
    fun setBackupOneDrive(context: Context, v: Boolean) = setPrefBool(context, KEY_BACKUP_ONEDRIVE, v)
    fun isBackupOneDriveBusiness(context: Context): Boolean = prefBool(context, KEY_BACKUP_ONEDRIVE_BUSINESS)
    fun setBackupOneDriveBusiness(context: Context, v: Boolean) = setPrefBool(context, KEY_BACKUP_ONEDRIVE_BUSINESS, v)
    fun isBackupFtp(context: Context): Boolean = prefBool(context, KEY_BACKUP_FTP)
    fun setBackupFtp(context: Context, v: Boolean) = setPrefBool(context, KEY_BACKUP_FTP, v)
    fun isBackupEmail(context: Context): Boolean = prefBool(context, KEY_BACKUP_EMAIL)
    fun setBackupEmail(context: Context, v: Boolean) = setPrefBool(context, KEY_BACKUP_EMAIL, v)
    fun isBackupOverCellular(context: Context): Boolean = prefBool(context, KEY_BACKUP_CELLULAR)
    fun setBackupOverCellular(context: Context, v: Boolean) = setPrefBool(context, KEY_BACKUP_CELLULAR, v)
    fun isDeleteAfterBackup(context: Context): Boolean = prefBool(context, KEY_DELETE_AFTER_BACKUP)
    fun setDeleteAfterBackup(context: Context, v: Boolean) = setPrefBool(context, KEY_DELETE_AFTER_BACKUP, v)
    fun isTitleStartsWithDate(context: Context): Boolean = prefBool(context, KEY_TITLE_STARTS_WITH_DATE)
    fun setTitleStartsWithDate(context: Context, v: Boolean) = setPrefBool(context, KEY_TITLE_STARTS_WITH_DATE, v)

    fun isHideRecordingControls(context: Context): Boolean = prefBool(context, KEY_HIDE_RECORDING_CONTROLS)
    fun setHideRecordingControls(context: Context, v: Boolean) = setPrefBool(context, KEY_HIDE_RECORDING_CONTROLS, v)
    fun getDarkThemeMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DARK_THEME, "auto") ?: "auto"
    fun setDarkThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_DARK_THEME, mode).apply()
    }
    fun isPostCallActionsEnabled(context: Context): Boolean = prefBool(context, KEY_POST_CALL_ACTIONS)
    fun setPostCallActionsEnabled(context: Context, v: Boolean) = setPrefBool(context, KEY_POST_CALL_ACTIONS, v)
    fun isShakeToMarkEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHAKE_TO_MARK, true)
    fun setShakeToMarkEnabled(context: Context, v: Boolean) = setPrefBool(context, KEY_SHAKE_TO_MARK, v)
    fun getShakeThreshold(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SHAKE_THRESHOLD, 30)
            .coerceIn(0, 100)
    fun setShakeThreshold(context: Context, v: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_SHAKE_THRESHOLD, v.coerceIn(0, 100)).apply()
    }
    fun isShakeVibrationEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHAKE_VIBRATION, true)
    fun setShakeVibrationEnabled(context: Context, v: Boolean) = setPrefBool(context, KEY_SHAKE_VIBRATION, v)
    fun isGeoTaggingEnabled(context: Context): Boolean = prefBool(context, KEY_GEO_TAGGING)
    fun setGeoTaggingEnabled(context: Context, v: Boolean) = setPrefBool(context, KEY_GEO_TAGGING, v)

    private fun prefBool(context: Context, key: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key, false)

    private fun setPrefBool(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply()
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
