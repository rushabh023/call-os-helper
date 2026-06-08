package com.example.helper_application.dialer.feature.recordings

import android.content.Context
import com.example.helper_application.dialer.core.data.local.entity.CallRecordingEntity
import com.example.helper_application.recording.RecordingStorage
import java.io.File
import java.text.DateFormat
import java.util.Date

object LegacyRecordingCatalog {

    private val AUDIO_EXTENSIONS = setOf("amr", "m4a", "mp4", "wav", "3gp", "aac")

    fun scanLegacyFiles(context: Context): List<RecordingListItem> {
        RecordingStorage.ensureFolderExists(context)
        val dir = RecordingStorage.getPublicFolderFile()
        if (!dir.isDirectory) return emptyList()
        val formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        return dir.listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.extension.lowercase() in AUDIO_EXTENSIONS &&
                    file.name != RecordingStorage.FOLDER_MARKER_FILE
            }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                val whenText = formatter.format(Date(file.lastModified()))
                val sizeKb = file.length() / 1024
                RecordingListItem(
                    entity = null,
                    filePath = file.absolutePath,
                    title = file.nameWithoutExtension,
                    subtitle = "$whenText · ${sizeKb}KB · ${file.extension.uppercase()}",
                    isLegacyOnly = true
                )
            }
            .orEmpty()
    }

    fun mergeWithRoom(
        roomItems: List<CallRecordingEntity>,
        legacyItems: List<RecordingListItem>
    ): List<RecordingListItem> {
        val roomPaths = roomItems.map { it.filePath }.toSet()
        val fromRoom = roomItems.map { entity ->
            val whenText = DateFormat.getDateTimeInstance().format(entity.createdAt)
            RecordingListItem(
                entity = entity,
                filePath = entity.filePath,
                title = entity.contactName?.takeIf { it.isNotBlank() } ?: entity.displayName,
                subtitle = "$whenText · ${entity.durationMs / 1000}s · ${entity.audioSourceLabel}",
                isLegacyOnly = false
            )
        }
        val extraLegacy = legacyItems.filter { it.filePath !in roomPaths }
        return (fromRoom + extraLegacy).sortedByDescending { item ->
            item.entity?.createdAt?.time ?: File(item.filePath).lastModified()
        }
    }
}

data class RecordingListItem(
    val entity: CallRecordingEntity?,
    val filePath: String,
    val title: String,
    val subtitle: String,
    val isLegacyOnly: Boolean
)
