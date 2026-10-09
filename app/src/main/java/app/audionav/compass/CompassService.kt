/*
 * Copyright (C) 2025-2026 Michael Whapples
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
package app.audionav.compass

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import app.audionav.compass.audio.CompassPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.android.ext.android.inject

@androidx.annotation.OptIn(UnstableApi::class)
class CompassService : MediaSessionService() {
    private val compassConnection: CompassConnection.ActiveCompassConnection by inject()
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = CompassPlayer(compassConnection)
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

class CompassServiceProvider(
    private val context: Context,
    private val activeCompassConnection: CompassConnection.ActiveCompassConnection
) : CompassProvider {
    override val compassConnection: Flow<CompassConnection> = callbackFlow {
        val sessionToken =
            SessionToken(context, ComponentName(context, CompassService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                val mediaControllerConnection = object : CompassConnection.ActiveCompassConnection {
                    override val compassEvents: Flow<CompassEvent>
                        get() = activeCompassConnection.compassEvents
                    override val course: StateFlow<Int>
                        get() = activeCompassConnection.course
                    override fun updateCourse(newCourse: Int) =
                        activeCompassConnection.updateCourse(newCourse)

                    override val audioPlaying: Flow<Boolean> = callbackFlow {
                        val listener = object : Player.Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                trySend(isPlaying)
                            }
                        }
                        trySend(controller.isPlaying)
                        controller.addListener(listener)
                        awaitClose { controller.removeListener(listener) }
                    }

                    override fun startAudio(scope: CoroutineScope) {
                        if (controller.playbackState == Player.STATE_IDLE) {
                            controller.prepare()
                        }
                        controller.play()
                    }

                    override fun stopAudio() {
                        controller.pause()
                    }
                }
                trySend(mediaControllerConnection)
            } catch (_: Exception) {
                trySend(CompassConnection.NoCompassConnection)
            }
        }, ContextCompat.getMainExecutor(context))

        awaitClose {
            MediaController.releaseFuture(controllerFuture)
        }
    }
}