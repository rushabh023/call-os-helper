package com.example.dialer.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.dialer.core.data.local.entity.CallRecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordingDao {
    @Query("SELECT * FROM call_recordings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM call_recordings WHERE displayName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<CallRecordingEntity>>

    @Insert
    suspend fun insert(entity: CallRecordingEntity): Long

    @Delete
    suspend fun delete(entity: CallRecordingEntity)

    @Query("DELETE FROM call_recordings WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
