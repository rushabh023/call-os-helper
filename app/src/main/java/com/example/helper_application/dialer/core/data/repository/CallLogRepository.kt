package com.example.helper_application.dialer.core.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import com.example.helper_application.dialer.core.utils.DialerTabPermissions
import com.example.helper_application.dialer.feature.calllog.CallLogEntry
import com.example.helper_application.dialer.feature.calllog.CallLogFilter
import com.example.helper_application.util.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallLogRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun canRead(): Boolean = DialerTabPermissions.hasCallLogPermission(context)

    fun load(
        filter: CallLogFilter,
        query: String,
        limit: Int = 200
    ): List<CallLogEntry> {
        if (!canRead()) return emptyList()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        return try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                buildSelection(filter),
                null,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                parseCursor(cursor, query).take(limit)
            }.orEmpty()
        } catch (e: SecurityException) {
            AppLog.w("Call log read denied: ${e.message}")
            emptyList()
        }
    }

    fun logCompletedCall(
        number: String,
        displayName: String?,
        type: Int,
        startedAtMs: Long,
        durationSec: Long
    ) {
        if (!DialerTabPermissions.hasWriteCallLogPermission(context)) {
            AppLog.w("Call log write skipped: WRITE_CALL_LOG not granted")
            return
        }
        if (number.isBlank()) return
        val resolvedName = displayName?.takeIf { it.isNotBlank() && it != number }
            ?: lookupContactName(number)
        val values = ContentValues().apply {
            put(CallLog.Calls.NUMBER, number)
            put(CallLog.Calls.CACHED_NAME, resolvedName)
            put(CallLog.Calls.TYPE, type)
            put(CallLog.Calls.DATE, startedAtMs)
            put(CallLog.Calls.DURATION, durationSec.coerceAtLeast(0))
            put(CallLog.Calls.NEW, 1)
        }
        try {
            context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
            AppLog.i("Call logged: type=$type number=$number duration=${durationSec}s")
        } catch (e: SecurityException) {
            AppLog.w("Call log write denied: ${e.message}")
        }
    }

    private fun parseCursor(cursor: Cursor, query: String): List<CallLogEntry> {
        val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
        val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
        val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
        val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
        val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
        val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
        val seen = LinkedHashSet<String>()
        return buildList {
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIdx).orEmpty()
                if (number.isBlank()) continue
                val name = cursor.getString(nameIdx).orEmpty().ifBlank { number }
                if (query.isNotBlank() &&
                    !name.contains(query, true) &&
                    !number.contains(query, true)
                ) continue
                val id = cursor.getLong(idIdx)
                val dedupeKey = "$id"
                if (!seen.add(dedupeKey)) continue
                add(
                    CallLogEntry(
                        id = id,
                        name = name,
                        number = number,
                        type = cursor.getInt(typeIdx),
                        typeLabel = typeLabel(cursor.getInt(typeIdx)),
                        timestampMs = cursor.getLong(dateIdx),
                        durationSec = cursor.getLong(durIdx)
                    )
                )
            }
        }
    }

    private fun buildSelection(filter: CallLogFilter): String? = when (filter) {
        CallLogFilter.MISSED -> "${CallLog.Calls.TYPE}=${CallLog.Calls.MISSED_TYPE}"
        CallLogFilter.INCOMING -> "${CallLog.Calls.TYPE}=${CallLog.Calls.INCOMING_TYPE}"
        CallLogFilter.OUTGOING -> "${CallLog.Calls.TYPE}=${CallLog.Calls.OUTGOING_TYPE}"
        CallLogFilter.ALL -> null
    }

    private fun typeLabel(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "Incoming"
        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        CallLog.Calls.MISSED_TYPE -> "Missed"
        CallLog.Calls.REJECTED_TYPE -> "Rejected"
        CallLog.Calls.BLOCKED_TYPE -> "Blocked"
        else -> "Call"
    }

    private fun lookupContactName(number: String): String? {
        if (!DialerTabPermissions.hasContactsPermission(context)) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
