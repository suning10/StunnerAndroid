package com.seclass.stunner.repository

import android.content.Context

/**
 * Persists user-configurable settings in SharedPreferences.
 * Currently holds the REST API base URL — editable from the Settings screen.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
        set(value) = prefs.edit().putString(KEY_API_URL, value).apply()

    companion object {
        private const val PREFS_NAME = "stunner_settings"
        private const val KEY_API_URL = "api_base_url"
        const val DEFAULT_API_URL = "http://192.168.1.1:8080/"
    }
}
