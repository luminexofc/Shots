package com.screenshotguard.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shots_prefs", Context.MODE_PRIVATE)

    private val _deleteDelayMinutes = MutableStateFlow(getDeleteDelayMinutes())
    val deleteDelayMinutes: StateFlow<Int> = _deleteDelayMinutes.asStateFlow()

    private val _defaultAction = MutableStateFlow(getDefaultAction())
    val defaultAction: StateFlow<Int> = _defaultAction.asStateFlow()

    private val _autoDismissTimeout = MutableStateFlow(getAutoDismissTimeout())
    val autoDismissTimeout: StateFlow<Int> = _autoDismissTimeout.asStateFlow()

    private val _notifyBeforeDelete = MutableStateFlow(getNotifyBeforeDelete())
    val notifyBeforeDelete: StateFlow<Boolean> = _notifyBeforeDelete.asStateFlow()

    private val _dynamicColor = MutableStateFlow(getDynamicColor())
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _darkTheme = MutableStateFlow(getDarkTheme())
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    fun getDeleteDelayMinutes(): Int = prefs.getInt(KEY_DELETE_DELAY_MINUTES, 120)

    fun setDeleteDelayMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DELETE_DELAY_MINUTES, minutes).apply()
        _deleteDelayMinutes.value = minutes
    }

    fun getDefaultAction(): Int = prefs.getInt(KEY_DEFAULT_ACTION, 0)

    fun setDefaultAction(action: Int) {
        prefs.edit().putInt(KEY_DEFAULT_ACTION, action).apply()
        _defaultAction.value = action
    }

    fun getAutoDismissTimeout(): Int = prefs.getInt(KEY_AUTO_DISMISS_TIMEOUT, 30)

    fun setAutoDismissTimeout(seconds: Int) {
        prefs.edit().putInt(KEY_AUTO_DISMISS_TIMEOUT, seconds).apply()
        _autoDismissTimeout.value = seconds
    }

    fun getNotifyBeforeDelete(): Boolean = prefs.getBoolean(KEY_NOTIFY_BEFORE_DELETE, true)

    fun setNotifyBeforeDelete(notify: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_BEFORE_DELETE, notify).apply()
        _notifyBeforeDelete.value = notify
    }

    fun getDynamicColor(): Boolean = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun getDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK_THEME, false)

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        _darkTheme.value = enabled
    }

    private val _onboardingComplete = MutableStateFlow(getOnboardingComplete())
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    fun getOnboardingComplete(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
        _onboardingComplete.value = complete
    }

    companion object {
        private const val KEY_DELETE_DELAY_MINUTES = "delete_delay_minutes"
        private const val KEY_DEFAULT_ACTION = "default_action"
        private const val KEY_AUTO_DISMISS_TIMEOUT = "auto_dismiss_timeout"
        private const val KEY_NOTIFY_BEFORE_DELETE = "notify_before_delete"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}
