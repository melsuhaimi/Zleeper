package com.zleeper.sleepapp.data.di

import com.zleeper.sleepapp.platform.sleep.PlayServicesSleepSignalSource
import com.zleeper.sleepapp.platform.sleep.SleepSignalSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds abstract fun bindSleepSignalSource(value: PlayServicesSleepSignalSource): SleepSignalSource
}
