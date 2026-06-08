package com.example.helper_application.dialer

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.helper_application.HelperRecordingCallbacks
import com.example.helper_application.R
import com.example.helper_application.dialer.core.utils.DefaultDialerGate
import com.example.helper_application.dialer.core.utils.DefaultDialerHelper
import com.example.helper_application.dialer.feature.incall.InCallUiLauncher
import com.example.helper_application.setup.PermissionHelper
import com.example.helper_application.recording.CallMonitorService
import com.example.helper_application.recording.MediaProjectionHolder
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.recording.RecordingStorage
import com.example.helper_application.setup.SetupPreferences
import com.example.helper_application.shizuku.ShizukuManager
import com.example.helper_application.telephony.CallMonitoringCoordinator
import com.example.helper_application.util.AppLog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DialerShellActivity : AppCompatActivity(), HelperRecordingCallbacks {

    private lateinit var mainContent: View
    private lateinit var defaultDialerGate: View
    private lateinit var bottomNav: BottomNavigationView
    private var navSetupDone = false
    private var setupPromptShown = false

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
    ) { /* granted after gate */ }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            CallMonitorService.grantMediaProjection(this, result.resultCode, result.data!!)
        } else {
            AppLog.w("Two-way recording: MediaProjection denied resultCode=${result.resultCode}")
            RecordingPreferences.setDualCaptureEnabled(this, false)
            MediaProjectionHolder.clear()
            RecordingPreferences.setLastError(
                this,
                "Two-way not enabled: tap Enable, then Share screen / Start on the system dialog."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialer_activity_main)
        mainContent = findViewById(R.id.mainContent)
        defaultDialerGate = findViewById(R.id.defaultDialerGate)
        bottomNav = findViewById(R.id.bottomNav)
        findViewById<MaterialButton>(R.id.btnSetDefault).setOnClickListener { promptDefaultDialer() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!DefaultDialerHelper.isDefaultDialer(this@DialerShellActivity)) {
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
        InCallUiLauncher.bringToFrontIfNeeded(this)
        refreshGate()
        ShizukuManager.resumeSetup(this)
        if (DefaultDialerHelper.isDefaultDialer(this)) {
            if (SetupPreferences.isSetupComplete(this)) {
                RecordingStorage.ensureFolderExists(this)
            } else {
                maybePromptSetup()
            }
            startBackgroundRecordingIfReady()
        }
    }

    private fun refreshGate() {
        DefaultDialerGate.applyToMain(
            activity = this,
            mainContent = mainContent,
            gate = defaultDialerGate
        )
        if (DefaultDialerHelper.isDefaultDialer(this)) {
            ensureNavSetup()
            requestRuntimePermissionsIfNeeded()
        }
    }

    private fun ensureNavSetup() {
        if (navSetupDone) return
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        bottomNav.setupWithNavController(navHost.navController)
        navSetupDone = true
    }

    private fun promptDefaultDialer() {
        DefaultDialerHelper.requestDefaultDialer(this, roleLauncher)
    }

    override fun startBackgroundRecordingIfReady() {
        CallMonitoringCoordinator.startIfReady(this)
    }

    override fun requestDualCaptureProjection() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            AppLog.w("Two-way capture requires Android 10+")
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.two_way_dialog_title)
            .setMessage(R.string.two_way_dialog_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.two_way_dialog_continue) { _, _ ->
                val mgr = getSystemService(MediaProjectionManager::class.java)
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    mgr.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                } else {
                    mgr.createScreenCaptureIntent()
                }
                mediaProjectionLauncher.launch(intent)
            }
            .show()
    }

    private fun maybePromptSetup() {
        if (setupPromptShown || SetupPreferences.isSetupComplete(this)) return
        setupPromptShown = true
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.setup_required_title)
            .setMessage(R.string.setup_required_message)
            .setNegativeButton(R.string.setup_required_later, null)
            .setPositiveButton(R.string.setup_required_go) { _, _ -> openHelperSetup() }
            .show()
    }

    private fun openHelperSetup() {
        ensureNavSetup()
        bottomNav.selectedItemId = R.id.more_nav
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navHost.navController.navigate(R.id.helperHostFragment)
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val required = PermissionHelper.requiredRuntimePermissions().toMutableList()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            required += Manifest.permission.WRITE_CALL_LOG
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            required += Manifest.permission.READ_CONTACTS
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            required += Manifest.permission.CALL_PHONE
        }
        val needed = required.distinct().filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }
}
