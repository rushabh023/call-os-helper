package com.example.helper_application.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.helper_application.recording.RecordingPreferences
import com.example.helper_application.util.AppLog

/**
 * Mes Validation Connection — accessibility service in Helper Application.
 * User enables under Settings → Accessibility → Mes Validation Connection.
 */
class AppConnectorService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppLog.i("Mes Validation Connection accessibility service connected")
        RecordingPreferences.setAppConnectorEnabled(applicationContext, true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally quiet — enable debug in AppLog if you need per-event traces.
    }

    override fun onInterrupt() {
        AppLog.w("App Connector interrupted")
    }

    override fun onDestroy() {
        AppLog.i("App Connector service destroyed")
        RecordingPreferences.setAppConnectorEnabled(applicationContext, false)
        super.onDestroy()
    }
}
