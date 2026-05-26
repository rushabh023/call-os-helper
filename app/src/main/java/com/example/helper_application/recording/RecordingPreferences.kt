package com.example.helper_application.recording

import android.content.Context

object RecordingPreferences {

    private const val PREFS = "call_recording_prefs"
    private const val KEY_AUTO_RECORD = "auto_record_enabled"
    private const val KEY_LAST_ERROR = "last_error"
    private const val KEY_RECENT_FILES = "recent_files"
    private const val KEY_APP_CONNECTOR = "app_connector_enabled"
    private const val KEY_UNSENT_COUNT = "unsent_count"
    private const val KEY_FOLDER_READY = "folder_ready"

    fun isAutoRecordEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_RECORD, false)
    }

    fun setAutoRecordEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_RECORD, enabled)
            .apply()
    }

    fun getLastError(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_ERROR, null)
    }

    fun setLastError(context: Context, message: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_ERROR, message)
            .apply()
    }

    fun addSavedRecording(context: Context, fileName: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_RECENT_FILES, "") ?: ""
        val list = if (current.isEmpty()) mutableListOf() else current.split("\n").toMutableList()
        list.add(0, fileName)
        while (list.size > 20) list.removeAt(list.lastIndex)
        prefs.edit().putString(KEY_RECENT_FILES, list.joinToString("\n")).apply()
    }

    fun getRecentRecordings(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_RECENT_FILES, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("\n")
    }

    fun setAppConnectorEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_APP_CONNECTOR, enabled)
            .apply()
    }

    fun isAppConnectorEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_APP_CONNECTOR, false)
    }

    fun getUnsentRecordCount(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_UNSENT_COUNT, 0)
    }

    fun setUnsentRecordCount(context: Context, count: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_UNSENT_COUNT, count.coerceAtLeast(0))
            .apply()
    }

    fun isFolderReady(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_FOLDER_READY, false)
    }

    fun setFolderReady(context: Context, ready: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FOLDER_READY, ready)
            .apply()
    }
}
