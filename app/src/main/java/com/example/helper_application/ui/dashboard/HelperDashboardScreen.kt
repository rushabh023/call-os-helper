package com.example.helper_application.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.AppConstants
import com.example.helper_application.R
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.recording.RecordingStorage
import com.example.helper_application.bridge.MesValidationConnectionBridge
import com.example.helper_application.setup.SystemSettingsHelper
import com.example.helper_application.ui.components.CubeDashboardGrayButton
import com.example.helper_application.ui.components.CubePurpleHeader
import com.example.helper_application.ui.theme.CubeDashboardBackground
import com.example.helper_application.ui.theme.CubePinkAccent

@Composable
fun HelperDashboardScreen(
    showSetupCompleteDialog: Boolean,
    onDismissSetupDialog: () -> Unit
) {
    val context = LocalContext.current
    var legalDialog by remember { mutableStateOf(false) }
    var folderReady by remember {
        mutableStateOf(RecordingStorage.isFolderReadyOnDisk() || RecordingPreferences.isFolderReady(context))
    }

    val mainInstalled = MesValidationConnectionBridge.isMesValidationInstalled(context)
    val mainVersion = MesValidationConnectionBridge.getMesValidationVersion(context)
    val unsentCount = RecordingPreferences.getUnsentRecordCount(context)
    val connectorOn = SystemSettingsHelper.isAppConnectorEnabled(context)

    Column(modifier = Modifier.fillMaxSize().background(CubeDashboardBackground)) {
        CubePurpleHeader(stringResource(R.string.dashboard_title))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
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
        }

        Text(
            text = stringResource(R.string.build_version, AppConstants.BUILD_VERSION),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            fontSize = 12.sp,
            color = Color.Gray
        )
    }

    if (showSetupCompleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissSetupDialog,
            title = null,
            text = {
                Text(stringResource(R.string.setup_complete_title))
            },
            confirmButton = {
                TextButton(onClick = {
                    onDismissSetupDialog()
                    if (mainInstalled) {
                        MesValidationConnectionBridge.openMesValidationApp(context)
                    }
                }) {
                    Text(stringResource(R.string.open_main_app), color = CubePinkAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSetupDialog) {
                    Text(stringResource(R.string.cancel), color = CubePinkAccent)
                }
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
