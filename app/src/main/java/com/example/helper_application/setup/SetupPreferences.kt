package com.example.helper_application.setup

import android.content.Context

object SetupPreferences {

    private const val PREFS = "helper_setup_prefs"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_BATTERY_STEP_DONE = "battery_step_done"
    private const val KEY_SHOW_COMPLETE_DIALOG = "show_complete_dialog"

    fun isSetupComplete(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SETUP_COMPLETE, false)
    }

    fun setSetupComplete(context: Context, complete: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SETUP_COMPLETE, complete)
            .apply()
    }

    fun isBatteryStepDone(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_BATTERY_STEP_DONE, false)
    }

    fun setBatteryStepDone(context: Context, done: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BATTERY_STEP_DONE, done)
            .apply()
    }

    fun shouldShowCompleteDialog(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_COMPLETE_DIALOG, false)
    }

    fun setShowCompleteDialog(context: Context, show: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_COMPLETE_DIALOG, show)
            .apply()
    }
}
