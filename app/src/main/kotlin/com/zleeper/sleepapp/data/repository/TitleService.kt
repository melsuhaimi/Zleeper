package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.ExpeditionDao
import com.zleeper.sleepapp.data.local.database.InventoryDao
import com.zleeper.sleepapp.data.local.database.PlayerTitleEntity
import com.zleeper.sleepapp.data.local.database.WorldDao
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TitleService @Inject constructor(
    private val database: ZleeperDatabase,
    private val expeditionDao: ExpeditionDao,
    private val inventoryDao: InventoryDao,
    private val worldDao: WorldDao,
    private val content: GameContentRepository,
    private val memoryService: PetMemoryService,
) {
    suspend fun evaluateAll(now: Long = System.currentTimeMillis()) {
        val unlocked = database.withTransaction {
            val revealCount = expeditionDao.resolvedCount()
            val discoveryCount = worldDao.discoveryCount()
            val craftCount = inventoryDao.craftCount()
            val memories = worldDao.hearthNow()?.memories ?: 0
            val hearthOrder = content.hearthStages.filter { memories >= it.requiredMemories }.maxOfOrNull { it.order } ?: 1
            buildList {
                content.titles.forEach { title ->
                    val value = when (title.conditionType) {
                        "MORNING_REVEALS" -> revealCount
                        "DISCOVERIES" -> discoveryCount
                        "CRAFTS" -> craftCount
                        "HEARTH_STAGE_ORDER" -> hearthOrder
                        else -> 0
                    }
                    if (value >= title.conditionValue && worldDao.title(title.id) == null) {
                        worldDao.putTitle(PlayerTitleEntity(title.id, now, false, now)); add(title.id)
                    }
                }
            }
        }
        unlocked.forEach { memoryService.remember("TITLE", it, "thought.title_unlock", now, it) }
    }

    suspend fun equip(titleId: String) = database.withTransaction {
        val unlocked = requireNotNull(worldDao.title(titleId)) { "Title is not unlocked" }
        val now = System.currentTimeMillis()
        worldDao.titlesSnapshot().filter { it.equipped }.forEach { worldDao.putTitle(it.copy(equipped = false, updatedAtEpochMs = now)) }
        worldDao.putTitle(unlocked.copy(equipped = true, updatedAtEpochMs = now))
    }
}
