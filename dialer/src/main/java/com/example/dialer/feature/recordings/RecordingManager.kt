package com.example.dialer.feature.recordings

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.dialer.core.utils.PhoneNumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingSessionState(
    val isRecording: Boolean = false,
    val startedAtMs: Long = 0L,
    val displayLabel: String = "",
    val outputPath: String? = null,
    val audioSourceLabel: String = ""
)

class RecordingManager(private val context: Context) {

    private val _state = MutableStateFlow(RecordingSessionState())
    val state: StateFlow<RecordingSessionState> = _state.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var lastAudioSourceLabel = "MIC"

    fun start(
        contactName: String?,
        phoneNumber: String?
    ): Boolean {
        if (_state.value.isRecording) return true
        val outputFile = createOutputFile(contactName, phoneNumber)
        val source = pickBestAudioSource()
        if (!startRecorder(outputFile, source.first, source.second)) {
            outputFile.delete()
            return false
        }
        currentOutputFile = outputFile
        lastAudioSourceLabel = source.second
        val label = contactName?.takeIf { it.isNotBlank() } ?: phoneNumber.orEmpty()
        _state.value = RecordingSessionState(
            isRecording = true,
            startedAtMs = System.currentTimeMillis(),
            displayLabel = label,
            outputPath = outputFile.absolutePath,
            audioSourceLabel = lastAudioSourceLabel
        )
        RecordingForegroundService.start(context, label)
        Timber.i("Recording started source=%s path=%s", lastAudioSourceLabel, outputFile.absolutePath)
        return true
    }

    fun stop(): RecordingResult? {
        if (!_state.value.isRecording) return null
        val path = currentOutputFile?.absolutePath
        val startedAt = _state.value.startedAtMs
        val label = _state.value.displayLabel
        val source = lastAudioSourceLabel
        releaseRecorder()
        RecordingForegroundService.stop(context)
        _state.value = RecordingSessionState()
        val file = path?.let { File(it) }
        if (file == null || !file.exists() || file.length() <= 0L) {
            file?.delete()
            Timber.w("Recording stop: empty file")
            return null
        }
        publishToMediaStore(file)
        return RecordingResult(
            filePath = file.absolutePath,
            displayName = file.nameWithoutExtension,
            durationMs = System.currentTimeMillis() - startedAt,
            fileSizeBytes = file.length(),
            audioSourceLabel = source,
            contactLabel = label
        )
    }

    fun isRecording(): Boolean = _state.value.isRecording

    private fun pickBestAudioSource(): Pair<Int, String> {
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaRecorder.AudioSource.VOICE_CALL to "VOICE_CALL")
                add(MediaRecorder.AudioSource.VOICE_DOWNLINK to "VOICE_DOWNLINK")
                add(MediaRecorder.AudioSource.VOICE_UPLINK to "VOICE_UPLINK")
            } else {
                add(MediaRecorder.AudioSource.VOICE_CALL to "VOICE_CALL")
            }
            add(MediaRecorder.AudioSource.VOICE_COMMUNICATION to "VOICE_COMMUNICATION")
            add(MediaRecorder.AudioSource.MIC to "MIC")
        }
        for ((source, label) in candidates) {
            val probe = File(context.cacheDir, "probe_${label}.m4a")
            if (tryProbe(probe, source, label)) {
                probe.delete()
                return source to label
            }
            probe.delete()
        }
        return MediaRecorder.AudioSource.MIC to "MIC"
    }

    private fun tryProbe(file: File, source: Int, label: String): Boolean {
        return try {
            val recorder = createMediaRecorder()
            recorder.setAudioSource(source)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setAudioSamplingRate(44_100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            Thread.sleep(120)
            recorder.stop()
            recorder.release()
            file.exists() && file.length() > 0L
        } catch (e: Exception) {
            Timber.d("Audio source probe failed %s: %s", label, e.message)
            false
        }
    }

    private fun startRecorder(file: File, source: Int, label: String): Boolean {
        return try {
            val recorder = createMediaRecorder()
            recorder.setAudioSource(source)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setAudioSamplingRate(44_100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            lastAudioSourceLabel = label
            true
        } catch (e: Exception) {
            Timber.e(e, "startRecorder failed for %s", label)
            releaseRecorder()
            false
        }
    }

    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun releaseRecorder() {
        mediaRecorder?.runCatching {
            stop()
            release()
        }
        mediaRecorder = null
        currentOutputFile = null
    }

    private fun createOutputFile(contactName: String?, phoneNumber: String?): File {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
        val label = PhoneNumberFormatter.displayNameForFile(contactName, phoneNumber)
        val fileName = "${stamp}_$label.m4a"
        val recordingsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS),
                "Dialer"
            )
        } else {
            @Suppress("DEPRECATION")
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS), "Dialer")
        }
        recordingsDir.mkdirs()
        return File(recordingsDir, fileName)
    }

    private fun publishToMediaStore(file: File) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RECORDINGS + "/Dialer")
            put(MediaStore.Audio.Media.IS_PENDING, 0)
        }
        context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
    }
}

data class RecordingResult(
    val filePath: String,
    val displayName: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val audioSourceLabel: String,
    val contactLabel: String
)
