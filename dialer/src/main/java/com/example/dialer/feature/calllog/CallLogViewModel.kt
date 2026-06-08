package com.example.dialer.feature.calllog

import android.content.Context
import android.provider.CallLog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

enum class CallLogFilter { ALL, MISSED, INCOMING, OUTGOING }

data class CallLogEntry(
    val name: String,
    val number: String,
    val typeLabel: String,
    val whenText: String,
    val durationSec: Long
)

data class CallLogUiState(
    val entries: List<CallLogEntry> = emptyList(),
    val loading: Boolean = false,
    val filter: CallLogFilter = CallLogFilter.ALL
)

@HiltViewModel
class CallLogViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CallLogUiState())
    val state: StateFlow<CallLogUiState> = _state.asStateFlow()
    private var query: String = ""

    fun setFilter(filter: CallLogFilter) {
        _state.value = _state.value.copy(filter = filter)
        refresh()
    }

    fun search(q: String) {
        query = q.trim()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(loading = true)
            val entries = loadEntries()
            _state.value = _state.value.copy(entries = entries, loading = false)
        }
    }

    private fun loadEntries(): List<CallLogEntry> {
        val projection = arrayOf(
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        val selection = buildSelection()
        val args = buildSelectionArgs()
        val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        return context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            args,
            "${CallLog.Calls.DATE} DESC LIMIT 200"
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
            buildList {
                while (cursor.moveToNext()) {
                    val number = cursor.getString(numberIdx).orEmpty()
                    val name = cursor.getString(nameIdx).orEmpty().ifBlank { number }
                    if (query.isNotBlank() &&
                        !name.contains(query, true) &&
                        !number.contains(query, true)
                    ) continue
                    add(
                        CallLogEntry(
                            name = name,
                            number = number,
                            typeLabel = typeLabel(cursor.getInt(typeIdx)),
                            whenText = formatter.format(Date(cursor.getLong(dateIdx))),
                            durationSec = cursor.getLong(durIdx)
                        )
                    )
                }
            }
        }.orEmpty()
    }

    private fun buildSelection(): String? {
        val parts = mutableListOf<String>()
        when (_state.value.filter) {
            CallLogFilter.MISSED -> parts += "${CallLog.Calls.TYPE}=${CallLog.Calls.MISSED_TYPE}"
            CallLogFilter.INCOMING -> parts += "${CallLog.Calls.TYPE}=${CallLog.Calls.INCOMING_TYPE}"
            CallLogFilter.OUTGOING -> parts += "${CallLog.Calls.TYPE}=${CallLog.Calls.OUTGOING_TYPE}"
            CallLogFilter.ALL -> Unit
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
    }

    private fun buildSelectionArgs(): Array<String>? = null

    private fun typeLabel(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "Incoming"
        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        CallLog.Calls.MISSED_TYPE -> "Missed"
        else -> "Call"
    }
}
