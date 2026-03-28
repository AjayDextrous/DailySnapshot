package com.example.dailysnapshot.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailysnapshot.data.repository.SettingsRepository
import com.example.dailysnapshot.util.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _requestPermission = Channel<Unit>(Channel.CONFLATED)
    val requestPermission = _requestPermission.receiveAsFlow()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            viewModelScope.launch {
                val hasRequested = settingsRepository.hasRequestedNotificationPermission.first()
                val reminderEnabled = settingsRepository.reminderEnabled.first()
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasRequested || (reminderEnabled && !granted)) {
                    _requestPermission.send(Unit)
                }
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
