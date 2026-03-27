package com.example.dailysnapshot.data.model

import com.example.dailysnapshot.data.db.SnapshotEntity

data class Snapshot(
    val id: Long,
    val date: String,
    val imagePath: String,
    val rawImagePath: String?,
    val caption: String,
    val filterApplied: String?,
    val createdAt: Long,
    val updatedAt: Long
)

fun SnapshotEntity.toDomain() = Snapshot(
    id = id,
    date = date,
    imagePath = imagePath,
    rawImagePath = rawImagePath,
    caption = caption,
    filterApplied = filterApplied,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Snapshot.toEntity() = SnapshotEntity(
    id = id,
    date = date,
    imagePath = imagePath,
    rawImagePath = rawImagePath,
    caption = caption,
    filterApplied = filterApplied,
    createdAt = createdAt,
    updatedAt = updatedAt
)
