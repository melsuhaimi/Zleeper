package com.zleeper.sleepapp.platform.sleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent
import com.zleeper.sleepapp.data.local.database.SleepDao
import com.zleeper.sleepapp.data.local.database.SleepSignalEntity
import com.zleeper.sleepapp.domain.sleep.SleepSignalType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SleepEventReceiver : BroadcastReceiver() {
    @Inject lateinit var sleepDao: SleepDao

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = sleepDao.trackingSession() ?: return@launch
                val receivedAt = System.currentTimeMillis()
                val classify = if (SleepClassifyEvent.hasEvents(intent)) SleepClassifyEvent.extractEvents(intent).orEmpty().map {
                    SleepSignalEntity(
                        SleepSignalIdentity.id(session.id, SleepSignalType.CLASSIFY, it.timestampMillis, null, it.confidence),
                        session.id,
                        SleepSignalType.CLASSIFY.name,
                        it.timestampMillis,
                        null,
                        it.confidence,
                        receivedAt,
                    )
                } else emptyList()
                val segments = if (SleepSegmentEvent.hasEvents(intent)) SleepSegmentEvent.extractEvents(intent).orEmpty().map {
                    SleepSignalEntity(
                        SleepSignalIdentity.id(session.id, SleepSignalType.SEGMENT, it.endTimeMillis, it.segmentDurationMillis, null),
                        session.id,
                        SleepSignalType.SEGMENT.name,
                        it.endTimeMillis,
                        it.segmentDurationMillis,
                        null,
                        receivedAt,
                    )
                } else emptyList()
                if (classify.isNotEmpty() || segments.isNotEmpty()) sleepDao.insertSignals(classify + segments)
            } finally {
                pending.finish()
            }
        }
    }
}
