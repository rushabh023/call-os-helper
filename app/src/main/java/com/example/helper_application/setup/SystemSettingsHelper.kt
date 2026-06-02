package com.example.helper_application.setup

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.helper_application.accessibility.AppConnectorService
import com.example.helper_application.util.AppLog

object SystemSettingsHelper {

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openAppSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun openBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
                .onFailure { openAppSettings(context) }
        } else {
            openAppSettings(context)
        }
    }

    fun openRestrictedSettings(context: Context) {
        openAppSettings(context)
    }

    fun isAppConnectorEnabled(context: Context): Boolean {
        val component = ComponentName(context, AppConnectorService::class.java)
        val flat = component.flattenToString()
        val shortFlat = "${context.packageName}/${AppConnectorService::class.java.simpleName}"

        val fromSecure = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        if (fromSecure.isNotEmpty()) {
            val secureHit = fromSecure.split(':').any { entry ->
                entry.equals(flat, ignoreCase = true) ||
                    entry.equals(shortFlat, ignoreCase = true) ||
                    (entry.contains(context.packageName, ignoreCase = true) &&
                        entry.contains("AppConnectorService", ignoreCase = true))
            }
            if (secureHit) {
                AppLog.d("isAppConnectorEnabled=true (Settings.Secure)")
                return true
            }
        }

        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val fullName = AppConnectorService::class.java.name
        val relativeName = ".accessibility.AppConnectorService"
        val on = enabled.any { service ->
            val si = service.resolveInfo.serviceInfo
            si.packageName == context.packageName &&
                (si.name == fullName ||
                    si.name == relativeName ||
                    si.name.endsWith("AppConnectorService"))
        }
        AppLog.d("isAppConnectorEnabled=$on (AccessibilityManager, enabledCount=${enabled.size})")
        return on
    }

    fun isMainRecorderInstalled(context: Context, packageName: String): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)
    }

    fun getMainAppVersion(context: Context, packageName: String): String? {
        return runCatching {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            info.versionName
        }.getOrNull()
    }

    fun openMainRecorderApp(context: Context, packageName: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openEmailSupport(context: Context, email: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
