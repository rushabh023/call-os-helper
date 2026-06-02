package com.example.helper_application.shizuku

import android.util.Log
import com.example.helper_application.util.AppLog
import rikka.shizuku.ShizukuProvider

/**
 * Lightweight [ShizukuProvider] — heavy work belongs in [com.example.helper_application.HelperApplication],
 * not provider onCreate (see Shizuku issue #451 / #1171 on OEMs).
 */
class HelperShizukuProvider : ShizukuProvider() {

    override fun onCreate(): Boolean {
        Log.d(AppLog.TAG, "[Shizuku] HelperShizukuProvider onCreate pid=${android.os.Process.myPid()}")
        return super.onCreate()
    }
}
