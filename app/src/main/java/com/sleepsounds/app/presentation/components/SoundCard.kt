package com.sleepsounds.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sleepsounds.app.domain.model.Sound
import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.domain.model.Tier

@Composable
fun SoundCard(
    sound: Sound,
    locked: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .then(if (compact) Modifier else Modifier.aspectRatio(1f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CoverImage(sound)
            BlurOverlayIfLocked(locked)

            if (locked) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp),
                    tint = Color.White,
                )
            } else {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                    tint = Color.White,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        )
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = sound.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                if (sound.tier != Tier.FREE) {
                    Text(
                        text = if (sound.tier == Tier.AI_GENERATED) "AI" else "PREMIUM",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CoverImage(sound: Sound) {
    val data: Any? = sound.coverUrl ?: sound.coverAsset?.let { "file:///android_asset/$it" }
    if (data != null) {
        AsyncImage(
            model = data,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                        )
                    )
                )
        )
    }
}

@Composable
private fun BoxScope.BlurOverlayIfLocked(locked: Boolean) {
    if (!locked) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    )
}

@Suppress("unused")
private fun Sound.previewSource(): String = when (val s = source) {
    is SoundSource.Asset -> s.path
    is SoundSource.Local -> s.path
    is SoundSource.Remote -> s.url
}
