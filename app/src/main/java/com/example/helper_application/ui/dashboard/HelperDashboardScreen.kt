package com.example.helper_application.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import com.example.helper_application.ui.components.SetupResponsive
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.AppConstants
import com.example.helper_application.R
import com.example.helper_application.recording.LastRecordingStatus
import com.example.helper_application.recording.MediaProjectionHolder
import com.example.helper_application.shizuku.ShizukuManager
import com.example.helper_application.recording.RecordingDiagnostics
import com.example.helper_application.recording.RecordingDiagnosticsSnapshot
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.recording.RecordingStorage
import com.example.helper_application.bridge.MesValidationConnectionBridge
import com.example.helper_application.setup.SystemSettingsHelper
import com.example.helper_application.ui.components.CubeDashboardGrayButton
import com.example.helper_application.ui.components.CubePurpleHeader
import com.example.helper_application.ui.theme.CubeDashboardBackground
import com.example.helper_application.ui.theme.CubePinkAccent
import com.example.helper_application.util.AppLog

@Composable
fun HelperDashboardScreen(
    showSetupCompleteDialog: Boolean,
    onDismissSetupDialog: () -> Unit,
    onRequestDualCapture: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var legalDialog by remember { mutableStateOf(false) }
    var folderReady by remember {
        mutableStateOf(RecordingStorage.isFolderReadyOnDisk() || RecordingPreferences.isFolderReady(context))
    }
    var diagnostics by remember {
        mutableStateOf(RecordingDiagnostics.snapshot(context))
    }
    var shizukuReady by remember {
        mutableStateOf(ShizukuManager.isReady())
    }
    var shizukuBinding by remember {
        mutableStateOf(ShizukuManager.isBindingInProgress())
    }
    var shizukuInstalled by remember {
        mutableStateOf(ShizukuManager.isShizukuInstalled(context))
    }
    var speakerBoostOn by remember {
        mutableStateOf(RecordingPreferences.isSpeakerBoostEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                diagnostics = RecordingDiagnostics.snapshot(context)
                folderReady = RecordingStorage.isFolderReadyOnDisk() ||
                    RecordingPreferences.isFolderReady(context)
                shizukuInstalled = ShizukuManager.isShizukuInstalled(context)
                shizukuReady = ShizukuManager.isReady()
                shizukuBinding = ShizukuManager.isBindingInProgress()
                ShizukuManager.resumeSetupAfterShizukuToggle(context)
                shizukuReady = ShizukuManager.isReady()
                shizukuBinding = ShizukuManager.isBindingInProgress()
                speakerBoostOn = RecordingPreferences.isSpeakerBoostEnabled(context)
                AppLog.logRecordingCapabilities(context)
                ShizukuManager.logStatus("dashboard_resume")
                AppLog.Dashboard.detail(
                    "refreshed",
                    "lastStatus" to diagnostics.lastStatusLabel,
                    "preferredEngine" to diagnostics.preferredEngineLabel,
                    "lastAudioSource" to diagnostics.lastAudioSource,
                    "folderReady" to folderReady,
                    "connectorOn" to SystemSettingsHelper.isAppConnectorEnabled(context),
                    "dualCaptureOn" to RecordingPreferences.isDualCaptureEnabled(context),
                    "acrStyle" to RecordingPreferences.isAcrStyleRecording(context),
                    "speakerBoost" to speakerBoostOn,
                    "shizukuReady" to shizukuReady,
                    "mesValidationInstalled" to MesValidationConnectionBridge.isMesValidationInstalled(context)
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(lifecycleOwner) {
        while (isActive) {
            delay(2_000)
            shizukuReady = ShizukuManager.isReady()
            shizukuBinding = ShizukuManager.isBindingInProgress()
            if (ShizukuManager.isUserServiceBlocked()) {
                shizukuBinding = false
            }
        }
    }

    val mainInstalled = MesValidationConnectionBridge.isMesValidationInstalled(context)
    val mainVersion = MesValidationConnectionBridge.getMesValidationVersion(context)
    val unsentCount = RecordingPreferences.getUnsentRecordCount(context)
    val connectorOn = SystemSettingsHelper.isAppConnectorEnabled(context)
    val dualCaptureOn = RecordingPreferences.isDualCaptureEnabled(context)
    val horizontalPad = SetupResponsive.horizontalPadding(LocalConfiguration.current.screenWidthDp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CubeDashboardBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        CubePurpleHeader(stringResource(R.string.dashboard_title))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPad, vertical = 16.dp)
        ) {
            DashboardSection(title = stringResource(R.string.main_app_section)) {
                Text(
                    text = if (mainInstalled) {
                        stringResource(R.string.main_app_found, mainVersion ?: "?")
                    } else {
                        stringResource(R.string.main_app_not_found)
                    },
                    fontSize = 15.sp
                )
                if (mainInstalled) {
                    TextButtonLink(stringResource(R.string.open)) {
                        MesValidationConnectionBridge.openMesValidationApp(context)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            RecordingDiagnosticsSection(snapshot = diagnostics)

            Spacer(modifier = Modifier.height(12.dp))

            DashboardSection(title = stringResource(R.string.acr_recording_section)) {
                Text(
                    text = stringResource(R.string.acr_recording_desc),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.acr_mode_on),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (speakerBoostOn) {
                        stringResource(R.string.acr_speaker_boost_on)
                    } else {
                        stringResource(R.string.acr_speaker_boost_off)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (speakerBoostOn) Color(0xFF2E7D32) else Color(0xFF5D4037)
                )
                Spacer(modifier = Modifier.height(10.dp))
                CubeDashboardGrayButton(
                    text = if (speakerBoostOn) {
                        stringResource(R.string.acr_speaker_boost_toggle_off)
                    } else {
                        stringResource(R.string.acr_speaker_boost_toggle_on)
                    },
                    onClick = {
                        val next = !speakerBoostOn
                        RecordingPreferences.setSpeakerBoostEnabled(context, next)
                        speakerBoostOn = next
                        AppLog.Dashboard.i("Speaker boost ${if (next) "enabled" else "disabled"}")
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DashboardSection(title = stringResource(R.string.shizuku_section)) {
                Text(
                    text = stringResource(R.string.shizuku_desc),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                val shizukuBlocked = ShizukuManager.readinessReason() == "service_bind_blocked"
                val shizukuStatusText = when {
                    shizukuReady -> stringResource(R.string.shizuku_status_ready)
                    !shizukuInstalled -> stringResource(R.string.shizuku_status_not_installed)
                    !ShizukuManager.isBinderAvailable() && ShizukuManager.isShizukuInstalled(context) ->
                        stringResource(R.string.shizuku_status_no_binder)
                    !ShizukuManager.isBinderAvailable() -> stringResource(R.string.shizuku_status_not_running)
                    !ShizukuManager.hasPermission() -> stringResource(R.string.shizuku_status_need_permission)
                    shizukuBlocked -> stringResource(
                        R.string.shizuku_status_bind_blocked,
                        ShizukuManager.bindAttemptCount(),
                        ShizukuManager.maxBindAttemptsBeforeBlocked()
                    )
                    shizukuBinding || ShizukuManager.readinessReason() == "service_binding" -> {
                        val attempt = ShizukuManager.bindAttemptCount() + 1
                        val max = ShizukuManager.maxBindAttemptsBeforeBlocked()
                        if (attempt > 1) {
                            stringResource(R.string.shizuku_status_binding_attempt, attempt, max)
                        } else {
                            stringResource(R.string.shizuku_status_binding)
                        }
                    }
                    ShizukuManager.hasShizukuAccess() ->
                        stringResource(R.string.shizuku_status_access_ok)
                    ShizukuManager.hasPermission() && ShizukuManager.isBinderAvailable() ->
                        stringResource(R.string.shizuku_status_disconnected)
                    else -> stringResource(R.string.shizuku_status_connecting)
                }
                Text(
                    text = shizukuStatusText,
                    fontSize = if (shizukuBlocked) 13.sp else 14.sp,
                    lineHeight = if (shizukuBlocked) 19.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        shizukuReady -> Color(0xFF2E7D32)
                        shizukuBlocked -> Color(0xFFB71C1C)
                        else -> Color(0xFF5D4037)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CubeDashboardGrayButton(
                    text = if (shizukuInstalled) {
                        stringResource(R.string.shizuku_open_button)
                    } else {
                        stringResource(R.string.shizuku_install_button)
                    },
                    onClick = { ShizukuManager.launchShizukuFlow(context) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButtonLink(stringResource(R.string.shizuku_guide_button)) {
                    ShizukuManager.openSetupGuide(context)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!RecordingPreferences.isAcrStyleRecording(context)) {
            DashboardSection(title = stringResource(R.string.two_way_recording_section)) {
                Text(
                    text = stringResource(R.string.two_way_recording_desc),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (dualCaptureOn) {
                        stringResource(R.string.two_way_recording_on)
                    } else {
                        stringResource(R.string.two_way_recording_off)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dualCaptureOn) Color(0xFF2E7D32) else Color(0xFF5D4037)
                )
                Spacer(modifier = Modifier.height(10.dp))
                CubeDashboardGrayButton(
                    text = if (dualCaptureOn) {
                        stringResource(R.string.two_way_recording_renew)
                    } else {
                        stringResource(R.string.two_way_recording_enable)
                    },
                    onClick = onRequestDualCapture
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            }

            DashboardSection(title = stringResource(R.string.recordings_folder)) {
                Text(
                    text = stringResource(R.string.recordings_folder_path),
                    fontSize = 15.sp
                )
                Text(
                    text = if (folderReady) "Status: Folder exists" else stringResource(R.string.recordings_folder_missing),
                    fontSize = 14.sp,
                    color = if (folderReady) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (!folderReady) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CubeDashboardGrayButton(
                        text = stringResource(R.string.create_folder),
                        onClick = {
                            folderReady = RecordingStorage.ensureFolderExists(context)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            DashboardSection(title = stringResource(R.string.unsent_records)) {
                Text(
                    text = if (unsentCount == 0) {
                        stringResource(R.string.unsent_ok)
                    } else {
                        stringResource(R.string.unsent_count, unsentCount)
                    },
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (connectorOn) "Mes Validation Connection: On" else "Mes Validation Connection: Off",
                fontSize = 13.sp,
                color = if (connectorOn) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            CubeDashboardGrayButton(
                text = stringResource(R.string.go_to_app_connector),
                onClick = { SystemSettingsHelper.openAccessibilitySettings(context) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            CubeDashboardGrayButton(
                text = stringResource(R.string.go_to_app_settings),
                onClick = { SystemSettingsHelper.openAppSettings(context) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            DashboardSection(title = stringResource(R.string.support)) {
                CubeDashboardGrayButton(
                    text = stringResource(R.string.contact_us),
                    onClick = {
                        SystemSettingsHelper.openEmailSupport(context, AppConstants.SUPPORT_EMAIL)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DashboardSection(title = stringResource(R.string.legal)) {
                CubeDashboardGrayButton(
                    text = stringResource(R.string.legal_notice_btn),
                    onClick = { legalDialog = true }
                )
                Spacer(modifier = Modifier.height(10.dp))
                CubeDashboardGrayButton(
                    text = stringResource(R.string.privacy_btn),
                    onClick = { legalDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.build_version, AppConstants.BUILD_VERSION),
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showSetupCompleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissSetupDialog,
            title = {
                Text(stringResource(R.string.setup_complete_title))
            },
            text = {
                Text(
                    if (mainInstalled) {
                        stringResource(R.string.setup_complete_mes_message)
                    } else {
                        stringResource(R.string.setup_complete_local_message)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDismissSetupDialog()
                    if (mainInstalled) {
                        MesValidationConnectionBridge.openMesValidationApp(context)
                    }
                }) {
                    Text(
                        if (mainInstalled) {
                            stringResource(R.string.open_main_app)
                        } else {
                            stringResource(R.string.got_it)
                        },
                        color = CubePinkAccent
                    )
                }
            },
            dismissButton = if (mainInstalled) {
                {
                    TextButton(onClick = onDismissSetupDialog) {
                        Text(stringResource(R.string.got_it), color = CubePinkAccent)
                    }
                }
            } else {
                null
            }
        )
    }

    if (legalDialog) {
        AlertDialog(
            onDismissRequest = { legalDialog = false },
            title = { Text(stringResource(R.string.legal_notice_btn)) },
            text = { Text(stringResource(R.string.legal_notice_full)) },
            confirmButton = {
                TextButton(onClick = { legalDialog = false }) {
                    Text(stringResource(R.string.ok), color = CubePinkAccent)
                }
            }
        )
    }
}

@Composable
private fun RecordingDiagnosticsSection(snapshot: RecordingDiagnosticsSnapshot) {
    val statusColor = when (snapshot.lastStatus) {
        LastRecordingStatus.SAVED -> Color(0xFF2E7D32)
        LastRecordingStatus.RECORDING -> Color(0xFF1565C0)
        LastRecordingStatus.FAILED_START,
        LastRecordingStatus.FAILED_EMPTY,
        LastRecordingStatus.FAILED_SAVE -> Color(0xFFC62828)
        null -> Color.DarkGray
    }

    DashboardSection(title = stringResource(R.string.recording_diagnostics)) {
        Text(
            text = if (snapshot.localRecordingMode) {
                stringResource(R.string.recording_mode_local)
            } else {
                stringResource(R.string.recording_mode_mes)
            },
            fontSize = 14.sp,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.recording_audio_limitation),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = Color(0xFF5D4037)
        )
        Spacer(modifier = Modifier.height(8.dp))
        snapshot.lastAudioSource?.let { source ->
            DiagnosticRow(
                label = stringResource(R.string.last_audio_source),
                value = source
            )
        }
        DiagnosticRow(
            label = stringResource(R.string.device_capture_verdict),
            value = snapshot.captureVerdictLabel,
            valueColor = if (snapshot.captureVerdictLabel.startsWith("Unsupported")) {
                Color(0xFFC62828)
            } else {
                Color(0xFF2E7D32)
            }
        )
        DiagnosticRow(
            label = stringResource(R.string.preferred_engine),
            value = snapshot.preferredEngineLabel
        )
        DiagnosticRow(
            label = stringResource(R.string.last_recording_status),
            value = snapshot.lastStatusLabel,
            valueColor = statusColor
        )
        if (!snapshot.mesValidationInstalled) {
            DiagnosticRow(
                label = stringResource(R.string.last_recording_engine),
                value = snapshot.lastEngineLabel
            )
            DiagnosticRow(
                label = stringResource(R.string.last_recording_file),
                value = snapshot.lastFile ?: stringResource(R.string.none_yet)
            )
            DiagnosticRow(
                label = stringResource(R.string.last_recording_time),
                value = snapshot.lastTimestampLabel
            )
            snapshot.lastError?.let { error ->
                DiagnosticRow(
                    label = stringResource(R.string.last_recording_error),
                    value = error,
                    valueColor = Color(0xFFC62828)
                )
            }
            if (snapshot.recentFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.recent_recordings),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                snapshot.recentFiles.forEach { file ->
                    Text(text = "• $file", fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(text = value, fontSize = 15.sp, color = valueColor)
    }
}

@Composable
private fun DashboardSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun TextButtonLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, color = CubePinkAccent, fontWeight = FontWeight.Bold)
    }
}
