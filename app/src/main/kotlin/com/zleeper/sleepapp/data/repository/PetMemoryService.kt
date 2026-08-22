package com.zleeper.sleepapp.data.repository

import com.zleeper.sleepapp.core.id.stableId
import com.zleeper.sleepapp.data.local.database.PetDao
import com.zleeper.sleepapp.data.local.database.PetMemoryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetMemoryService @Inject constructor(private val petDao: PetDao) {
    suspend fun remember(memoryType: String, sourceId: String, thoughtKey: String, occurredAtEpochMs: Long = System.currentTimeMillis(), subjectId: String? = null, detail: String? = null): Boolean {
        require(memoryType.isNotBlank() && sourceId.isNotBlank() && thoughtKey.isNotBlank())
        val pet = petDao.pet() ?: return false
        val id = stableId("pet-memory", pet.instanceId, memoryType, sourceId)
        return petDao.insertMemory(PetMemoryEntity(id, pet.instanceId, memoryType, sourceId, subjectId, thoughtKey, detail, occurredAtEpochMs)) != -1L
    }
}
