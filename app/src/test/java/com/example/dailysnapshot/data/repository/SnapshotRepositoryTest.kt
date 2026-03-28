package com.example.dailysnapshot.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.dailysnapshot.data.db.SnapshotDao
import com.example.dailysnapshot.data.db.SnapshotEntity
import com.example.dailysnapshot.util.ImageProcessor
import com.example.dailysnapshot.util.MainDispatcherRule
import com.example.dailysnapshot.util.TestFixtures
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class SnapshotRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao: SnapshotDao = mockk(relaxed = true)
    private val imageProcessor: ImageProcessor = mockk(relaxed = true)
    private val tempDir: File = createTempDirectory("snapshot_repo_test").toFile()
    private val context: Context = mockk {
        every { filesDir } returns tempDir
    }
    private lateinit var repository: SnapshotRepository

    @Before
    fun setUp() {
        repository = SnapshotRepository(dao, imageProcessor, context)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // ── saveSnapshot ──────────────────────────────────────────────────────────

    @Test
    fun saveSnapshot_writesRawFile_underSnapshotsRawDirectory() = runTest {
        coEvery { dao.insert(any()) } returns 1L
        val bitmap = mockk<Bitmap>(relaxed = true)

        repository.saveSnapshot(bitmap, "caption", null, "2024-01-01")

        val rawDir = File(tempDir, "snapshots/raw")
        assertTrue(rawDir.exists())
        assertEquals(1, rawDir.listFiles()?.size)
        assertTrue(rawDir.listFiles()!![0].name.endsWith(".jpg"))
    }

    @Test
    fun saveSnapshot_invokesCompositePolaroid_withFramedFilePath() = runTest {
        coEvery { dao.insert(any()) } returns 1L
        val bitmap = mockk<Bitmap>(relaxed = true)
        val outputFileSlot = slot<File>()
        every { imageProcessor.compositePolaroid(any(), any(), any(), capture(outputFileSlot)) } just Runs

        repository.saveSnapshot(bitmap, "caption", null, "2024-01-01")

        val capturedPath = outputFileSlot.captured.absolutePath
        assertTrue(capturedPath.contains("snapshots/framed/"))
        assertTrue(capturedPath.endsWith("_framed.jpg"))
    }

    @Test
    fun saveSnapshot_insertsEntityIntoDao() = runTest {
        coEvery { dao.insert(any()) } returns 1L
        val bitmap = mockk<Bitmap>(relaxed = true)

        repository.saveSnapshot(bitmap, "caption", null, "2024-01-01")

        coVerify(exactly = 1) { dao.insert(any()) }
    }

    @Test
    fun saveSnapshot_returnsSnapshot_withAssignedId() = runTest {
        coEvery { dao.insert(any()) } returns 42L
        val bitmap = mockk<Bitmap>(relaxed = true)

        val result = repository.saveSnapshot(bitmap, "caption", null, "2024-01-01")

        assertEquals(42L, result.id)
    }

    @Test
    fun saveSnapshot_usesSuppliedDateAndCaption() = runTest {
        val entitySlot = slot<SnapshotEntity>()
        coEvery { dao.insert(capture(entitySlot)) } returns 1L
        val bitmap = mockk<Bitmap>(relaxed = true)

        repository.saveSnapshot(bitmap, "my caption", "sepia", "2024-06-15")

        assertEquals("2024-06-15", entitySlot.captured.date)
        assertEquals("my caption", entitySlot.captured.caption)
        assertEquals("sepia", entitySlot.captured.filterApplied)
    }

    // ── deleteSnapshot ────────────────────────────────────────────────────────

    @Test
    fun deleteSnapshot_deletesFramedFile() = runTest {
        val framedFile = File(tempDir, "framed.jpg").also { it.createNewFile() }
        val rawFile = File(tempDir, "raw.jpg").also { it.createNewFile() }
        val snapshot = TestFixtures.buildSnapshot(
            imagePath = framedFile.absolutePath,
            rawImagePath = rawFile.absolutePath
        )

        repository.deleteSnapshot(snapshot)

        assertFalse(framedFile.exists())
    }

    @Test
    fun deleteSnapshot_deletesRawFile() = runTest {
        val framedFile = File(tempDir, "framed.jpg").also { it.createNewFile() }
        val rawFile = File(tempDir, "raw.jpg").also { it.createNewFile() }
        val snapshot = TestFixtures.buildSnapshot(
            imagePath = framedFile.absolutePath,
            rawImagePath = rawFile.absolutePath
        )

        repository.deleteSnapshot(snapshot)

        assertFalse(rawFile.exists())
    }

    @Test
    fun deleteSnapshot_callsDaoDelete() = runTest {
        val snapshot = TestFixtures.buildSnapshot(
            imagePath = File(tempDir, "framed.jpg").absolutePath,
            rawImagePath = File(tempDir, "raw.jpg").absolutePath
        )

        repository.deleteSnapshot(snapshot)

        coVerify(exactly = 1) { dao.delete(any()) }
    }

    @Test
    fun deleteSnapshot_doesNotThrow_whenRawImagePathIsNull() = runTest {
        val framedFile = File(tempDir, "framed.jpg").also { it.createNewFile() }
        val snapshot = TestFixtures.buildSnapshot(
            imagePath = framedFile.absolutePath,
            rawImagePath = null
        )

        repository.deleteSnapshot(snapshot) // must not throw
    }

    // ── updateSnapshot ────────────────────────────────────────────────────────

    @Test
    fun updateSnapshot_updatesUpdatedAt_toCurrentTime() = runTest {
        val originalEntity = SnapshotEntity(
            id = 1L, date = "2024-01-01",
            imagePath = File(tempDir, "framed.jpg").also { it.createNewFile() }.absolutePath,
            rawImagePath = null, caption = "", filterApplied = null,
            createdAt = 0L, updatedAt = 0L
        )
        coEvery { dao.getById(1L) } returns originalEntity
        val entitySlot = slot<SnapshotEntity>()
        coEvery { dao.update(capture(entitySlot)) } just Runs

        val timeBefore = System.currentTimeMillis()
        repository.updateSnapshot(1L, "new caption", null)

        assertTrue(entitySlot.captured.updatedAt >= timeBefore)
    }

    @Test
    fun updateSnapshot_updatesCaption_andFilter() = runTest {
        val originalEntity = SnapshotEntity(
            id = 1L, date = "2024-01-01",
            imagePath = File(tempDir, "framed.jpg").also { it.createNewFile() }.absolutePath,
            rawImagePath = null, caption = "old", filterApplied = null,
            createdAt = 0L, updatedAt = 0L
        )
        coEvery { dao.getById(1L) } returns originalEntity
        val entitySlot = slot<SnapshotEntity>()
        coEvery { dao.update(capture(entitySlot)) } just Runs

        repository.updateSnapshot(1L, "new caption", "sepia")

        assertEquals("new caption", entitySlot.captured.caption)
        assertEquals("sepia", entitySlot.captured.filterApplied)
    }

    @Test
    fun updateSnapshot_doesNothing_forUnknownId() = runTest {
        coEvery { dao.getById(99L) } returns null

        repository.updateSnapshot(99L, "caption", null)

        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun updateSnapshot_keepsExistingRawPath_whenBitmapIsNull() = runTest {
        // rawImagePath = null avoids BitmapFactory.decodeFile (Android-only) while still
        // verifying that passing newRawBitmap = null leaves the raw path unchanged.
        val originalEntity = SnapshotEntity(
            id = 1L, date = "2024-01-01",
            imagePath = File(tempDir, "framed.jpg").also { it.createNewFile() }.absolutePath,
            rawImagePath = null,
            caption = "", filterApplied = null,
            createdAt = 0L, updatedAt = 0L
        )
        coEvery { dao.getById(1L) } returns originalEntity
        val entitySlot = slot<SnapshotEntity>()
        coEvery { dao.update(capture(entitySlot)) } just Runs

        repository.updateSnapshot(1L, "caption", null, newRawBitmap = null)

        assertEquals(null, entitySlot.captured.rawImagePath)
    }

    // ── softDeleteSnapshot / undoDeleteSnapshot / deleteSnapshotFiles ─────────

    @Test
    fun softDeleteSnapshot_callsDaoDeleteOnly_noFileDeletion() = runTest {
        val framedFile = File(tempDir, "framed.jpg").also { it.createNewFile() }
        val rawFile = File(tempDir, "raw.jpg").also { it.createNewFile() }
        val snapshot = TestFixtures.buildSnapshot(
            imagePath = framedFile.absolutePath,
            rawImagePath = rawFile.absolutePath
        )

        repository.softDeleteSnapshot(snapshot)

        coVerify(exactly = 1) { dao.delete(any()) }
        assertTrue("framed file should still exist", framedFile.exists())
        assertTrue("raw file should still exist", rawFile.exists())
    }

    @Test
    fun undoDeleteSnapshot_callsDaoInsert() = runTest {
        val snapshot = TestFixtures.buildSnapshot(id = 7L)

        repository.undoDeleteSnapshot(snapshot)

        val entitySlot = slot<SnapshotEntity>()
        coVerify(exactly = 1) { dao.insert(any()) }
    }

    @Test
    fun deleteSnapshotFiles_deletesFiles_withoutCallingDao() = runTest {
        val framedFile = File(tempDir, "framed.jpg").also { it.createNewFile() }
        val rawFile = File(tempDir, "raw.jpg").also { it.createNewFile() }
        val snapshot = TestFixtures.buildSnapshot(
            imagePath = framedFile.absolutePath,
            rawImagePath = rawFile.absolutePath
        )

        repository.deleteSnapshotFiles(snapshot)

        assertFalse(framedFile.exists())
        assertFalse(rawFile.exists())
        coVerify(exactly = 0) { dao.delete(any()) }
    }
}
