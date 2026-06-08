package com.example.helper_application.dialer.core.utils

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.example.helper_application.R
import com.example.helper_application.dialer.feature.incall.CallSessionController
import com.example.helper_application.util.AppLog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialerCallPlacer {

    private var lastPlacedAtMs = 0L
    private var lastPlacedNumber = ""
    @Volatile
    private var placingInFlight = false

    fun placeCall(context: Context, number: String) {
        val sanitized = PhoneNumberFormatter.sanitizeForDial(number)
        if (sanitized.isBlank()) return

        val now = System.currentTimeMillis()
        if (placingInFlight || CallSessionController.hasActiveCall()) {
            AppLog.w("placeCall ignored: call already active or placing")
            return
        }
        if (sanitized == lastPlacedNumber && now - lastPlacedAtMs < 2_000L) {
            AppLog.w("placeCall ignored: duplicate tap for $sanitized")
            return
        }
        lastPlacedNumber = sanitized
        lastPlacedAtMs = now
        placingInFlight = true

        val telecom = context.getSystemService(TelecomManager::class.java) ?: return
        val handles = telecom.callCapablePhoneAccounts
        if (handles.isEmpty()) {
            AppLog.w("placeCall failed: no call-capable phone accounts")
            placingInFlight = false
            return
        }
        if (handles.size > 1) {
            showSimChooser(context, telecom, handles, sanitized)
        } else {
            dial(telecom, handles.first(), sanitized)
        }
    }

    private fun showSimChooser(
        context: Context,
        telecom: TelecomManager,
        handles: List<PhoneAccountHandle>,
        number: String
    ) {
        val labels = handles.mapIndexed { index, _ -> "SIM ${index + 1}" }.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.choose_sim)
            .setItems(labels) { _, which ->
                dial(telecom, handles[which], number)
            }
            .show()
    }

    private fun dial(telecom: TelecomManager, handle: PhoneAccountHandle, number: String) {
        val uri = Uri.fromParts("tel", number, null)
        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
        }
        AppLog.i("placeCall -> $number via ${handle.id}")
        try {
            telecom.placeCall(uri, extras)
        } catch (e: SecurityException) {
            AppLog.w("placeCall failed: ${e.message}")
            placingInFlight = false
        }
    }

    internal fun clearPlacingInFlight() {
        placingInFlight = false
    }
}
