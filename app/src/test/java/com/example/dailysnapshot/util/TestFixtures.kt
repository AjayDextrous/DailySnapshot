package com.example.dailysnapshot.util

import com.example.dailysnapshot.data.model.Snapshot

object TestFixtures {
    fun buildSnapshot(
        id: Long = 1L,
        date: String = "2024-01-01",
        imagePath: String = "/tmp/test_framed.jpg",
        rawImagePath: String? = "/tmp/test.jpg",
        caption: String = "",
        filterApplied: String? = null,
        createdAt: Long = 1000L,
        updatedAt: Long = 1000L
    ) = Snapshot(
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
