package com.example.helper_application.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.helper_application.R
import com.example.helper_application.recording.RecordingPreferences

@Composable
fun MiscellaneousSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hideControls by remember { mutableStateOf(RecordingPreferences.isHideRecordingControls(context)) }
    var darkTheme by remember { mutableStateOf(RecordingPreferences.getDarkThemeMode(context)) }
    var postCall by remember { mutableStateOf(RecordingPreferences.isPostCallActionsEnabled(context)) }
    var shakeOn by remember { mutableStateOf(RecordingPreferences.isShakeToMarkEnabled(context)) }
    var shakeThreshold by remember {
        mutableFloatStateOf(RecordingPreferences.getShakeThreshold(context).toFloat())
    }
    var shakeVibrate by remember { mutableStateOf(RecordingPreferences.isShakeVibrationEnabled(context)) }
    var geoTag by remember { mutableStateOf(RecordingPreferences.isGeoTaggingEnabled(context)) }
    var showThemeDialog by remember { mutableStateOf(false) }

    CubeSettingsScreen(
        title = stringResource(R.string.misc_settings_title),
        onBack = onBack
    ) {
        CubeSettingsSection(stringResource(R.string.section_user_interface))
        CubeValueRow(
            title = stringResource(R.string.setting_recording_controls_mode),
            value = stringResource(R.string.setting_recording_controls_notification),
            onClick = { }
        )
        CubeSwitchRow(
            title = stringResource(R.string.setting_hide_recording_controls),
            description = stringResource(R.string.setting_hide_recording_controls_desc),
            checked = hideControls,
            onCheckedChange = {
                hideControls = it
                RecordingPreferences.setHideRecordingControls(context, it)
            }
        )
        CubeValueRow(
            title = stringResource(R.string.setting_dark_theme),
            value = themeLabel(darkTheme),
            onClick = { showThemeDialog = true }
        )
        CubeSwitchRow(
            title = stringResource(R.string.setting_post_call_actions),
            description = stringResource(R.string.setting_post_call_actions_desc),
            checked = postCall,
            onCheckedChange = {
                postCall = it
                RecordingPreferences.setPostCallActionsEnabled(context, it)
            }
        )

        CubeSettingsSection(stringResource(R.string.section_shake_to_mark))
        CubeSwitchRow(
            title = stringResource(R.string.setting_shake_to_mark),
            description = stringResource(R.string.setting_shake_to_mark_desc),
            checked = shakeOn,
            onCheckedChange = {
                shakeOn = it
                RecordingPreferences.setShakeToMarkEnabled(context, it)
            }
        )
        CubeSliderRow(
            title = stringResource(R.string.setting_shake_threshold),
            description = stringResource(R.string.setting_shake_threshold_desc),
            value = shakeThreshold,
            valueLabel = if (shakeThreshold <= 5f) "MIN" else if (shakeThreshold >= 95f) "MAX" else "${shakeThreshold.toInt()}",
            onValueChange = { shakeThreshold = it },
            onValueChangeFinished = {
                RecordingPreferences.setShakeThreshold(context, shakeThreshold.toInt())
            }
        )
        CubeSwitchRow(
            title = stringResource(R.string.setting_shake_vibration),
            description = stringResource(R.string.setting_shake_vibration_desc),
            checked = shakeVibrate,
            onCheckedChange = {
                shakeVibrate = it
                RecordingPreferences.setShakeVibrationEnabled(context, it)
            }
        )

        CubeSettingsSection(stringResource(R.string.section_geo_tagging))
        CubeSwitchRow(
            title = stringResource(R.string.setting_geo_tagging),
            description = stringResource(R.string.setting_geo_tagging_desc),
            checked = geoTag,
            onCheckedChange = {
                geoTag = it
                RecordingPreferences.setGeoTaggingEnabled(context, it)
            }
        )
    }

    if (showThemeDialog) {
        CubeRadioPickerDialog(
            title = stringResource(R.string.setting_dark_theme),
            options = listOf("Auto", "Light", "Dark"),
            selected = themeLabel(darkTheme),
            onSelect = { label ->
                val mode = when (label) {
                    "Light" -> "light"
                    "Dark" -> "dark"
                    else -> "auto"
                }
                darkTheme = mode
                RecordingPreferences.setDarkThemeMode(context, mode)
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

private fun themeLabel(mode: String): String = when (mode) {
    "light" -> "Light"
    "dark" -> "Dark"
    else -> "Auto"
}
