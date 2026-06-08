package com.example.helper_application.dialer.core.utils

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.result.ActivityResultLauncher

object DefaultDialerHelper {

    fun isDefaultDialer(context: Context): Boolean {
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return false
        return telecom.defaultDialerPackage == context.packageName
    }

    fun requestDefaultDialer(
        activity: Activity,
        roleLauncher: ActivityResultLauncher<Intent>? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) == true &&
                !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                if (roleLauncher != null) {
                    roleLauncher.launch(intent)
                } else {
                    activity.startActivity(intent)
                }
                return
            }
        }
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.packageName)
        }
        if (roleLauncher != null) {
            roleLauncher.launch(intent)
        } else {
            activity.startActivity(intent)
        }
    }
}
