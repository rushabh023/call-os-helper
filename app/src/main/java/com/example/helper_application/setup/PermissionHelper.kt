package com.example.helper_application.setup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.helper_application.util.AppLog

object PermissionHelper {

    fun requiredRuntimePermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        // Needed on many Samsung devices to create Documents/MesValidationCallRecorder folder
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return list.toTypedArray()
    }

    fun hasAllPermissions(context: Context): Boolean {
        val missing = requiredRuntimePermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) !=
                PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            AppLog.d("Missing permissions: ${missing.joinToString()}")
        }
        return missing.isEmpty()
    }
}
