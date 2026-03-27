package com.example.dailysnapshot.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // Populated in DAI-7: @Provides SnapshotDatabase and SnapshotDao
}
