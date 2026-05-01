package com.sleepsounds.app.audio

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sleepsounds.app.domain.model.PlaybackState
import com.sleepsounds.app.domain.model.SleepTimer
import com.sleepsounds.app.domain.model.SoundSource
import com.sleepsounds.app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class SleepPlaybackService : MediaSessionService() {

    @Inject lateinit var controller: PlaybackController

    private val players: MutableMap<String, ExoPlayer> = mutableMapOf()
    private var primarySession: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var fadeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        scope.launch { observeState() }
        scope.launch { observeTimer() }
    }

    private fun primaryPlayer(): ExoPlayer {
        return players.values.firstOrNull() ?: createPlayer().also { players["__primary__"] = it }
    }

    private fun createPlayer(): ExoPlayer {
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.repeatMode = Player.REPEAT_MODE_ONE
        return player
    }

    private suspend fun observeState() {
        controller.state.collectLatest { state ->
            applyState(state)
            if (primarySession == null) {
                primarySession = MediaSession.Builder(this, primaryPlayer())
                    .setSessionActivity(
                        PendingIntent.getActivity(
                            this,
                            0,
                            Intent(this, MainActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        )
                    )
                    .build()
            }
        }
    }

    private fun applyState(state: PlaybackState) {
        // Remove players whose sounds are no longer active.
        val activeIds = state.activeTracks.map { it.sound.id }.toSet()
        players.keys.filter { it != "__primary__" && it !in activeIds }
            .forEach { id -> players.remove(id)?.release() }

        for (track in state.activeTracks) {
            val player = players.getOrPut(track.sound.id) { createPlayer() }
            val uri = when (val s = track.sound.source) {
                is SoundSource.Asset -> "asset:///${s.path}"
                is SoundSource.Local -> "file://${s.path}"
                is SoundSource.Remote -> s.url
            }
            if (player.currentMediaItem?.localConfiguration?.uri?.toString() != uri) {
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
            }
            player.volume = track.volume
            if (state.isPlaying && !player.isPlaying) player.play()
            if (!state.isPlaying && player.isPlaying) player.pause()
        }

        if (state.activeTracks.isEmpty()) {
            players.values.forEach { it.pause() }
        }
    }

    private suspend fun observeTimer() {
        controller.timer.collectLatest { timer ->
            fadeJob?.cancel()
            if (timer is SleepTimer.Active) {
                fadeJob = scope.launch { runFadeOut(timer.totalMs) }
            }
        }
    }

    private suspend fun runFadeOut(totalMs: Long) {
        // Hold full volume until the last 30s, then linearly fade to 0 and pause.
        val fadeMs = (totalMs.coerceAtLeast(30_000L)).coerceAtMost(60_000L)
        val holdMs = (totalMs - fadeMs).coerceAtLeast(0L)
        if (holdMs > 0) delay(holdMs)
        val originalVolumes = players.mapValues { it.value.volume }
        val steps = 30
        val stepMs = fadeMs / steps
        repeat(steps) { i ->
            val frac = 1f - (i + 1).toFloat() / steps
            for ((id, player) in players) {
                val base = originalVolumes[id] ?: player.volume
                player.volume = (base * frac).coerceIn(0f, 1f)
            }
            delay(stepMs)
        }
        players.values.forEach { it.pause() }
        controller.scheduleTimer(SleepTimer.Disabled)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        primarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = primarySession?.player
        if (player != null && (!player.playWhenReady || player.mediaItemCount == 0)) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        scope.cancel()
        primarySession?.run { player.release(); release() }
        primarySession = null
        players.values.forEach { it.release() }
        players.clear()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "sleep_playback"
    }
}
