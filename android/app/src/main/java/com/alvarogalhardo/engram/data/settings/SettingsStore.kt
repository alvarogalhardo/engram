package com.alvarogalhardo.engram.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val newPerDayKey = intPreferencesKey("new_per_day")

    val newPerDay: Flow<Int> = context.dataStore.data.map { it[newPerDayKey] ?: DEFAULT_NEW_PER_DAY }

    suspend fun setNewPerDay(value: Int) {
        context.dataStore.edit { it[newPerDayKey] = value.coerceIn(0, 500) }
    }

    companion object {
        const val DEFAULT_NEW_PER_DAY = 20
    }
}
