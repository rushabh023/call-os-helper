package com.example.helper_application.recording

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/**
 * Detects wired/BT headsets during calls. Cube ACR FAQ: headset routing often
 * captures only your mic, not the remote party (other side plays in earphones only).
 */
data class CallRouteSnapshot(
    val wiredHeadset: Boolean,
    val bluetoothHeadset: Boolean,
    val communicationDeviceType: Int?,
    val summary: String
) {
    val blocksLikelyOtherSide: Boolean
        get() = wiredHeadset || bluetoothHeadset
}

object CallRouteDiagnostics {

    private val headsetOutputTypes = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO
    )

    fun snapshot(context: Context): CallRouteSnapshot {
        val audioManager = context.applicationContext
            .getSystemService(AudioManager::class.java)
            ?: return CallRouteSnapshot(false, false, null, "unknown")

        var wired = false
        var bluetooth = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (device in audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE -> wired = true
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> bluetooth = true
                }
            }
        }

        @Suppress("DEPRECATION")
        if (!wired) wired = audioManager.isWiredHeadsetOn
        @Suppress("DEPRECATION")
        if (!bluetooth) {
            bluetooth = audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn
        }

        var communicationType: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            communicationType = audioManager.communicationDevice?.type
            when (communicationType) {
                in headsetOutputTypes -> {
                    if (communicationType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        communicationType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    ) {
                        bluetooth = true
                    } else {
                        wired = true
                    }
                }
            }
        }

        val summary = when {
            wired && bluetooth -> "wired+bluetooth_headset"
            wired -> "wired_headset"
            bluetooth -> "bluetooth_headset"
            else -> "phone_speaker_or_earpiece"
        }

        return CallRouteSnapshot(
            wiredHeadset = wired,
            bluetoothHeadset = bluetooth,
            communicationDeviceType = communicationType,
            summary = summary
        )
    }
}
