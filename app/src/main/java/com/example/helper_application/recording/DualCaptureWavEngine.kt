package com.example.helper_application.recording

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import com.example.helper_application.util.AppLog
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread
import kotlin.math.max

/**
 * Two-way capture: playback (remote) via MediaProjection + MIC (you).
 * Stereo WAV: L = playback, R = mic. Samsung often blocks call playback capture.
 */
class DualCaptureWavEngine(
    private val cacheDir: File,
    private val appContext: Context
) : RecorderEngine {

    override val id: String = "dual_playback_mic_wav"
    override val outputExtension: String = "wav"
    override var lastAudioSourceLabel: String? = null
        private set

    private val sampleRate = 16_000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var playbackRecord: AudioRecord? = null
    private var micRecord: AudioRecord? = null
    private var playbackWorker: Thread? = null
    private var micWorker: Thread? = null
    private var playbackPcm: File? = null
    private var micPcm: File? = null
    private var outputWav: File? = null
    @Volatile private var running = false
    @Volatile private var playbackBytes = 0L
    @Volatile private var micBytes = 0L
    @Volatile private var playbackPeak = 0
    @Volatile private var micPeak = 0

    override val lastSignalPeak: Int?
        get() = if (micPeak > 0) micPeak else null

    override val lastPlaybackSignalPeak: Int?
        get() = if (playbackPeak > 0) playbackPeak else null

    override val isRecording: Boolean
        get() = running

    override fun start(): File? {
        if (running) return outputWav
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            AppLog.Engine.w("DualCapture requires Android 10+")
            return null
        }
        val projection = MediaProjectionHolder.projection
        if (projection == null) {
            AppLog.Engine.w("DualCapture: MediaProjection not granted")
            return null
        }

        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuffer <= 0) return null

        val playbackConfig = buildPlaybackCaptureConfig(projection)
        val playback = try {
            AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(playbackConfig)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer * 4)
                .build()
        } catch (e: Exception) {
            AppLog.Engine.e("DualCapture playback AudioRecord failed", e)
            return null
        }

        val mic = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBuffer * 4
            )
        } catch (e: Exception) {
            AppLog.Engine.e("DualCapture mic AudioRecord failed", e)
            playback.release()
            return null
        }

        if (playback.state != AudioRecord.STATE_INITIALIZED ||
            mic.state != AudioRecord.STATE_INITIALIZED
        ) {
            playback.release()
            mic.release()
            AppLog.Engine.e("DualCapture: AudioRecord not initialized")
            return null
        }

        val wavOut = File(cacheDir, "call_dual_${System.currentTimeMillis()}.$outputExtension")
        val base = wavOut.name.substringBeforeLast('.')
        val pbFile = File(cacheDir, "${base}_pb.pcm")
        val micFile = File(cacheDir, "${base}_mic.pcm")

        return try {
            playbackPeak = 0
            micPeak = 0
            playbackBytes = 0L
            micBytes = 0L
            pbFile.writeBytes(ByteArray(0))
            micFile.writeBytes(ByteArray(0))
            running = true
            playbackRecord = playback
            micRecord = mic
            playbackPcm = pbFile
            micPcm = micFile
            outputWav = wavOut

            playback.startRecording()
            mic.startRecording()

            playbackWorker = thread(name = "dual-playback") {
                pumpPcm(playback, pbFile, trackPlayback = true) { playbackBytes = it }
            }
            micWorker = thread(name = "dual-mic") {
                pumpPcm(mic, micFile, trackPlayback = false) { micBytes = it }
            }

            lastAudioSourceLabel = "DUAL_PLAYBACK+MIC"
            RecordingPreferences.setLastAudioSource(appContext, lastAudioSourceLabel!!)
            AppLog.Engine.i("DualCaptureWavEngine started → ${wavOut.absolutePath}")
            wavOut
        } catch (e: Exception) {
            AppLog.Engine.e("DualCapture start failed", e)
            releaseAll()
            wavOut.delete()
            pbFile.delete()
            micFile.delete()
            null
        }
    }

    override fun stop(): File? {
        val wav = outputWav
        running = false
        try {
            runCatching { playbackRecord?.stop() }
            runCatching { micRecord?.stop() }
            playbackWorker?.join(2_000)
            micWorker?.join(2_000)
            playbackRecord?.release()
            micRecord?.release()
            playbackRecord = null
            micRecord = null
            playbackWorker = null
            micWorker = null

            val pb = playbackPcm
            val mc = micPcm
            AppLog.Engine.detail(
                "dual_capture_stop",
                "playbackBytes" to playbackBytes,
                "micBytes" to micBytes,
                "playbackPeak" to playbackPeak,
                "micPeak" to micPeak
            )

            if (pb == null || mc == null || wav == null) {
                wav?.delete()
                return null
            }

            val playbackSilent = playbackPeak < PLAYBACK_PEAK_MIN
            val micAudible = micPeak >= MIC_PEAK_MIN
            if (playbackSilent && micAudible) {
                AppLog.Engine.w(
                    "DualCapture: playback channel silent (Samsung/OEM may block call audio). " +
                        "pbPeak=$playbackPeak micPeak=$micPeak"
                )
                pb.delete()
                mc.delete()
                wav.delete()
                return null
            }

            if (micBytes <= 0L && playbackBytes <= 0L) {
                AppLog.Engine.w("DualCapture: no PCM data")
                wav.delete()
                return null
            }

            mergeStereoWav(pb, mc, wav, playbackBytes, micBytes)
            pb.delete()
            mc.delete()
            AppLog.Engine.i(
                "DualCapture stopped: playbackBytes=$playbackBytes micBytes=$micBytes " +
                    "pbPeak=$playbackPeak micPeak=$micPeak out=${wav.length()}"
            )
            return wav.takeIf { it.exists() && it.length() > 44 }
        } catch (e: Exception) {
            AppLog.Engine.e("DualCapture stop failed", e)
            wav?.delete()
            return null
        } finally {
            playbackPcm = null
            micPcm = null
            outputWav = null
        }
    }

    private fun buildPlaybackCaptureConfig(
        projection: android.media.projection.MediaProjection
    ): AudioPlaybackCaptureConfiguration {
        val builder = AudioPlaybackCaptureConfiguration.Builder(projection)
        val usages = intArrayOf(
            AudioAttributes.USAGE_VOICE_COMMUNICATION,
            AudioAttributes.USAGE_MEDIA,
            AudioAttributes.USAGE_NOTIFICATION,
            AudioAttributes.USAGE_ALARM,
            AudioAttributes.USAGE_ASSISTANCE_SONIFICATION,
            AudioAttributes.USAGE_GAME
        )
        for (usage in usages) {
            builder.addMatchingUsage(usage)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (pkg in PHONE_PACKAGES) {
                runCatching {
                    val uid = appContext.packageManager.getApplicationInfo(pkg, 0).uid
                    builder.addMatchingUid(uid)
                    AppLog.Engine.d("DualCapture addMatchingUid pkg=$pkg uid=$uid")
                }
            }
        }
        return builder.build()
    }

    private fun pumpPcm(
        record: AudioRecord,
        file: File,
        trackPlayback: Boolean,
        onBytes: (Long) -> Unit
    ) {
        val buffer = ByteArray(4096)
        try {
            RandomAccessFile(file, "rw").use { raf ->
                while (running) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        raf.seek(raf.length())
                        raf.write(buffer, 0, read)
                        onBytes(raf.length())
                        updatePeak(buffer, read, trackPlayback)
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.Engine.e("DualCapture pump failed", e)
        }
    }

    private fun updatePeak(buffer: ByteArray, bytes: Int, playback: Boolean) {
        var i = 0
        while (i + 1 < bytes) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xff)).toShort()
            val abs = kotlin.math.abs(sample.toInt())
            if (playback) {
                if (abs > playbackPeak) playbackPeak = abs
            } else {
                if (abs > micPeak) micPeak = abs
            }
            i += 2
        }
    }

    private fun mergeStereoWav(
        playbackFile: File,
        micFile: File,
        outWav: File,
        playbackLen: Long,
        micLen: Long
    ) {
        val pbSamples = playbackLen / 2
        val mcSamples = micLen / 2
        val samples = max(pbSamples, mcSamples)
        if (samples <= 0) return

        RandomAccessFile(outWav, "rw").use { out ->
            out.setLength(0)
            out.write(ByteArray(44))
            out.seek(44)

            RandomAccessFile(playbackFile, "r").use { pb ->
                RandomAccessFile(micFile, "r").use { mc ->
                    val pbBuf = ByteArray(2)
                    val mcBuf = ByteArray(2)
                    repeat(samples.toInt()) {
                        val pbRead = if (pb.filePointer < playbackLen) pb.read(pbBuf) else 0
                        val mcRead = if (mc.filePointer < micLen) mc.read(mcBuf) else 0
                        if (pbRead > 0) out.write(pbBuf) else out.write(byteArrayOf(0, 0))
                        if (mcRead > 0) out.write(mcBuf) else out.write(byteArrayOf(0, 0))
                    }
                }
            }
            writeWavHeader(out, samples * 4, channels = 2)
        }
    }

    private fun writeWavHeader(raf: RandomAccessFile, pcmDataSize: Long, channels: Int) {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val totalDataLen = pcmDataSize + 36
        raf.seek(0)
        raf.writeBytes("RIFF")
        raf.writeIntLE(totalDataLen.toInt())
        raf.writeBytes("WAVE")
        raf.writeBytes("fmt ")
        raf.writeIntLE(16)
        raf.writeShortLE(1)
        raf.writeShortLE(channels.toShort())
        raf.writeIntLE(sampleRate)
        raf.writeIntLE(byteRate)
        raf.writeShortLE((channels * bitsPerSample / 8).toShort())
        raf.writeShortLE(bitsPerSample.toShort())
        raf.writeBytes("data")
        raf.writeIntLE(pcmDataSize.toInt())
    }

    private fun releaseAll() {
        running = false
        runCatching { playbackRecord?.stop() }
        runCatching { micRecord?.stop() }
        runCatching { playbackRecord?.release() }
        runCatching { micRecord?.release() }
        playbackRecord = null
        micRecord = null
    }

    companion object {
        private const val PLAYBACK_PEAK_MIN = 220
        private const val MIC_PEAK_MIN = 220
        private val PHONE_PACKAGES = listOf(
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.android.dialer",
            "com.google.android.dialer"
        )
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
