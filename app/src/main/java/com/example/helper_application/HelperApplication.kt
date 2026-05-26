package com.example.helper_application

import android.app.Application
import com.example.helper_application.recording.RecordingStorage
import com.example.helper_application.setup.SetupPreferences
import com.example.helper_application.util.AppLog

class HelperApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AppLog.e("UNCAUGHT CRASH on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        AppLog.i("HelperApplication process started")
        if (SetupPreferences.isSetupComplete(this)) {
            RecordingStorage.ensureFolderExists(this)
        }
    }
}
