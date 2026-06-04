package com.example.helper_application.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.helper_application.MainActivity
import com.example.helper_application.R
import android.util.Log
import com.example.helper_application.bridge.MesValidationConnectionBridge
import com.example.helper_application.shizuku.ShizukuManager
import com.example.helper_application.setup.SystemSettingsHelper
import com.example.helper_application.telephony.CallMonitoringCoordinator
import com.example.helper_application.util.AppLog

/**
 * Stays in the foreground while call monitoring is enabled so we can record
 * without crashing when a call arrives (Android 12+ blocks starting a new FGS from background).
 */
class CallMonitorService : Service() {

    private lateinit var callRecorder: CallRecorder
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isMonitoring = false
    private var callDirection: CallDirection? = null
    private var phoneNumber: String? = null
    private var audioRouteBoostApplied = false
    private var callRouteSnapshot: CallRouteSnapshot? = null

    private val routeRefreshRunnable: Runnable = object : Runnable {
        override fun run() {
            if (::callRecorder.isInitialized && callRecorder.isRecording && audioRouteBoostApplied) {
                CallAudioBoost.applyForCall(this@CallMonitorService, force = true)
                mainHandler.postDelayed(this, 3_000L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        callRecorder = CallRecorder(applicationContext)
        AppLog.Recording.i("CallMonitorService onCreate")
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.Recording.d("onStartCommand action=${intent?.action}")
        try {
            when (intent?.action) {
                ACTION_START_MONITORING -> startMonitoring()
                ACTION_STOP_MONITORING -> stopMonitoring()
                ACTION_BEGIN_CALL -> {
                    val direction = intent.getStringExtra(EXTRA_DIRECTION)?.let {
                        runCatching { CallDirection.valueOf(it) }.getOrNull()
                    } ?: CallDirection.INCOMING
                    val number = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                    beginCallRecording(direction, number)
                }
                ACTION_END_CALL -> endCallRecording()
                ACTION_GRANT_MEDIA_PROJECTION -> applyMediaProjectionGrant(intent)
                else -> {
                    if (!isMonitoring) startMonitoring()
                }
            }
        } catch (e: Exception) {
            AppLog.Recording.e("onStartCommand failed", e)
            RecordingPreferences.setLastError(
                this,
                "Call monitor error: ${e.message ?: "unknown"}"
            )
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (isMonitoring) {
            AppLog.Recording.d("Already monitoring")
            return
        }
        AppLog.Recording.i("Foreground monitoring started (listening for calls)")
        promoteToForeground(NOTIFICATION_MONITORING, "Listening for calls…")
        isMonitoring = true
        CallMonitoringCoordinator.startTelephonyListener(this)
        RecordingPreferences.setLastError(this, null)
    }

    private fun stopMonitoring() {
        AppLog.Recording.i("CallMonitorService stopping")
        mainHandler.removeCallbacks(routeRefreshRunnable)
        if (callRecorder.isRecording) {
            endCallRecording()
        }
        CallMonitoringCoordinator.stopTelephonyListener()
        isMonitoring = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun beginCallRecording(direction: CallDirection, number: String?) {
        mainHandler.post {
            try {
                if (!isMonitoring) {
                    startMonitoring()
                }
                if (callRecorder.isRecording) {
                    AppLog.Recording.w("Already recording, skip begin")
                    return@post
                }
                if (!SystemSettingsHelper.isAppConnectorEnabled(this@CallMonitorService)) {
                    AppLog.Recording.e(
                        "Recording blocked: Mes Validation Connection (Accessibility) is OFF — " +
                            "Cube/ACR requires App Connector enabled for call audio path"
                    )
                    RecordingPreferences.setLastError(
                        this@CallMonitorService,
                        "Enable Mes Validation Connection in Accessibility settings."
                    )
                    RecordingPreferences.setLastRecordingStatus(
                        this@CallMonitorService,
                        LastRecordingStatus.FAILED_START
                    )
                    return@post
                }
                val session = AppLog.beginCallSession()
                callDirection = direction
                phoneNumber = number
                AppLog.Recording.detail(
                    "begin",
                    "session" to session,
                    "direction" to direction.name,
                    "number" to (number ?: "hidden"),
                    "monitoring" to isMonitoring,
                    "strictShizuku" to RecordingPreferences.isStrictShizukuTestMode(this@CallMonitorService),
                    "shizukuReady" to ShizukuManager.isReady(),
                    "shizukuReason" to ShizukuManager.readinessReason()
                )
                promoteToForeground(NOTIFICATION_RECORDING, "Recording call…")
                callRouteSnapshot = CallRouteDiagnostics.snapshot(this@CallMonitorService)
                val forceAudioRoute = shouldForceAudioRouteBoost()
                RecordingPreferences.setLastCallHeadsetConnected(
                    this@CallMonitorService,
                    callRouteSnapshot?.blocksLikelyOtherSide == true
                )
                AppLog.Recording.detail(
                    "route_strategy",
                    "forceAudioRoute" to forceAudioRoute,
                    "acrStylePref" to RecordingPreferences.isAcrStyleRecording(this@CallMonitorService),
                    "strictShizuku" to RecordingPreferences.isStrictShizukuTestMode(this@CallMonitorService),
                    "shizukuReason" to ShizukuManager.readinessReason(),
                    "routeSummary" to (callRouteSnapshot?.summary ?: "unknown"),
                    "wiredHeadset" to (callRouteSnapshot?.wiredHeadset ?: false),
                    "bluetoothHeadset" to (callRouteSnapshot?.bluetoothHeadset ?: false)
                )
                if (forceAudioRoute) {
                    CallAudioBoost.applyForCall(this@CallMonitorService, force = true)
                    audioRouteBoostApplied = true
                }
                val routeDelayMs = if (forceAudioRoute) {
                    RecordingPreferences.recordingStartDelayMs(this@CallMonitorService)
                } else {
                    0L
                }
                if (RecordingPreferences.isShizukuOptIn(this@CallMonitorService) &&
                    ShizukuManager.hasShizukuAccess()
                ) {
                    ShizukuManager.ensureBindForCall(this@CallMonitorService)
                }
                scheduleRecordingStartAfterShizuku(routeDelayMs, shizukuWaitAttempt = 0)
            } catch (e: Exception) {
                AppLog.Recording.e("beginCallRecording crashed", e)
                RecordingPreferences.setLastError(
                    this@CallMonitorService,
                    "Recording error: ${e.message}"
                )
            }
        }
    }

    /**
     * APH starts shell recording only when Shizuku UserService is up. We poll briefly at OFFHOOK
     * so we do not fall back to app-process VOICE_CALL (silent .m4a on Android 10+).
     */
    private fun scheduleRecordingStartAfterShizuku(routeDelayMs: Long, shizukuWaitAttempt: Int) {
        val delayMs = if (shizukuWaitAttempt == 0) routeDelayMs else ShizukuManager.CALL_BIND_POLL_MS
        mainHandler.postDelayed({
            if (callRecorder.isRecording) return@postDelayed
            val optIn = RecordingPreferences.isShizukuOptIn(this)
            val pending = optIn && ShizukuManager.isPrivilegedServicePending()
            val maxAttempts = (ShizukuManager.CALL_BIND_WAIT_MS / ShizukuManager.CALL_BIND_POLL_MS).toInt()
            if (pending && shizukuWaitAttempt < maxAttempts) {
                ShizukuManager.ensureBindForCall(this)
                AppLog.Recording.detail(
                    "shizuku_wait_for_call",
                    "attempt" to (shizukuWaitAttempt + 1),
                    "max" to maxAttempts,
                    "reason" to ShizukuManager.readinessReason()
                )
                scheduleRecordingStartAfterShizuku(routeDelayMs, shizukuWaitAttempt + 1)
                return@postDelayed
            }
            if (pending) {
                AppLog.Recording.w(
                    "Shizuku privileged recorder not ready after ${ShizukuManager.CALL_BIND_WAIT_MS}ms — " +
                        "using mic/speaker engines (not silent VOICE_CALL in app process)"
                )
            }
            startRecordingEnginesNow()
        }, delayMs)
    }

    private fun startRecordingEnginesNow() {
        try {
            if (callRecorder.isRecording) return
            if (audioRouteBoostApplied) {
                CallAudioBoost.applyForCall(this, force = true)
            }
            val file = callRecorder.start()
            if (file == null) {
                val strictShizuku = RecordingPreferences.isStrictShizukuTestMode(this)
                val shizukuBlocked = ShizukuManager.readinessReason() == "service_bind_blocked"
                val startError = if (strictShizuku && shizukuBlocked) {
                    "Shizuku service is blocked on this device build; using fallback recording mode."
                } else if (strictShizuku) {
                    "Strict Shizuku mode: Shizuku is not ready. Open Dashboard, complete Shizuku setup, then retry."
                } else {
                    "Could not start recording. Enable Accessibility + speaker boost."
                }
                AppLog.Recording.e(
                    "All recorder engines failed — check [Engine] logs (engine= shizuku_voice_call or AMR/MIC)"
                )
                RecordingPreferences.setLastError(this, startError)
                RecordingPreferences.setLastRecordingStatus(
                    this,
                    LastRecordingStatus.FAILED_START
                )
                promoteToForeground(NOTIFICATION_MONITORING, "Listening for calls…")
                AppLog.endCallSession()
            } else {
                AppLog.Recording.detail(
                    "active",
                    "engine" to callRecorder.activeEngineId,
                    "extension" to callRecorder.activeOutputExtension,
                    "shizukuReady" to ShizukuManager.isReady(),
                    "temp" to file.absolutePath,
                    "tempBytes" to file.length()
                )
                RecordingPreferences.setLastError(this, null)
                RecordingPreferences.setLastRecordingStatus(
                    this,
                    LastRecordingStatus.RECORDING,
                    engineId = callRecorder.activeEngineId
                )
                if (audioRouteBoostApplied) {
                    mainHandler.removeCallbacks(routeRefreshRunnable)
                    mainHandler.postDelayed(routeRefreshRunnable, 3_000L)
                }
            }
        } catch (e: Exception) {
            AppLog.Recording.e("beginCallRecording start crashed", e)
            RecordingPreferences.setLastError(this, "Recording error: ${e.message}")
        }
    }

    private fun endCallRecording() {
        mainHandler.post {
            try {
                if (!callRecorder.isRecording) {
                    AppLog.Recording.d("END call: not recording")
                    promoteToForeground(NOTIFICATION_MONITORING, "Listening for calls…")
                    return@post
                }
                val direction = callDirection ?: CallDirection.INCOMING
                val number = phoneNumber
                callDirection = null
                phoneNumber = null

                AppLog.Recording.detail(
                    "end",
                    "direction" to direction.name,
                    "number" to (number ?: "hidden"),
                    "targetFolder" to RecordingStorage.RELATIVE_FOLDER
                )
                val temp = callRecorder.stop()
                if (audioRouteBoostApplied) {
                    CallAudioBoost.release(this@CallMonitorService)
                    audioRouteBoostApplied = false
                }
                callRouteSnapshot = null
                mainHandler.removeCallbacks(routeRefreshRunnable)
                val stopSummary = callRecorder.lastStopSummary
                AppLog.Recording.d("Temp after stop: ${AppLog.fileInfo(temp)}")
                if (temp != null && temp.exists() && temp.length() > 0L) {
                    val quality = RecordingQualityEvaluator.evaluate(
                        file = temp,
                        engineId = stopSummary?.engineId,
                        durationMs = stopSummary?.durationMs ?: 0L,
                        signalPeak = stopSummary?.signalPeak
                    )
                    AppLog.Engine.detail(
                        "quality_probe",
                        "engine" to (stopSummary?.engineId ?: callRecorder.activeEngineId ?: "unknown"),
                        "source" to (stopSummary?.audioSource ?: "unknown"),
                        "durationMs" to (stopSummary?.durationMs ?: 0L),
                        "signalPeak" to (stopSummary?.signalPeak ?: -1),
                        "bytes" to temp.length(),
                        "likelySilent" to quality.likelySilent,
                        "reason" to quality.reason
                    )
                    val fileName = RecordingStorage.buildFileName(
                        direction,
                        number,
                        callRecorder.activeOutputExtension
                    )
                    AppLog.Recording.i("Saving as $fileName (${temp.length()} bytes)")
                    val uri = RecordingStorage.saveRecording(this@CallMonitorService, temp, fileName)
                    if (uri != null) {
                        AppLog.Recording.detail(
                            "save_ok",
                            "fileName" to fileName,
                            "uri" to uri.toString(),
                            "fileManagerPath" to "Internal storage/Documents/${RecordingStorage.FOLDER_NAME}/$fileName"
                        )
                        RecordingPreferences.addSavedRecording(this@CallMonitorService, fileName)
                        val usedEngine = stopSummary?.engineId ?: callRecorder.activeEngineId
                        if (quality.likelySilent) {
                            RecordingPreferences.observeLikelySilentCapture(
                                this@CallMonitorService,
                                quality.reason
                            )
                            if (usedEngine != null) {
                                RecordingPreferences.incrementEnginePenalty(this@CallMonitorService, usedEngine)
                            }
                            val blockedVerdict = RecordingPreferences.isInCallCaptureBlocked(this@CallMonitorService)
                            val silentStreak = RecordingPreferences.getZeroSignalStreak(this@CallMonitorService)
                            RecordingPreferences.setLastError(
                                this@CallMonitorService,
                                if (blockedVerdict) {
                                    "Device verdict: in-call audio capture blocked on this build " +
                                        "(repeated zero-signal captures)."
                                } else {
                                    "Recording saved but likely silent (${quality.reason}); " +
                                        "switching engine next call."
                                }
                            )
                            RecordingPreferences.setLastRecordingStatus(
                                this@CallMonitorService,
                                LastRecordingStatus.FAILED_EMPTY,
                                engineId = usedEngine,
                                fileName = fileName
                            )
                            AppLog.Engine.w(
                                "Quality check flagged likely-silent capture: reason=${quality.reason} " +
                                    "engine=${usedEngine ?: "unknown"} streak=$silentStreak " +
                                    "blockedVerdict=$blockedVerdict"
                            )
                        } else {
                            RecordingPreferences.observeAudibleCapture(this@CallMonitorService)
                            if (usedEngine != null) {
                                RecordingPreferences.reduceEnginePenalty(this@CallMonitorService, usedEngine)
                            }
                            val hint = headsetTwoWayHint()
                                ?: otherPartyMissingHint(stopSummary?.audioSource)
                                ?: oneSidedSpeakerHint(stopSummary?.audioSource)
                            RecordingPreferences.setLastError(this@CallMonitorService, hint)
                            RecordingPreferences.setLastRecordingStatus(
                                this@CallMonitorService,
                                LastRecordingStatus.SAVED,
                                engineId = usedEngine,
                                fileName = fileName
                            )
                        }
                        MesValidationConnectionBridge.notifyRecordingComplete(
                            this@CallMonitorService,
                            fileName = fileName,
                            absolutePath = temp.absolutePath
                        )
                    } else {
                        AppLog.Recording.e("SAVE FAILED: $fileName — see [Storage] logs above for method/error")
                        RecordingPreferences.setLastError(
                            this@CallMonitorService,
                            "Recording empty or could not save."
                        )
                        RecordingPreferences.setLastRecordingStatus(
                            this@CallMonitorService,
                            LastRecordingStatus.FAILED_SAVE,
                            engineId = callRecorder.activeEngineId
                        )
                    }
                } else {
                    AppLog.Recording.detail(
                        "empty_capture",
                        "temp" to AppLog.fileInfo(temp),
                        "hint" to "Try Shizuku two-way, speakerphone, or dual MediaProjection WAV",
                        level = Log.WARN
                    )
                    val emptyHint = if (
                        stopSummary?.engineId == "dual_playback_mic_wav" ||
                        RecordingPreferences.isDualCaptureEnabled(this@CallMonitorService)
                    ) {
                        getString(R.string.dual_playback_blocked_hint)
                    } else {
                        "No audio captured — try speaker mode or check device support."
                    }
                    RecordingPreferences.setLastError(this@CallMonitorService, emptyHint)
                    RecordingPreferences.setLastRecordingStatus(
                        this@CallMonitorService,
                        LastRecordingStatus.FAILED_EMPTY,
                        engineId = callRecorder.activeEngineId
                    )
                    temp?.delete()
                }
                promoteToForeground(NOTIFICATION_MONITORING, "Listening for calls…")
                AppLog.endCallSession()
            } catch (e: Exception) {
                AppLog.Recording.e("endCallRecording crashed", e)
                if (audioRouteBoostApplied) {
                    runCatching { CallAudioBoost.release(this@CallMonitorService) }
                    audioRouteBoostApplied = false
                }
                RecordingPreferences.setLastError(
                    this@CallMonitorService,
                    "Save error: ${e.message}"
                )
                RecordingPreferences.setLastRecordingStatus(
                    this@CallMonitorService,
                    LastRecordingStatus.FAILED_SAVE,
                    engineId = if (::callRecorder.isInitialized) callRecorder.activeEngineId else null
                )
                promoteToForeground(NOTIFICATION_MONITORING, "Listening for calls…")
                AppLog.endCallSession()
            }
        }
    }

    private fun applyMediaProjectionGrant(intent: Intent?) {
        val resultCode = intent?.getIntExtra(EXTRA_PROJECTION_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val resultData = readProjectionResultData(intent)
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            RecordingPreferences.setDualCaptureEnabled(this, false)
            MediaProjectionHolder.clear()
            RecordingPreferences.setLastError(
                this,
                "Two-way not enabled: permission was cancelled. Tap Enable and tap Share screen / Start."
            )
            AppLog.Setup.w("MediaProjection grant cancelled resultCode=$resultCode")
            return
        }
        try {
            if (!isMonitoring) {
                promoteToForeground(
                    NOTIFICATION_MONITORING,
                    "Two-way recording ready…",
                    mediaProjectionGrant = true
                )
                isMonitoring = true
            } else {
                promoteToForeground(
                    NOTIFICATION_MONITORING,
                    "Listening for calls…",
                    mediaProjectionGrant = true
                )
            }
            val mgr = getSystemService(MediaProjectionManager::class.java)
            val projection = mgr.getMediaProjection(resultCode, resultData)
            MediaProjectionHolder.setProjection(projection)
            RecordingPreferences.setDualCaptureEnabled(this, true)
            RecordingPreferences.setLastError(this, null)
            AppLog.Setup.i("Two-way recording: MediaProjection granted in CallMonitorService")
        } catch (e: Exception) {
            RecordingPreferences.setDualCaptureEnabled(this, false)
            MediaProjectionHolder.clear()
            val detail = e.message ?: e.javaClass.simpleName
            RecordingPreferences.setLastError(
                this,
                "Two-way failed on this device ($detail). Use Speaker boost during calls instead."
            )
            AppLog.Setup.e("MediaProjection getMediaProjection failed", e)
        }
    }

    private fun readProjectionResultData(intent: Intent?): Intent? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PROJECTION_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PROJECTION_RESULT_DATA)
        }
    }

    private fun promoteToForeground(
        notificationId: Int,
        text: String,
        mediaProjectionGrant: Boolean = false
    ) {
        val notification = buildNotification(text)
        val recordingActive = ::callRecorder.isInitialized && callRecorder.isRecording
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    notificationId,
                    notification,
                    foregroundServiceType(
                        recordingActive = recordingActive,
                        mediaProjectionGrant = mediaProjectionGrant
                    )
                )
            } else {
                startForeground(notificationId, notification)
            }
            AppLog.Recording.d(
                "FGS promoted: id=$notificationId recordingActive=$recordingActive type=${foregroundServiceType(recordingActive)}"
            )
            RecordingPreferences.setLastError(this, null)
        } catch (e: Exception) {
            AppLog.Recording.e("startForeground failed", e)
            RecordingPreferences.setLastError(
                this,
                "Could not start call monitor in background. Open the app and try again."
            )
        }
    }

    /**
     * Do not use [ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL] — requires Dialer role
     * or MANAGE_OWN_CALLS, which this helper app does not have.
     */
    private fun foregroundServiceType(
        recordingActive: Boolean,
        mediaProjectionGrant: Boolean = false
    ): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            if (recordingActive) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            if (mediaProjectionGrant || MediaProjectionHolder.isReady()) {
                type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            return type
        }
        return ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Call monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        AppLog.Recording.i("CallMonitorService onDestroy")
        mainHandler.removeCallbacks(routeRefreshRunnable)
        if (::callRecorder.isInitialized && callRecorder.isRecording) {
            runCatching { callRecorder.stop() }
        }
        if (audioRouteBoostApplied) {
            runCatching { CallAudioBoost.release(this) }
            audioRouteBoostApplied = false
        }
        isMonitoring = false
        super.onDestroy()
    }

    private fun shouldForceAudioRouteBoost(): Boolean {
        if (callRouteSnapshot?.blocksLikelyOtherSide == true) return false
        return RecordingPreferences.isCubeCompatibilityMode(this) &&
            RecordingPreferences.isSpeakerBoostEnabled(this)
    }

    private fun headsetTwoWayHint(): String? {
        if (callRouteSnapshot?.blocksLikelyOtherSide != true) return null
        return getString(R.string.headset_two_way_hint)
    }

    private fun otherPartyMissingHint(audioSource: String?): String? {
        if (!RecordingPreferences.isCubeCompatibilityMode(this)) return null
        if (callRouteSnapshot?.blocksLikelyOtherSide == true) return null
        if (!CallAudioBoost.isSpeakerphoneActive(this)) {
            return getString(R.string.other_party_need_phone_speaker)
        }
        if (audioSource == "VOICE_RECOGNITION" || audioSource == "VOICE_COMMUNICATION") {
            return getString(R.string.other_party_need_phone_speaker)
        }
        return null
    }

    private fun oneSidedSpeakerHint(audioSource: String?): String? {
        if (!RecordingPreferences.isSpeakerBoostEnabled(this)) {
            return getString(R.string.one_sided_enable_speaker_boost_hint)
        }
        if (audioSource == "VOICE_RECOGNITION") {
            return getString(R.string.one_sided_try_communication_hint)
        }
        return null
    }

    companion object {
        private const val CHANNEL_ID = "call_monitor"
        private const val NOTIFICATION_MONITORING = 1001
        private const val NOTIFICATION_RECORDING = 1002

        const val ACTION_START_MONITORING =
            "com.example.helper_application.ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING =
            "com.example.helper_application.ACTION_STOP_MONITORING"
        const val ACTION_BEGIN_CALL =
            "com.example.helper_application.ACTION_BEGIN_CALL"
        const val ACTION_END_CALL =
            "com.example.helper_application.ACTION_END_CALL"
        const val ACTION_GRANT_MEDIA_PROJECTION =
            "com.example.helper_application.ACTION_GRANT_MEDIA_PROJECTION"
        const val EXTRA_DIRECTION = "direction"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_PROJECTION_RESULT_CODE = "projection_result_code"
        const val EXTRA_PROJECTION_RESULT_DATA = "projection_result_data"

        fun grantMediaProjection(context: Context, resultCode: Int, resultData: Intent) {
            AppLog.Setup.i("Forwarding MediaProjection grant to CallMonitorService")
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_GRANT_MEDIA_PROJECTION
                putExtra(EXTRA_PROJECTION_RESULT_CODE, resultCode)
                putExtra(EXTRA_PROJECTION_RESULT_DATA, resultData)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                AppLog.Setup.e("grantMediaProjection startForegroundService failed", e)
                RecordingPreferences.setLastError(
                    context,
                    "Could not activate two-way: ${e.message ?: "open app and retry"}"
                )
            }
        }

        fun startMonitoring(context: Context) {
            AppLog.Recording.i("Request start CallMonitorService")
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                AppLog.Recording.e("startMonitoring failed (FGS not allowed?)", e)
                RecordingPreferences.setLastError(
                    context,
                    "Could not start call monitor. Open the app once, then try again."
                )
            }
        }

        fun stopMonitoring(context: Context) {
            AppLog.Recording.i("Request stop CallMonitorService")
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }

        fun beginCall(context: Context, direction: CallDirection, phoneNumber: String?) {
            AppLog.Recording.d("beginCall intent: ${direction.name}")
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_BEGIN_CALL
                putExtra(EXTRA_DIRECTION, direction.name)
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                AppLog.Recording.e("beginCall intent failed", e)
            }
        }

        fun endCall(context: Context) {
            AppLog.Recording.d("endCall intent")
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_END_CALL
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                AppLog.Recording.e("endCall intent failed", e)
            }
        }
    }
}
