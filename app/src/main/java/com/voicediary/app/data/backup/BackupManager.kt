package com.voicediary.app.data.backup

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.voicediary.app.data.local.VoiceEntry
import com.voicediary.app.data.repository.VoiceEntryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val entries: List<VoiceEntry>
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VoiceEntryRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val backupDir: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    /**
     * DB 데이터를 JSON으로 export하고 오디오 파일들을 함께 zip으로 묶어 반환.
     */
    suspend fun createBackup(): BackupResult {
        val entries = repository.getAllEntriesOnce()

        // JSON 데이터 파일 생성
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val jsonFile = File(backupDir, "backup_$timestamp.json")
        val backupData = BackupData(entries = entries)
        jsonFile.writeText(gson.toJson(backupData))

        // 관련 오디오 파일 경로 수집
        val audioFiles = entries.mapNotNull { entry ->
            val file = File(entry.audioFilePath)
            if (file.exists()) file else null
        }

        return BackupResult(
            jsonFile = jsonFile,
            audioFiles = audioFiles,
            entryCount = entries.size
        )
    }

    /**
     * JSON 백업 파일에서 데이터를 복원.
     */
    suspend fun restoreFromJson(jsonContent: String): Int {
        val backupData = gson.fromJson(jsonContent, BackupData::class.java)
        var count = 0
        for (entry in backupData.entries) {
            // ID를 0으로 설정하여 새로 insert
            repository.insert(entry.copy(id = 0))
            count++
        }
        return count
    }
}

data class BackupResult(
    val jsonFile: File,
    val audioFiles: List<File>,
    val entryCount: Int
)
