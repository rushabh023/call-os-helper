package com.example.helper_application.accessibility

import android.content.Context

/**
 * Active [AppConnectorService] instance — Cube/ACR record from the accessibility-bound process.
 */
object AppConnectorHolder {

    @Volatile
    var service: AppConnectorService? = null

    /** Prefer accessibility service context for MediaRecorder on Android 10+. */
    fun recordingContext(fallback: Context): Context = service ?: fallback
}
