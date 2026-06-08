package com.example.helper_application.dialer.feature.settings

import androidx.fragment.app.FragmentActivity
import com.example.helper_application.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object RecordingDisclaimerDialog {
    fun showIfNeeded(activity: FragmentActivity, onAccepted: () -> Unit) {
        val prefs = DialerRecordingPreferences(activity.applicationContext)
        if (prefs.isDisclaimerAccepted()) {
            onAccepted()
            return
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.recording_disclaimer_title)
            .setMessage(R.string.recording_disclaimer_message)
            .setCancelable(false)
            .setPositiveButton(R.string.recording_disclaimer_accept) { _, _ ->
                prefs.setDisclaimerAccepted(true)
                onAccepted()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
