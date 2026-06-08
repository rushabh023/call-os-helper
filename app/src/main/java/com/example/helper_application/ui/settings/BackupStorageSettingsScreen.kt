package com.example.helper_application.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.helper_application.R
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.recording.RecordingStorage

@Composable
fun BackupStorageSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var gdrive by remember { mutableStateOf(RecordingPreferences.isBackupGoogleDrive(context)) }
    var dropbox by remember { mutableStateOf(RecordingPreferences.isBackupDropbox(context)) }
    var onedrive by remember { mutableStateOf(RecordingPreferences.isBackupOneDrive(context)) }
    var onedriveBiz by remember { mutableStateOf(RecordingPreferences.isBackupOneDriveBusiness(context)) }
    var ftp by remember { mutableStateOf(RecordingPreferences.isBackupFtp(context)) }
    var email by remember { mutableStateOf(RecordingPreferences.isBackupEmail(context)) }
    var backupCellular by remember { mutableStateOf(RecordingPreferences.isBackupOverCellular(context)) }
    var deleteAfter by remember { mutableStateOf(RecordingPreferences.isDeleteAfterBackup(context)) }
    var titleDate by remember { mutableStateOf(RecordingPreferences.isTitleStartsWithDate(context)) }

    CubeSettingsScreen(
        title = stringResource(R.string.backup_storage_title),
        onBack = onBack
    ) {
        CubeSettingsSection(stringResource(R.string.section_backup_restore))
        CubeSwitchRow("Google Drive", checked = gdrive) {
            gdrive = it; RecordingPreferences.setBackupGoogleDrive(context, it)
        }
        CubeSwitchRow("Dropbox", checked = dropbox) {
            dropbox = it; RecordingPreferences.setBackupDropbox(context, it)
        }
        CubeSwitchRow("OneDrive Personal", checked = onedrive) {
            onedrive = it; RecordingPreferences.setBackupOneDrive(context, it)
        }
        CubeSwitchRow("OneDrive for Business", checked = onedriveBiz) {
            onedriveBiz = it; RecordingPreferences.setBackupOneDriveBusiness(context, it)
        }
        CubeSwitchRow("FTP", checked = ftp) {
            ftp = it; RecordingPreferences.setBackupFtp(context, it)
        }
        CubeSwitchRow("Email", checked = email) {
            email = it; RecordingPreferences.setBackupEmail(context, it)
        }
        CubeSwitchRow(
            title = stringResource(R.string.setting_backup_cellular),
            description = stringResource(R.string.setting_backup_cellular_desc),
            checked = backupCellular
        ) {
            backupCellular = it; RecordingPreferences.setBackupOverCellular(context, it)
        }
        CubeSwitchRow(
            title = stringResource(R.string.setting_delete_after_backup),
            description = stringResource(R.string.setting_delete_after_backup_desc),
            checked = deleteAfter
        ) {
            deleteAfter = it; RecordingPreferences.setDeleteAfterBackup(context, it)
        }
        CubeSwitchRow(
            title = stringResource(R.string.setting_title_starts_date),
            description = stringResource(R.string.setting_title_starts_date_desc),
            checked = titleDate
        ) {
            titleDate = it; RecordingPreferences.setTitleStartsWithDate(context, it)
        }

        CubeSettingsSection(stringResource(R.string.section_storage_management))
        Text(
            text = stringResource(R.string.recordings_folder_path),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        CubeValueRow(
            title = stringResource(R.string.create_folder),
            value = if (RecordingStorage.isFolderReadyOnDisk()) {
                stringResource(R.string.folder_ready)
            } else {
                stringResource(R.string.recordings_folder_missing)
            },
            onClick = { RecordingStorage.ensureFolderExists(context) }
        )
    }
}
