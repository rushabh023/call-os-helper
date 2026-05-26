package com.example.helper_application.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.recording.RecordingStorage
import com.example.helper_application.util.AppLog
import com.example.helper_application.setup.PermissionHelper
import com.example.helper_application.setup.SetupPreferences
import com.example.helper_application.setup.SystemSettingsHelper
import com.example.helper_application.ui.dashboard.HelperDashboardScreen
import com.example.helper_application.ui.setup.AppConnectorScreen
import com.example.helper_application.ui.setup.BatteryOptimizationScreen
import com.example.helper_application.ui.setup.PermissionsScreen

private enum class SetupStep {
    Permissions,
    Battery,
    AppConnector,
    Dashboard
}

@Composable
fun HelperAppRoot(
    onStartMonitoring: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var step by remember {
        mutableStateOf(
            when {
                SetupPreferences.isSetupComplete(context) -> SetupStep.Dashboard
                !PermissionHelper.hasAllPermissions(context) -> SetupStep.Permissions
                !SetupPreferences.isBatteryStepDone(context) -> SetupStep.Battery
                else -> SetupStep.AppConnector
            }.also { initial ->
                AppLog.i("Setup initial step: $initial")
            }
        )
    }
    var showCompleteDialog by remember {
        mutableStateOf(SetupPreferences.shouldShowCompleteDialog(context))
    }
    var connectorCheckTick by remember { mutableStateOf(0) }

    LaunchedEffect(step, connectorCheckTick) {
        if (step == SetupStep.Dashboard) {
            val folderOk = RecordingStorage.ensureFolderExists(context)
            val connectorOn = SystemSettingsHelper.isAppConnectorEnabled(context)
            AppLog.d("Dashboard: folderOk=$folderOk, appConnector=$connectorOn")
            if (connectorOn) {
                RecordingPreferences.setAppConnectorEnabled(context, true)
                onStartMonitoring()
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                connectorCheckTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (step) {
        SetupStep.Permissions -> PermissionsScreen(
            onPermissionsGranted = {
                AppLog.i("Setup: permissions granted → Battery")
                RecordingStorage.ensureFolderExists(context)
                step = SetupStep.Battery
            }
        )
        SetupStep.Battery -> BatteryOptimizationScreen(
            onContinue = {
                AppLog.i("Setup: battery step done → App Connector")
                SetupPreferences.setBatteryStepDone(context, true)
                step = SetupStep.AppConnector
            }
        )
        SetupStep.AppConnector -> AppConnectorScreen(
            onContinue = {
                val folderOk = RecordingStorage.ensureFolderExists(context)
                val connectorOn = SystemSettingsHelper.isAppConnectorEnabled(context)
                AppLog.i("Setup complete: folderOk=$folderOk, appConnector=$connectorOn")
                SetupPreferences.setSetupComplete(context, true)
                SetupPreferences.setShowCompleteDialog(context, true)
                showCompleteDialog = true
                step = SetupStep.Dashboard
            },
            onResumeCheck = {
                SystemSettingsHelper.isAppConnectorEnabled(context).also { enabled ->
                    AppLog.d("App Connector check on resume: enabled=$enabled")
                    RecordingPreferences.setAppConnectorEnabled(context, enabled)
                }
            }
        )
        SetupStep.Dashboard -> HelperDashboardScreen(
            showSetupCompleteDialog = showCompleteDialog,
            onDismissSetupDialog = {
                showCompleteDialog = false
                SetupPreferences.setShowCompleteDialog(context, false)
            }
        )
    }
}
