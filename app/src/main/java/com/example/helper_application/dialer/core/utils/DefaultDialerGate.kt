package com.example.helper_application.dialer.core.utils

import android.app.Activity
import android.content.Intent
import android.view.View
import com.example.helper_application.dialer.DialerShellActivity

/**
 * Hard gate: the dialer cannot be used until [DefaultDialerHelper.isDefaultDialer] is true.
 */
object DefaultDialerGate {

    fun applyToMain(activity: Activity, mainContent: View, gate: View) {
        val isDefault = DefaultDialerHelper.isDefaultDialer(activity)
        mainContent.visibility = if (isDefault) View.VISIBLE else View.GONE
        gate.visibility = if (isDefault) View.GONE else View.VISIBLE
    }

    /** Redirect external entry points (e.g. DIAL intent) back to the gate activity. */
    fun redirectIfNotDefault(activity: Activity): Boolean {
        if (DefaultDialerHelper.isDefaultDialer(activity)) return false
        activity.startActivity(
            Intent(activity, DialerShellActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        activity.finish()
        return true
    }
}
