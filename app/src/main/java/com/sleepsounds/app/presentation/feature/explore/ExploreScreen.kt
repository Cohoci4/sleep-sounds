package com.sleepsounds.app.presentation.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepsounds.app.R
import com.sleepsounds.app.domain.model.Category
import com.sleepsounds.app.domain.repository.SubscriptionProduct
import com.sleepsounds.app.presentation.components.SoundCard
import com.sleepsounds.app.presentation.components.gradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(viewModel: ExploreViewModel = hiltViewModel()) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.explore_title)) }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .gradientBackground()
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { ProductsBanner(ui.products) }
                items(Category.entries.toList()) { cat ->
                    val sounds = ui.byCategory[cat].orEmpty()
                    if (sounds.isEmpty()) return@items
                    CategorySection(
                        category = cat,
                        sounds = sounds,
                        onClick = viewModel::onSoundClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    category: Category,
    sounds: List<com.sleepsounds.app.domain.usecase.DisplayableSound>,
    onClick: (com.sleepsounds.app.domain.model.Sound) -> Unit,
) {
    Text(
        text = category.localized(),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sounds, key = { it.sound.id }) { displayable ->
            SoundCard(
                sound = displayable.sound,
                locked = displayable.locked,
                favorite = displayable.favorite,
                onClick = { onClick(displayable.sound) },
                onFavoriteClick = {},
                modifier = Modifier.fillMaxWidth(0.5f),
                compact = true,
            )
        }
    }
}

@Composable
private fun ProductsBanner(products: List<SubscriptionProduct>) {
    if (products.isEmpty()) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.subscribe_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            products.forEach { product ->
                Text(
                    text = "${product.title} – ${product.formattedPrice}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun Category.localized(): String = when (this) {
    Category.NATURE -> stringResource(R.string.category_nature)
    Category.SPACE -> stringResource(R.string.category_space)
    Category.CAFE -> stringResource(R.string.category_cafe)
    Category.MEDITATION -> stringResource(R.string.category_meditation)
    Category.AI_EXCLUSIVE -> stringResource(R.string.category_ai_exclusive)
}
