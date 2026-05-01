package com.sleepsounds.app.di

import com.sleepsounds.app.data.repository.AuthRepositoryImpl
import com.sleepsounds.app.data.repository.GenerationRepositoryImpl
import com.sleepsounds.app.data.repository.PlaybackRepositoryImpl
import com.sleepsounds.app.data.repository.SettingsRepositoryImpl
import com.sleepsounds.app.data.repository.SoundRepositoryImpl
import com.sleepsounds.app.data.repository.SubscriptionRepositoryImpl
import com.sleepsounds.app.domain.repository.AuthRepository
import com.sleepsounds.app.domain.repository.GenerationRepository
import com.sleepsounds.app.domain.repository.PlaybackRepository
import com.sleepsounds.app.domain.repository.SettingsRepository
import com.sleepsounds.app.domain.repository.SoundRepository
import com.sleepsounds.app.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindSoundRepository(impl: SoundRepositoryImpl): SoundRepository
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository
    @Binds @Singleton abstract fun bindGenerationRepository(impl: GenerationRepositoryImpl): GenerationRepository
    @Binds @Singleton abstract fun bindPlaybackRepository(impl: PlaybackRepositoryImpl): PlaybackRepository
}
