package com.voicediary.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VoiceEntry::class],
    version = 1,
    exportSchema = false
)
abstract class VoiceDiaryDatabase : RoomDatabase() {
    abstract fun voiceEntryDao(): VoiceEntryDao
}
