package com.example.helper_application.shizuku

import android.content.Context
import com.example.helper_application.recording.RecorderEngine
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.util.AppLog
import java.io.File

/**
 * Records via Shizuku user service (shell UID) — ACR-style two-way call capture.
 */
class ShizukuRecordingEngine(private val context: Context) : RecorderEngine {

    override val id: String = "shizuku_voice_call"
    override val outputExtension: String = "m4a"
    override var lastAudioSourceLabel: String? = null
        private set

    private var tempFile: File? = null

    override val isRecording: Boolean
        get() = runCatching {
            ShizukuManager.getService()?.isRecording == true
        }.getOrDefault(false)

    override fun start(): File? {
        ShizukuManager.logStatus("engine_start_attempt")
        if (!ShizukuManager.isBinderAvailable()) {
            AppLog.Shizuku.w("Engine start failed: Shizuku binder not available")
            AppLog.Shizuku.detail("engine_start_blocked", "reason" to ShizukuManager.readinessReason())
            return null
        }
        if (!ShizukuManager.hasPermission()) {
            AppLog.Shizuku.w("Engine start failed: Shizuku permission not granted")
            AppLog.Shizuku.detail("engine_start_blocked", "reason" to ShizukuManager.readinessReason())
            return null
        }
        ShizukuManager.bindUserServiceIfNeeded(context)
        val service = ShizukuManager.getService()
        if (service == null) {
            AppLog.Shizuku.w(
                "Engine start failed: user service not connected yet — " +
                    "open Dashboard, tap SETUP SHIZUKU RECORDING, wait for Ready"
            )
            AppLog.Shizuku.detail("engine_start_blocked", "reason" to ShizukuManager.readinessReason())
            return null
        }

        val dir = context.externalCacheDir ?: context.cacheDir
        val file = File(dir, "call_shizuku_${System.currentTimeMillis()}.$outputExtension")
        AppLog.Shizuku.detail(
            "engine_start",
            "tempPath" to file.absolutePath,
            "cacheDir" to dir.absolutePath
        )
        return try {
            val started = service.startRecording(file.absolutePath)
            if (!started) {
                AppLog.Shizuku.e("User service startRecording returned false — all shell audio sources failed")
                AppLog.Shizuku.detail(
                    "engine_start_rejected",
                    "reason" to "user_service_start_false",
                    "lastAudioSource" to service.lastAudioSource
                )
                file.delete()
                return null
            }
            tempFile = file
            lastAudioSourceLabel = service.lastAudioSource
            RecordingPreferences.setLastAudioSource(context, lastAudioSourceLabel ?: "SHIZUKU")
            RecordingPreferences.setShizukuRecordingEnabled(context, true)
            AppLog.Shizuku.detail(
                "engine_started",
                "audioSource" to lastAudioSourceLabel,
                "tempPath" to file.absolutePath,
                "file" to AppLog.fileInfo(file)
            )
            file
        } catch (e: Exception) {
            AppLog.Shizuku.e("Engine start exception", e)
            file.delete()
            null
        }
    }

    override fun stop(): File? {
        val expected = tempFile
        AppLog.Shizuku.d("Engine stop: ${AppLog.fileInfo(expected, "expectedTemp")}")
        return try {
            val svc = ShizukuManager.getService()
            val path = svc?.stopRecording().orEmpty()
            lastAudioSourceLabel = svc?.lastAudioSource
            tempFile = null
            if (path.isNotEmpty()) {
                val file = File(path)
                if (file.exists() && file.length() > 0L) {
                    AppLog.Shizuku.detail(
                        "engine_stopped",
                        "audioSource" to lastAudioSourceLabel,
                        "path" to path,
                        "bytes" to file.length()
                    )
                    return file
                }
                AppLog.Shizuku.w("stop returned path but file empty: $path")
            } else {
                AppLog.Shizuku.w("User service stopRecording returned empty path")
            }
            expected?.takeIf { it.exists() && it.length() > 0L }?.also {
                AppLog.Shizuku.i("Using fallback temp file: ${AppLog.fileInfo(it)}")
            }
        } catch (e: Exception) {
            AppLog.Shizuku.e("Engine stop exception", e)
            expected?.takeIf { it.exists() && it.length() > 0L }
        }
    }
}
