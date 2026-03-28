package com.example.dailysnapshot.ui.settings

import app.cash.turbine.test
import com.example.dailysnapshot.data.repository.SettingsRepository
import com.example.dailysnapshot.util.MainDispatcherRule
import com.example.dailysnapshot.util.ReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reminderEnabledFlow = MutableStateFlow(false)
    private val reminderHourFlow = MutableStateFlow(20)
    private val reminderMinuteFlow = MutableStateFlow(0)
    private val hasRequestedFlow = MutableStateFlow(false)

    private val settingsRepository: SettingsRepository = mockk {
        every { reminderEnabled } returns reminderEnabledFlow
        every { reminderHour } returns reminderHourFlow
        every { reminderMinute } returns reminderMinuteFlow
        every { hasRequestedNotificationPermission } returns hasRequestedFlow
        coEvery { setReminderEnabled(any()) } coAnswers { reminderEnabledFlow.value = firstArg() }
        coEvery { setReminderTime(any(), any()) } coAnswers {
            reminderHourFlow.value = firstArg()
            reminderMinuteFlow.value = secondArg()
        }
        coEvery { setNotificationPermissionRequested() } coAnswers { hasRequestedFlow.value = true }
    }
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        viewModel = SettingsViewModel(settingsRepository, reminderScheduler)
    }

    // ── uiState ───────────────────────────────────────────────────────────────

    @Test
    fun uiState_initialValue_reflectsRepositoryDefaults() = runTest {
        val state = viewModel.uiState.first()
        assertFalse(state.reminderEnabled)
        assertEquals(20, state.reminderHour)
        assertEquals(0, state.reminderMinute)
        assertFalse(state.hasRequestedNotificationPermission)
    }

    @Test
    fun uiState_updatesWhenRepositoryFlowChanges() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            reminderEnabledFlow.value = true
            val updated = awaitItem()
            assertTrue(updated.reminderEnabled)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── setReminderEnabled ────────────────────────────────────────────────────

    @Test
    fun setReminderEnabled_true_persistsToRepository() = runTest {
        viewModel.setReminderEnabled(true)
        coVerify { settingsRepository.setReminderEnabled(true) }
    }

    @Test
    fun setReminderEnabled_true_schedulesReminder_withRepositoryTime() = runTest {
        reminderHourFlow.value = 9
        reminderMinuteFlow.value = 30

        viewModel.setReminderEnabled(true)

        coVerify { reminderScheduler.schedule(9, 30) }
    }

    @Test
    fun setReminderEnabled_false_cancelsReminder() = runTest {
        viewModel.setReminderEnabled(false)
        coVerify { reminderScheduler.cancel() }
    }

    @Test
    fun setReminderEnabled_false_doesNotSchedule() = runTest {
        viewModel.setReminderEnabled(false)
        coVerify(exactly = 0) { reminderScheduler.schedule(any(), any()) }
    }

    // ── setReminderTime ───────────────────────────────────────────────────────

    @Test
    fun setReminderTime_persistsToRepository() = runTest {
        viewModel.setReminderTime(21, 15)
        coVerify { settingsRepository.setReminderTime(21, 15) }
    }

    @Test
    fun setReminderTime_schedulesReminder_whenEnabled() = runTest {
        // uiState uses WhileSubscribed — subscribe first so the upstream combine starts running
        viewModel.uiState.test {
            awaitItem() // initial state
            reminderEnabledFlow.value = true
            awaitItem() // state with reminderEnabled = true

            viewModel.setReminderTime(21, 15)

            coVerify { reminderScheduler.schedule(21, 15) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setReminderTime_doesNotSchedule_whenDisabled() = runTest {
        // reminderEnabled defaults to false; subscribe to start the upstream flow
        viewModel.uiState.test {
            awaitItem() // initial state (reminderEnabled = false)

            viewModel.setReminderTime(21, 15)

            coVerify(exactly = 0) { reminderScheduler.schedule(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onPermissionResult ────────────────────────────────────────────────────

    @Test
    fun onPermissionResult_granted_enablesReminderAndSchedules() = runTest {
        reminderHourFlow.value = 8
        reminderMinuteFlow.value = 0

        viewModel.onPermissionResult(granted = true)

        coVerify { settingsRepository.setReminderEnabled(true) }
        coVerify { settingsRepository.setNotificationPermissionRequested() }
        coVerify { reminderScheduler.schedule(8, 0) }
    }

    @Test
    fun onPermissionResult_denied_disablesReminderAndCancels() = runTest {
        viewModel.onPermissionResult(granted = false)

        coVerify { settingsRepository.setReminderEnabled(false) }
        coVerify { settingsRepository.setNotificationPermissionRequested() }
        coVerify { reminderScheduler.cancel() }
    }

    @Test
    fun onPermissionResult_denied_doesNotSchedule() = runTest {
        viewModel.onPermissionResult(granted = false)
        coVerify(exactly = 0) { reminderScheduler.schedule(any(), any()) }
    }
}
