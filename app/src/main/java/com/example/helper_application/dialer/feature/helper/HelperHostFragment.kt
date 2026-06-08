package com.example.helper_application.dialer.feature.helper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.example.helper_application.HelperRecordingCallbacks
import com.example.helper_application.ui.HelperAppRoot
import com.example.helper_application.ui.theme.Helper_applicationTheme

class HelperHostFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val callbacks = activity as? HelperRecordingCallbacks
        return ComposeView(requireContext()).apply {
            setContent {
                Helper_applicationTheme(dynamicColor = false) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        HelperAppRoot(
                            onStartMonitoring = { callbacks?.startBackgroundRecordingIfReady() },
                            onRequestDualCapture = { callbacks?.requestDualCaptureProjection() }
                        )
                    }
                }
            }
        }
    }
}
