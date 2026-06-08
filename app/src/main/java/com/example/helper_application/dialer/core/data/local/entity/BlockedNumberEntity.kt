package com.example.helper_application.dialer.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class BlockPattern {
    EXACT,
    STARTS_WITH,
    CONTAINS
}

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val pattern: BlockPattern = BlockPattern.EXACT,
    val label: String? = null,
    val addedAt: Date = Date()
)
