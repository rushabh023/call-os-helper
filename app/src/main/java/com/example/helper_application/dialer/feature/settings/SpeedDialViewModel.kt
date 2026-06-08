package com.example.helper_application.dialer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helper_application.dialer.core.data.local.dao.SpeedDialDao
import com.example.helper_application.dialer.core.data.local.entity.SpeedDialEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpeedDialSlotUi(
    val slot: Int,
    val displayName: String?,
    val phoneNumber: String?
)

@HiltViewModel
class SpeedDialViewModel @Inject constructor(
    private val speedDialDao: SpeedDialDao
) : ViewModel() {

    val slots = speedDialDao.observeAll()
        .map { entries ->
            val bySlot = entries.associateBy { it.slot }
            (2..9).map { slot ->
                val entry = bySlot[slot]
                SpeedDialSlotUi(slot, entry?.displayName, entry?.phoneNumber)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(slot: Int, name: String, number: String) {
        viewModelScope.launch {
            speedDialDao.upsert(
                SpeedDialEntryEntity(
                    slot = slot,
                    contactId = null,
                    displayName = name.ifBlank { number },
                    phoneNumber = number
                )
            )
        }
    }

    fun clear(slot: Int) {
        viewModelScope.launch { speedDialDao.deleteSlot(slot) }
    }
}
