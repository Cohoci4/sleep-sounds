package com.sleepsounds.app.presentation.feature.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sleepsounds.app.R
import com.sleepsounds.app.domain.model.GenerationStage
import com.sleepsounds.app.presentation.components.gradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    onClose: () -> Unit,
    viewModel: GenerateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.generate_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .gradientBackground()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PaddingValues(16.dp)),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = viewModel::onPromptChange,
                    placeholder = { Text(stringResource(R.string.generate_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    enabled = state.stage !is GenerationStage.GeneratingAudio &&
                        state.stage !is GenerationStage.GeneratingCover &&
                        state.stage !is GenerationStage.SubmittingPrompt,
                )
                Button(
                    onClick = viewModel::onGenerateClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.prompt.isNotBlank() && state.stage.isTerminal(),
                ) {
                    Text(stringResource(R.string.generate_action))
                }
                ProgressSection(state.stage)
                Spacer(Modifier.height(8.dp))
                ResultSection(state.stage, onReset = viewModel::reset)
            }
        }
    }
}

@Composable
private fun ProgressSection(stage: GenerationStage) {
    val (label, fraction) = when (stage) {
        GenerationStage.Idle -> return
        GenerationStage.SubmittingPrompt ->
            stringResource(R.string.generate_stage_submit) to 0.1f
        GenerationStage.GeneratingCover ->
            stringResource(R.string.generate_stage_cover) to 0.35f
        GenerationStage.GeneratingAudio ->
            stringResource(R.string.generate_stage_audio) to 0.7f
        GenerationStage.Finalizing ->
            stringResource(R.string.generate_stage_finalize) to 0.95f
        is GenerationStage.Done ->
            stringResource(R.string.generate_stage_done) to 1f
        is GenerationStage.Failed ->
            (stage.cause.message ?: stringResource(R.string.generate_stage_failed)) to 0f
    }
    Text(label, style = MaterialTheme.typography.titleMedium)
    if (stage !is GenerationStage.Failed) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ResultSection(stage: GenerationStage, onReset: () -> Unit) {
    if (stage !is GenerationStage.Done) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = stage.generation.sound.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp)),
            )
            Text(stage.generation.sound.title, style = MaterialTheme.typography.titleLarge)
            Text(stage.generation.prompt, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.generate_action_reset))
            }
        }
    }
}

private fun GenerationStage.isTerminal(): Boolean = this is GenerationStage.Idle ||
    this is GenerationStage.Done ||
    this is GenerationStage.Failed
