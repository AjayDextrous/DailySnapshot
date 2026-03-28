package com.example.dailysnapshot

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.dailysnapshot.data.repository.SettingsRepository
import com.example.dailysnapshot.util.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class DailySnapshotApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        rescheduleReminderOnStartup()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DAILY_REMINDER,
                "Daily Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /** Re-enqueues the reminder after device restart (WorkManager persists work but safe to re-enqueue with UPDATE). */
    private fun rescheduleReminderOnStartup() {
        runBlocking {
            val enabled = settingsRepository.reminderEnabled.first()
            if (enabled) {
                val hour   = settingsRepository.reminderHour.first()
                val minute = settingsRepository.reminderMinute.first()
                reminderScheduler.schedule(hour, minute)
            }
        }
    }

    companion object {
        const val CHANNEL_DAILY_REMINDER = "daily_reminder"
    }
}
