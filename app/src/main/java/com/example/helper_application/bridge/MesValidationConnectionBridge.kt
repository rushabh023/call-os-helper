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
            AppLog.Bridge.w("Mes Validation launch intent not found")
        }
    }

    fun startRecording(context: Context, direction: CallDirection, phoneNumber: String?) {
        if (!isMesValidationInstalled(context)) {
            AppLog.Bridge.e("Mes Validation not installed — cannot START_RECORD")
            return
        }
        AppLog.Bridge.detail(
            "start_record",
            "direction" to direction.name,
            "number" to (phoneNumber ?: "hidden"),
            "action" to AppConstants.ACTION_START_RECORD,
            "targetPackage" to AppConstants.MES_VALIDATION_PACKAGE
        )
        val intent = Intent(AppConstants.ACTION_START_RECORD).apply {
            setPackage(AppConstants.MES_VALIDATION_PACKAGE)
            putExtra(AppConstants.EXTRA_DIRECTION, direction.name)
            putExtra(AppConstants.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        try {
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            AppLog.Bridge.e("sendBroadcast START_RECORD failed", e)
        }
    }

    fun notifyRecordingComplete(
        context: Context,
        fileName: String,
        absolutePath: String?
    ) {
        if (!isMesValidationInstalled(context)) return
        AppLog.Bridge.detail(
            "record_complete",
            "fileName" to fileName,
            "path" to (absolutePath ?: "unknown"),
            "action" to AppConstants.ACTION_RECORD_COMPLETE
        )
        val intent = Intent(AppConstants.ACTION_RECORD_COMPLETE).apply {
            setPackage(AppConstants.MES_VALIDATION_PACKAGE)
            putExtra(AppConstants.EXTRA_FILE_NAME, fileName)
            absolutePath?.let { putExtra(AppConstants.EXTRA_FILE_PATH, it) }
        }
        try {
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            AppLog.Bridge.e("sendBroadcast RECORD_COMPLETE failed", e)
        }
    }

    fun stopRecording(context: Context) {
        if (!isMesValidationInstalled(context)) {
            AppLog.Bridge.w("Mes Validation not installed — cannot STOP_RECORD")
            return
        }
        AppLog.Bridge.detail(
            "stop_record",
            "action" to AppConstants.ACTION_STOP_RECORD,
            "targetPackage" to AppConstants.MES_VALIDATION_PACKAGE
        )
        val intent = Intent(AppConstants.ACTION_STOP_RECORD).apply {
            setPackage(AppConstants.MES_VALIDATION_PACKAGE)
        }
        try {
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            AppLog.Bridge.e("sendBroadcast STOP_RECORD failed", e)
        }
    }
}
