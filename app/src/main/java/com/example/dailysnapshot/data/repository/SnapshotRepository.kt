package com.example.dailysnapshot.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.dailysnapshot.data.db.SnapshotDao
import com.example.dailysnapshot.data.db.SnapshotEntity
import com.example.dailysnapshot.data.model.Snapshot
import com.example.dailysnapshot.data.model.toDomain
import com.example.dailysnapshot.data.model.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepository @Inject constructor(
    private val dao: SnapshotDao,
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
        val rawUuid = UUID.randomUUID().toString()
        val framedUuid = UUID.randomUUID().toString()

        val rawFile = rawFile(rawUuid)
        val framedFile = framedFile(framedUuid)

        rawFile.parentFile?.mkdirs()
        rawFile.outputStream().use { rawBitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }

        // TODO DAI-12: replace with ImageProcessor.compositePolaroid(rawBitmap, caption, filter, framedFile)
        framedFile.parentFile?.mkdirs()
        framedFile.outputStream().use { rawBitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }

        val now = System.currentTimeMillis()
        val entity = SnapshotEntity(
            date = date,
            imagePath = framedFile.absolutePath,
            rawImagePath = rawFile.absolutePath,
            caption = caption,
            filterApplied = filter,
            createdAt = now,
            updatedAt = now
        )
        val id = dao.insert(entity)

        // TODO DAI-33: GlanceAppWidgetManager.getInstance(context).updateAll(SnapshotWidget())

        return entity.copy(id = id).toDomain()
    }

    suspend fun updateSnapshot(
        id: Long,
        caption: String? = null,
        filter: String? = null,
        newRawBitmap: Bitmap? = null
    ) {
        val entity = dao.getById(id) ?: return
        val now = System.currentTimeMillis()

        val updatedRawPath = if (newRawBitmap != null) {
            entity.rawImagePath?.let { File(it).delete() }
            val rawUuid = UUID.randomUUID().toString()
            val rawFile = rawFile(rawUuid)
            rawFile.parentFile?.mkdirs()
            rawFile.outputStream().use { newRawBitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            rawFile.absolutePath
        } else {
            entity.rawImagePath
        }

        // TODO DAI-12: regenerate framed image via ImageProcessor when caption/filter/photo changes

        dao.update(
            entity.copy(
                rawImagePath = updatedRawPath,
                caption = caption ?: entity.caption,
                filterApplied = filter ?: entity.filterApplied,
                updatedAt = now
            )
        )

        // TODO DAI-33: GlanceAppWidgetManager.getInstance(context).updateAll(SnapshotWidget())
    }

    suspend fun getSnapshotById(id: Long): Snapshot? =
        dao.getById(id)?.toDomain()

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
