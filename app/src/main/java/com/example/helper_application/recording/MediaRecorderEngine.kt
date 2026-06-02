package com.example.helper_application.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.helper_application.accessibility.AppConnectorHolder
import com.example.helper_application.shizuku.ShizukuManager
import com.example.helper_application.util.AppLog
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MediaRecorder engine. ACR/Cube-style uses AMR-NB (.amr); advanced mode uses AAC (.m4a).
 */
class MediaRecorderEngine(private val context: Context) : RecorderEngine {

    private val useAmr = RecordingPreferences.isCubeCompatibilityMode(context)

    override val id: String = if (useAmr) "media_recorder_amr" else "media_recorder_m4a"
    override val outputExtension: String = if (useAmr) "amr" else "m4a"

    private var mediaRecorder: MediaRecorder? = null
    private var tempFile: File? = null
    private val amplitudeMonitorRunning = AtomicBoolean(false)
    @Volatile
    private var amplitudeMonitorThread: Thread? = null
    @Volatile
    private var peakAmplitude: Int = 0
    override var lastAudioSourceLabel: String? = null
        private set
    override val lastSignalPeak: Int?
        get() = if (peakAmplitude > 0) peakAmplitude else null

    override val isRecording: Boolean
        get() = mediaRecorder != null

    override fun start(): File? {
        if (isRecording) return tempFile
        lastAudioSourceLabel = null
        peakAmplitude = 0
        val file = File(context.cacheDir, "call_${System.currentTimeMillis()}.$outputExtension")
        val shizukuBlocked = ShizukuManager.readinessReason() == "service_bind_blocked"
        val sourceOrder = RecordingAudioSources.captureOrderFor(
            context = context,
            shizukuBlockedProfile = shizukuBlocked
        )
        AppLog.Engine.detail(
            "source_profile",
            "engine" to id,
            "shizukuBlocked" to shizukuBlocked,
            "order" to sourceOrder.joinToString(" -> ") { it.label }
        )

        // Do not probe at call start — line is often silent; breaks VOICE_RECOGNITION path that worked before.
        for (candidate in sourceOrder) {
            releaseQuietly()
            val started = tryStartWithSource(file, candidate)
            if (started) {
                lastAudioSourceLabel = candidate.label
                RecordingPreferences.setLastAudioSource(context, candidate.label)
                tempFile = file
                startAmplitudeMonitor()
                AppLog.Engine.i(
                    "MediaRecorderEngine started format=${if (useAmr) "AMR_NB" else "AAC_M4A"} " +
                        "source=${candidate.label} path=${file.absolutePath}"
                )
                return file
            }
            AppLog.Engine.d("MediaRecorderEngine: ${candidate.label} not available, trying next")
        }

        file.delete()
        AppLog.Engine.e("MediaRecorderEngine: all audio sources failed (format=$outputExtension)")
        return null
    }

    private fun tryStartWithSource(file: File, candidate: RecordingAudioSources.Candidate): Boolean {
        val recordContext = AppConnectorHolder.recordingContext(context)
        val viaAccessibility = AppConnectorHolder.service != null
        return tryStartWithSourceOnContext(file, candidate, recordContext, viaAccessibility) ||
            (viaAccessibility && tryStartWithSourceOnContext(file, candidate, context, false))
    }

    private fun tryStartWithSourceOnContext(
        file: File,
        candidate: RecordingAudioSources.Candidate,
        recordContext: Context,
        viaAccessibility: Boolean
    ): Boolean {
        return try {
            val recorder = if (useAmr) {
                @Suppress("DEPRECATION")
                MediaRecorder()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(recordContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(candidate.source)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // VOICE_COMMUNICATION defaults to privacy-sensitive capture.
                // For call-recording fallback we prefer shared capture behavior.
                runCatching { recorder.setPrivacySensitive(false) }
            }
            if (useAmr) {
                @Suppress("DEPRECATION")
                recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                @Suppress("DEPRECATION")
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                recorder.setAudioSamplingRate(8_000)
            } else {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128_000)
                recorder.setAudioSamplingRate(44_100)
            }
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            AppLog.Engine.d(
                "MediaRecorder OK: source=${candidate.label} format=$outputExtension " +
                    "viaAccessibility=$viaAccessibility"
            )
            true
        } catch (e: Exception) {
            AppLog.Engine.detail(
                "mediarecorder_source_failed",
                "format" to outputExtension,
                "label" to candidate.label,
                "viaAccessibility" to viaAccessibility,
                "error" to (e.message ?: e.javaClass.simpleName),
                "type" to e.javaClass.simpleName,
                level = Log.DEBUG
            )
            releaseQuietly()
            if (file.exists()) file.delete()
            false
        }
    }

    override fun stop(): File? {
        val file = tempFile
        val recorder = mediaRecorder
        stopAmplitudeMonitor()
        sampleAmplitude(recorder)
        mediaRecorder = null
        tempFile = null
        try {
            recorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    AppLog.Engine.w("MediaRecorder.stop: ${e.message}")
                }
                try {
                    release()
                } catch (e: Exception) {
                    AppLog.Engine.w("MediaRecorder.release: ${e.message}")
                }
            }
        } catch (e: Exception) {
            AppLog.Engine.e("MediaRecorderEngine stop failed", e)
        }
        AppLog.Engine.d(
            "MediaRecorderEngine stopped, format=$outputExtension, source=$lastAudioSourceLabel, " +
                "bytes=${file?.length() ?: 0} peak=$peakAmplitude"
        )
        return file
    }

    private fun startAmplitudeMonitor() {
        stopAmplitudeMonitor()
        amplitudeMonitorRunning.set(true)
        amplitudeMonitorThread = Thread({
            while (amplitudeMonitorRunning.get()) {
                sampleAmplitude(mediaRecorder)
                try {
                    Thread.sleep(250L)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }, "mr-amplitude-monitor").also { it.start() }
    }

    private fun stopAmplitudeMonitor() {
        amplitudeMonitorRunning.set(false)
        amplitudeMonitorThread?.interrupt()
        amplitudeMonitorThread = null
    }

    private fun sampleAmplitude(recorder: MediaRecorder?) {
        if (recorder == null) return
        val amplitude = runCatching { recorder.maxAmplitude }.getOrNull() ?: 0
        if (amplitude > peakAmplitude) {
            peakAmplitude = amplitude
        }
    }

    private fun releaseQuietly() {
        stopAmplitudeMonitor()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
    }
}
