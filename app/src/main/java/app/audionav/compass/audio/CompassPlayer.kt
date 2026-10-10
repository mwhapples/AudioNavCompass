/*
 * Copyright (C) 2026 Michael Whapples
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package app.audionav.compass.audio

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import app.audionav.compass.CompassConnection
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

@androidx.annotation.OptIn(UnstableApi::class)
class CompassPlayer(
    private val compassConnection: CompassConnection.ActiveCompassConnection,
    private val onSeekToNext: () -> Unit,
    looper: Looper = Looper.getMainLooper()
) : SimpleBasePlayer(looper) {

    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val heading = compassConnection.headingInDegrees.stateIn(
        scope = playerScope,
        started = SharingStarted.Eagerly,
        initialValue = 0f
    )
    private var isPlayingCompass: Boolean = false
    private var playbackState: Int = STATE_IDLE

    private val mediaItem = MediaItem.Builder()
        .setMediaId("audio_nav_compass")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("AudioNav Compass")
                .setDisplayTitle("AudioNav Compass")
                .build()
        )
        .build()

    private val mediaItemData = MediaItemData.Builder("audio_nav_compass_uid")
        .setMediaItem(mediaItem)
        .setDurationUs(C.TIME_UNSET)
        .setIsDynamic(true)
        .setIsSeekable(false)
        .build()

    init {
        compassConnection.audioPlaying
            .onEach { playing ->
                if (isPlayingCompass != playing) {
                    isPlayingCompass = playing
                    if (!playing && playbackState == STATE_READY) {
                        // Stopped from connection/audio focus
                    }
                    invalidateState()
                }
            }
            .launchIn(playerScope)
    }

    override fun getState(): State {
        val playWhenReady = isPlayingCompass
        val state = if (playWhenReady) STATE_READY else playbackState
        return State.Builder()
            .setAvailableCommands(
                Player.Commands.Builder()
                    .addAll(
                        COMMAND_PLAY_PAUSE,
                        COMMAND_SEEK_TO_PREVIOUS,
                        COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        COMMAND_SEEK_TO_NEXT,
                        COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        COMMAND_PREPARE,
                        COMMAND_STOP,
                        COMMAND_GET_CURRENT_MEDIA_ITEM,
                        COMMAND_GET_TIMELINE,
                        COMMAND_GET_METADATA
                    )
                    .build()
            )
            .setPlaylist(listOf(mediaItemData))
            .setCurrentMediaItemIndex(0)
            .setPlayWhenReady(playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(state)
            .build()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int
    ): ListenableFuture<*> {
        when (seekCommand) {
            COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                compassConnection.updateCourse(heading.value.roundToInt())
            }
            COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> onSeekToNext()
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) {
            playbackState = STATE_READY
            isPlayingCompass = true
            compassConnection.startAudio(playerScope)
        } else {
            isPlayingCompass = false
            compassConnection.stopAudio()
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        playbackState = STATE_READY
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        playbackState = STATE_IDLE
        isPlayingCompass = false
        compassConnection.stopAudio()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        playerScope.cancel()
        compassConnection.stopAudio()
        return Futures.immediateVoidFuture()
    }
}
