package com.example.helper_application.recording

import java.io.File

interface RecorderEngine {
    val id: String
    val outputExtension: String
    /** Which [MediaRecorder.AudioSource] succeeded on last start, if any. */
    val lastAudioSourceLabel: String?
    /** Optional signal peak metric (PCM engines / mic side of dual capture). */
    val lastSignalPeak: Int?
        get() = null
    /** Playback-side peak for [DualCaptureWavEngine] only. */
    val lastPlaybackSignalPeak: Int?
        get() = null
    val isRecording: Boolean

    fun start(): File?
    fun stop(): File?
}
