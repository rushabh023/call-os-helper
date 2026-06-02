package com.example.helper_application.shizuku

import android.app.Application
import android.content.Context
import android.os.Build

object ShizukuProcess {

    const val USER_SERVICE_SUFFIX = "service"

    fun isUserServiceProcess(context: Context): Boolean {
        val name = processName(context) ?: return false
        return name.contains(USER_SERVICE_SUFFIX)
    }

    fun processName(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        return context.applicationInfo.processName
    }
}
