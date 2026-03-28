package com.example.dailysnapshot.util

import com.example.dailysnapshot.data.db.SnapshotEntity

object TestFixtures {
    fun buildEntity(
        id: Long = 0,
        date: String = "2024-01-01",
        imagePath: String = "/data/framed/test_framed.jpg",
        rawImagePath: String? = "/data/raw/test.jpg",
        caption: String = "",
        filterApplied: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ) = SnapshotEntity(
        id = id,
        date = date,
        imagePath = imagePath,
        rawImagePath = rawImagePath,
        caption = caption,
        filterApplied = filterApplied,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
