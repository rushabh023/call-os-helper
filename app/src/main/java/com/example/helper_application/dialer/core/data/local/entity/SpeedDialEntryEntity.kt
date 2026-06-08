package com.example.helper_application.dialer.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_dial")
data class SpeedDialEntryEntity(
    @PrimaryKey val slot: Int,
    val contactId: Long?,
    val displayName: String,
    val phoneNumber: String
)
