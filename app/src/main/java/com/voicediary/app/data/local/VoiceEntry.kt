package com.voicediary.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_entries")
data class VoiceEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "raw_transcript")
    val rawTranscript: String,

    @ColumnInfo(name = "corrected_text")
    val correctedText: String,

    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "review_status", defaultValue = "unreviewed")
    val reviewStatus: String = "unreviewed"
)
