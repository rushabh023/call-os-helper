package com.example.helper_application.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.helper_application.recording.CallAudioBoost
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.util.AppLog

/**
 * Mes Validation Connection — accessibility service in Helper Application.
 * User enables under Settings → Accessibility → Mes Validation Connection.
 */
class AppConnectorService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppConnectorHolder.service = this
        AppLog.i("Mes Validation Connection accessibility service connected")
        RecordingPreferences.setAppConnectorEnabled(applicationContext, true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        // Cube ACR Helper uses Accessibility to stay bound during calls and monitor dialer/VoIP UI.
        // We only trace call-related windows at debug level to avoid log spam.
        val pkg = event.packageName?.toString() ?: return
        val isCallUi = pkg.contains("dialer", ignoreCase = true) ||
            pkg.contains("telecom", ignoreCase = true) ||
            pkg.contains("incall", ignoreCase = true) ||
            pkg == "com.android.phone"
        if (isCallUi && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            AppLog.Setup.d("App Connector: call UI foreground ($pkg)")
            if (RecordingPreferences.isAcrStyleRecording(applicationContext)) {
                CallAudioBoost.applyForCall(this, force = true)
            }
        }
    }

    override fun onInterrupt() {
        AppLog.w("App Connector interrupted")
    }

    override fun onDestroy() {
        AppConnectorHolder.service = null
        AppLog.i("App Connector service destroyed")
        RecordingPreferences.setAppConnectorEnabled(applicationContext, false)
        super.onDestroy()
    }
}
