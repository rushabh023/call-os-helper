package com.example.helper_application.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.AppConstants
import com.example.helper_application.R
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.setup.SystemSettingsHelper
import com.example.helper_application.ui.components.HelperTopBar
import com.example.helper_application.ui.dashboard.HelperDashboardScreen
import com.example.helper_application.ui.info.FaqScreen
import com.example.helper_application.ui.info.TermsScreen
import com.example.helper_application.ui.settings.BackupStorageSettingsScreen
import com.example.helper_application.ui.settings.MapPlaceholderScreen
import com.example.helper_application.ui.settings.MiscellaneousSettingsScreen
import com.example.helper_application.ui.settings.RecordingsTimelineScreen
import com.example.helper_application.ui.settings.RecordingSettingsScreen
import com.example.helper_application.ui.theme.CubePinkAccent
import kotlinx.coroutines.launch

enum class HelperScreen {
    Timeline,
    Map,
    RecordingSettings,
    BackupStorage,
    Miscellaneous,
    HelperStatus,
    Faq,
    Terms
}

@Composable
fun HelperDrawerShell(
    showSetupCompleteDialog: Boolean,
    onDismissSetupDialog: () -> Unit,
    onRequestDualCapture: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(HelperScreen.Timeline) }
    var recordingEnabled by remember {
        mutableStateOf(RecordingPreferences.isRecordingMasterEnabled(context))
    }

    val usesOwnHeader = currentScreen in setOf(
        HelperScreen.RecordingSettings,
        HelperScreen.BackupStorage,
        HelperScreen.Miscellaneous
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.drawer_enable_recording),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Switch(
                        checked = recordingEnabled,
                        onCheckedChange = {
                            recordingEnabled = it
                            RecordingPreferences.setRecordingMasterEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = CubePinkAccent,
                            checkedThumbColor = Color.White
                        )
                    )
                }
                Divider()
                DrawerItem(Icons.Default.Schedule, stringResource(R.string.drawer_timeline)) {
                    currentScreen = HelperScreen.Timeline
                    scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.Map, stringResource(R.string.drawer_map)) {
                    currentScreen = HelperScreen.Map
                    scope.launch { drawerState.close() }
                }
                Text(
                    text = stringResource(R.string.drawer_settings),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                DrawerItem(Icons.Default.Mic, stringResource(R.string.drawer_recording)) {
                    currentScreen = HelperScreen.RecordingSettings
                    scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.SdCard, stringResource(R.string.drawer_backup)) {
                    currentScreen = HelperScreen.BackupStorage
                    scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.Settings, stringResource(R.string.drawer_misc)) {
                    currentScreen = HelperScreen.Miscellaneous
                    scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.Accessibility, stringResource(R.string.drawer_helper_status)) {
                    currentScreen = HelperScreen.HelperStatus
                    scope.launch { drawerState.close() }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem(Icons.Default.Help, stringResource(R.string.drawer_faq)) {
                    currentScreen = HelperScreen.Faq
                    scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.Description, stringResource(R.string.drawer_terms)) {
                    currentScreen = HelperScreen.Terms
                    scope.launch { drawerState.close() }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem(Icons.Default.Email, stringResource(R.string.contact_us)) {
                    SystemSettingsHelper.openEmailSupport(context, AppConstants.SUPPORT_EMAIL)
                    scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.ThumbUp, stringResource(R.string.drawer_rate_app)) {
                    SystemSettingsHelper.openAppSettings(context)
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!usesOwnHeader) {
                HelperTopBar(
                    title = screenTitle(currentScreen),
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            when (currentScreen) {
                HelperScreen.Timeline -> RecordingsTimelineScreen()
                HelperScreen.Map -> MapPlaceholderScreen()
                HelperScreen.RecordingSettings -> RecordingSettingsScreen(
                    onBack = { currentScreen = HelperScreen.Timeline }
                )
                HelperScreen.BackupStorage -> BackupStorageSettingsScreen(
                    onBack = { currentScreen = HelperScreen.Timeline }
                )
                HelperScreen.Miscellaneous -> MiscellaneousSettingsScreen(
                    onBack = { currentScreen = HelperScreen.Timeline }
                )
                HelperScreen.HelperStatus -> HelperDashboardScreen(
                    showSetupCompleteDialog = showSetupCompleteDialog,
                    onDismissSetupDialog = onDismissSetupDialog,
                    onRequestDualCapture = onRequestDualCapture,
                    showHeader = false
                )
                HelperScreen.Faq -> FaqScreen(
                    onOpenShizukuGuide = { currentScreen = HelperScreen.HelperStatus }
                )
                HelperScreen.Terms -> TermsScreen()
            }
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.DarkGray)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontSize = 16.sp)
    }
}

@Composable
private fun screenTitle(screen: HelperScreen): String {
    val res = when (screen) {
        HelperScreen.Timeline -> R.string.drawer_timeline
        HelperScreen.Map -> R.string.drawer_map
        HelperScreen.RecordingSettings -> R.string.recording_settings_title
        HelperScreen.BackupStorage -> R.string.backup_storage_title
        HelperScreen.Miscellaneous -> R.string.misc_settings_title
        HelperScreen.HelperStatus -> R.string.drawer_helper_status
        HelperScreen.Faq -> R.string.drawer_faq
        HelperScreen.Terms -> R.string.drawer_terms
    }
    return stringResource(res)
}
