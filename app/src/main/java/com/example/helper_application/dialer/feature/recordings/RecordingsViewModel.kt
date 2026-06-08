package com.example.helper_application.dialer.feature.recordings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helper_application.dialer.core.data.local.entity.CallRecordingEntity
import com.example.helper_application.dialer.core.data.repository.RecordingRepository
import com.example.helper_application.recording.RecordingStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RecordingRepository
) : ViewModel() {

    private val legacyScan = MutableStateFlow<List<RecordingListItem>>(emptyList())

    val recordings = combine(repository.observeRecordings(), legacyScan) { room, legacy ->
        LegacyRecordingCatalog.mergeWithRoom(room, legacy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folderPath = RecordingStorage.getPublicFolderFile().absolutePath

    init {
        refreshLegacyScan()
    }

    fun refreshLegacyScan() {
        viewModelScope.launch {
            val scanned = withContext(Dispatchers.IO) {
                LegacyRecordingCatalog.scanLegacyFiles(context)
            }
            legacyScan.value = scanned
        }
    }

    fun delete(item: RecordingListItem) {
        viewModelScope.launch {
            java.io.File(item.filePath).delete()
            item.entity?.let { repository.delete(it) }
            refreshLegacyScan()
        }
    }

    fun deleteEntity(entity: CallRecordingEntity) {
        delete(
            RecordingListItem(
                entity = entity,
                filePath = entity.filePath,
                title = entity.displayName,
                subtitle = "",
                isLegacyOnly = false
            )
        )
    }
}
