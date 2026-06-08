package com.example.dialer.feature.dialpad

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dialer.R
import com.example.dialer.core.utils.DefaultDialerGate
import com.example.dialer.databinding.ActivityDialpadHostBinding

class DialpadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DefaultDialerGate.redirectIfNotDefault(this)) return

        val binding = ActivityDialpadHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
