package com.example.helper_application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.recording.RecordingStorage
import com.example.helper_application.setup.PermissionHelper
import com.example.helper_application.setup.SetupPreferences
import com.example.helper_application.setup.SystemSettingsHelper
import com.example.helper_application.recording.CallMonitorService
import com.example.helper_application.telephony.CallStateMonitor
import com.example.helper_application.ui.HelperAppRoot
import com.example.helper_application.ui.theme.Helper_applicationTheme
import com.example.helper_application.util.AppLog

class MainActivity : ComponentActivity() {

    private lateinit var callStateMonitor: CallStateMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.i("MainActivity onCreate")
        enableEdgeToEdge()
        callStateMonitor = CallStateMonitor(applicationContext)

        setContent {
            Helper_applicationTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HelperAppRoot(
                        onStartMonitoring = { startBackgroundRecordingIfReady() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppLog.d("MainActivity onResume")
        if (SetupPreferences.isSetupComplete(this)) {
            val folderOk = RecordingStorage.ensureFolderExists(this)
            AppLog.i("ensureFolderExists on resume: success=$folderOk")
        }
        startBackgroundRecordingIfReady()
    }

    override fun onDestroy() {
        AppLog.d("MainActivity onDestroy")
        if (::callStateMonitor.isInitialized) {
            callStateMonitor.stop()
        }
        // Keep CallMonitorService running after leaving app so calls still record.
        super.onDestroy()
    }

    private fun startBackgroundRecordingIfReady() {
        if (!SetupPreferences.isSetupComplete(this)) {
            AppLog.d("Call monitoring skipped: setup not complete")
            return
        }
        if (!PermissionHelper.hasAllPermissions(this)) {
            AppLog.w("Call monitoring skipped: missing permissions")
            return
        }
        if (!SystemSettingsHelper.isAppConnectorEnabled(this)) {
            AppLog.w("Call monitoring skipped: App Connector disabled")
            return
        }
        AppLog.i("Starting call monitor + telephony listener")
        RecordingPreferences.setAutoRecordEnabled(this, true)
        CallMonitorService.startMonitoring(this)
        callStateMonitor.start()
    }

    override fun onPause() {
        super.onPause()
        AppLog.d("MainActivity onPause — monitor keeps running in background")
    }
}
