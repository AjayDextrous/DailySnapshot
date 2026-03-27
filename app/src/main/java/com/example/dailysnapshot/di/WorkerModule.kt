package com.example.dailysnapshot.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    // HiltWorkerFactory is automatically provided by Hilt when hilt-work is on the classpath.
    // Individual workers are registered via @HiltWorker + @AssistedInject — no manual bindings needed here.
}
