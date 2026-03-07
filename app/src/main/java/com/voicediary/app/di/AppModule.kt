package com.voicediary.app.di

import android.content.Context
import androidx.room.Room
import com.voicediary.app.data.local.VoiceDiaryDatabase
import com.voicediary.app.data.local.VoiceEntryDao
import com.voicediary.app.data.recording.AudioPlayerManager
import com.voicediary.app.data.recording.RecordingManager
import com.voicediary.app.data.recording.SpeechRecognitionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VoiceDiaryDatabase {
        return Room.databaseBuilder(
            context,
            VoiceDiaryDatabase::class.java,
            "voice_diary_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideVoiceEntryDao(database: VoiceDiaryDatabase): VoiceEntryDao {
        return database.voiceEntryDao()
    }

    @Provides
    fun provideRecordingManager(@ApplicationContext context: Context): RecordingManager {
        return RecordingManager(context)
    }

    @Provides
    fun provideSpeechRecognitionManager(@ApplicationContext context: Context): SpeechRecognitionManager {
        return SpeechRecognitionManager(context)
    }

    @Provides
    fun provideAudioPlayerManager(): AudioPlayerManager {
        return AudioPlayerManager()
    }
}
