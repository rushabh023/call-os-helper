package com.example.helper_application.dialer.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "call_recordings")
data class CallRecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val displayName: String,
    val phoneNumber: String?,
    val contactName: String?,
    val createdAt: Date,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val audioSourceLabel: String
)
