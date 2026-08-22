package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.EquipmentSlotEntity
import com.zleeper.sleepapp.data.local.database.InventoryDao
import com.zleeper.sleepapp.data.local.database.PetDao
import com.zleeper.sleepapp.data.local.database.PetEntity
import com.zleeper.sleepapp.data.local.database.WorldDao
import com.zleeper.sleepapp.data.local.database.WorldUnlockEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.domain.inventory.EquipmentSlot
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetLifecycleService @Inject constructor(
    private val database: ZleeperDatabase,
    private val petDao: PetDao,
    private val inventoryDao: InventoryDao,
    private val worldDao: WorldDao,
    private val content: GameContentRepository,
    private val worldProgressService: WorldProgressService,
    private val questProgressService: QuestProgressService,
    private val titleService: TitleService,
) {
    suspend fun create(name: String, now: Long = System.currentTimeMillis()): Boolean {
        val clean = name.trim().take(24)
        require(clean.length >= 2) { "Pet name must contain at least two characters" }
        val species = requireNotNull(content.species.firstOrNull()) { "Pet species content is unavailable" }
        val form = requireNotNull(content.forms.firstOrNull { it.id == species.initialFormId }) { "Initial pet form is unavailable" }

        val created = database.withTransaction {
            if (petDao.pet() != null) return@withTransaction false
            petDao.insert(
                PetEntity(
                    instanceId = UUID.randomUUID().toString(),
                    speciesId = species.id,
                    displayName = clean,
                    formId = form.id,
                    level = 1,
                    totalXp = 0,
                    energy = 3,
                    focus = 3,
                    resilience = 3,
                    energyAffinity = 0,
                    focusAffinity = 0,
                    resilienceAffinity = 0,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                ),
            )
            EquipmentSlot.entries.forEach { slot ->
                inventoryDao.putEquipment(EquipmentSlotEntity(slot.name, null, now))
            }
            content.regions.firstOrNull()?.let { region ->
                worldDao.unlock(WorldUnlockEntity(region.id, "REGION", "onboarding", now))
            }
            true
        }

        if (created) {
            worldProgressService.ensureHearth(now)
            worldProgressService.recordCollection(form.id, "PET_FORM", 1, "onboarding:${form.id}", now)
            questProgressService.initialize(now)
            titleService.evaluateAll(now)
        }
        return created
    }

    suspend fun rename(name: String, now: Long = System.currentTimeMillis()) {
        val clean = name.trim().take(24)
        require(clean.length >= 2) { "Pet name must contain at least two characters" }
        petDao.pet()?.let { pet ->
            petDao.update(pet.copy(displayName = clean, updatedAtEpochMs = now))
        }
    }
}
