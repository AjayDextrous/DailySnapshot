package com.example.dailysnapshot.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {

    @Query("SELECT * FROM snapshot_entries ORDER BY date DESC, createdAt ASC")
    fun getAllSnapshots(): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshot_entries WHERE date = :date ORDER BY createdAt ASC")
    fun getSnapshotsByDate(date: String): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshot_entries WHERE date BETWEEN :from AND :to ORDER BY date ASC, createdAt ASC")
    fun getSnapshotsInRange(from: String, to: String): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshot_entries ORDER BY createdAt DESC LIMIT 1")
    fun getLatestSnapshot(): Flow<SnapshotEntity?>

    @Query("SELECT DISTINCT date FROM snapshot_entries")
    fun getAllDatesWithEntries(): Flow<List<String>>

    @Query("SELECT * FROM snapshot_entries WHERE id = :id")
    suspend fun getById(id: Long): SnapshotEntity?

    @Query("DELETE FROM snapshot_entries")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(entry: SnapshotEntity): Long

    @Update
    suspend fun update(entry: SnapshotEntity)

    @Delete
    suspend fun delete(entry: SnapshotEntity)
}
