package com.example.helper_application.recording

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.helper_application.shizuku.ShizukuManager
import com.example.helper_application.util.AppLog
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread

class AudioRecordWavEngine(
    private val cacheDir: File,
    private val appContext: android.content.Context
) : RecorderEngine {
    override val id: String = "audio_record_mic_wav"
    override val outputExtension: String = "wav"

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    @Volatile private var selectedSampleRate = 16_000

    private var recorder: AudioRecord? = null
    private var tempFile: File? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    @Volatile private var bytesWritten = 0L
    @Volatile private var signalPeak = 0
    override var lastAudioSourceLabel: String? = null
        private set
    override val lastSignalPeak: Int?
        get() = signalPeak

    override val isRecording: Boolean
        get() = running

    override fun start(): File? {
        if (running) return tempFile
        lastAudioSourceLabel = null
        bytesWritten = 0L
        signalPeak = 0
        val shizukuBlocked = ShizukuManager.readinessReason() == "service_bind_blocked"
        val sourceOrder = RecordingAudioSources.captureOrderFor(
            context = appContext,
            shizukuBlockedProfile = shizukuBlocked
        )
        val sampleRateOrder = if (shizukuBlocked) {
            listOf(48_000, 16_000, 8_000)
        } else {
            listOf(16_000, 8_000)
        }
        AppLog.Engine.detail(
            "source_profile",
            "engine" to id,
            "shizukuBlocked" to shizukuBlocked,
            "order" to sourceOrder.joinToString(" -> ") { it.label },
            "sampleRates" to sampleRateOrder.joinToString(" -> ")
        )

        val output = File(cacheDir, "call_${System.currentTimeMillis()}.$outputExtension")

        for (sampleRate in sampleRateOrder) {
            val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBuffer <= 0) {
                AppLog.Engine.d("AudioRecordWavEngine skip sampleRate=$sampleRate invalid buffer=$minBuffer")
                continue
            }
            for (candidate in sourceOrder) {
                releaseInternal()
                val started = tryStartWithSource(output, minBuffer, sampleRate, candidate)
                if (started) {
                    selectedSampleRate = sampleRate
                    lastAudioSourceLabel = "${candidate.label}@${sampleRate}Hz"
                    RecordingPreferences.setLastAudioSource(appContext, lastAudioSourceLabel!!)
                    tempFile = output
                    AppLog.Engine.i(
                        "AudioRecordWavEngine started (${lastAudioSourceLabel} → ${output.absolutePath})"
                    )
                    return output
                }
                AppLog.Engine.d(
                    "AudioRecordWavEngine: ${candidate.label}@${sampleRate}Hz not available, trying next"
                )
            }
        }

        output.delete()
        AppLog.Engine.e("AudioRecordWavEngine: all audio sources failed")
        return null
    }

    private fun tryStartWithSource(
        output: File,
        minBuffer: Int,
        sampleRate: Int,
        candidate: RecordingAudioSources.Candidate
    ): Boolean {
        val audioRecord = try {
            AudioRecord(
                candidate.source,
                sampleRate,
                channelConfig,
                audioFormat,
                minBuffer * 2
            )
        } catch (e: Exception) {
            AppLog.Engine.d("AudioRecord ${candidate.label} create failed: ${e.message}")
            return false
        }
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return false
        }

        return try {
            RandomAccessFile(output, "rw").use { raf ->
                raf.setLength(0)
                raf.write(ByteArray(44))
            }
            running = true
            recorder = audioRecord
            audioRecord.startRecording()
            worker = thread(start = true, name = "wav-recorder-writer") {
                writeLoop(audioRecord, output)
            }
            true
        } catch (e: Exception) {
            AppLog.Engine.d("AudioRecord ${candidate.label} start failed: ${e.message}")
            releaseInternal()
            if (output.exists()) output.delete()
            false
        }
    }

    override fun stop(): File? {
        val file = tempFile
        running = false
        try {
            runCatching { recorder?.stop() }
            worker?.join(1_500)
            worker = null
            recorder?.release()
            recorder = null
            if (file != null && file.exists() && bytesWritten > 0) {
                writeWavHeader(file, bytesWritten)
            }
        } catch (e: Exception) {
            AppLog.Engine.e("AudioRecordWavEngine stop failed", e)
        } finally {
            tempFile = null
        }
        AppLog.Engine.d(
            "AudioRecordWavEngine stopped, source=$lastAudioSourceLabel, " +
                "sampleRate=$selectedSampleRate, pcmBytes=$bytesWritten, fileBytes=${file?.length() ?: 0}"
        )
        return file
    }

    private fun writeLoop(audioRecord: AudioRecord, output: File) {
        val buffer = ByteArray(4096)
        try {
            RandomAccessFile(output, "rw").use { raf ->
                raf.seek(44)
                while (running) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        raf.write(buffer, 0, read)
                        bytesWritten += read.toLong()
                        updatePeak(buffer, read)
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.Engine.e("AudioRecordWavEngine writeLoop failed", e)
        }
    }

    private fun writeWavHeader(file: File, pcmDataSize: Long) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = selectedSampleRate * channels * bitsPerSample / 8
        val totalDataLen = pcmDataSize + 36
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.writeBytes("RIFF")
            raf.writeIntLE(totalDataLen.toInt())
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            raf.writeIntLE(16)
            raf.writeShortLE(1.toShort())
            raf.writeShortLE(channels.toShort())
            raf.writeIntLE(selectedSampleRate)
            raf.writeIntLE(byteRate)
            raf.writeShortLE((channels * bitsPerSample / 8).toShort())
            raf.writeShortLE(bitsPerSample.toShort())
            raf.writeBytes("data")
            raf.writeIntLE(pcmDataSize.toInt())
        }
    }

    private fun releaseInternal() {
        running = false
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        worker = null
        bytesWritten = 0
    }

    private fun updatePeak(buffer: ByteArray, bytes: Int) {
        var i = 0
        while (i + 1 < bytes) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xff)).toShort()
            val abs = kotlin.math.abs(sample.toInt())
            if (abs > signalPeak) signalPeak = abs
            i += 2
        }
    }
}

private fun RandomAccessFile.writeIntLE(value: Int) {
    write(
        byteArrayOf(
            (value and 0xff).toByte(),
            (value shr 8 and 0xff).toByte(),
            (value shr 16 and 0xff).toByte(),
            (value shr 24 and 0xff).toByte()
        )
    )
}

private fun RandomAccessFile.writeShortLE(value: Short) {
    write(
        byteArrayOf(
            (value.toInt() and 0xff).toByte(),
            (value.toInt() shr 8 and 0xff).toByte()
        )
    )
}
