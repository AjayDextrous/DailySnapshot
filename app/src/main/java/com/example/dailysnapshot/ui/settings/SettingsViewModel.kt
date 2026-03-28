package com.example.dailysnapshot.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailysnapshot.data.repository.SettingsRepository
import com.example.dailysnapshot.util.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    data class UiState(
        val reminderEnabled: Boolean = false,
        val reminderHour: Int = 20,
        val reminderMinute: Int = 0,
        val hasRequestedNotificationPermission: Boolean = false
    )

    val uiState: StateFlow<UiState> = combine(
        settingsRepository.reminderEnabled,
        settingsRepository.reminderHour,
        settingsRepository.reminderMinute,
        settingsRepository.hasRequestedNotificationPermission
    ) { enabled, hour, minute, hasRequested ->
        UiState(enabled, hour, minute, hasRequested)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState()
    )

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            if (enabled) {
                val state = uiState.value
                reminderScheduler.schedule(state.reminderHour, state.reminderMinute)
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            if (uiState.value.reminderEnabled) {
                reminderScheduler.schedule(hour, minute)
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(granted)
            settingsRepository.setNotificationPermissionRequested()
            if (granted) {
                val hour = settingsRepository.reminderHour.first()
                val minute = settingsRepository.reminderMinute.first()
                reminderScheduler.schedule(hour, minute)
            } else {
                reminderScheduler.cancel()
            }
        }
    }
}
