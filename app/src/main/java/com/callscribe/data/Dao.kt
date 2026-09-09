package com.callscribe.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Insert
    suspend fun insert(capture: Capture): Long

    @Update
    suspend fun update(capture: Capture)

    @Delete
    suspend fun delete(capture: Capture)

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun byId(id: Long): Capture?

    @Query("SELECT * FROM captures ORDER BY startedAt DESC LIMIT 500")
    fun observeAll(): Flow<List<Capture>>

    @Query("SELECT * FROM captures WHERE status IN ('NEW','TRANSCRIBING','ANALYZING') ORDER BY startedAt ASC")
    suspend fun pending(): List<Capture>

    @Query("SELECT COUNT(*) FROM captures WHERE kind = 'SMS' AND phone = :phone AND text = :text AND ABS(startedAt - :at) < 60000")
    suspend fun countSimilarSms(phone: String?, text: String?, at: Long): Int
}

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskRow): Long

    @Update
    suspend fun update(task: TaskRow)

    @Delete
    suspend fun delete(task: TaskRow)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun byId(id: Long): TaskRow?

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TaskRow>>

    @Query("SELECT * FROM tasks WHERE status = 'OPEN' AND reminderAt IS NOT NULL")
    suspend fun withPendingReminders(): List<TaskRow>

    @Query("SELECT * FROM tasks WHERE captureId = :captureId")
    suspend fun byCapture(captureId: Long): List<TaskRow>
}
