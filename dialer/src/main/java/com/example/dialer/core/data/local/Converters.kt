package com.example.dialer.core.data.local

import androidx.room.TypeConverter
import com.example.dialer.core.data.local.entity.BlockPattern
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromBlockPattern(value: BlockPattern): String = value.name

    @TypeConverter
    fun toBlockPattern(value: String): BlockPattern = BlockPattern.valueOf(value)
}
