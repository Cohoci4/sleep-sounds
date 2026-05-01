package com.sleepsounds.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.delay

/**
 * A subtle "breathing" linear gradient that slowly shifts colours, evoking the
 * feel of slow breathing for a sleep app.
 */
fun Modifier.gradientBackground(): Modifier = composed {
    val colors = MaterialTheme.colorScheme
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            t = (t + 0.005f) % 1f
        }
    }
    val frac = (kotlin.math.sin(t * Math.PI.toFloat() * 2f) + 1f) / 2f
    val brush = Brush.verticalGradient(
        listOf(
            colors.background,
            colors.surface.copy(alpha = 0.85f + 0.1f * frac),
            colors.background,
        )
    )
    background(brush)
}
