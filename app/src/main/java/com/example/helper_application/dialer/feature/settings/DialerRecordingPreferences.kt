package com.example.helper_application.dialer.feature.settings

import android.content.Context

class DialerRecordingPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("dialer_recording_prefs", Context.MODE_PRIVATE)

    fun isDisclaimerAccepted(): Boolean = prefs.getBoolean(KEY_DISCLAIMER, false)

    fun setDisclaimerAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_DISCLAIMER, accepted).apply()
    }

    fun isAutoRecordAll(): Boolean = prefs.getBoolean(KEY_AUTO_ALL, false)

    fun setAutoRecordAll(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ALL, enabled).apply()
    }

    fun isAutoRecordUnknownOnly(): Boolean = prefs.getBoolean(KEY_AUTO_UNKNOWN, false)

    fun setAutoRecordUnknownOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_UNKNOWN, enabled).apply()
    }

    companion object {
        private const val KEY_DISCLAIMER = "disclaimer_accepted"
        private const val KEY_AUTO_ALL = "auto_record_all"
        private const val KEY_AUTO_UNKNOWN = "auto_record_unknown"
    }
}
