package com.example.dailysnapshot.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Populated in DAI-8: @Provides SnapshotRepository
    // Populated in DAI-19: @Provides SettingsRepository
}
