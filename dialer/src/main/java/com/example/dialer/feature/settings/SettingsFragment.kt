package com.example.dialer.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.dialer.core.utils.DefaultDialerHelper
import com.example.dialer.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = RecordingPreferences(requireContext())
        binding.switchAutoRecord.isChecked = prefs.isAutoRecordAll()
        binding.switchAutoUnknown.isChecked = prefs.isAutoRecordUnknownOnly()
        binding.switchAutoRecord.setOnCheckedChangeListener { _, checked ->
            prefs.setAutoRecordAll(checked)
        }
        binding.switchAutoUnknown.setOnCheckedChangeListener { _, checked ->
            prefs.setAutoRecordUnknownOnly(checked)
        }
        binding.btnDefaultDialer.setOnClickListener {
            DefaultDialerHelper.requestDefaultDialer(requireActivity())
        }
        refreshDefaultStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshDefaultStatus()
    }

    private fun refreshDefaultStatus() {
        val isDefault = DefaultDialerHelper.isDefaultDialer(requireContext())
        binding.defaultDialerStatus.text = if (isDefault) {
            getString(com.example.dialer.R.string.default_dialer_active)
        } else {
            getString(com.example.dialer.R.string.default_dialer_inactive)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
