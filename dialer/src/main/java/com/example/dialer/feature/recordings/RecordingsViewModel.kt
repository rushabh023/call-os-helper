package com.example.dialer.feature.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dialer.core.data.local.entity.CallRecordingEntity
import com.example.dialer.core.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val repository: RecordingRepository
) : ViewModel() {

    val recordings = repository.observeRecordings().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun delete(entity: CallRecordingEntity) {
        viewModelScope.launch {
            java.io.File(entity.filePath).delete()
            repository.delete(entity)
        }
    }
}
