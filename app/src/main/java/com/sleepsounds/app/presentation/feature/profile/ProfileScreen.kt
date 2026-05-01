package com.sleepsounds.app.presentation.feature.profile

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepsounds.app.R
import com.sleepsounds.app.data.repository.SubscriptionRepositoryImpl
import com.sleepsounds.app.domain.model.SubscriptionTier
import com.sleepsounds.app.domain.repository.DownloadQuality
import com.sleepsounds.app.presentation.components.gradientBackground
import com.sleepsounds.app.presentation.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.profile_title)) }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .gradientBackground()
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SubscriptionCard(
                        ui = ui,
                        onMonthly = {
                            val a = activity
                            if (a != null) viewModel.launchPurchase(a, SubscriptionRepositoryImpl.PRODUCT_MONTHLY)
                        },
                        onYearly = {
                            val a = activity
                            if (a != null) viewModel.launchPurchase(a, SubscriptionRepositoryImpl.PRODUCT_YEARLY)
                        },
                        onRestore = viewModel::restorePurchases,
                    )
                }
                item { ThemeSection(ui.themeMode, viewModel::setThemeMode) }
                item { TimerSection(ui.timerMinutes, viewModel::setTimerMinutes) }
                item { QualitySection(ui.quality, viewModel::setDownloadQuality) }
                if (ui.history.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.profile_history),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    items(ui.history, key = { it.id }) { gen ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(gen.sound.title, style = MaterialTheme.typography.titleMedium)
                                Text(gen.prompt, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = viewModel::signOut) {
                        Text(stringResource(R.string.profile_sign_out))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    ui: ProfileUiState,
    onMonthly: () -> Unit,
    onYearly: () -> Unit,
    onRestore: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.profile_subscription),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                if (ui.subscription.tier == SubscriptionTier.FREE)
                    stringResource(R.string.profile_subscription_free)
                else stringResource(R.string.profile_subscription_active),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (ui.subscription.tier == SubscriptionTier.FREE) {
                Button(onClick = onMonthly) { Text(stringResource(R.string.subscribe_monthly)) }
                OutlinedButton(onClick = onYearly) { Text(stringResource(R.string.subscribe_yearly)) }
            }
            OutlinedButton(onClick = onRestore) { Text(stringResource(R.string.profile_restore_purchases)) }
        }
    }
}

@Composable
private fun ThemeSection(current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    SectionCard(title = stringResource(R.string.profile_theme)) {
        ChipsRow(
            options = ThemeMode.entries.toList(),
            current = current,
            label = { it.localizedLabel() },
            onSelect = onChange,
        )
    }
}

@Composable
private fun ThemeMode.localizedLabel(): String = when (this) {
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
}

@Composable
private fun TimerSection(currentMinutes: Int, onChange: (Int) -> Unit) {
    SectionCard(title = stringResource(R.string.profile_default_timer)) {
        ChipsRow(
            options = listOf(0, 10, 20, 30, 60),
            current = currentMinutes,
            label = { if (it == 0) "Off" else "${it}m" },
            onSelect = onChange,
        )
    }
}

@Composable
private fun QualitySection(current: DownloadQuality, onChange: (DownloadQuality) -> Unit) {
    SectionCard(title = stringResource(R.string.profile_quality)) {
        ChipsRow(
            options = DownloadQuality.entries.toList(),
            current = current,
            label = {
                when (it) {
                    DownloadQuality.HIGH -> stringResource(R.string.quality_high)
                    DownloadQuality.ECONOMY -> stringResource(R.string.quality_economy)
                }
            },
            onSelect = onChange,
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private fun android.content.Context.findActivity(): Activity? {
    var ctx: android.content.Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun <T> ChipsRow(
    options: List<T>,
    current: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { option ->
            FilterChip(
                selected = option == current,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}
