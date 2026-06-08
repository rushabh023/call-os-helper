package com.example.helper_application.telephony

import android.content.Context
import com.example.helper_application.recording.CallMonitorService
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.setup.PermissionHelper
import com.example.helper_application.setup.SetupPreferences
import com.example.helper_application.setup.SystemSettingsHelper
import com.example.helper_application.util.AppLog

/**
 * Process-wide telephony listener. Must survive [DialerShellActivity] destruction (e.g. user answers a call).
 */
object CallMonitoringCoordinator {

    @Volatile
    private var monitor: CallStateMonitor? = null

    fun isRunning(): Boolean = monitor != null

    /**
     * Starts foreground monitor service + telephony listener when setup is complete.
     * Safe to call repeatedly from Activity resume or boot.
     */
    fun startIfReady(context: Context) {
        val app = context.applicationContext
        if (!SetupPreferences.isSetupComplete(app)) {
            AppLog.Setup.d("Call monitoring skipped: setup not complete")
            return
        }
        if (!PermissionHelper.hasAllPermissions(app)) {
            AppLog.Setup.w("Call monitoring skipped: missing permissions")
            return
        }
        if (!SystemSettingsHelper.isAppConnectorEnabled(app)) {
            AppLog.Setup.w("Call monitoring skipped: App Connector disabled")
            return
        }
        AppLog.Telephony.i("Starting call monitor + telephony listener")
        CallMonitorService.startMonitoring(app)
        startTelephonyListener(app)
    }

    fun startTelephonyListener(context: Context) {
        val app = context.applicationContext
        synchronized(this) {
            if (monitor != null) {
                AppLog.Telephony.d("CallStateMonitor already running (coordinator)")
                return
            }
            monitor = CallStateMonitor(app).also { it.start() }
        }
    }

    /** Stops telephony listener only — used when user disables monitoring or service stops. */
    fun stopTelephonyListener() {
        synchronized(this) {
            val m = monitor ?: return
            m.stop()
            monitor = null
            AppLog.Telephony.i("CallStateMonitor stopped (coordinator)")
        }
    }

    /** Stops FGS and telephony. */
    fun stopAll(context: Context) {
        stopTelephonyListener()
        CallMonitorService.stopMonitoring(context.applicationContext)
    }
}
