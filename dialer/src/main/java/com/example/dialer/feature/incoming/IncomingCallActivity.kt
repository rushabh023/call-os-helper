package com.example.dialer.feature.incoming

import android.os.Bundle
import android.telecom.Call
import android.telecom.VideoProfile
import androidx.appcompat.app.AppCompatActivity
import com.example.dialer.databinding.ActivityIncomingCallBinding
import com.example.dialer.feature.incall.CallSessionController
import com.example.dialer.feature.incall.InCallActivity

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val displayName = intent.getStringExtra(InCallActivity.EXTRA_DISPLAY_NAME).orEmpty()
        val phoneNumber = intent.getStringExtra(InCallActivity.EXTRA_PHONE_NUMBER).orEmpty()
        binding.incomingName.text = displayName.ifBlank { phoneNumber }
        binding.incomingNumber.text = phoneNumber

        binding.btnAnswer.setOnClickListener { answer() }
        binding.btnDecline.setOnClickListener { decline() }
        binding.btnDeclineMessage.setOnClickListener { declineWithSms() }
    }

    private fun answer() {
        val call = CallSessionController.primaryCall() ?: return
        call.answer(VideoProfile.STATE_AUDIO_ONLY)
        startActivity(
            android.content.Intent(this, InCallActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(InCallActivity.EXTRA_DISPLAY_NAME, binding.incomingName.text)
                putExtra(InCallActivity.EXTRA_PHONE_NUMBER, binding.incomingNumber.text)
            }
        )
        finish()
    }

    private fun decline() {
        CallSessionController.primaryCall()?.disconnect()
        finish()
    }

    private fun declineWithSms() {
        val number = binding.incomingNumber.text.toString()
        CallSessionController.primaryCall()?.disconnect()
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("sms:$number")
            putExtra("sms_body", getString(com.example.dialer.R.string.decline_sms_default))
        }
        startActivity(intent)
        finish()
    }
}
