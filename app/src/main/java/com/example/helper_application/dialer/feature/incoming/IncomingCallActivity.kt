package com.example.helper_application.dialer.feature.incoming

import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.VideoProfile
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.AvatarHelper
import com.example.helper_application.dialer.feature.incall.CallSessionController
import com.example.helper_application.dialer.feature.incall.InCallActivity
import com.example.helper_application.dialer.feature.incall.InCallUiLauncher
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var incomingName: TextView
    private lateinit var incomingNumber: TextView
    private var displayName: String = ""
    private var phoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyIncomingWindowFlags()
        setContentView(R.layout.activity_incoming_call)
        incomingName = findViewById(R.id.incomingName)
        incomingNumber = findViewById(R.id.incomingNumber)

        displayName = intent.getStringExtra(InCallActivity.EXTRA_DISPLAY_NAME).orEmpty()
        phoneNumber = intent.getStringExtra(InCallActivity.EXTRA_PHONE_NUMBER).orEmpty()
        val shownName = displayName.ifBlank { phoneNumber }
        incomingName.text = shownName
        incomingNumber.text = phoneNumber
        AvatarHelper.bindInitials(findViewById(R.id.incomingAvatar), shownName)

        findViewById<FloatingActionButton>(R.id.btnAnswer).setOnClickListener { answer() }
        findViewById<MaterialButton>(R.id.btnDecline).setOnClickListener { decline() }
        findViewById<MaterialButton>(R.id.btnDeclineMessage).setOnClickListener { declineWithSms() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    val call = CallSessionController.primaryCall()
                    when {
                        call == null -> finish()
                        call.state == Call.STATE_DISCONNECTED ||
                            call.state == Call.STATE_DISCONNECTING -> finish()
                        call.state == Call.STATE_ACTIVE ||
                            call.state == Call.STATE_CONNECTING ||
                            call.state == Call.STATE_HOLDING -> {
                            InCallUiLauncher.show(
                                this@IncomingCallActivity,
                                call,
                                displayName.ifBlank { phoneNumber },
                                phoneNumber,
                                answered = true
                            )
                            finish()
                        }
                    }
                    delay(400)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        displayName = intent.getStringExtra(InCallActivity.EXTRA_DISPLAY_NAME).orEmpty()
        phoneNumber = intent.getStringExtra(InCallActivity.EXTRA_PHONE_NUMBER).orEmpty()
        val shownName = displayName.ifBlank { phoneNumber }
        incomingName.text = shownName
        incomingNumber.text = phoneNumber
    }

    private fun answer() {
        val call = CallSessionController.primaryCall() ?: return
        call.answer(VideoProfile.STATE_AUDIO_ONLY)
        InCallUiLauncher.show(
            this,
            call,
            displayName.ifBlank { phoneNumber },
            phoneNumber,
            answered = true
        )
        finish()
    }

    private fun applyIncomingWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun decline() {
        CallSessionController.primaryCall()?.disconnect()
        finish()
    }

    private fun declineWithSms() {
        val number = phoneNumber
        CallSessionController.primaryCall()?.disconnect()
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("sms:$number")
            putExtra("sms_body", getString(R.string.decline_sms_default))
        }
        startActivity(intent)
        finish()
    }
}
