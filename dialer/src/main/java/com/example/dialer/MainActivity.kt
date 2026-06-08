package com.example.dialer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.dialer.core.utils.DefaultDialerGate
import com.example.dialer.core.utils.DefaultDialerHelper
import com.example.dialer.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var navSetupDone = false

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshGate()
        if (!DefaultDialerHelper.isDefaultDialer(this)) {
            Toast.makeText(this, R.string.default_dialer_gate_denied, Toast.LENGTH_LONG).show()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* requested after gate passes */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSetDefault.setOnClickListener { promptDefaultDialer() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!DefaultDialerHelper.isDefaultDialer(this@MainActivity)) {
                    // Do not allow entering the app without default dialer.
                    moveTaskToBack(true)
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

        refreshGate()
    }

    override fun onResume() {
        super.onResume()
        refreshGate()
    }

    private fun refreshGate() {
        val isDefault = DefaultDialerHelper.isDefaultDialer(this)
        DefaultDialerGate.applyToMain(
            activity = this,
            mainContent = binding.mainContent,
            gate = binding.defaultDialerGate
        )
        if (isDefault) {
            ensureNavSetup()
            requestRuntimePermissionsIfNeeded()
        }
    }

    private fun ensureNavSetup() {
        if (navSetupDone) return
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHost.navController)
        navSetupDone = true
    }

    private fun promptDefaultDialer() {
        DefaultDialerHelper.requestDefaultDialer(this, roleLauncher)
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.READ_PHONE_STATE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.READ_CALL_LOG
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.READ_CONTACTS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.CALL_PHONE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.RECORD_AUDIO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }
}
