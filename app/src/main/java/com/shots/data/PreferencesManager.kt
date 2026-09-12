package com.shots.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shots_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val DEFAULT_ACTION = stringPreferencesKey("default_action")
        val TIMER_MINUTES = intPreferencesKey("timer_minutes")
        val AUTO_DELETE = booleanPreferencesKey("auto_delete")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DARK_MODE = intPreferencesKey("dark_mode") // 0=system, 1=dark, 2=light
    }

    val defaultAction: Flow<String> = context.dataStore.data.map { it[Keys.DEFAULT_ACTION] ?: "keep" }
    val timerMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.TIMER_MINUTES] ?: 5 }
    val autoDelete: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_DELETE] ?: true }
    val notifications: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS] ?: true }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val darkMode: Flow<Int> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: 0 }

    suspend fun setDefaultAction(action: String) {
        context.dataStore.edit { it[Keys.DEFAULT_ACTION] = action }
    }

    suspend fun setTimerMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.TIMER_MINUTES] = minutes }
    }

    suspend fun setAutoDelete(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_DELETE] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setDarkMode(mode: Int) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode }
    }
}
