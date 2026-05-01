package com.sleepsounds.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.sleepsounds.app.audio.PlaybackController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class DefaultDispatcher
@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class MainDispatcher
@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class ApplicationScopeQualifier

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @IoDispatcher fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    @Provides @DefaultDispatcher fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    @Provides @MainDispatcher fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate

    @Provides
    @Singleton
    fun provideApplicationScope(@IoDispatcher dispatcher: CoroutineDispatcher): CoroutineScope {
        return CoroutineScope(SupervisorJob() + dispatcher)
    }

    @Provides
    @Singleton
    fun providePlaybackController(): PlaybackController = PlaybackController()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("sleep_sounds_prefs") },
        )
}
