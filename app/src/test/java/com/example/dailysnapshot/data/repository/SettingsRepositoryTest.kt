package com.example.dailysnapshot.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.example.dailysnapshot.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val prefsFlow = MutableStateFlow(emptyPreferences())
    private val dataStore: DataStore<Preferences> = mockk {
        every { data } returns prefsFlow
        // edit() is an extension that calls updateData(), so mock updateData directly
        coEvery { updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val result = transform(prefsFlow.value)
            prefsFlow.value = result
            result
        }
    }

    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        repository = SettingsRepository(dataStore)
    }

    // ── reminderEnabled ───────────────────────────────────────────────────────

    @Test
    fun reminderEnabled_defaultsToFalse() = runTest {
        assertFalse(repository.reminderEnabled.first())
    }

    @Test
    fun setReminderEnabled_true_emitsTrue() = runTest {
        repository.setReminderEnabled(true)
        assertTrue(repository.reminderEnabled.first())
    }

    @Test
    fun setReminderEnabled_false_afterTrue_emitsFalse() = runTest {
        repository.setReminderEnabled(true)
        repository.setReminderEnabled(false)
        assertFalse(repository.reminderEnabled.first())
    }

    // ── reminderHour / reminderMinute ─────────────────────────────────────────

    @Test
    fun reminderHour_defaultsTo20() = runTest {
        assertEquals(20, repository.reminderHour.first())
    }

    @Test
    fun reminderMinute_defaultsTo0() = runTest {
        assertEquals(0, repository.reminderMinute.first())
    }

    @Test
    fun setReminderTime_updatesHourAndMinute() = runTest {
        repository.setReminderTime(8, 30)
        assertEquals(8, repository.reminderHour.first())
        assertEquals(30, repository.reminderMinute.first())
    }

    @Test
    fun setReminderTime_overwritesPreviousValues() = runTest {
        repository.setReminderTime(8, 30)
        repository.setReminderTime(21, 15)
        assertEquals(21, repository.reminderHour.first())
        assertEquals(15, repository.reminderMinute.first())
    }

    // ── hasRequestedNotificationPermission ───────────────────────────────────

    @Test
    fun hasRequestedNotificationPermission_defaultsToFalse() = runTest {
        assertFalse(repository.hasRequestedNotificationPermission.first())
    }

    @Test
    fun setNotificationPermissionRequested_emitsTrue() = runTest {
        repository.setNotificationPermissionRequested()
        assertTrue(repository.hasRequestedNotificationPermission.first())
    }

    // ── defaultFilter ─────────────────────────────────────────────────────────

    @Test
    fun defaultFilter_defaultsToNone() = runTest {
        assertEquals("none", repository.defaultFilter.first())
    }

    @Test
    fun setDefaultFilter_updatesValue() = runTest {
        repository.setDefaultFilter("sepia")
        assertEquals("sepia", repository.defaultFilter.first())
    }
}
