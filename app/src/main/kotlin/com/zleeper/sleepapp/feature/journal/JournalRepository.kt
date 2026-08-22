package com.zleeper.sleepapp.feature.journal

import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.ExpeditionDao
import com.zleeper.sleepapp.data.local.database.MorningDao
import com.zleeper.sleepapp.data.local.database.SleepDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class JournalRepository @Inject constructor(
    private val sleepDao: SleepDao,
    private val expeditionDao: ExpeditionDao,
    private val morningDao: MorningDao,
    private val content: GameContentRepository,
) {
    val history: Flow<List<JournalNight>> = combine(
        sleepDao.sessions(),
        expeditionDao.expeditions(),
        morningDao.notes(),
    ) { sessions, expeditions, notes ->
        val noteBySession = notes.associateBy { it.sleepSessionId }
        val expeditionBySession = expeditions.associateBy { it.sleepSessionId }
        val narrativeByKey = content.expeditionNarrative.associate { it.textKey to it.text }

        sessions
            .filter { it.state == "FINALIZED" || it.state == "EXPEDITION_RESOLVED" }
            .map { session ->
                val outcome = sleepDao.outcome(session.id)
                val expedition = expeditionBySession[session.id]
                val path = expedition?.let { expeditionDao.path(it.id) }.orEmpty()
                val rewards = expedition?.let { expeditionDao.rewards(it.id) }.orEmpty()
                val note = noteBySession[session.id]
                JournalNight(
                    sessionId = session.id,
                    startedAtEpochMs = session.sessionStartEpochMs,
                    estimatedSleepStartEpochMs = session.estimatedSleepStartEpochMs,
                    estimatedSleepEndEpochMs = session.estimatedSleepEndEpochMs,
                    estimatedSleepMinutes = session.estimatedSleepMinutes ?: 0,
                    targetSleepMinutes = session.targetSleepMinutes,
                    targetWakeMinutes = session.targetWakeMinutes,
                    timingOffsetMinutes = session.timingOffsetMinutes ?: 0,
                    confidence = session.confidence ?: "LOW",
                    resolutionMethod = session.resolutionMethod ?: "MANUAL",
                    windDownCompleted = session.windDownCompleted,
                    progressionQuality = outcome?.progressionQuality,
                    xpGranted = outcome?.xpGranted,
                    reachBand = outcome?.reachBand ?: expedition?.reachBand,
                    regionId = expedition?.regionId,
                    morningMood = note?.mood,
                    morningNote = note?.note?.takeIf { it.isNotBlank() },
                    journey = path.map { node ->
                        JournalJourneyStep(
                            sequence = node.sequence,
                            nodeType = node.nodeType,
                            outcomeTextKey = node.outcomeTextKey,
                            narrative = narrativeByKey[node.outcomeTextKey]
                                ?: error("Missing expedition narrative for ${node.outcomeTextKey}"),
                        )
                    },
                    rewards = rewards.map { reward ->
                        JournalReward(reward.rewardType, reward.contentId, reward.quantity)
                    },
                )
            }
    }
}
