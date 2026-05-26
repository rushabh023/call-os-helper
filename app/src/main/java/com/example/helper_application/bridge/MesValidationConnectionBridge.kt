package com.example.helper_application.bridge

import android.content.Context
import android.content.Intent
import com.example.helper_application.AppConstants
import com.example.helper_application.recording.CallDirection
import com.example.helper_application.util.AppLog

/**
 * Helper app (DEF) → Mes Validation (com.mesvalidation) communication.
 */
object MesValidationConnectionBridge {

    fun isMesValidationInstalled(context: Context): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(AppConstants.MES_VALIDATION_PACKAGE, 0)
            true
        }.getOrDefault(false)
    }

    fun getMesValidationVersion(context: Context): String? {
        return runCatching {
            context.packageManager.getPackageInfo(AppConstants.MES_VALIDATION_PACKAGE, 0).versionName
        }.getOrNull()
    }

    fun openMesValidationApp(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(AppConstants.MES_VALIDATION_PACKAGE)
        if (launch != null) {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            AppLog.w("Mes Validation launch intent not found")
        }
    }

    fun startRecording(context: Context, direction: CallDirection, phoneNumber: String?) {
        if (!isMesValidationInstalled(context)) {
            AppLog.e("Mes Validation not installed — cannot START_RECORD")
            return
        }
        AppLog.i("Helper → Mes Validation: START_RECORD ${direction.name} number=$phoneNumber")
        val intent = Intent(AppConstants.ACTION_START_RECORD).apply {
            setPackage(AppConstants.MES_VALIDATION_PACKAGE)
            putExtra(AppConstants.EXTRA_DIRECTION, direction.name)
            putExtra(AppConstants.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        try {
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            AppLog.e("sendBroadcast START_RECORD failed", e)
        }
    }

    fun stopRecording(context: Context) {
        if (!isMesValidationInstalled(context)) {
            AppLog.w("Mes Validation not installed — cannot STOP_RECORD")
            return
        }
        AppLog.i("Helper → Mes Validation: STOP_RECORD")
        val intent = Intent(AppConstants.ACTION_STOP_RECORD).apply {
            setPackage(AppConstants.MES_VALIDATION_PACKAGE)
        }
        try {
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            AppLog.e("sendBroadcast STOP_RECORD failed", e)
        }
    }
}
