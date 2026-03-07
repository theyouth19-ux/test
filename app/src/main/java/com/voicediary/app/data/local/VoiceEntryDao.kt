package com.voicediary.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceEntryDao {

    @Query("SELECT * FROM voice_entries ORDER BY created_at DESC")
    fun getAllEntries(): Flow<List<VoiceEntry>>

    @Query("SELECT * FROM voice_entries WHERE type = :type ORDER BY created_at DESC")
    fun getEntriesByType(type: String): Flow<List<VoiceEntry>>

    @Query("SELECT * FROM voice_entries WHERE review_status = :status ORDER BY created_at DESC")
    fun getEntriesByReviewStatus(status: String): Flow<List<VoiceEntry>>

    @Query("SELECT * FROM voice_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): VoiceEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VoiceEntry): Long

    @Update
    suspend fun update(entry: VoiceEntry)

    @Delete
    suspend fun delete(entry: VoiceEntry)

    @Query("SELECT * FROM voice_entries WHERE created_at BETWEEN :startTime AND :endTime ORDER BY created_at DESC")
    fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<VoiceEntry>>
}
