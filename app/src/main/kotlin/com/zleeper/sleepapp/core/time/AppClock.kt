package com.zleeper.sleepapp.core.time

import javax.inject.Inject

fun interface AppClock { fun nowEpochMillis(): Long }

class SystemAppClock @Inject constructor() : AppClock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
