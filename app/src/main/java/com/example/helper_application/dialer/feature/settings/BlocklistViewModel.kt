package com.example.helper_application.dialer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helper_application.dialer.core.data.local.entity.BlockPattern
import com.example.helper_application.dialer.core.data.local.entity.BlockedNumberEntity
import com.example.helper_application.dialer.core.data.repository.BlocklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlocklistViewModel @Inject constructor(
    private val repository: BlocklistRepository
) : ViewModel() {

    val blocked = repository.observeBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(number: String, label: String?) {
        if (number.isBlank()) return
        viewModelScope.launch {
            repository.add(number = number, pattern = BlockPattern.EXACT, label = label)
        }
    }

    fun remove(entry: BlockedNumberEntity) {
        viewModelScope.launch { repository.remove(entry.id) }
    }
}
