package com.voicediary.app.di

import android.content.Context
import androidx.room.Room
import com.voicediary.app.data.local.DiaryDao
import com.voicediary.app.data.local.VoiceDiaryDatabase
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
    fun provideDiaryDao(database: VoiceDiaryDatabase): DiaryDao {
        return database.diaryDao()
    }
}
