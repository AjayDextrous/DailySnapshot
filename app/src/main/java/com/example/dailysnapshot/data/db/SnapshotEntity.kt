package com.example.dailysnapshot.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "snapshot_entries",
    indices = [Index(value = ["date"])]
)
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val imagePath: String,
    val rawImagePath: String?,
    val caption: String = "",
    val filterApplied: String?,
    val createdAt: Long,
    val updatedAt: Long
)
