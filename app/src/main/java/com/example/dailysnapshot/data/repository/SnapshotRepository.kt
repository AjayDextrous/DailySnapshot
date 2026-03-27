package com.example.dailysnapshot.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.dailysnapshot.data.db.SnapshotDao
import com.example.dailysnapshot.data.db.SnapshotEntity
import com.example.dailysnapshot.data.model.Snapshot
import com.example.dailysnapshot.data.model.toDomain
import com.example.dailysnapshot.data.model.toEntity
import com.example.dailysnapshot.util.ImageProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepository @Inject constructor(
    private val dao: SnapshotDao,
    private val imageProcessor: ImageProcessor,
    @ApplicationContext private val context: Context
) {

    fun getAllSnapshots(): Flow<List<Snapshot>> =
        dao.getAllSnapshots().map { it.map(SnapshotEntity::toDomain) }

    fun getSnapshotsByDate(date: String): Flow<List<Snapshot>> =
        dao.getSnapshotsByDate(date).map { it.map(SnapshotEntity::toDomain) }

    fun getSnapshotsInRange(from: String, to: String): Flow<List<Snapshot>> =
        dao.getSnapshotsInRange(from, to).map { it.map(SnapshotEntity::toDomain) }

    fun getLatestSnapshot(): Flow<Snapshot?> =
        dao.getLatestSnapshot().map { it?.toDomain() }

    fun getAllDatesWithEntries(): Flow<List<String>> =
        dao.getAllDatesWithEntries()

    suspend fun saveSnapshot(
        rawBitmap: Bitmap,
        caption: String,
        filter: String?,
        date: String
    ): Snapshot {
        val rawUuid    = UUID.randomUUID().toString()
        val framedUuid = UUID.randomUUID().toString()

        val rawFile    = rawFile(rawUuid)
        val framedFile = framedFile(framedUuid)

        rawFile.parentFile?.mkdirs()
        rawFile.outputStream().use { rawBitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }

        imageProcessor.compositePolaroid(rawBitmap, caption, filter, framedFile)

        val now = System.currentTimeMillis()
        val entity = SnapshotEntity(
            date          = date,
            imagePath     = framedFile.absolutePath,
            rawImagePath  = rawFile.absolutePath,
            caption       = caption,
            filterApplied = filter,
            createdAt     = now,
            updatedAt     = now
        )
        val id = dao.insert(entity)

        // TODO DAI-33: GlanceAppWidgetManager.getInstance(context).updateAll(SnapshotWidget())

        return entity.copy(id = id).toDomain()
    }

    /**
     * Updates an existing snapshot's caption, filter, and optionally its raw photo.
     *
     * [caption] and [filter] always reflect the user's current intent:
     *  - [filter] = null means no filter (not "leave unchanged").
     *
     * The framed image is regenerated whenever any of these values change.
     */
    suspend fun updateSnapshot(
        id: Long,
        caption: String,
        filter: String?,
        newRawBitmap: Bitmap? = null
    ) {
        val entity = dao.getById(id) ?: return
        val now = System.currentTimeMillis()

        // Persist new raw file if the photo was replaced
        val updatedRawPath = if (newRawBitmap != null) {
            entity.rawImagePath?.let { File(it).delete() }
            val rawFile = rawFile(UUID.randomUUID().toString())
            rawFile.parentFile?.mkdirs()
            rawFile.outputStream().use { newRawBitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            rawFile.absolutePath
        } else {
            entity.rawImagePath
        }

        // Regenerate framed image in-place (overwrite existing imagePath)
        val sourceBitmap = newRawBitmap
            ?: updatedRawPath?.let { BitmapFactory.decodeFile(it) }
        if (sourceBitmap != null) {
            imageProcessor.compositePolaroid(
                rawBitmap  = sourceBitmap,
                caption    = caption,
                filterId   = filter,
                outputFile = File(entity.imagePath)
            )
            if (sourceBitmap !== newRawBitmap) sourceBitmap.recycle()
        }

        dao.update(
            entity.copy(
                rawImagePath  = updatedRawPath,
                caption       = caption,
                filterApplied = filter,
                updatedAt     = now
            )
        )

        // TODO DAI-33: GlanceAppWidgetManager.getInstance(context).updateAll(SnapshotWidget())
    }

    suspend fun getSnapshotById(id: Long): Snapshot? =
        dao.getById(id)?.toDomain()

    /** Debug only — deletes all DB rows and their associated image files. */
    suspend fun deleteAllSnapshots() {
        dao.getAllSnapshots()
            .map { it.map(SnapshotEntity::toDomain) }
            .first()
            .forEach { snapshot ->
                File(snapshot.imagePath).delete()
                snapshot.rawImagePath?.let { File(it).delete() }
            }
        dao.deleteAll()
    }

    suspend fun deleteSnapshot(snapshot: Snapshot) {
        File(snapshot.imagePath).delete()
        snapshot.rawImagePath?.let { File(it).delete() }
        dao.delete(snapshot.toEntity())
    }

    private fun rawFile(uuid: String) =
        File(context.filesDir, "snapshots/raw/$uuid.jpg")

    private fun framedFile(uuid: String) =
        File(context.filesDir, "snapshots/framed/${uuid}_framed.jpg")
}
