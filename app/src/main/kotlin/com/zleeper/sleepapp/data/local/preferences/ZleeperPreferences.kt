package com.zleeper.sleepapp.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private const val PREFERENCES_NAME = "zleeper_preferences"

val Context.zleeperPreferences by preferencesDataStore(name = PREFERENCES_NAME)
