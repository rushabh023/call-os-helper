package com.example.helper_application.recording

import java.io.File
import kotlin.math.max

object RecordingQualityEvaluator {

    data class Result(
        val likelySilent: Boolean,
        val reason: String
    )

    fun evaluate(
        file: File?,
        engineId: String?,
        durationMs: Long,
        signalPeak: Int?
    ): Result {
        if (file == null || !file.exists()) {
            return Result(likelySilent = true, reason = "missing_file")
        }
        val bytes = file.length()
        val seconds = max(1L, durationMs / 1000L)
        val bytesPerSecond = bytes / seconds

        if (bytes < 1024L) return Result(true, "too_small")

        return when {
            engineId == "audio_record_8k_voice" -> {
                val peak = signalPeak ?: 0
                if (peak < 220) {
                    Result(true, "pcm_peak_low:$peak")
                } else {
                    Result(false, "pcm_peak_ok:$peak")
                }
            }
            engineId == "audio_record_mic_wav" -> {
                val peak = signalPeak ?: 0
                when {
                    peak < 220 -> Result(true, "wav_peak_low:$peak")
                    bytesPerSecond < 2_000L -> Result(true, "wav_bps_low:$bytesPerSecond")
                    else -> Result(false, "wav_ok:peak=$peak,bps=$bytesPerSecond")
                }
            }
            engineId == "media_recorder_amr" -> {
                if (bytesPerSecond < 300L) {
                    Result(true, "amr_bps_low:$bytesPerSecond")
                } else {
                    Result(false, "amr_bps_ok:$bytesPerSecond")
                }
            }
            engineId == "dual_playback_mic_wav" -> {
                val micPeak = signalPeak ?: 0
                if (bytesPerSecond < 2_000L) {
                    Result(true, "dual_bps_low:$bytesPerSecond")
                } else if (micPeak < 220) {
                    Result(true, "dual_mic_low:$micPeak")
                } else {
                    Result(false, "dual_ok:mic=$micPeak")
                }
            }
            engineId == "media_recorder_m4a" -> {
                val peak = signalPeak ?: 0
                when {
                    peak <= 0 -> Result(true, "m4a_peak_zero:$peak")
                    peak in 1..350 -> Result(true, "m4a_peak_low:$peak")
                    bytesPerSecond < 1200L -> Result(true, "m4a_bps_low:$bytesPerSecond")
                    else -> Result(false, "m4a_ok:peak=$peak,bps=$bytesPerSecond")
                }
            }
            else -> {
                if (bytesPerSecond < 1200L) {
                    Result(true, "bps_low:$bytesPerSecond")
                } else {
                    Result(false, "bps_ok:$bytesPerSecond")
                }
            }
        }
    }
}

