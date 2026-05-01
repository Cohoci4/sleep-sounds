package com.sleepsounds.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sleepsounds.app.domain.repository.DownloadQuality
import com.sleepsounds.app.domain.repository.SettingsRepository
import com.sleepsounds.app.presentation.theme.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeThemeMode(): Flow<ThemeMode> = dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[KEY_THEME] ?: ThemeMode.DARK.name) }
            .getOrDefault(ThemeMode.DARK)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    override fun observeDefaultTimerMinutes(): Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_TIMER] ?: 30
    }

    override suspend fun setDefaultTimerMinutes(minutes: Int) {
        dataStore.edit { it[KEY_TIMER] = minutes }
    }

    override fun observeDownloadQuality(): Flow<DownloadQuality> = dataStore.data.map { prefs ->
        runCatching { DownloadQuality.valueOf(prefs[KEY_QUALITY] ?: DownloadQuality.HIGH.name) }
            .getOrDefault(DownloadQuality.HIGH)
    }

    override suspend fun setDownloadQuality(quality: DownloadQuality) {
        dataStore.edit { it[KEY_QUALITY] = quality.name }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_TIMER = intPreferencesKey("default_timer_minutes")
        val KEY_QUALITY = stringPreferencesKey("download_quality")
    }
}
