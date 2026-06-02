package com.example.helper_application.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.helper_application.recording.MediaProjectionHolder
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.shizuku.ShizukuManager
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * All app logs use tag [TAG]. Filter Logcat with: HelperApplication
 *
 * Categories (prefix in message):
 * - Telephony — call state RINGING / OFFHOOK / IDLE
 * - Recording — CallMonitorService start/stop/save
 * - Engine — MediaRecorder / AudioRecord / dual capture engines
 * - Shizuku — binder, permission, user service bind, shell recording
 * - Storage — folder + file save paths and method used
 * - Bridge — Helper → Mes Validation broadcasts
 * - Setup — permissions, accessibility, boot
 * - Dashboard — UI diagnostics snapshot
 * - Diagnostics — device snapshot, recording capability matrix
 *
 * Structured lines use key=value pairs, e.g.:
 * [Engine] start | call#3 engine=shizuku_voice_call audioSource=VOICE_CALL bytes=484120
 */
object AppLog {

    const val TAG = "HelperApplication"

    private val callSessionCounter = AtomicInteger(0)

    @Volatile
    var currentCallSession: Int = 0
        private set

    /** Starts a new correlated log session for one phone call. */
    fun beginCallSession(): Int {
        currentCallSession = callSessionCounter.incrementAndGet()
        i("Call session #$currentCallSession started")
        return currentCallSession
    }

    fun endCallSession() {
        if (currentCallSession > 0) {
            i("Call session #$currentCallSession ended")
        }
        currentCallSession = 0
    }

    fun sessionTag(): String =
        if (currentCallSession > 0) "call#$currentCallSession" else "call#-"

    object Telephony {
        fun d(message: String) = log(Log.DEBUG, "Telephony", message)
        fun i(message: String) = log(Log.INFO, "Telephony", message)
        fun w(message: String) = log(Log.WARN, "Telephony", message)
        fun e(message: String, throwable: Throwable? = null) = log(Log.ERROR, "Telephony", message, throwable)
        fun detail(event: String, vararg pairs: Pair<String, Any?>, level: Int = Log.INFO) =
            structured(level, "Telephony", event, pairs)
    }

    object Recording {
        fun d(message: String) = log(Log.DEBUG, "Recording", message)
        fun i(message: String) = log(Log.INFO, "Recording", message)
        fun w(message: String) = log(Log.WARN, "Recording", message)
        fun e(message: String, throwable: Throwable? = null) = log(Log.ERROR, "Recording", message, throwable)
        fun detail(event: String, vararg pairs: Pair<String, Any?>, level: Int = Log.INFO) =
            structured(level, "Recording", event, pairs)
    }

    object Engine {
        fun d(message: String) = log(Log.DEBUG, "Engine", message)
        fun i(message: String) = log(Log.INFO, "Engine", message)
        fun w(message: String) = log(Log.WARN, "Engine", message)
        fun e(message: String, throwable: Throwable? = null) = log(Log.ERROR, "Engine", message, throwable)
        fun detail(event: String, vararg pairs: Pair<String, Any?>, level: Int = Log.INFO) =
            structured(level, "Engine", event, pairs)
    }

    object Shizuku {
        fun d(message: String) = log(Log.DEBUG, "Shizuku", message)
        fun i(message: String) = log(Log.INFO, "Shizuku", message)
        fun w(message: String) = log(Log.WARN, "Shizuku", message)
        fun e(message: String, throwable: Throwable? = null) = log(Log.ERROR, "Shizuku", message, throwable)
        fun detail(event: String, vararg pairs: Pair<String, Any?>, level: Int = Log.INFO) =
            structured(level, "Shizuku", event, pairs)
    }

    object Storage {
        fun d(message: String) = log(Log.DEBUG, "Storage", message)
        fun i(message: String) = log(Log.INFO, "Storage", message)
        fun w(message: String) = log(Log.WARN, "Storage", message)
        fun e(message: String, throwable: Throwable? = null) = log(Log.ERROR, "Storage", message, throwable)
        fun detail(event: String, vararg pairs: Pair<String, Any?>, level: Int = Log.INFO) =
            structured(level, "Storage", event, pairs)
    }

    object Bridge {
        fun d(message: String) = log(Log.DEBUG, "Bridge", message)
        fun i(message: String) = log(Log.INFO, "Bridge", message)
        fun w(message: String) = log(Log.WARN, "Bridge", message)
        fun e(message: String, throwable: Throwable? = null) = log(Log.ERROR, "Bridge", message, throwable)
        fun detail(event: String, vararg pairs: Pair<String, Any?>, level: Int = Log.INFO) =
            structured(level, "Bridge", event, pairs)
    }

    object Setup {
        fun d(message: String) = log(Log.DEBUG, "Setup", message)
        fun i(message: String) = log(Log.INFO, "Setup", message)
        fun w(message: String) = log(Log.WARN, "Setup", message)
        fun e(message: String, throwable: Throwable? = null) = log(Log.ERROR, "Setup", message, throwable)
        fun detail(event: String, vararg pairs: Pair<String, Any?>, level: Int = Log.INFO) =
            structured(level, "Setup", event, pairs)
    }

    object Dashboard {
        fun d(message: String) = log(Log.DEBUG, "Dashboard", message)
        fun i(message: String) = log(Log.INFO, "Dashboard", message)
        fun detail(event: String, vararg pairs: Pair<String, Any?>, level: Int = Log.INFO) =
            structured(level, "Dashboard", event, pairs)
    }

    object Diagnostics {
        fun d(message: String) = log(Log.DEBUG, "Diagnostics", message)
        fun i(message: String) = log(Log.INFO, "Diagnostics", message)
        fun detail(event: String, vararg pairs: Pair<String, Any?>, level: Int = Log.INFO) =
            structured(level, "Diagnostics", event, pairs)
    }

    /** General / legacy logs */
    fun d(message: String) = log(Log.DEBUG, "App", message)
    fun i(message: String) = log(Log.INFO, "App", message)
    fun w(message: String) = log(Log.WARN, "App", message)
    fun e(message: String, throwable: Throwable? = null) = log(Log.ERROR, "App", message, throwable)

    /** One-line device + capability snapshot (log on app start and before first recording). */
    fun logDeviceSnapshot(context: Context) {
        Diagnostics.detail(
            "device_snapshot",
            "sdk" to Build.VERSION.SDK_INT,
            "release" to (Build.VERSION.RELEASE ?: "?"),
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "brand" to Build.BRAND,
            "product" to Build.PRODUCT,
            "abis" to Build.SUPPORTED_ABIS.joinToString(","),
            "package" to context.packageName
        )
    }

    /** Recording engines available on this device right now. */
    fun logRecordingCapabilities(context: Context) {
        Diagnostics.detail(
            "recording_capabilities",
            "shizukuInstalled" to ShizukuManager.isShizukuInstalled(context),
            "shizukuBinder" to ShizukuManager.isBinderAvailable(),
            "shizukuPermission" to ShizukuManager.hasPermission(),
            "shizukuServiceConnected" to (ShizukuManager.getService() != null),
            "shizukuReady" to ShizukuManager.isReady(),
            "mediaProjection" to MediaProjectionHolder.isReady(),
            "dualCaptureEligible" to (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q),
            "acrStyle" to RecordingPreferences.isAcrStyleRecording(context),
            "speakerBoost" to RecordingPreferences.isSpeakerBoostEnabled(context)
        )
    }

    fun fileInfo(file: File?, label: String = "file"): String {
        if (file == null) return "$label=null"
        return "$label path=${file.absolutePath} exists=${file.exists()} bytes=${if (file.exists()) file.length() else -1}"
    }

    private fun structured(
        level: Int,
        category: String,
        event: String,
        pairs: Array<out Pair<String, Any?>>
    ) {
        val body = pairs.joinToString(separator = " ") { (k, v) -> "$k=${v ?: "null"}" }
        val message = "${sessionTag()} | $event | $body"
        log(level, category, message)
    }

    private fun log(priority: Int, category: String, message: String, throwable: Throwable? = null) {
        val formatted = "[$category] $message"
        when (priority) {
            Log.DEBUG -> if (throwable != null) Log.d(TAG, formatted, throwable) else Log.d(TAG, formatted)
            Log.INFO -> if (throwable != null) Log.i(TAG, formatted, throwable) else Log.i(TAG, formatted)
            Log.WARN -> if (throwable != null) Log.w(TAG, formatted, throwable) else Log.w(TAG, formatted)
            Log.ERROR -> if (throwable != null) Log.e(TAG, formatted, throwable) else Log.e(TAG, formatted)
            else -> if (throwable != null) Log.v(TAG, formatted, throwable) else Log.v(TAG, formatted)
        }
    }
}
