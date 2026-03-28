package com.example.dailysnapshot.di

import android.content.Context
import androidx.room.Room
import com.example.dailysnapshot.data.db.SnapshotDao
import com.example.dailysnapshot.data.db.SnapshotDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSnapshotDatabase(@ApplicationContext context: Context): SnapshotDatabase =
        Room.databaseBuilder(context, SnapshotDatabase::class.java, "snapshot_db")
            .build()

    @Provides
    @Singleton
    fun provideSnapshotDao(database: SnapshotDatabase): SnapshotDao = database.snapshotDao()
}
