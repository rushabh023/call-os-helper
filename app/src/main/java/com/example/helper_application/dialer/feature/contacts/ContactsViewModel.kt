package com.example.helper_application.dialer.feature.contacts

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helper_application.dialer.core.data.repository.ContactsRepository
import com.example.helper_application.dialer.core.utils.DialerTabPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactRow(
    val id: Long,
    val name: String,
    val number: String,
    val displayNumber: String,
    val photoUri: String? = null
)

data class ContactsUiState(
    val contacts: List<ContactRow> = emptyList(),
    val loading: Boolean = false,
    val permissionGranted: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ContactsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ContactsUiState())
    val state: StateFlow<ContactsUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    private var observerRegistered = false
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            reload(_state.value.searchQuery)
        }
    }

    init {
        reload("")
    }

    private fun ensureObserverRegistered() {
        if (observerRegistered || !repository.canRead()) return
        context.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            observer
        )
        observerRegistered = true
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(150)
            reload(query)
        }
    }

    fun refresh() {
        reload(_state.value.searchQuery)
    }

    private fun reload(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val granted = DialerTabPermissions.hasContactsPermission(context)
            _state.value = _state.value.copy(loading = true, permissionGranted = granted, searchQuery = query)
            if (!granted) {
                _state.value = _state.value.copy(contacts = emptyList(), loading = false)
                return@launch
            }
            ensureObserverRegistered()
            val contacts = repository.load(query)
            _state.value = _state.value.copy(
                contacts = contacts,
                loading = false,
                permissionGranted = true
            )
        }
    }

    override fun onCleared() {
        if (observerRegistered) {
            context.contentResolver.unregisterContentObserver(observer)
        }
        super.onCleared()
    }
}
