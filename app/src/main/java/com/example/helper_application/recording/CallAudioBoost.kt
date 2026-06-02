package com.example.helper_application.recording

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.example.helper_application.util.AppLog

/**
 * Routes call audio through the loudspeaker so the mic can pick up both sides (Cube/ACR path).
 */
object CallAudioBoost {

    private data class SavedRoute(
        val mode: Int,
        val speakerOn: Boolean
    )

    @Volatile
    private var savedRoute: SavedRoute? = null

    private var focusRequest: AudioFocusRequest? = null

    fun isSpeakerphoneActive(context: Context): Boolean {
        val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
            ?: return false
        @Suppress("DEPRECATION")
        return audioManager.isSpeakerphoneOn
    }

    /** Apply before MediaRecorder.start. [force] re-applies speaker even if already boosted. */
    fun applyForCall(context: Context, force: Boolean = false) {
        val app = context.applicationContext
        val audioManager = app.getSystemService(AudioManager::class.java) ?: return

        if (savedRoute != null && !force) return

        if (force && savedRoute != null) {
            abandonAudioFocus(audioManager)
        }

        if (savedRoute == null) {
            @Suppress("DEPRECATION")
            savedRoute = SavedRoute(
                mode = audioManager.mode,
                speakerOn = audioManager.isSpeakerphoneOn
            )
        }

        @Suppress("DEPRECATION")
        runCatching {
            audioManager.isMicrophoneMute = false
        }
        val routeSnapshot = CallRouteDiagnostics.snapshot(app)
        AppLog.Engine.detail(
            "audio_route_before",
            "force" to force,
            "mode" to audioManager.mode,
            "speakerOn" to speakerOn(audioManager),
            "micMute" to micMute(audioManager),
            "routeSummary" to routeSnapshot.summary,
            "wiredHeadset" to routeSnapshot.wiredHeadset,
            "bluetoothHeadset" to routeSnapshot.bluetoothHeadset
        )

        if (!routeSnapshot.blocksLikelyOtherSide) {
            // Cube FAQ: in-communication mode — only when not on headset.
            @Suppress("DEPRECATION")
            runCatching {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            }
        }

        if (routeSnapshot.blocksLikelyOtherSide) {
            AppLog.Engine.w("CallAudioBoost skipped: headset connected")
        } else if (RecordingPreferences.isSpeakerBoostEnabled(app)) {
            steerAwayFromBluetoothSco(audioManager)
            routeToSpeaker(audioManager)
            @Suppress("DEPRECATION")
            runCatching { audioManager.isSpeakerphoneOn = true }
            boostVoiceCallVolume(audioManager)
            AppLog.Engine.i("ACR-style: speakerphone ON (force=$force)")
        }

        // Do not take audio focus during a live call — it can mute call audio for the user.
        AppLog.Engine.detail(
            "audio_route_after",
            "force" to force,
            "mode" to audioManager.mode,
            "speakerOn" to speakerOn(audioManager),
            "micMute" to micMute(audioManager)
        )
    }

    private fun steerAwayFromBluetoothSco(audioManager: AudioManager) {
        @Suppress("DEPRECATION")
        runCatching {
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                AppLog.Engine.d("Stopped Bluetooth SCO for call recording route")
            }
        }
    }

    private fun boostVoiceCallVolume(audioManager: AudioManager) {
        @Suppress("DEPRECATION")
        runCatching {
            val stream = AudioManager.STREAM_VOICE_CALL
            val max = audioManager.getStreamMaxVolume(stream)
            val current = audioManager.getStreamVolume(stream)
            if (current < max) {
                audioManager.setStreamVolume(stream, max, 0)
                AppLog.Engine.d("Raised STREAM_VOICE_CALL volume $current -> $max")
            }
        }
    }

    private fun routeToSpeaker(audioManager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val speaker = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speaker != null) {
                runCatching {
                    audioManager.setCommunicationDevice(speaker)
                    AppLog.Engine.d("setCommunicationDevice=BUILTIN_SPEAKER")
                }.onFailure { e ->
                    AppLog.Engine.w("setCommunicationDevice failed: ${e.message}")
                }
            }
        }
    }

    fun release(context: Context) {
        val app = context.applicationContext
        val audioManager = app.getSystemService(AudioManager::class.java) ?: return
        val saved = savedRoute ?: return
        savedRoute = null

        abandonAudioFocus(audioManager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { audioManager.clearCommunicationDevice() }
        }

        @Suppress("DEPRECATION")
        runCatching {
            audioManager.isSpeakerphoneOn = saved.speakerOn
            audioManager.mode = saved.mode
        }.onFailure { e ->
            AppLog.Engine.w("CallAudioBoost restore failed: ${e.message}")
        }
        AppLog.Engine.detail(
            "audio_route_restored",
            "mode" to audioManager.mode,
            "speakerOn" to speakerOn(audioManager),
            "micMute" to micMute(audioManager)
        )
        AppLog.Engine.i("ACR-style: audio route restored")
    }

    private fun requestAudioFocus(audioManager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .build()
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun abandonAudioFocus(audioManager: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun speakerOn(audioManager: AudioManager): Boolean {
        @Suppress("DEPRECATION")
        return audioManager.isSpeakerphoneOn
    }

    private fun micMute(audioManager: AudioManager): Boolean {
        @Suppress("DEPRECATION")
        return audioManager.isMicrophoneMute
    }
}
