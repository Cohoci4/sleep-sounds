package com.sleepsounds.app.di

import android.content.Context
import androidx.room.Room
import com.sleepsounds.app.data.local.db.SleepSoundsDatabase
import com.sleepsounds.app.data.local.db.dao.FavoriteDao
import com.sleepsounds.app.data.local.db.dao.GenerationDao
import com.sleepsounds.app.data.local.db.dao.SoundDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SleepSoundsDatabase =
        Room.databaseBuilder(context, SleepSoundsDatabase::class.java, SleepSoundsDatabase.NAME)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideSoundDao(db: SleepSoundsDatabase): SoundDao = db.soundDao()
    @Provides fun provideFavoriteDao(db: SleepSoundsDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideGenerationDao(db: SleepSoundsDatabase): GenerationDao = db.generationDao()
}
