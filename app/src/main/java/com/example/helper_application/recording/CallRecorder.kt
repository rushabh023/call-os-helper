package com.example.helper_application.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.example.helper_application.util.AppLog
import java.io.File

class CallRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var tempFile: File? = null

    val isRecording: Boolean
        get() = mediaRecorder != null

    fun start(): File? {
        if (isRecording) return tempFile
        val file = File(context.cacheDir, "call_${System.currentTimeMillis()}.m4a")
        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setAudioSamplingRate(44_100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            tempFile = file
            AppLog.i("MediaRecorder started (MIC source)")
            file
        } catch (e: Exception) {
            AppLog.e("MediaRecorder start failed", e)
            releaseQuietly()
            file.delete()
            null
        }
    }

    fun stop(): File? {
        val file = tempFile
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {
                    // Can throw if recording was too short or blocked by the system.
                }
                release()
            }
        } finally {
            mediaRecorder = null
            tempFile = null
        }
        AppLog.d("MediaRecorder stopped, file=${file?.absolutePath}, size=${file?.length() ?: 0}")
        return file
    }

    private fun releaseQuietly() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
        tempFile = null
    }
}
