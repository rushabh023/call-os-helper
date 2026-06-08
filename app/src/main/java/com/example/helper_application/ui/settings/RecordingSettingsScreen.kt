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
import com.example.helper_application.recording.CubeAudioSourceOptions
import com.example.helper_application.recording.RecordingPreferences

@Composable
fun RecordingSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var autostart by remember { mutableStateOf(RecordingPreferences.isAutostartRecording(context)) }
    var skipHeadset by remember { mutableStateOf(RecordingPreferences.isSkipHeadsetCalls(context)) }
    var phoneSourceId by remember { mutableStateOf(RecordingPreferences.getPhoneAudioSourceId(context)) }
    var voipSourceId by remember { mutableStateOf(RecordingPreferences.getVoipAudioSourceId(context)) }
    var phoneClarity by remember { mutableFloatStateOf(RecordingPreferences.getPhoneClarityLevel(context).toFloat()) }
    var voipClarity by remember { mutableFloatStateOf(RecordingPreferences.getVoipClarityLevel(context).toFloat()) }
    var forceInCommunication by remember { mutableStateOf(RecordingPreferences.isForceInCommunicationMode(context)) }
    var forceInCallVoip by remember { mutableStateOf(RecordingPreferences.isForceInCallModeVoip(context)) }
    var phoneDelaySec by remember {
        mutableFloatStateOf(RecordingPreferences.getPhoneRecordingDelayMs(context) / 1000f)
    }
    var voipDelaySec by remember {
        mutableFloatStateOf(RecordingPreferences.getVoipRecordingDelayMs(context) / 1000f)
    }
    var maximizeVolume by remember { mutableStateOf(RecordingPreferences.isMaximizeInCallVolume(context)) }
    var recordCellular by remember { mutableStateOf(RecordingPreferences.isRecordCellularEnabled(context)) }
    var recordMeet by remember { mutableStateOf(RecordingPreferences.isRecordMeetEnabled(context)) }
    var showPhoneSourceDialog by remember { mutableStateOf(false) }
    var showVoipSourceDialog by remember { mutableStateOf(false) }

    CubeSettingsScreen(
        title = stringResource(R.string.recording_settings_title),
        onBack = onBack
    ) {
        CubeSettingsSection(stringResource(R.string.section_autorecording))
        CubeSwitchRow(
            title = stringResource(R.string.setting_autostart_recording),
            description = stringResource(R.string.setting_autostart_recording_desc),
            checked = autostart,
            onCheckedChange = {
                autostart = it
                RecordingPreferences.setAutostartRecording(context, it)
            }
        )
        CubeValueRow(
            title = stringResource(R.string.setting_excluded_callers),
            description = stringResource(R.string.setting_excluded_callers_desc),
            value = stringResource(R.string.setting_excluded_callers_value),
            onClick = { /* future: contact picker */ }
        )
        CubeSwitchRow(
            title = stringResource(R.string.setting_skip_headset),
            description = stringResource(R.string.setting_skip_headset_desc),
            checked = skipHeadset,
            onCheckedChange = {
                skipHeadset = it
                RecordingPreferences.setSkipHeadsetCalls(context, it)
            }
        )

        CubeSettingsSection(stringResource(R.string.section_phone_recording))
        CubeValueRow(
            title = stringResource(R.string.setting_phone_audio_source),
            value = CubeAudioSourceOptions.labelForId(phoneSourceId),
            onClick = { showPhoneSourceDialog = true }
        )
        CubeSliderRow(
            title = stringResource(R.string.setting_phone_clarity),
            description = stringResource(R.string.setting_clarity_desc),
            value = phoneClarity,
            valueLabel = clarityLabel(phoneClarity.toInt()),
            onValueChange = { phoneClarity = it },
            onValueChangeFinished = {
                RecordingPreferences.setPhoneClarityLevel(context, phoneClarity.toInt())
            }
        )
        CubeSwitchRow(
            title = stringResource(R.string.setting_force_in_communication),
            description = stringResource(R.string.setting_force_in_communication_desc),
            checked = forceInCommunication,
            onCheckedChange = {
                forceInCommunication = it
                RecordingPreferences.setForceInCommunicationMode(context, it)
            }
        )
        CubeSliderRow(
            title = stringResource(R.string.setting_phone_delay),
            description = stringResource(R.string.setting_phone_delay_desc),
            value = phoneDelaySec.coerceIn(0f, 10f),
            valueLabel = stringResource(R.string.setting_delay_value, phoneDelaySec),
            onValueChange = { phoneDelaySec = it },
            onValueChangeFinished = {
                RecordingPreferences.setPhoneRecordingDelayMs(context, (phoneDelaySec * 1000).toLong())
            }
        )

        CubeSettingsSection(stringResource(R.string.section_voip_recording))
        CubeValueRow(
            title = stringResource(R.string.setting_voip_audio_source),
            value = CubeAudioSourceOptions.labelForId(voipSourceId),
            onClick = { showVoipSourceDialog = true }
        )
        CubeSliderRow(
            title = stringResource(R.string.setting_voip_clarity),
            description = stringResource(R.string.setting_clarity_desc),
            value = voipClarity,
            valueLabel = clarityLabel(voipClarity.toInt()),
            onValueChange = { voipClarity = it },
            onValueChangeFinished = {
                RecordingPreferences.setVoipClarityLevel(context, voipClarity.toInt())
            }
        )
        CubeSliderRow(
            title = stringResource(R.string.setting_voip_delay),
            description = stringResource(R.string.setting_voip_delay_desc),
            value = voipDelaySec.coerceIn(0f, 10f),
            valueLabel = stringResource(R.string.setting_delay_value, voipDelaySec),
            onValueChange = { voipDelaySec = it },
            onValueChangeFinished = {
                RecordingPreferences.setVoipRecordingDelayMs(context, (voipDelaySec * 1000).toLong())
            }
        )
        CubeSwitchRow(
            title = stringResource(R.string.setting_force_in_call_voip),
            description = stringResource(R.string.setting_force_in_call_voip_desc),
            checked = forceInCallVoip,
            onCheckedChange = {
                forceInCallVoip = it
                RecordingPreferences.setForceInCallModeVoip(context, it)
            }
        )

        CubeSettingsSection(stringResource(R.string.section_other_recording))
        CubeSwitchRow(
            title = stringResource(R.string.setting_maximize_volume),
            description = stringResource(R.string.setting_maximize_volume_desc),
            checked = maximizeVolume,
            onCheckedChange = {
                maximizeVolume = it
                RecordingPreferences.setMaximizeInCallVolume(context, it)
                RecordingPreferences.setSpeakerBoostEnabled(context, it)
            }
        )
        CubeValueRow(
            title = stringResource(R.string.setting_services_to_record),
            value = servicesLabel(recordCellular, recordMeet),
            onClick = { /* toggles below */ }
        )
        CubeSwitchRow(
            title = stringResource(R.string.service_cellular),
            checked = recordCellular,
            onCheckedChange = {
                recordCellular = it
                RecordingPreferences.setRecordCellularEnabled(context, it)
            }
        )
        CubeSwitchRow(
            title = stringResource(R.string.service_meet),
            description = stringResource(R.string.service_meet_desc),
            checked = recordMeet,
            onCheckedChange = {
                recordMeet = it
                RecordingPreferences.setRecordMeetEnabled(context, it)
            }
        )
    }

    if (showPhoneSourceDialog) {
        CubeAudioSourcePicker(
            title = stringResource(R.string.setting_phone_audio_source),
            selectedId = phoneSourceId,
            onSelect = {
                phoneSourceId = it
                RecordingPreferences.setPhoneAudioSourceId(context, it)
            },
            onDismiss = { showPhoneSourceDialog = false }
        )
    }
    if (showVoipSourceDialog) {
        CubeAudioSourcePicker(
            title = stringResource(R.string.setting_voip_audio_source),
            selectedId = voipSourceId,
            onSelect = {
                voipSourceId = it
                RecordingPreferences.setVoipAudioSourceId(context, it)
            },
            onDismiss = { showVoipSourceDialog = false }
        )
    }
}

@Composable
private fun CubeAudioSourcePicker(
    title: String,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val labels = CubeAudioSourceOptions.allOptions.map { it.label }
    val selectedLabel = CubeAudioSourceOptions.labelForId(selectedId)
    CubeRadioPickerDialog(
        title = title,
        options = labels,
        selected = selectedLabel,
        onSelect = { label ->
            val id = CubeAudioSourceOptions.allOptions.firstOrNull { it.label == label }?.id
            if (id != null) onSelect(id)
        },
        onDismiss = onDismiss
    )
}

private fun clarityLabel(level: Int): String = when {
    level <= 0 -> "OFF"
    level >= 100 -> "MAX"
    else -> "$level%"
}

private fun servicesLabel(cellular: Boolean, meet: Boolean): String {
    val parts = buildList {
        if (cellular) add("Cellular calls")
        if (meet) add("Meet")
    }
    return parts.joinToString(", ").ifBlank { "None" }
}
