package com.example.dialer.feature.dialpad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dialer.core.data.local.dao.SpeedDialDao
import com.example.dialer.core.domain.model.ContactMatch
import com.example.dialer.core.domain.usecase.ContactSearchUseCase
import com.example.dialer.core.utils.PhoneNumberFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DialpadUiState(
    val digits: String = "",
    val matches: List<ContactMatch> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class DialpadViewModel @Inject constructor(
    private val contactSearchUseCase: ContactSearchUseCase,
    private val speedDialDao: SpeedDialDao
) : ViewModel() {

    private val _state = MutableStateFlow(DialpadUiState())
    val state: StateFlow<DialpadUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun appendDigit(digit: Char) {
        updateDigits(_state.value.digits + digit)
    }

    fun backspace() {
        val current = _state.value.digits
        if (current.isNotEmpty()) updateDigits(current.dropLast(1))
    }

    fun clearAll() = updateDigits("")

    fun setDigits(raw: String) = updateDigits(PhoneNumberFormatter.sanitizeForDial(raw))

    private fun updateDigits(digits: String) {
        _state.value = _state.value.copy(digits = digits)
        searchJob?.cancel()
        if (digits.isBlank()) {
            _state.value = _state.value.copy(matches = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)
            delay(120)
            val results = contactSearchUseCase.search(digits)
            _state.value = _state.value.copy(matches = results, isSearching = false)
        }
    }

    suspend fun speedDialFor(slot: Int): String? =
        speedDialDao.getBySlot(slot)?.phoneNumber
}
