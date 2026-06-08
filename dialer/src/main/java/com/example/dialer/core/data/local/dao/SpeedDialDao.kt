package com.example.dialer.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dialer.core.data.local.entity.SpeedDialEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedDialDao {
    @Query("SELECT * FROM speed_dial ORDER BY slot ASC")
    fun observeAll(): Flow<List<SpeedDialEntryEntity>>

    @Query("SELECT * FROM speed_dial WHERE slot = :slot LIMIT 1")
    suspend fun getBySlot(slot: Int): SpeedDialEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SpeedDialEntryEntity)

    @Query("DELETE FROM speed_dial WHERE slot = :slot")
    suspend fun deleteSlot(slot: Int)
}
