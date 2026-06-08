package com.example.helper_application.dialer.feature.calllog

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helper_application.dialer.core.data.repository.CallLogRepository
import com.example.helper_application.dialer.core.utils.DialerTabPermissions
import com.example.helper_application.dialer.core.utils.RelativeTimeFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CallLogFilter { ALL, MISSED, INCOMING, OUTGOING }

data class CallLogEntry(
    val id: Long,
    val name: String,
    val number: String,
    val type: Int,
    val typeLabel: String,
    val timestampMs: Long,
    val durationSec: Long,
    val whenText: String = "",
    val metaText: String = ""
)

data class CallLogUiState(
    val entries: List<CallLogEntry> = emptyList(),
    val loading: Boolean = false,
    val filter: CallLogFilter = CallLogFilter.ALL,
    val permissionGranted: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class CallLogViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CallLogRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CallLogUiState())
    val state: StateFlow<CallLogUiState> = _state.asStateFlow()

    private var observerRegistered = false
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    init {
        refresh()
    }

    private fun ensureObserverRegistered() {
        if (observerRegistered || !repository.canRead()) return
        context.contentResolver.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            observer
        )
        observerRegistered = true
    }

    fun setFilter(filter: CallLogFilter) {
        _state.value = _state.value.copy(filter = filter)
        refresh()
    }

    fun search(q: String) {
        _state.value = _state.value.copy(searchQuery = q.trim())
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val granted = DialerTabPermissions.hasCallLogPermission(context)
            _state.value = _state.value.copy(loading = true, permissionGranted = granted)
            if (!granted) {
                _state.value = _state.value.copy(entries = emptyList(), loading = false)
                return@launch
            }
            ensureObserverRegistered()
            val raw = repository.load(_state.value.filter, _state.value.searchQuery)
            val entries = raw.map { entry ->
                val whenText = RelativeTimeFormatter.format(context, entry.timestampMs)
                val durationLabel = if (entry.typeLabel == "Missed") {
                    ""
                } else if (entry.durationSec > 0) {
                    " · ${entry.durationSec}s"
                } else {
                    ""
                }
                entry.copy(
                    whenText = whenText,
                    metaText = "${entry.typeLabel} · $whenText$durationLabel"
                )
            }
            _state.value = _state.value.copy(entries = entries, loading = false, permissionGranted = true)
        }
    }

    override fun onCleared() {
        if (observerRegistered) {
            context.contentResolver.unregisterContentObserver(observer)
        }
        super.onCleared()
    }
}
