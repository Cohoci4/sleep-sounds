package com.sleepsounds.app.presentation.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sleepsounds.app.domain.model.ActiveTrack
import com.sleepsounds.app.domain.model.SleepTimer
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun PlayerBottomSheet(viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AnimatedVisibility(
        visible = state.playback.activeTracks.isNotEmpty(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        PlayerCard(
            tracks = state.playback.activeTracks,
            isPlaying = state.playback.isPlaying,
            timer = state.playback.timer,
            onPlayPause = viewModel::togglePlayPause,
            onStop = viewModel::stop,
            onVolumeChange = viewModel::setVolume,
            onTimer = viewModel::setTimer,
        )
    }
}

@Composable
private fun PlayerCard(
    tracks: List<ActiveTrack>,
    isPlaying: Boolean,
    timer: SleepTimer,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onVolumeChange: (String, Float) -> Unit,
    onTimer: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoverHeader(
                track = tracks.firstOrNull(),
                isPlaying = isPlaying,
            )
            tracks.forEach { track ->
                TrackVolumeRow(track = track, onVolumeChange = onVolumeChange)
            }
            ControlsRow(
                isPlaying = isPlaying,
                onPlayPause = onPlayPause,
                onStop = onStop,
            )
            TimerRow(timer = timer, onTimer = onTimer)
        }
    }
}

@Composable
private fun CoverHeader(track: ActiveTrack?, isPlaying: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            val data: Any? = track?.sound?.coverUrl ?: track?.sound?.coverAsset?.let {
                "file:///android_asset/$it"
            }
            if (data != null) {
                AsyncImage(
                    model = data,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track?.sound?.title ?: "—",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SoundWave(isAnimating = isPlaying)
        }
    }
}

@Composable
private fun TrackVolumeRow(
    track: ActiveTrack,
    onVolumeChange: (String, Float) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                track.sound.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(110.dp),
            )
            Slider(
                value = track.volume,
                onValueChange = { onVolumeChange(track.sound.id, it) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ControlsRow(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(onClick = onPlayPause) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
            )
        }
        IconButton(onClick = onStop) {
            Icon(Icons.Filled.Close, contentDescription = null)
        }
    }
}

@Composable
private fun TimerRow(timer: SleepTimer, onTimer: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.Timer, contentDescription = null)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(listOf(0, 10, 20, 30, 60)) { minutes ->
                val selected = when (timer) {
                    SleepTimer.Disabled -> minutes == 0
                    is SleepTimer.Active -> minutes * 60_000L == timer.totalMs
                }
                FilterChip(
                    selected = selected,
                    onClick = { onTimer(minutes) },
                    label = { Text(if (minutes == 0) "Off" else "${minutes}m") },
                )
            }
        }
    }
}

@Composable
private fun SoundWave(isAnimating: Boolean) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isAnimating) {
        while (isAnimating) {
            delay(32)
            phase += 0.18f
        }
    }
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val midY = size.height / 2f
        val step = 4f
        var x = 0f
        var prev: Offset? = null
        while (x <= size.width) {
            val y = midY + sin(x / 18f + phase) * (if (isAnimating) midY * 0.7f else midY * 0.05f)
            val current = Offset(x, y)
            val previous = prev
            if (previous != null) {
                drawLine(color = color, start = previous, end = current, strokeWidth = 3f)
            }
            prev = current
            x += step
        }
    }
}
