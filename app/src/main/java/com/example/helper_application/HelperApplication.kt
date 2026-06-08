package com.example.helper_application

import android.app.Application
import com.example.helper_application.recording.RecordingStorage
import com.example.helper_application.setup.SetupPreferences
import com.example.helper_application.shizuku.ShizukuManager
import com.example.helper_application.shizuku.ShizukuProcess
import com.example.helper_application.util.AppLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HelperApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val process = ShizukuProcess.processName(this) ?: "unknown"
        if (ShizukuProcess.isUserServiceProcess(this)) {
            AppLog.i("HelperApplication shizuku user-service process: $process pid=${android.os.Process.myPid()}")
            return
        }
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLog.e("UNCAUGHT CRASH on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        AppLog.i("HelperApplication process started ($process)")
        AppLog.logDeviceSnapshot(this)
        runCatching { ShizukuManager.init(this) }
            .onFailure { e ->
                AppLog.e("ShizukuManager.init failed — app continues without Shizuku", e)
            }
        AppLog.logRecordingCapabilities(this)
        if (SetupPreferences.isSetupComplete(this)) {
            RecordingStorage.ensureFolderExists(this)
        }
    }
}
