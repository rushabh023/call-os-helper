package com.example.helper_application.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.example.helper_application.shizuku.ShizukuManager

/**
 * Android 9+ blocks true line-level call capture for normal apps.
 * ACR-style (Accessibility): mic + loudspeaker — use [acrAccessibilityOrder].
 */
object RecordingAudioSources {

    data class Candidate(
        val source: Int,
        val label: String
    )

    /**
     * Cube default (Android 10+): VOICE_RECOGNITION — good for starting capture, often one-sided.
     */
    val acrAccessibilityOrder: List<Candidate> = buildList {
        add(Candidate(MediaRecorder.AudioSource.VOICE_RECOGNITION, "VOICE_RECOGNITION"))
        add(Candidate(MediaRecorder.AudioSource.MIC, "MIC"))
        add(Candidate(MediaRecorder.AudioSource.VOICE_COMMUNICATION, "VOICE_COMMUNICATION"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Candidate(MediaRecorder.AudioSource.VOICE_PERFORMANCE, "VOICE_PERFORMANCE"))
        }
        @Suppress("DEPRECATION")
        add(Candidate(MediaRecorder.AudioSource.DEFAULT, "DEFAULT"))
    }

    /**
     * Cube FAQ when you hear only yourself: use voice communication + loudspeaker so the mic
     * also picks up the remote party from the speaker.
     */
    /** Loudspeaker path: MIC hears both sides from the speaker; software sources often = your voice only. */
    val acrMicSpeakerOrder: List<Candidate> = buildList {
        add(Candidate(MediaRecorder.AudioSource.MIC, "MIC"))
        add(Candidate(MediaRecorder.AudioSource.VOICE_COMMUNICATION, "VOICE_COMMUNICATION"))
        add(Candidate(MediaRecorder.AudioSource.VOICE_RECOGNITION, "VOICE_RECOGNITION"))
        @Suppress("DEPRECATION")
        add(Candidate(MediaRecorder.AudioSource.VOICE_CALL, "VOICE_CALL"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Candidate(MediaRecorder.AudioSource.VOICE_PERFORMANCE, "VOICE_PERFORMANCE"))
        }
        @Suppress("DEPRECATION")
        add(Candidate(MediaRecorder.AudioSource.DEFAULT, "DEFAULT"))
    }

    fun captureOrderFor(context: Context, shizukuBlockedProfile: Boolean = false): List<Candidate> {
        val cubeOrBlocked = shizukuBlockedProfile || RecordingPreferences.isCubeCompatibilityMode(context)
        val acr = RecordingPreferences.isAcrStyleRecording(context)
        val speakerBoost = RecordingPreferences.isSpeakerBoostEnabled(context)
        val phoneSpeakerOn = CallAudioBoost.isSpeakerphoneActive(context)
        // Shizuku access OK but shell recorder not up yet: VOICE_CALL in app UID → valid file, no audio (Android 10+).
        val shizukuPending = ShizukuManager.isPrivilegedServicePending()
        return when {
            (cubeOrBlocked || acr || shizukuPending) && speakerBoost && phoneSpeakerOn -> acrMicSpeakerOrder
            shizukuBlockedProfile || cubeOrBlocked || acr || shizukuPending -> acrAccessibilityOrder
            else -> privilegedCaptureOrder
        }
    }

    /** Full ladder including VOICE_CALL (for Shizuku / root-style engines). */
    val privilegedCaptureOrder: List<Candidate> = buildList {
        @Suppress("DEPRECATION")
        add(Candidate(MediaRecorder.AudioSource.VOICE_CALL, "VOICE_CALL"))
        @Suppress("DEPRECATION")
        add(Candidate(MediaRecorder.AudioSource.VOICE_DOWNLINK, "VOICE_DOWNLINK"))
        @Suppress("DEPRECATION")
        add(Candidate(MediaRecorder.AudioSource.VOICE_UPLINK, "VOICE_UPLINK"))
        add(Candidate(MediaRecorder.AudioSource.VOICE_COMMUNICATION, "VOICE_COMMUNICATION"))
        add(Candidate(MediaRecorder.AudioSource.VOICE_RECOGNITION, "VOICE_RECOGNITION"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Candidate(MediaRecorder.AudioSource.VOICE_PERFORMANCE, "VOICE_PERFORMANCE"))
        }
        add(Candidate(MediaRecorder.AudioSource.MIC, "MIC"))
        @Suppress("DEPRECATION")
        add(Candidate(MediaRecorder.AudioSource.DEFAULT, "DEFAULT"))
    }

    /** @deprecated Use [captureOrderFor] */
    val captureOrder: List<Candidate> = privilegedCaptureOrder
}
