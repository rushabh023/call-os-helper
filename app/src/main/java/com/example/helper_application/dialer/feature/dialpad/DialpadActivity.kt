package com.example.helper_application.dialer.feature.dialpad

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.DefaultDialerGate

class DialpadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DefaultDialerGate.redirectIfNotDefault(this)) return
        setContentView(R.layout.activity_dialpad_host)

        val data = intent?.data
        val initial = when {
            data?.scheme == "tel" -> data.schemeSpecificPart.orEmpty()
            else -> intent?.getStringExtra(android.content.Intent.EXTRA_PHONE_NUMBER).orEmpty()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.dialpad_host, DialpadFragment.newInstance(initial))
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        DefaultDialerGate.redirectIfNotDefault(this)
    }
}
