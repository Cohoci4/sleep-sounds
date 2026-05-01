package com.sleepsounds.app.presentation.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepsounds.app.R
import com.sleepsounds.app.presentation.components.SoundCard
import com.sleepsounds.app.presentation.components.gradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToGenerate: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.RequiresSubscription ->
                    snackbar.showSnackbar(
                        message = "Sleep Premium required to play this sound."
                    )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToGenerate,
                icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
                text = { Text(stringResource(R.string.action_create_sound)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .gradientBackground()
            .padding(padding)) {
            HomeContent(
                ui = ui,
                onSoundClick = viewModel::onSoundClick,
                onFavoriteClick = viewModel::onFavoriteClick,
            )
        }
    }
}

@Composable
private fun HomeContent(
    ui: HomeUiState,
    onSoundClick: (com.sleepsounds.app.domain.model.Sound) -> Unit,
    onFavoriteClick: (com.sleepsounds.app.domain.model.Sound, Boolean) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            FeaturedSection(ui.featured, onSoundClick)
        }
        items(ui.grid, key = { it.sound.id }) { displayable ->
            SoundCard(
                sound = displayable.sound,
                locked = displayable.locked,
                favorite = displayable.favorite,
                onClick = { onSoundClick(displayable.sound) },
                onFavoriteClick = { onFavoriteClick(displayable.sound, displayable.favorite) },
            )
        }
    }
}

@Composable
private fun FeaturedSection(
    featured: List<com.sleepsounds.app.domain.usecase.DisplayableSound>,
    onClick: (com.sleepsounds.app.domain.model.Sound) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.home_featured),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (featured.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(20.dp),
                    )
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(featured, key = { it.sound.id }) { displayable ->
                    Row {
                        Spacer(modifier = Modifier.width(0.dp))
                        SoundCard(
                            sound = displayable.sound,
                            locked = displayable.locked,
                            favorite = displayable.favorite,
                            onClick = { onClick(displayable.sound) },
                            onFavoriteClick = { },
                            modifier = Modifier.size(180.dp),
                            compact = true,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
