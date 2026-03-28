package com.example.dailysnapshot.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SnapshotDatabase::class.java
    )

    @Test
    fun version1Schema_containsSnapshotEntriesTable() {
        val db = helper.createDatabase("migration_test", 1)
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='snapshot_entries'"
        )
        assertEquals(1, cursor.count)
        cursor.close()
        db.close()
    }

    @Test
    fun version1Schema_hasAllExpectedColumns() {
        val db = helper.createDatabase("migration_test", 1)
        val cursor = db.query("PRAGMA table_info(snapshot_entries)")
        val columns = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            columns += cursor.getString(cursor.getColumnIndexOrThrow("name"))
        }
        cursor.close()
        db.close()

        val expected = setOf("id", "date", "imagePath", "rawImagePath", "caption", "filterApplied", "createdAt", "updatedAt")
        assertEquals(expected, columns)
    }

    @Test
    fun version1Schema_hasDateIndex() {
        val db = helper.createDatabase("migration_test", 1)
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='snapshot_entries'"
        )
        val indices = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            indices += cursor.getString(0)
        }
        cursor.close()
        db.close()

        assertTrue("Expected an index on date column", indices.any { it.contains("date", ignoreCase = true) })
    }

    @Test
    fun version1Schema_canInsertAndQueryEntity() {
        val db = helper.createDatabase("migration_test", 1)
        db.execSQL(
            """INSERT INTO snapshot_entries (date, imagePath, rawImagePath, caption, filterApplied, createdAt, updatedAt)
               VALUES ('2024-01-01', '/framed.jpg', '/raw.jpg', 'test', NULL, 1000, 1000)"""
        )
        val cursor = db.query("SELECT * FROM snapshot_entries WHERE date = '2024-01-01'")
        assertEquals(1, cursor.count)
        cursor.close()
        db.close()
    }
}
