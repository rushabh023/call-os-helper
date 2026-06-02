package com.example.helper_application.recording

import android.content.Context
import com.example.helper_application.shizuku.ShizukuManager
import com.example.helper_application.shizuku.ShizukuRecordingEngine
import com.example.helper_application.util.AppLog
import java.io.File

class CallRecorder(private val context: Context) {

    private val appContext = context.applicationContext
    private var startAtMs: Long = 0L

    data class StopSummary(
        val engineId: String?,
        val outputExtension: String,
        val audioSource: String?,
        val signalPeak: Int?,
        val durationMs: Long
    )

    var lastStopSummary: StopSummary? = null
        private set

    private fun engines(): List<RecorderEngine> = buildList {
        val strictShizukuRequested = RecordingPreferences.isStrictShizukuTestMode(appContext)
        val shizukuReason = ShizukuManager.readinessReason()
        val shizukuBlocked = shizukuReason == "service_bind_blocked"
        val strictShizuku = strictShizukuRequested && !shizukuBlocked
        val cubeCompat = RecordingPreferences.isCubeCompatibilityMode(appContext)
        val acrStylePref = RecordingPreferences.isAcrStyleRecording(appContext)
        val acrStyle = (acrStylePref || cubeCompat) && !strictShizuku

        // Shizuku two-way test: only shell VOICE_CALL engine (no AMR/dual fallback).
        if (strictShizuku) {
            ShizukuManager.bindUserServiceIfNeeded(appContext)
            if (ShizukuManager.isReady()) {
                add(ShizukuRecordingEngine(appContext))
            }
            return@buildList
        }

        if (acrStyle) {
            if (ShizukuManager.isReady()) {
                add(ShizukuRecordingEngine(appContext))
            } else if (!shizukuBlocked &&
                RecordingPreferences.isShizukuOptIn(appContext) &&
                ShizukuManager.isBinderAvailable() &&
                ShizukuManager.hasPermission()
            ) {
                ShizukuManager.bindUserServiceIfNeeded(appContext)
            }
            add(MediaRecorderEngine(appContext))
            add(AudioRecordVoiceEngine(appContext.cacheDir, appContext))
            return@buildList
        }

        val shizukuOptIn = RecordingPreferences.isShizukuOptIn(appContext)
        val shizukuBinder = ShizukuManager.isBinderAvailable()
        val shizukuPermission = ShizukuManager.hasPermission()
        val shizukuReady = ShizukuManager.isReady()
        AppLog.Engine.detail(
            "engine_gate",
            "acrStylePref" to acrStylePref,
            "cubeCompat" to cubeCompat,
            "acrStyleEffective" to acrStyle,
            "strictShizukuRequested" to strictShizukuRequested,
            "strictShizukuEffective" to strictShizuku,
            "shizukuOptIn" to shizukuOptIn,
            "shizukuBinder" to shizukuBinder,
            "shizukuPermission" to shizukuPermission,
            "shizukuReady" to shizukuReady,
            "shizukuReason" to shizukuReason
        )
        if (strictShizukuRequested && !strictShizuku) {
            AppLog.Engine.w(
                "Strict Shizuku mode auto-disabled: user service blocked on this device; using fallback engines"
            )
        }
        if (!shizukuBlocked && shizukuOptIn && shizukuBinder && shizukuPermission) {
            ShizukuManager.bindUserServiceIfNeeded(appContext)
            if (!strictShizuku || shizukuReady) {
                add(ShizukuRecordingEngine(appContext))
            } else {
                AppLog.Engine.w(
                    "Strict Shizuku mode: binder+permission present but user service not ready yet"
                )
            }
        }
        if (strictShizuku) {
            if (isEmpty()) {
                AppLog.Engine.detail(
                    "strict_gate_block",
                    "reason" to "no_shizuku_engine_available",
                    "optIn" to shizukuOptIn,
                    "binder" to shizukuBinder,
                    "permission" to shizukuPermission,
                    "ready" to shizukuReady
                )
            }
            return@buildList
        }
        if (shizukuBlocked) {
            add(MediaRecorderEngine(appContext))
            add(AudioRecordVoiceEngine(appContext.cacheDir, appContext))
            add(AudioRecordWavEngine(appContext.cacheDir, appContext))
            if (MediaProjectionHolder.isReady() && RecordingPreferences.isDualCaptureEnabled(appContext)) {
                add(DualCaptureWavEngine(appContext.cacheDir, appContext))
            }
        } else {
            if (MediaProjectionHolder.isReady()) {
                add(DualCaptureWavEngine(appContext.cacheDir, appContext))
            }
            add(MediaRecorderEngine(appContext))
            add(AudioRecordWavEngine(appContext.cacheDir, appContext))
        }
    }

    private var activeEngine: RecorderEngine? = null
    private var tempFile: File? = null
    var activeEngineId: String? = null
        private set
    var activeOutputExtension: String = "amr"
        private set

    val isRecording: Boolean
        get() = activeEngine?.isRecording == true

    fun start(): File? {
        if (isRecording) {
            AppLog.Engine.d("start skipped: already recording ${AppLog.fileInfo(tempFile)}")
            return tempFile
        }

        val strictShizuku = RecordingPreferences.isStrictShizukuTestMode(appContext) &&
            ShizukuManager.readinessReason() != "service_bind_blocked"
        val acrStyle = RecordingPreferences.isCubeCompatibilityMode(appContext) && !strictShizuku

        if (acrStyle &&
            RecordingPreferences.getPreferredEngine(appContext) == "media_recorder_mic"
        ) {
            RecordingPreferences.setPreferredEngine(appContext, null)
        }

        AppLog.logRecordingCapabilities(appContext)
        AppLog.Engine.detail(
            "engine_context",
            "shizukuOptIn" to RecordingPreferences.isShizukuOptIn(appContext),
            "strictShizukuRequested" to RecordingPreferences.isStrictShizukuTestMode(appContext),
            "strictShizukuEffective" to strictShizuku,
            "shizukuBinder" to ShizukuManager.isBinderAvailable(),
            "shizukuPermission" to ShizukuManager.hasPermission(),
            "shizukuReady" to ShizukuManager.isReady(),
            "shizukuReason" to ShizukuManager.readinessReason(),
            "mediaProjectionReady" to MediaProjectionHolder.isReady()
        )
        val shizukuBlocked = ShizukuManager.readinessReason() == "service_bind_blocked"
        val preferredEngineId = if (shizukuBlocked) {
            null
        } else {
            RecordingPreferences.getPreferredEngine(appContext)
        }
        val sortedEngines = if (shizukuBlocked) {
            val blockedPriority = mapOf(
                "media_recorder_amr" to 0,
                "media_recorder_m4a" to 1,
                "audio_record_8k_voice" to 2,
                "audio_record_mic_wav" to 3
            )
            engines().sortedWith(
                compareBy<RecorderEngine> { engine ->
                    RecordingPreferences.getEnginePenalty(appContext, engine.id)
                }.thenBy { engine ->
                    blockedPriority[engine.id] ?: 99
                }
            )
        } else {
            engines().sortedWith(
                compareBy<RecorderEngine> { engine ->
                    if (engine.id == preferredEngineId) 0 else 1
                }.thenBy { engine ->
                    RecordingPreferences.getEnginePenalty(appContext, engine.id)
                }
            )
        }
        val penalties = sortedEngines.joinToString(",") { engine ->
            "${engine.id}:${RecordingPreferences.getEnginePenalty(appContext, engine.id)}"
        }
        AppLog.Engine.detail(
            "engine_ladder",
            "mode" to when {
                acrStyle -> "acr_accessibility"
                ShizukuManager.readinessReason() == "service_bind_blocked" -> "cube_compat_shizuku_blocked"
                else -> "advanced"
            },
            "order" to sortedEngines.joinToString(" -> ") { it.id },
            "preferred" to (preferredEngineId ?: "none"),
            "penalties" to penalties,
            "speakerBoost" to RecordingPreferences.isSpeakerBoostEnabled(appContext)
        )
        if (sortedEngines.isEmpty()) {
            AppLog.Engine.e("Engine ladder empty: no eligible engines for current mode/configuration")
        }

        for ((index, engine) in sortedEngines.withIndex()) {
            AppLog.Engine.i("Trying engine ${index + 1}/${sortedEngines.size}: ${engine.id}")
            val file = runCatching { engine.start() }
                .onFailure { e -> AppLog.Engine.e("Engine ${engine.id} threw", e) }
                .getOrNull()
            if (file != null) {
                activeEngine = engine
                tempFile = file
                activeEngineId = engine.id
                activeOutputExtension = engine.outputExtension
                RecordingPreferences.setPreferredEngine(appContext, engine.id)
                startAtMs = System.currentTimeMillis()
                AppLog.Engine.detail(
                    "engine_selected",
                    "engine" to engine.id,
                    "audioSource" to (engine.lastAudioSourceLabel ?: "unknown"),
                    "extension" to engine.outputExtension,
                    "temp" to file.absolutePath,
                    "tempBytes" to file.length()
                )
                return file
            }
            AppLog.Engine.w(
                "Engine ${engine.id} failed — source=${engine.lastAudioSourceLabel ?: "none"} " +
                    "peak=${engine.lastSignalPeak ?: -1}; trying next"
            )
        }

        if (acrStyle) {
            CallAudioBoost.release(appContext)
        }
        activeEngine = null
        tempFile = null
        activeEngineId = null
        AppLog.Engine.e("All ${sortedEngines.size} engines failed — no recording started")
        return null
    }

    fun stop(): File? {
        val strictShizuku = RecordingPreferences.isStrictShizukuTestMode(appContext) &&
            ShizukuManager.readinessReason() != "service_bind_blocked"
        val acrStyle = RecordingPreferences.isCubeCompatibilityMode(appContext) && !strictShizuku
        val engine = activeEngine
        val engineId = engine?.id
        AppLog.Engine.i("Stopping engine: ${engineId ?: "none"}")
        val file = runCatching { engine?.stop() }
            .onFailure { e -> AppLog.Engine.e("Engine $engineId stop threw", e) }
            .getOrNull()
        if (file == null && engineId == "dual_playback_mic_wav") {
            RecordingPreferences.incrementEnginePenalty(appContext, engineId)
            AppLog.Engine.w("DualCapture produced no usable file — penalized for next call fallback")
        }

        if (acrStyle) {
            CallAudioBoost.release(appContext)
        }

        AppLog.Engine.detail(
            "engine_stopped",
            "engine" to (engineId ?: "none"),
            "audioSource" to (engine?.lastAudioSourceLabel ?: "unknown"),
            "signalPeak" to (engine?.lastSignalPeak ?: -1),
            "result" to AppLog.fileInfo(file)
        )
        val durationMs = if (startAtMs > 0L) System.currentTimeMillis() - startAtMs else 0L
        lastStopSummary = StopSummary(
            engineId = engineId,
            outputExtension = activeOutputExtension,
            audioSource = engine?.lastAudioSourceLabel,
            signalPeak = engine?.lastSignalPeak,
            durationMs = durationMs
        )
        activeEngine = null
        tempFile = null
        activeEngineId = null
        startAtMs = 0L
        return file
    }
}
