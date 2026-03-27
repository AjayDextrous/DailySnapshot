package com.example.dailysnapshot.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SnapshotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SnapshotDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
}
