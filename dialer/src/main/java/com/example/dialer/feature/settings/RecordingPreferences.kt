package com.example.dialer.feature.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("recording_prefs", Context.MODE_PRIVATE)

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
