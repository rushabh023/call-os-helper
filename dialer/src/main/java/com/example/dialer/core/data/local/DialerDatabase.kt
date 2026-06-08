package com.example.dialer.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dialer.core.data.local.dao.BlockedNumberDao
import com.example.dialer.core.data.local.dao.CallRecordingDao
import com.example.dialer.core.data.local.dao.SpeedDialDao
import com.example.dialer.core.data.local.entity.BlockedNumberEntity
import com.example.dialer.core.data.local.entity.CallRecordingEntity
import com.example.dialer.core.data.local.entity.SpeedDialEntryEntity

@Database(
    entities = [
        CallRecordingEntity::class,
        BlockedNumberEntity::class,
        SpeedDialEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DialerDatabase : RoomDatabase() {
    abstract fun callRecordingDao(): CallRecordingDao
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun speedDialDao(): SpeedDialDao
}
