package com.example.helper_application.dialer.core.data.repository

import com.example.helper_application.dialer.core.data.local.dao.BlockedNumberDao
import com.example.helper_application.dialer.core.data.local.entity.BlockPattern
import com.example.helper_application.dialer.core.data.local.entity.BlockedNumberEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistRepository @Inject constructor(
    private val dao: BlockedNumberDao
) {
    fun observeBlocked(): Flow<List<BlockedNumberEntity>> = dao.observeAll()

    suspend fun add(number: String, pattern: BlockPattern = BlockPattern.EXACT, label: String? = null) {
        dao.insert(
            BlockedNumberEntity(
                number = normalize(number),
                pattern = pattern,
                label = label
            )
        )
    }

    suspend fun remove(id: Long) = dao.deleteById(id)

    suspend fun isBlocked(rawNumber: String): Boolean {
        val normalized = normalize(rawNumber)
        return dao.getAllOnce().any { entry ->
            when (entry.pattern) {
                BlockPattern.EXACT -> normalized == entry.number
                BlockPattern.STARTS_WITH -> normalized.startsWith(entry.number)
                BlockPattern.CONTAINS -> normalized.contains(entry.number)
            }
        }
    }

    private fun normalize(number: String): String =
        number.filter { it.isDigit() || it == '+' }
}
