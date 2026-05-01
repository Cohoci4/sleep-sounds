package com.sleepsounds.app.domain.repository

import com.sleepsounds.app.presentation.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    fun observeDefaultTimerMinutes(): Flow<Int>
    suspend fun setDefaultTimerMinutes(minutes: Int)

    fun observeDownloadQuality(): Flow<DownloadQuality>
    suspend fun setDownloadQuality(quality: DownloadQuality)
}

enum class DownloadQuality { ECONOMY, HIGH }
