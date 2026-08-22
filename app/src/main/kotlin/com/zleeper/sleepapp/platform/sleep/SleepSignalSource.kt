package com.zleeper.sleepapp.platform.sleep

interface SleepSignalSource {
    suspend fun subscribe(): Result<Unit>
    suspend fun unsubscribe(): Result<Unit>
    fun isAvailable(): Boolean
}
