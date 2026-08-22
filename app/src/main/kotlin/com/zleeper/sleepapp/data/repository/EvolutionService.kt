package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.core.id.stableId
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.PetDao
import com.zleeper.sleepapp.data.local.database.PetProgressionEventEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvolutionService @Inject constructor(
    private val database: ZleeperDatabase,
    private val petDao: PetDao,
    private val content: GameContentRepository,
    private val worldProgressService: WorldProgressService,
    private val memoryService: PetMemoryService,
) {
    suspend fun choose(formId: String) = database.withTransaction {
        val pet = requireNotNull(petDao.pet()) { "Create a companion first" }
        val form = requireNotNull(content.forms.firstOrNull { it.id == formId }) { "Unknown companion form" }
        require(form.speciesId == pet.speciesId && pet.level >= form.levelGate && form.id != pet.formId)
        val now = System.currentTimeMillis()
        petDao.update(pet.copy(formId = form.id, updatedAtEpochMs = now))
        petDao.insertEvents(listOf(PetProgressionEventEntity(stableId("pet-event", pet.instanceId, "form", form.id), pet.instanceId, "FORM_CHOSEN", 1, form.id, content.progressionRules.version, now)))
        worldProgressService.recordCollection(form.id, "PET_FORM", 1, "form:${form.id}", now)
        memoryService.remember("EVOLUTION", form.id, "thought.evolution", now, form.id)
    }
}
