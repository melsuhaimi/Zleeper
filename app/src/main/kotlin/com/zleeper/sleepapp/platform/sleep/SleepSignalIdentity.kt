package com.zleeper.sleepapp.platform.sleep

import com.zleeper.sleepapp.core.id.stableId
import com.zleeper.sleepapp.domain.sleep.SleepSignalType

object SleepSignalIdentity {
    fun id(
        sessionId: String,
        type: SleepSignalType,
        occurredAtEpochMs: Long,
        durationMillis: Long?,
        confidencePercent: Int?,
    ): String = stableId(
        "sleep-signal",
        sessionId,
        type.name,
        occurredAtEpochMs.toString(),
        durationMillis?.toString() ?: "-",
        confidencePercent?.toString() ?: "-",
    )
}
