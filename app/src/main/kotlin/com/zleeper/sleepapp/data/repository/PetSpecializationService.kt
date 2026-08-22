package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.PetDao
import com.zleeper.sleepapp.data.local.database.PetSpecializationEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetSpecializationService @Inject constructor(
    private val database: ZleeperDatabase,
    private val petDao: PetDao,
    private val content: GameContentRepository,
    private val memoryService: PetMemoryService,
) {
    suspend fun spend(nodeId: String) = database.withTransaction {
        val pet = requireNotNull(petDao.pet()) { "Create a companion first" }
        val node = requireNotNull(content.specializations.firstOrNull { it.id == nodeId }) { "Unknown specialization" }
        val ranks = petDao.specializationsNow().associateBy { it.nodeId }
        require(node.prerequisites.all { (ranks[it]?.rank ?: 0) > 0 }) { "Specialization prerequisites are not met" }
        val current = ranks[nodeId]
        val nextRank = (current?.rank ?: 0) + 1
        require(nextRank <= node.maxRank) { "Specialization is already max rank" }
        require(pet.dreamSparksAvailable >= node.costPerRank) { "Not enough Dream Sparks" }
        val now = System.currentTimeMillis()
        petDao.update(pet.copy(dreamSparksAvailable = pet.dreamSparksAvailable - node.costPerRank, updatedAtEpochMs = now))
        petDao.putSpecialization(PetSpecializationEntity(pet.instanceId, node.id, nextRank, current?.unlockedAtEpochMs ?: now, now))
        memoryService.remember("SPECIALIZATION", "${node.id}:$nextRank", "thought.specialization", now, node.id)
    }
}
