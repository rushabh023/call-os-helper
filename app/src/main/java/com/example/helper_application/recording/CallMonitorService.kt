package com.example.helper_application.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import com.example.helper_application.util.AppLog

/**
 * Stays in the foreground while call monitoring is enabled so we can record
 * without crashing when a call arrives (Android 12+ blocks starting a new FGS from background).
 */
class CallMonitorService : Service() {

    private val callRecorder = CallRecorder(this)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isMonitoring = false
    private var callDirection: CallDirection? = null
    private var phoneNumber: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AppLog.i("CallMonitorService onCreate")
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.d("CallMonitorService onStartCommand action=${intent?.action}")
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
                else -> {
                    if (!isMonitoring) startMonitoring()
                }
            }
        } catch (e: Exception) {
            AppLog.e("CallMonitorService onStartCommand failed", e)
            RecordingPreferences.setLastError(
                this,
                "Call monitor error: ${e.message ?: "unknown"}"
            )
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (isMonitoring) {
            AppLog.d("CallMonitorService already monitoring")
            return
        }
        AppLog.i("CallMonitorService starting foreground monitoring")
        promoteToForeground(NOTIFICATION_MONITORING, "Listening for calls…")
        isMonitoring = true
        RecordingPreferences.setLastError(this, null)
    }

    private fun stopMonitoring() {
        AppLog.i("CallMonitorService stopping")
        if (callRecorder.isRecording) {
            endCallRecording()
        }
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
                    AppLog.w("Already recording, skip begin")
                    return@post
                }
                callDirection = direction
                phoneNumber = number
                AppLog.i("Begin recording: ${direction.name}, number=$number")
                promoteToForeground(NOTIFICATION_RECORDING, "Recording call…")
                val file = callRecorder.start()
                if (file == null) {
                    AppLog.e("MediaRecorder failed to start")
                    RecordingPreferences.setLastError(
                        this@CallMonitorService,
                        "Could not start recording. Device may block mic during calls."
                    )
                    promoteToForeground(NOTIFICATION_MONITORING, "Listening for calls…")
                } else {
                    AppLog.i("Recording file: ${file.absolutePath}")
                    RecordingPreferences.setLastError(this@CallMonitorService, null)
                }
            } catch (e: Exception) {
                AppLog.e("beginCallRecording crashed", e)
                RecordingPreferences.setLastError(
                    this@CallMonitorService,
                    "Recording error: ${e.message}"
                )
            }
        }
    }

    private fun endCallRecording() {
        mainHandler.post {
            try {
                if (!callRecorder.isRecording) {
                    AppLog.d("endCallRecording: not recording")
                    promoteToForeground(NOTIFICATION_MONITORING, "Listening for calls…")
                    return@post
                }
                val direction = callDirection ?: CallDirection.INCOMING
                val number = phoneNumber
                callDirection = null
                phoneNumber = null

                AppLog.i("End recording, saving…")
                val temp = callRecorder.stop()
                if (temp != null && temp.exists() && temp.length() > 0L) {
                    val fileName = RecordingStorage.buildFileName(direction, number)
                    val uri = RecordingStorage.saveRecording(this@CallMonitorService, temp, fileName)
                    if (uri != null) {
                        AppLog.i("Saved: $fileName")
                        RecordingPreferences.addSavedRecording(this@CallMonitorService, fileName)
                        RecordingPreferences.setLastError(this@CallMonitorService, null)
                    } else {
                        AppLog.e("Save failed: $fileName")
                        RecordingPreferences.setLastError(
                            this@CallMonitorService,
                            "Recording empty or could not save."
                        )
                    }
                } else {
                    AppLog.w("No audio captured (file empty or missing)")
                    RecordingPreferences.setLastError(
                        this@CallMonitorService,
                        "No audio captured — try speaker mode or check device support."
                    )
                    temp?.delete()
                }
                promoteToForeground(NOTIFICATION_MONITORING, "Listening for calls…")
            } catch (e: Exception) {
                AppLog.e("endCallRecording crashed", e)
                RecordingPreferences.setLastError(
                    this@CallMonitorService,
                    "Save error: ${e.message}"
                )
            }
        }
    }

    private fun promoteToForeground(notificationId: Int, text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(notificationId, notification)
        }
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
        AppLog.i("CallMonitorService onDestroy")
        if (callRecorder.isRecording) {
            runCatching { callRecorder.stop() }
        }
        isMonitoring = false
        super.onDestroy()
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
        const val EXTRA_DIRECTION = "direction"
        const val EXTRA_PHONE_NUMBER = "phone_number"

        fun startMonitoring(context: Context) {
            AppLog.i("Request start CallMonitorService")
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
                AppLog.e("startMonitoring failed (FGS not allowed?)", e)
                RecordingPreferences.setLastError(
                    context,
                    "Could not start call monitor. Open the app once, then try again."
                )
            }
        }

        fun stopMonitoring(context: Context) {
            AppLog.i("Request stop CallMonitorService")
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }

        fun beginCall(context: Context, direction: CallDirection, phoneNumber: String?) {
            AppLog.d("beginCall intent: ${direction.name}")
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_BEGIN_CALL
                putExtra(EXTRA_DIRECTION, direction.name)
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                AppLog.e("beginCall intent failed", e)
            }
        }

        fun endCall(context: Context) {
            AppLog.d("endCall intent")
            val intent = Intent(context, CallMonitorService::class.java).apply {
                action = ACTION_END_CALL
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                AppLog.e("endCall intent failed", e)
            }
        }
    }
}
