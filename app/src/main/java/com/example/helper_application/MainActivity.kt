package com.example.helper_application

import android.app.Activity
import android.app.AlertDialog
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.helper_application.recording.CallMonitorService
import com.example.helper_application.recording.MediaProjectionHolder
import com.example.helper_application.shizuku.ShizukuManager
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.recording.RecordingStorage
import com.example.helper_application.setup.SetupPreferences
import com.example.helper_application.telephony.CallMonitoringCoordinator
import com.example.helper_application.ui.HelperAppRoot
import com.example.helper_application.ui.theme.Helper_applicationTheme
import com.example.helper_application.util.AppLog

class MainActivity : ComponentActivity() {

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            CallMonitorService.grantMediaProjection(
                this,
                result.resultCode,
                result.data!!
            )
        } else {
            AppLog.w("Two-way recording: MediaProjection denied resultCode=${result.resultCode}")
            RecordingPreferences.setDualCaptureEnabled(this, false)
            MediaProjectionHolder.clear()
            RecordingPreferences.setLastError(
                this,
                "Two-way not enabled: tap Enable, then Share screen / Start on the system dialog."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.Setup.i("MainActivity onCreate")
        enableEdgeToEdge()

        setContent {
            Helper_applicationTheme(dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HelperAppRoot(
                        onStartMonitoring = { startBackgroundRecordingIfReady() },
                        onRequestDualCapture = { requestDualCaptureProjection() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppLog.Setup.d("MainActivity onResume")
        ShizukuManager.resumeSetup(this)
        if (SetupPreferences.isSetupComplete(this)) {
            val folderOk = RecordingStorage.ensureFolderExists(this)
            AppLog.i("ensureFolderExists on resume: success=$folderOk")
        }
        startBackgroundRecordingIfReady()
    }

    override fun onDestroy() {
        AppLog.Setup.d(
            "MainActivity onDestroy — telephonyStillRunning=${CallMonitoringCoordinator.isRunning()}"
        )
        super.onDestroy()
    }

    private fun startBackgroundRecordingIfReady() {
        CallMonitoringCoordinator.startIfReady(this)
    }

    override fun onPause() {
        super.onPause()
        AppLog.Setup.d("MainActivity onPause — monitor keeps running in background")
    }

    fun requestDualCaptureProjection() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            AppLog.w("Two-way capture requires Android 10+")
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.two_way_dialog_title)
            .setMessage(R.string.two_way_dialog_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.two_way_dialog_continue) { _, _ ->
                launchMediaProjectionConsent()
            }
            .show()
    }

    private fun launchMediaProjectionConsent() {
        val mgr = getSystemService(MediaProjectionManager::class.java)
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mgr.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            mgr.createScreenCaptureIntent()
        }
        mediaProjectionLauncher.launch(intent)
    }
}
