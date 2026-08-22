package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.core.id.stableId
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.CollectionEntryEntity
import com.zleeper.sleepapp.data.local.database.HearthProgressEntity
import com.zleeper.sleepapp.data.local.database.HearthProgressionEventEntity
import com.zleeper.sleepapp.data.local.database.WorldDao
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorldProgressService @Inject constructor(
    private val database: ZleeperDatabase,
    private val worldDao: WorldDao,
    private val content: GameContentRepository,
    private val memoryService: PetMemoryService,
) {
    suspend fun ensureHearth(now: Long = System.currentTimeMillis()): HearthProgressEntity = database.withTransaction {
        val existing = worldDao.hearthNow()
        if (existing != null) existing else HearthProgressEntity(memories = 0, updatedAtEpochMs = now).also { worldDao.putHearth(it) }
    }

    suspend fun addHearthMemories(amount: Int, reason: String, sourceId: String, now: Long = System.currentTimeMillis()): Int {
        require(amount >= 0 && reason.isNotBlank() && sourceId.isNotBlank())
        if (amount == 0) return worldDao.hearthNow()?.memories ?: 0
        val total = database.withTransaction {
            val eventId = stableId("hearth", reason, sourceId)
            if (worldDao.hearthEventsForSource(sourceId).any { it.id == eventId }) return@withTransaction worldDao.hearthNow()?.memories ?: 0
            val hearth = worldDao.hearthNow() ?: HearthProgressEntity(memories = 0, updatedAtEpochMs = now)
            if (worldDao.insertHearthEvent(HearthProgressionEventEntity(eventId, amount, reason, sourceId, now)) == -1L) return@withTransaction hearth.memories
            val next = hearth.copy(memories = hearth.memories + amount, updatedAtEpochMs = now)
            worldDao.putHearth(next)
            next.memories
        }
        memoryService.remember("HEARTH_GROWTH", "$reason:$sourceId", "thought.hearth_growth", now)
        return total
    }

    suspend fun recordCollection(entryId: String, category: String, quantity: Int, sourceId: String, now: Long = System.currentTimeMillis()): Boolean {
        require(quantity > 0)
        val first = database.withTransaction {
            val existing = worldDao.collectionEntry(entryId)
            worldDao.putCollection(
                CollectionEntryEntity(
                    entryId = entryId,
                    category = category,
                    quantity = (existing?.quantity ?: 0) + quantity,
                    firstDiscoveredAtEpochMs = existing?.firstDiscoveredAtEpochMs ?: now,
                    updatedAtEpochMs = now,
                ),
            )
            existing == null
        }
        if (first) {
            addHearthMemories(content.progressionRules.hearthMemoryRewards?.newCollectionEntry ?: 0, "NEW_COLLECTION_ENTRY", sourceId, now)
        }
        return first
    }
}
