package com.voicediary.app.data.repository

import com.voicediary.app.data.local.VoiceEntry
import com.voicediary.app.data.local.VoiceEntryDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceEntryRepository @Inject constructor(
    private val voiceEntryDao: VoiceEntryDao
) {
    fun getAllEntries(): Flow<List<VoiceEntry>> =
        voiceEntryDao.getAllEntries()

    fun getEntriesByType(type: String): Flow<List<VoiceEntry>> =
        voiceEntryDao.getEntriesByType(type)

    fun getEntriesByReviewStatus(status: String): Flow<List<VoiceEntry>> =
        voiceEntryDao.getEntriesByReviewStatus(status)

    suspend fun getEntryById(id: Long): VoiceEntry? =
        voiceEntryDao.getEntryById(id)

    suspend fun insert(entry: VoiceEntry): Long =
        voiceEntryDao.insert(entry)

    suspend fun update(entry: VoiceEntry) =
        voiceEntryDao.update(entry)

    suspend fun delete(entry: VoiceEntry) =
        voiceEntryDao.delete(entry)

    fun getEntriesByDateRange(startTime: Long, endTime: Long): Flow<List<VoiceEntry>> =
        voiceEntryDao.getEntriesByDateRange(startTime, endTime)
}
