package com.example.dailysnapshot.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.dailysnapshot.util.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotDaoTest {

    private lateinit var db: SnapshotDatabase
    private lateinit var dao: SnapshotDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SnapshotDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.snapshotDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Insert / retrieve ──────────────────────────────────────────────────────

    @Test
    fun insert_returnsPositiveId() = runTest {
        val id = dao.insert(TestFixtures.buildEntity())
        assertTrue(id > 0)
    }

    @Test
    fun getById_returnsInsertedEntity() = runTest {
        val entity = TestFixtures.buildEntity(caption = "hello", date = "2024-03-01")
        val id = dao.insert(entity)
        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals("hello", result!!.caption)
        assertEquals("2024-03-01", result.date)
    }

    @Test
    fun getById_returnsNull_forMissingId() = runTest {
        assertNull(dao.getById(999L))
    }

    // ── getAllSnapshots ordering ───────────────────────────────────────────────

    @Test
    fun getAllSnapshots_orderedByDateDescThenCreatedAtAsc() = runTest {
        dao.insert(TestFixtures.buildEntity(date = "2024-01-01", createdAt = 100))
        dao.insert(TestFixtures.buildEntity(date = "2024-01-02", createdAt = 200))
        dao.insert(TestFixtures.buildEntity(date = "2024-01-02", createdAt = 100))

        dao.getAllSnapshots().test {
            val items = awaitItem()
            assertEquals(3, items.size)
            // date DESC: both 2024-01-02 first
            assertEquals("2024-01-02", items[0].date)
            assertEquals("2024-01-02", items[1].date)
            // createdAt ASC within same date: 100 before 200
            assertTrue(items[0].createdAt < items[1].createdAt)
            assertEquals("2024-01-01", items[2].date)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllSnapshots_emptyList_whenNoEntries() = runTest {
        dao.getAllSnapshots().test {
            assertEquals(emptyList<SnapshotEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getSnapshotsByDate ────────────────────────────────────────────────────

    @Test
    fun getSnapshotsByDate_returnsOnlyMatchingDate() = runTest {
        dao.insert(TestFixtures.buildEntity(date = "2024-01-01"))
        dao.insert(TestFixtures.buildEntity(date = "2024-01-01"))
        dao.insert(TestFixtures.buildEntity(date = "2024-01-02"))

        dao.getSnapshotsByDate("2024-01-01").test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertTrue(items.all { it.date == "2024-01-01" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getSnapshotsByDate_orderedByCreatedAtAsc() = runTest {
        dao.insert(TestFixtures.buildEntity(date = "2024-01-01", createdAt = 200))
        dao.insert(TestFixtures.buildEntity(date = "2024-01-01", createdAt = 100))

        dao.getSnapshotsByDate("2024-01-01").test {
            val items = awaitItem()
            assertEquals(100L, items[0].createdAt)
            assertEquals(200L, items[1].createdAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getSnapshotsByDate_returnsEmptyList_forUnknownDate() = runTest {
        dao.getSnapshotsByDate("1970-01-01").test {
            assertEquals(emptyList<SnapshotEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getLatestSnapshot ─────────────────────────────────────────────────────

    @Test
    fun getLatestSnapshot_returnsEntityWithHighestCreatedAt() = runTest {
        dao.insert(TestFixtures.buildEntity(createdAt = 1000))
        dao.insert(TestFixtures.buildEntity(createdAt = 2000))

        dao.getLatestSnapshot().test {
            val item = awaitItem()
            assertNotNull(item)
            assertEquals(2000L, item!!.createdAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getLatestSnapshot_emitsNull_onEmptyDatabase() = runTest {
        dao.getLatestSnapshot().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getAllDatesWithEntries ─────────────────────────────────────────────────

    @Test
    fun getAllDatesWithEntries_returnsDistinctDates() = runTest {
        dao.insert(TestFixtures.buildEntity(date = "2024-01-01"))
        dao.insert(TestFixtures.buildEntity(date = "2024-01-01"))
        dao.insert(TestFixtures.buildEntity(date = "2024-01-02"))

        dao.getAllDatesWithEntries().test {
            val dates = awaitItem()
            assertEquals(2, dates.size)
            assertTrue(dates.containsAll(listOf("2024-01-01", "2024-01-02")))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllDatesWithEntries_returnsEmptyList_whenNoEntries() = runTest {
        dao.getAllDatesWithEntries().test {
            assertEquals(emptyList<String>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    fun update_persistsChangedFields() = runTest {
        val id = dao.insert(TestFixtures.buildEntity(caption = "original", updatedAt = 0L))
        val entity = dao.getById(id)!!
        dao.update(entity.copy(caption = "changed", updatedAt = 9999L))

        val updated = dao.getById(id)
        assertEquals("changed", updated!!.caption)
        assertEquals(9999L, updated.updatedAt)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    fun delete_removesEntityFromDatabase() = runTest {
        val id = dao.insert(TestFixtures.buildEntity())
        val entity = dao.getById(id)!!
        dao.delete(entity)
        assertNull(dao.getById(id))
    }

    @Test
    fun delete_doesNotAffectOtherEntities() = runTest {
        val id1 = dao.insert(TestFixtures.buildEntity(caption = "keep"))
        val id2 = dao.insert(TestFixtures.buildEntity(caption = "delete"))
        dao.delete(dao.getById(id2)!!)

        dao.getAllSnapshots().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals(id1, items[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── deleteAll ─────────────────────────────────────────────────────────────

    @Test
    fun deleteAll_removesAllRows() = runTest {
        dao.insert(TestFixtures.buildEntity())
        dao.insert(TestFixtures.buildEntity())
        dao.deleteAll()

        dao.getAllSnapshots().test {
            assertEquals(emptyList<SnapshotEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getSnapshotsInRange ───────────────────────────────────────────────────

    @Test
    fun getSnapshotsInRange_returnsEntitiesBetweenDatesInclusive() = runTest {
        dao.insert(TestFixtures.buildEntity(date = "2024-01-01"))
        dao.insert(TestFixtures.buildEntity(date = "2024-01-15"))
        dao.insert(TestFixtures.buildEntity(date = "2024-02-01"))

        dao.getSnapshotsInRange("2024-01-01", "2024-01-31").test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertTrue(items.all { it.date <= "2024-01-31" })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
