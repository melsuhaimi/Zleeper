package com.zleeper.sleepapp.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.zleeper.sleepapp.core.time.AppClock
import com.zleeper.sleepapp.core.time.SystemAppClock
import com.zleeper.sleepapp.data.local.database.ExpeditionDao
import com.zleeper.sleepapp.data.local.database.InventoryDao
import com.zleeper.sleepapp.data.local.database.MorningDao
import com.zleeper.sleepapp.data.local.database.PetDao
import com.zleeper.sleepapp.data.local.database.QuestDao
import com.zleeper.sleepapp.data.local.database.SleepDao
import com.zleeper.sleepapp.data.local.database.WorldDao
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.data.local.preferences.zleeperPreferences
import com.zleeper.sleepapp.data.repository.RoomSleepSessionRepository
import com.zleeper.sleepapp.domain.sleep.SleepSessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton fun provideClock(): AppClock = SystemAppClock()
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ZleeperDatabase = Room.databaseBuilder(
        context,
        ZleeperDatabase::class.java,
        "zleeper.db",
    ).build()

    @Provides
    @Singleton
    fun providePreferences(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.zleeperPreferences

    @Provides fun provideSleepDao(database: ZleeperDatabase): SleepDao = database.sleepDao()
    @Provides fun provideExpeditionDao(database: ZleeperDatabase): ExpeditionDao = database.expeditionDao()
    @Provides fun providePetDao(database: ZleeperDatabase): PetDao = database.petDao()
    @Provides fun provideInventoryDao(database: ZleeperDatabase): InventoryDao = database.inventoryDao()
    @Provides fun provideQuestDao(database: ZleeperDatabase): QuestDao = database.questDao()
    @Provides fun provideWorldDao(database: ZleeperDatabase): WorldDao = database.worldDao()
    @Provides fun provideMorningDao(database: ZleeperDatabase): MorningDao = database.morningDao()
    @Provides @Singleton fun provideSleepSessionRepository(value: RoomSleepSessionRepository): SleepSessionRepository = value
}
