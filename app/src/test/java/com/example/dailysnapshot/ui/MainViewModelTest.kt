package com.example.dailysnapshot.ui

import android.content.Context
import com.example.dailysnapshot.data.repository.SettingsRepository
import com.example.dailysnapshot.util.MainDispatcherRule
import com.example.dailysnapshot.util.ReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [MainViewModel].
 *
 * The init block is guarded by `Build.VERSION.SDK_INT >= TIRAMISU` (33). In the JVM test
 * environment SDK_INT == 0, so the permission-request Channel never fires — that path is
 * covered by the instrumented test suite. This class focuses on [MainViewModel.onPermissionResult].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

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
        coEvery { setNotificationPermissionRequested() } coAnswers { hasRequestedFlow.value = true }
    }
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        viewModel = MainViewModel(settingsRepository, reminderScheduler, context)
    }

    // ── onPermissionResult ────────────────────────────────────────────────────

    @Test
    fun onPermissionResult_granted_enablesReminder() = runTest {
        viewModel.onPermissionResult(granted = true)
        coVerify { settingsRepository.setReminderEnabled(true) }
    }

    @Test
    fun onPermissionResult_granted_marksPermissionRequested() = runTest {
        viewModel.onPermissionResult(granted = true)
        coVerify { settingsRepository.setNotificationPermissionRequested() }
    }

    @Test
    fun onPermissionResult_granted_schedulesReminderWithRepositoryTime() = runTest {
        reminderHourFlow.value = 19
        reminderMinuteFlow.value = 45

        viewModel.onPermissionResult(granted = true)

        coVerify { reminderScheduler.schedule(19, 45) }
    }

    @Test
    fun onPermissionResult_denied_disablesReminder() = runTest {
        viewModel.onPermissionResult(granted = false)
        coVerify { settingsRepository.setReminderEnabled(false) }
    }

    @Test
    fun onPermissionResult_denied_marksPermissionRequested() = runTest {
        viewModel.onPermissionResult(granted = false)
        coVerify { settingsRepository.setNotificationPermissionRequested() }
    }

    @Test
    fun onPermissionResult_denied_cancelsReminder() = runTest {
        viewModel.onPermissionResult(granted = false)
        coVerify { reminderScheduler.cancel() }
    }

    @Test
    fun onPermissionResult_denied_doesNotSchedule() = runTest {
        viewModel.onPermissionResult(granted = false)
        coVerify(exactly = 0) { reminderScheduler.schedule(any(), any()) }
    }
}
