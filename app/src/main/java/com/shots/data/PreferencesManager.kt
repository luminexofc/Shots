package com.shots.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shots_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val TIMER_MINUTES = intPreferencesKey("timer_minutes")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DARK_MODE = intPreferencesKey("dark_mode") // 0=system, 1=dark, 2=light
        val SHOW_EDIT_BUTTON = booleanPreferencesKey("show_edit_button")
    }

    val timerMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.TIMER_MINUTES] ?: 5 }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val darkMode: Flow<Int> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: 0 }
    val showEditButton: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_EDIT_BUTTON] ?: false }

    suspend fun setTimerMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.TIMER_MINUTES] = minutes }
    }

    suspend fun setShowEditButton(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_EDIT_BUTTON] = enabled }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setDarkMode(mode: Int) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode }
    }
}
