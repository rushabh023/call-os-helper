package com.example.helper_application.recording

import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper

/**
 * Holds the user-granted [MediaProjection] used for playback capture (remote voice / speaker path).
 * Used by [DualCaptureWavEngine] during calls.
 */
object MediaProjectionHolder {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var projection: MediaProjection? = null
        private set

    private var stopCallback: MediaProjection.Callback? = null

    fun isReady(): Boolean = projection != null

    fun setProjection(mediaProjection: MediaProjection?) {
        clear()
        if (mediaProjection == null) return
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                clear()
            }
        }
        stopCallback = callback
        mediaProjection.registerCallback(callback, mainHandler)
        projection = mediaProjection
    }

    fun clear() {
        val current = projection
        val callback = stopCallback
        stopCallback = null
        projection = null
        if (current != null && callback != null) {
            runCatching { current.unregisterCallback(callback) }
        }
        runCatching { current?.stop() }
    }
}
