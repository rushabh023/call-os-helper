package com.example.helper_application.recording

import android.media.MediaRecorder
import android.os.Build

/**
 * Cube ACR audio source picker options (phone + VoIP).
 * "(software)" = prefer [com.example.helper_application.accessibility.AppConnectorHolder] context.
 */
object CubeAudioSourceOptions {

    data class Option(
        val id: String,
        val label: String,
        val source: Int,
        val preferSoftwareContext: Boolean
    )

    val allOptions: List<Option> = buildList {
        add(Option("VOICE_COMMUNICATION", "Voice communication", MediaRecorder.AudioSource.VOICE_COMMUNICATION, false))
        add(Option("VOICE_COMMUNICATION_SOFTWARE", "Voice communication (software)", MediaRecorder.AudioSource.VOICE_COMMUNICATION, true))
        @Suppress("DEPRECATION")
        add(Option("VOICE_CALL", "Voice call", MediaRecorder.AudioSource.VOICE_CALL, false))
        @Suppress("DEPRECATION")
        add(Option("VOICE_CALL_SOFTWARE", "Voice call (software)", MediaRecorder.AudioSource.VOICE_CALL, true))
        add(Option("VOICE_RECOGNITION", "Voice recognition", MediaRecorder.AudioSource.VOICE_RECOGNITION, false))
        add(Option("VOICE_RECOGNITION_SOFTWARE", "Voice recognition (software)", MediaRecorder.AudioSource.VOICE_RECOGNITION, true))
        add(Option("MIC", "Microphone", MediaRecorder.AudioSource.MIC, false))
    }

    const val DEFAULT_PHONE_ID = "VOICE_RECOGNITION_SOFTWARE"
    const val DEFAULT_VOIP_ID = "VOICE_RECOGNITION_SOFTWARE"

    fun findById(id: String?): Option? = allOptions.firstOrNull { it.id == id }

    fun labelForId(id: String?): String = findById(id)?.label ?: id ?: "Auto"

    fun toCandidates(option: Option): RecordingAudioSources.Candidate =
        RecordingAudioSources.Candidate(option.source, option.id.substringBefore("_SOFTWARE"))

    fun orderedCandidates(preferredId: String?, fallbackOrder: List<RecordingAudioSources.Candidate>): List<RecordingAudioSources.Candidate> {
        val preferred = findById(preferredId)?.let { toCandidates(it) }
        if (preferred == null) return fallbackOrder
        val rest = fallbackOrder.filter { it.source != preferred.source || it.label != preferred.label }
        return listOf(preferred) + rest
    }

    fun preferSoftwareContext(preferredId: String?): Boolean =
        findById(preferredId)?.preferSoftwareContext == true

    fun supportsVoipPerformance(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}
