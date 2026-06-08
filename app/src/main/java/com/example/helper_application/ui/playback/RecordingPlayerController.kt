package com.example.helper_application.ui.playback

import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import com.example.helper_application.R
import com.example.helper_application.util.AppLog
import java.io.File

class RecordingPlayerController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    var playingPath: String? = null
        private set

    var onStateChanged: (() -> Unit)? = null

    fun isPlaying(path: String): Boolean =
        playingPath == path && mediaPlayer?.isPlaying == true

    fun toggle(file: File) {
        val path = file.absolutePath
        if (isPlaying(path)) {
            stop()
            return
        }
        play(file)
    }

    fun play(file: File) {
        stop()
        if (!file.exists() || file.length() <= 0L) {
            Toast.makeText(context, R.string.playback_failed, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            mediaPlayer = MediaPlayer().apply {
                setOnErrorListener { _, what, extra ->
                    AppLog.w("Timeline playback error what=$what extra=$extra path=${file.path}")
                    Toast.makeText(context, R.string.playback_failed, Toast.LENGTH_SHORT).show()
                    stop()
                    true
                }
                setOnCompletionListener { stop() }
                setDataSource(file.absolutePath)
                setOnPreparedListener {
                    start()
                    playingPath = file.absolutePath
                    onStateChanged?.invoke()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            AppLog.e("Timeline playback failed", e)
            Toast.makeText(context, R.string.playback_failed, Toast.LENGTH_SHORT).show()
            stop()
        }
    }

    fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        playingPath = null
        onStateChanged?.invoke()
    }

    fun release() = stop()
}
