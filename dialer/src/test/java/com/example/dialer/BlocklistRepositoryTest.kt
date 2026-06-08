package com.example.dialer

import com.example.dialer.core.data.local.dao.BlockedNumberDao
import com.example.dialer.core.data.local.entity.BlockPattern
import com.example.dialer.core.data.local.entity.BlockedNumberEntity
import com.example.dialer.core.data.repository.BlocklistRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class BlocklistRepositoryTest {

    private lateinit var repository: BlocklistRepository
    private val store = mutableListOf<BlockedNumberEntity>()

    @Before
    fun setup() {
        val dao = object : BlockedNumberDao {
            override fun observeAll() = kotlinx.coroutines.flow.flowOf(store.toList())
            override suspend fun insert(entity: BlockedNumberEntity): Long {
                val id = (store.maxOfOrNull { it.id } ?: 0L) + 1
                store += entity.copy(id = id)
                return id
            }
            override suspend fun deleteById(id: Long) { store.removeAll { it.id == id } }
            override suspend fun getAllOnce(): List<BlockedNumberEntity> = store.toList()
        }
        repository = BlocklistRepository(dao)
    }

    @Test
    fun exactBlock_matchesNormalizedNumber() = runBlocking {
        repository.add("+1 (555) 123-4567", BlockPattern.EXACT)
        assertTrue(repository.isBlocked("15551234567"))
        assertFalse(repository.isBlocked("15551234568"))
    }

    @Test
    fun startsWithBlock_matchesPrefix() = runBlocking {
        store += BlockedNumberEntity(1, "555", BlockPattern.STARTS_WITH, null, Date())
        assertTrue(repository.isBlocked("5551234"))
        assertFalse(repository.isBlocked("4441234"))
    }
}
