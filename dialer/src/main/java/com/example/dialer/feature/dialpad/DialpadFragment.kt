package com.example.dialer.feature.dialpad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dialer.R
import com.example.dialer.core.domain.model.ContactMatch
import com.example.dialer.core.utils.PhoneNumberFormatter
import com.example.dialer.databinding.FragmentDialpadBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DialpadFragment : Fragment() {

    private var _binding: FragmentDialpadBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DialpadViewModel by viewModels()
    private lateinit var matchAdapter: DialpadMatchAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialpadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val initial = arguments?.getString(ARG_INITIAL_NUMBER).orEmpty()
        if (initial.isNotBlank()) viewModel.setDigits(initial)

        matchAdapter = DialpadMatchAdapter { match ->
            placeCall(match.phoneNumber)
        }
        binding.matchList.layoutManager = LinearLayoutManager(requireContext())
        binding.matchList.adapter = matchAdapter

        val keys = listOf(
            binding.key1 to '1', binding.key2 to '2', binding.key3 to '3',
            binding.key4 to '4', binding.key5 to '5', binding.key6 to '6',
            binding.key7 to '7', binding.key8 to '8', binding.key9 to '9',
            binding.keyStar to '*', binding.key0 to '0', binding.keyHash to '#'
        )
        keys.forEach { (button, digit) ->
            button.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                vibrateTick()
                viewModel.appendDigit(digit)
            }
            if (digit in '2'..'9') {
                button.setOnLongClickListener {
                    lifecycleScope.launch {
                        val slot = digit.digitToInt()
                        val number = viewModel.speedDialFor(slot)
                        if (number != null) placeCall(number)
                        else Toast.makeText(requireContext(), R.string.speed_dial_empty, Toast.LENGTH_SHORT).show()
                    }
                    true
                }
            }
        }

        binding.key0.setOnLongClickListener {
            viewModel.appendDigit('+')
            true
        }

        binding.btnBackspace.setOnClickListener { viewModel.backspace() }
        binding.btnBackspace.setOnLongClickListener {
            viewModel.clearAll()
            true
        }

        binding.btnCall.setOnClickListener {
            val number = viewModel.state.value.digits
            if (number.isNotBlank()) placeCall(number)
        }

        binding.numberField.setOnLongClickListener {
            val clip = requireContext().getSystemService(ClipboardManager::class.java)
            clip.setPrimaryClip(ClipData.newPlainText("number", viewModel.state.value.digits))
            Toast.makeText(requireContext(), R.string.copied_number, Toast.LENGTH_SHORT).show()
            true
        }

        binding.btnAddContact.setOnClickListener {
            val number = viewModel.state.value.digits
            if (number.isBlank()) return@setOnClickListener
            val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                type = "vnd.android.cursor.dir/contact"
                putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, number)
            }
            startActivity(intent)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.numberField.text = state.digits
                    matchAdapter.submit(state.matches)
                }
            }
        }
    }

    private fun placeCall(number: String) {
        val sanitized = PhoneNumberFormatter.sanitizeForDial(number)
        if (sanitized.isBlank()) return
        val telecom = requireContext().getSystemService(TelecomManager::class.java) ?: return
        val handles = telecom.callCapablePhoneAccounts
        if (handles.size > 1) {
            showSimChooser(handles, sanitized)
        } else {
            dial(telecom, handles.firstOrNull(), sanitized)
        }
    }

    private fun showSimChooser(handles: List<PhoneAccountHandle>, number: String) {
        val labels = handles.mapIndexed { index, _ -> "SIM ${index + 1}" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_sim)
            .setItems(labels) { _, which ->
                val telecom = requireContext().getSystemService(TelecomManager::class.java)
                dial(telecom, handles[which], number)
            }
            .show()
    }

    private fun dial(telecom: TelecomManager?, handle: PhoneAccountHandle?, number: String) {
        if (telecom == null) return
        val uri = Uri.fromParts("tel", number, null)
        val extras = Bundle()
        if (handle != null) extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
        telecom.placeCall(uri, extras)
    }

    private fun vibrateTick() {
        val vibrator = requireContext().getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_INITIAL_NUMBER = "initial_number"

        fun newInstance(initialNumber: String? = null): DialpadFragment =
            DialpadFragment().apply {
                arguments = bundleOf(ARG_INITIAL_NUMBER to initialNumber.orEmpty())
            }
    }
}
