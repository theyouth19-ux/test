package com.voicediary.app.data.repository

import com.voicediary.app.data.local.DiaryDao
import com.voicediary.app.data.local.DiaryEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao
) {
    fun getAllEntries(): Flow<List<DiaryEntry>> = diaryDao.getAllEntries()

    suspend fun getEntryById(id: Long): DiaryEntry? = diaryDao.getEntryById(id)

    suspend fun insertEntry(entry: DiaryEntry): Long = diaryDao.insertEntry(entry)

    suspend fun updateEntry(entry: DiaryEntry) = diaryDao.updateEntry(entry)

    suspend fun deleteEntry(entry: DiaryEntry) = diaryDao.deleteEntry(entry)
}
