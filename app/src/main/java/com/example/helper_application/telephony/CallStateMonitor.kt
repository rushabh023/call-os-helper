package com.example.helper_application.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.helper_application.bridge.MesValidationConnectionBridge
import com.example.helper_application.recording.CallDirection
import com.example.helper_application.recording.CallMonitorService
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.util.AppLog

class CallStateMonitor(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private var ringDetected = false
    private var isMonitoring = false

    @Suppress("DEPRECATION")
    private val legacyListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            handleStateChange(state, phoneNumber)
        }
    }

    private val telephonyCallback: TelephonyCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleStateChange(state, null)
                }
            }
        } else {
            null
        }

    fun start() {
        if (isMonitoring) {
            AppLog.d("CallStateMonitor already running")
            return
        }
        if (!hasPhonePermission()) {
            AppLog.w("CallStateMonitor start failed: READ_PHONE_STATE not granted")
            return
        }
        AppLog.i("CallStateMonitor started")
        isMonitoring = true
        lastState = TelephonyManager.CALL_STATE_IDLE
        ringDetected = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyCallback != null) {
            try {
                telephonyManager.registerTelephonyCallback(context.mainExecutor, telephonyCallback)
            } catch (e: Exception) {
                AppLog.e("registerTelephonyCallback failed", e)
                @Suppress("DEPRECATION")
                telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_CALL_STATE)
            }
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun stop() {
        if (!isMonitoring) return
        AppLog.i("CallStateMonitor stopped")
        isMonitoring = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyCallback != null) {
            telephonyManager.unregisterTelephonyCallback(telephonyCallback)
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(legacyListener, PhoneStateListener.LISTEN_NONE)
        }
    }

    private fun handleStateChange(state: Int, phoneNumber: String?) {
        mainHandler.post {
            try {
                handleStateChangeOnMain(state, phoneNumber)
            } catch (e: Exception) {
                AppLog.e("handleStateChange crashed", e)
            }
        }
    }

    private fun handleStateChangeOnMain(state: Int, phoneNumber: String?) {
        if (!RecordingPreferences.isAutoRecordEnabled(context)) return

        val stateName = callStateName(state)
        AppLog.d("Call state: $stateName (last=${callStateName(lastState)}, number=$phoneNumber)")

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                ringDetected = true
                AppLog.i("Incoming call ringing")
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (lastState != TelephonyManager.CALL_STATE_OFFHOOK) {
                    val direction = if (ringDetected) {
                        CallDirection.INCOMING
                    } else {
                        CallDirection.OUTGOING
                    }
                    AppLog.i("Call active — starting recording (${direction.name})")
                    if (MesValidationConnectionBridge.isMesValidationInstalled(context)) {
                        MesValidationConnectionBridge.startRecording(context, direction, phoneNumber)
                    } else {
                        AppLog.w("Mes Validation not installed — recording locally in Helper")
                        CallMonitorService.beginCall(context, direction, phoneNumber)
                    }
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    AppLog.i("Call ended — stopping recording")
                    if (MesValidationConnectionBridge.isMesValidationInstalled(context)) {
                        MesValidationConnectionBridge.stopRecording(context)
                    } else {
                        CallMonitorService.endCall(context)
                    }
                }
                ringDetected = false
            }
        }
        lastState = state
    }

    private fun callStateName(state: Int): String = when (state) {
        TelephonyManager.CALL_STATE_IDLE -> "IDLE"
        TelephonyManager.CALL_STATE_RINGING -> "RINGING"
        TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
        else -> "UNKNOWN($state)"
    }

    private fun hasPhonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
