package com.example.dailysnapshot.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val reminderEnabled: Flow<Boolean> =
        dataStore.data.map { it[Keys.REMINDER_ENABLED] ?: false }

    val hasRequestedNotificationPermission: Flow<Boolean> =
        dataStore.data.map { it[Keys.NOTIFICATION_PERMISSION_REQUESTED] ?: false }

    val reminderHour: Flow<Int> =
        dataStore.data.map { it[Keys.REMINDER_HOUR] ?: 20 }

    val reminderMinute: Flow<Int> =
        dataStore.data.map { it[Keys.REMINDER_MINUTE] ?: 0 }

    val defaultFilter: Flow<String> =
        dataStore.data.map { it[Keys.DEFAULT_FILTER] ?: "none" }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setDefaultFilter(filter: String) {
        dataStore.edit { it[Keys.DEFAULT_FILTER] = filter }
    }

    suspend fun setNotificationPermissionRequested() {
        dataStore.edit { it[Keys.NOTIFICATION_PERMISSION_REQUESTED] = true }
    }

    private object Keys {
        val REMINDER_ENABLED                  = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR                     = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE                   = intPreferencesKey("reminder_minute")
        val DEFAULT_FILTER                    = stringPreferencesKey("default_filter")
        val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
