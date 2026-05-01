package com.sleepsounds.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.sleepsounds.app.domain.repository.DownloadQuality
import com.sleepsounds.app.presentation.theme.ThemeMode
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsRepositoryImplTest {

    private lateinit var tempFile: File
    private lateinit var repo: SettingsRepositoryImpl

    @BeforeEach
    fun setUp() {
        tempFile = Files.createTempFile("settings", ".preferences_pb").toFile()
        tempFile.delete()
        val store = PreferenceDataStoreFactory.create(produceFile = { tempFile })
        repo = SettingsRepositoryImpl(store)
    }

    @Test
    fun `default theme is dark`() = runTest {
        repo.observeThemeMode().test {
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    @Test
    fun `setThemeMode persists`() = runTest {
        repo.setThemeMode(ThemeMode.LIGHT)
        repo.observeThemeMode().test {
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
    }

    @Test
    fun `download quality round-trip`() = runTest {
        repo.setDownloadQuality(DownloadQuality.ECONOMY)
        repo.observeDownloadQuality().test {
            assertEquals(DownloadQuality.ECONOMY, awaitItem())
        }
    }

    @Test
    fun `default timer is 30 minutes`() = runTest {
        repo.observeDefaultTimerMinutes().test {
            assertEquals(30, awaitItem())
        }
    }
}
