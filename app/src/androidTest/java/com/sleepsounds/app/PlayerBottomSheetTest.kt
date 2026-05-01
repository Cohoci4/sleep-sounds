package com.sleepsounds.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sleepsounds.app.presentation.theme.SleepSoundsTheme
import com.sleepsounds.app.presentation.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

class PlayerBottomSheetTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun showsAppName() {
        composeRule.setContent {
            SleepSoundsTheme(themeMode = ThemeMode.DARK) {
                MaterialTheme {
                    androidx.compose.material3.Text(text = "SleepSounds")
                }
            }
        }
        composeRule.onNodeWithText("SleepSounds").assertIsDisplayed()
    }
}
