package com.zleeper.sleepapp.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.data.local.preferences.zleeperPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
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
}
