package com.example.dialer.core.data.repository

import com.example.dialer.core.data.local.dao.CallRecordingDao
import com.example.dialer.core.data.local.entity.CallRecordingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val dao: CallRecordingDao
) {
    fun observeRecordings(): Flow<List<CallRecordingEntity>> = dao.observeAll()

    fun search(query: String): Flow<List<CallRecordingEntity>> = dao.search(query)

    suspend fun save(entity: CallRecordingEntity): Long = dao.insert(entity)

    suspend fun delete(entity: CallRecordingEntity) = dao.delete(entity)

    suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)
}
