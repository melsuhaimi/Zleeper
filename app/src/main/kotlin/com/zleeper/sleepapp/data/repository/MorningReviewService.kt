package com.zleeper.sleepapp.data.repository

import com.zleeper.sleepapp.data.local.database.MorningDao
import com.zleeper.sleepapp.data.local.database.MorningNoteEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MorningReviewService @Inject constructor(
    private val morningDao: MorningDao,
) {
    suspend fun saveReflection(
        sleepSessionId: String,
        mood: Int,
        note: String,
        now: Long = System.currentTimeMillis(),
    ) {
        require(mood in 1..5)
        val existing = morningDao.note(sleepSessionId)
        morningDao.put(
            MorningNoteEntity(
                id = existing?.id ?: "morning-note:$sleepSessionId",
                sleepSessionId = sleepSessionId,
                mood = mood,
                note = note.trim().take(500),
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now,
            ),
        )
    }

    suspend fun hasReflection(sleepSessionId: String): Boolean = morningDao.note(sleepSessionId) != null
}
