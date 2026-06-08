package com.example.helper_application.dialer.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.DefaultDialerHelper
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.recording.RecordingStorage
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {

    private lateinit var defaultDialerStatus: TextView
    private lateinit var recordingsFolderPath: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        defaultDialerStatus = view.findViewById(R.id.defaultDialerStatus)
        recordingsFolderPath = view.findViewById(R.id.recordingsFolderPath)

        val dialerPrefs = DialerRecordingPreferences(requireContext())
        val switchAutoRecord: SwitchMaterial = view.findViewById(R.id.switchAutoRecord)
        val switchAutoUnknown: SwitchMaterial = view.findViewById(R.id.switchAutoUnknown)
        val switchSpeakerBoost: SwitchMaterial = view.findViewById(R.id.switchSpeakerBoost)

        switchAutoRecord.isChecked = dialerPrefs.isAutoRecordAll() ||
            RecordingPreferences.isAutoRecordEnabled(requireContext())
        switchAutoUnknown.isChecked = dialerPrefs.isAutoRecordUnknownOnly()
        switchSpeakerBoost.isChecked = RecordingPreferences.isSpeakerBoostEnabled(requireContext())

        switchAutoRecord.setOnCheckedChangeListener { _, checked ->
            dialerPrefs.setAutoRecordAll(checked)
            RecordingPreferences.setAutoRecordEnabled(requireContext(), checked)
        }
        switchAutoUnknown.setOnCheckedChangeListener { _, checked ->
            dialerPrefs.setAutoRecordUnknownOnly(checked)
        }
        switchSpeakerBoost.setOnCheckedChangeListener { _, checked ->
            RecordingPreferences.setSpeakerBoostEnabled(requireContext(), checked)
        }

        view.findViewById<MaterialButton>(R.id.btnDefaultDialer).setOnClickListener {
            DefaultDialerHelper.requestDefaultDialer(requireActivity())
        }
        view.findViewById<MaterialButton>(R.id.btnSpeedDial).setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_speedDialFragment)
        }
        view.findViewById<MaterialButton>(R.id.btnBlocklist).setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_blocklistFragment)
        }
        view.findViewById<MaterialButton>(R.id.btnCreateFolder).setOnClickListener {
            RecordingStorage.ensureFolderExists(requireContext())
            refreshFolderPath()
        }

        refreshDefaultStatus()
        refreshFolderPath()
    }

    override fun onResume() {
        super.onResume()
        refreshDefaultStatus()
        refreshFolderPath()
    }

    private fun refreshDefaultStatus() {
        val isDefault = DefaultDialerHelper.isDefaultDialer(requireContext())
        defaultDialerStatus.text = if (isDefault) {
            getString(R.string.default_dialer_active)
        } else {
            getString(R.string.default_dialer_inactive)
        }
    }

    private fun refreshFolderPath() {
        RecordingStorage.ensureFolderExists(requireContext())
        val folder = RecordingStorage.getPublicFolderFile()
        recordingsFolderPath.text = if (folder.isDirectory) {
            getString(R.string.recordings_folder_path)
        } else {
            getString(R.string.recordings_folder_missing)
        }
    }
}
