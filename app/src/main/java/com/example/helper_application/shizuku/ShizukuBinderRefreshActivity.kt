package com.example.helper_application.shizuku

import android.app.Activity
import android.os.Bundle
import com.example.helper_application.util.AppLog

/**
 * Invisible activity used after the user re-enables Helper in Shizuku Manager.
 * Shizuku often re-sends the binder when an Activity starts (see Shizuku-API README).
 */
class ShizukuBinderRefreshActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.Shizuku.i("ShizukuBinderRefreshActivity — waking binder delivery after Shizuku toggle")
        ShizukuManager.resumeSetupAfterShizukuToggle(this)
        finish()
    }
}
