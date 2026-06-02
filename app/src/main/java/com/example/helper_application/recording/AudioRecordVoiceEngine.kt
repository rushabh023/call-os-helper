package com.example.helper_application.recording

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.helper_application.util.AppLog
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread

/**
 * Fallback when MediaRecorder AMR is silent: raw PCM via AudioRecord at 8 kHz (voice-band),
 * MIC / VOICE_RECOGNITION first — often works with speaker boost on Samsung.
 */
class AudioRecordVoiceEngine(
    private val cacheDir: File,
    private val appContext: android.content.Context
) : RecorderEngine {
    override val id: String = "audio_record_8k_voice"
    override val outputExtension: String = "wav"

    private val sampleRate = 8_000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private fun sourceOrder(): List<Int> {
        val order = RecordingAudioSources.captureOrderFor(appContext, shizukuBlockedProfile = true)
        return order.map { it.source }
    }

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
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuffer <= 0) {
            AppLog.Engine.e("AudioRecordVoiceEngine invalid min buffer: $minBuffer")
            return null
        }
        val output = File(cacheDir, "call_voice_${System.currentTimeMillis()}.$outputExtension")

        for (source in sourceOrder()) {
            releaseInternal()
            val label = sourceLabel(source)
            if (tryStart(output, minBuffer, source, label)) {
                lastAudioSourceLabel = label
                RecordingPreferences.setLastAudioSource(appContext, label)
                tempFile = output
                AppLog.Engine.i("AudioRecordVoiceEngine started ($label @ ${sampleRate}Hz)")
                return output
            }
        }
        output.delete()
        return null
    }

    private fun tryStart(output: File, minBuffer: Int, source: Int, label: String): Boolean {
        val audioRecord = try {
            AudioRecord(source, sampleRate, channelConfig, audioFormat, minBuffer * 4)
        } catch (e: Exception) {
            AppLog.Engine.d("AudioRecordVoice $label: ${e.message}")
            return false
        }
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return false
        }
        return try {
            RandomAccessFile(output, "rw").use { it.setLength(0); it.write(ByteArray(44)) }
            running = true
            recorder = audioRecord
            audioRecord.startRecording()
            worker = thread(name = "voice-8k-writer") { writeLoop(audioRecord, output) }
            true
        } catch (e: Exception) {
            releaseInternal()
            output.delete()
            false
        }
    }

    override fun stop(): File? {
        val file = tempFile
        running = false
        runCatching { recorder?.stop() }
        worker?.join(2_000)
        recorder?.release()
        recorder = null
        worker = null
        tempFile = null
        if (file != null && file.exists() && bytesWritten > 0) {
            writeWavHeader(file, bytesWritten)
        }
        AppLog.Engine.d(
            "AudioRecordVoiceEngine stopped source=$lastAudioSourceLabel pcm=$bytesWritten " +
                "file=${file?.length() ?: 0}"
        )
        return file
    }

    private fun writeLoop(audioRecord: AudioRecord, output: File) {
        val buffer = ByteArray(1600)
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
            AppLog.Engine.e("AudioRecordVoiceEngine writeLoop", e)
        }
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

    private fun writeWavHeader(file: File, pcmSize: Long) {
        val channels = 1
        val bits = 16
        val byteRate = sampleRate * channels * bits / 8
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.writeBytes("RIFF")
            writeIntLe(raf, (pcmSize + 36).toInt())
            raf.writeBytes("WAVEfmt ")
            writeIntLe(raf, 16)
            writeShortLe(raf, 1)
            writeShortLe(raf, channels.toShort())
            writeIntLe(raf, sampleRate)
            writeIntLe(raf, byteRate)
            writeShortLe(raf, (channels * bits / 8).toShort())
            writeShortLe(raf, bits.toShort())
            raf.writeBytes("data")
            writeIntLe(raf, pcmSize.toInt())
        }
    }

    private fun releaseInternal() {
        running = false
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        worker = null
    }

    private fun sourceLabel(source: Int): String = when (source) {
        MediaRecorder.AudioSource.MIC -> "MIC"
        MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
        MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
        @Suppress("DEPRECATION")
        MediaRecorder.AudioSource.VOICE_CALL -> "VOICE_CALL"
        else -> "SRC_$source"
    }
}

private fun writeIntLe(raf: RandomAccessFile, v: Int) {
    raf.write(byteArrayOf(
        (v and 0xff).toByte(), (v shr 8 and 0xff).toByte(),
        (v shr 16 and 0xff).toByte(), (v shr 24 and 0xff).toByte()
    ))
}

private fun writeShortLe(raf: RandomAccessFile, v: Short) {
    raf.write(byteArrayOf(
        (v.toInt() and 0xff).toByte(), (v.toInt() shr 8 and 0xff).toByte()
    ))
}

private fun writeShortLe(raf: RandomAccessFile, v: Int) {
    writeShortLe(raf, v.toShort())
}
