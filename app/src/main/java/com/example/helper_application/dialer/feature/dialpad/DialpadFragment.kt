package com.example.helper_application.dialer.feature.dialpad

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.DialerCallPlacer
import com.example.helper_application.dialer.core.utils.attachVerticalList
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DialpadFragment : Fragment() {

    private val viewModel: DialpadViewModel by viewModels()
    private lateinit var matchAdapter: DialpadMatchAdapter
    private lateinit var numberField: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dialpad, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val initial = arguments?.getString(ARG_INITIAL_NUMBER).orEmpty()
        if (initial.isNotBlank()) viewModel.setDigits(initial)

        numberField = view.findViewById(R.id.numberField)
        matchAdapter = DialpadMatchAdapter { match -> placeCall(match.phoneNumber) }
        view.findViewById<RecyclerView>(R.id.matchList).apply {
            attachVerticalList()
            adapter = matchAdapter
        }

        val keys = listOf(
            view.findViewById<MaterialButton>(R.id.key1) to '1',
            view.findViewById<MaterialButton>(R.id.key2) to '2',
            view.findViewById<MaterialButton>(R.id.key3) to '3',
            view.findViewById<MaterialButton>(R.id.key4) to '4',
            view.findViewById<MaterialButton>(R.id.key5) to '5',
            view.findViewById<MaterialButton>(R.id.key6) to '6',
            view.findViewById<MaterialButton>(R.id.key7) to '7',
            view.findViewById<MaterialButton>(R.id.key8) to '8',
            view.findViewById<MaterialButton>(R.id.key9) to '9',
            view.findViewById<MaterialButton>(R.id.keyStar) to '*',
            view.findViewById<MaterialButton>(R.id.key0) to '0',
            view.findViewById<MaterialButton>(R.id.keyHash) to '#'
        )
        styleDialKeyLabels(view)

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

        view.findViewById<MaterialButton>(R.id.key0).setOnLongClickListener {
            viewModel.appendDigit('+')
            true
        }

        val btnBackspace: ImageButton = view.findViewById(R.id.btnBackspace)
        btnBackspace.setOnClickListener { viewModel.backspace() }
        btnBackspace.setOnLongClickListener {
            viewModel.clearAll()
            true
        }

        view.findViewById<FloatingActionButton>(R.id.btnCall).setOnClickListener {
            val number = viewModel.state.value.digits
            if (number.isNotBlank()) placeCall(number)
        }

        numberField.setOnLongClickListener {
            val clip = requireContext().getSystemService(ClipboardManager::class.java)
            clip.setPrimaryClip(ClipData.newPlainText("number", viewModel.state.value.digits))
            Toast.makeText(requireContext(), R.string.copied_number, Toast.LENGTH_SHORT).show()
            true
        }

        view.findViewById<MaterialButton>(R.id.btnAddContact).setOnClickListener {
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
                    numberField.text = state.digits
                    matchAdapter.submit(state.matches)
                }
            }
        }
    }

    private fun placeCall(number: String) {
        DialerCallPlacer.placeCall(requireContext(), number)
    }

    private fun styleDialKeyLabels(root: View) {
        val letters = mapOf(
            R.id.key2 to "ABC",
            R.id.key3 to "DEF",
            R.id.key4 to "GHI",
            R.id.key5 to "JKL",
            R.id.key6 to "MNO",
            R.id.key7 to "PQRS",
            R.id.key8 to "TUV",
            R.id.key9 to "WXYZ",
            R.id.key0 to "+"
        )
        val subColor = ContextCompat.getColor(requireContext(), R.color.dialer_on_surface_variant)
        letters.forEach { (id, sub) ->
            val button = root.findViewById<MaterialButton>(id)
            val digit = button.text.first().toString()
            val text = "$digit\n$sub"
            val span = SpannableString(text)
            span.setSpan(RelativeSizeSpan(0.42f), digit.length + 1, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(ForegroundColorSpan(subColor), digit.length + 1, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(StyleSpan(Typeface.NORMAL), digit.length + 1, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            button.text = span
        }
    }

    private fun vibrateTick() {
        val vibrator = requireContext().getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    companion object {
        private const val ARG_INITIAL_NUMBER = "initial_number"

        fun newInstance(initialNumber: String? = null): DialpadFragment =
            DialpadFragment().apply {
                arguments = bundleOf(ARG_INITIAL_NUMBER to initialNumber.orEmpty())
            }
    }
}
