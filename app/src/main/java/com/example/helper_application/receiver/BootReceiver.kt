package com.example.helper_application.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.helper_application.setup.PermissionHelper
import com.example.helper_application.setup.SetupPreferences
import com.example.helper_application.setup.SystemSettingsHelper
import com.example.helper_application.recording.CallMonitorService
import com.example.helper_application.telephony.CallStateMonitor
import com.example.helper_application.util.AppLog

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        AppLog.i("BOOT_COMPLETED received")
        if (!SetupPreferences.isSetupComplete(context)) {
            AppLog.d("Boot: setup not complete, skip monitor")
            return
        }
        if (!PermissionHelper.hasAllPermissions(context)) {
            AppLog.w("Boot: missing permissions, skip monitor")
            return
        }
        if (!SystemSettingsHelper.isAppConnectorEnabled(context)) {
            AppLog.w("Boot: App Connector off, skip monitor")
            return
        }
        AppLog.i("Boot: starting call monitor")
        CallMonitorService.startMonitoring(context.applicationContext)
        CallStateMonitor(context.applicationContext).start()
    }
}
