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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.audionav.compass.CompassConnection
import app.audionav.compass.CompassEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class CompassPlayerTest {

    private class FakeCompassConnection : CompassConnection.ActiveCompassConnection {
        override val compassEvents: Flow<CompassEvent> = emptyFlow()
        private val _course = MutableStateFlow(0)
        override val course: StateFlow<Int> = _course.asStateFlow()
        override fun updateCourse(newCourse: Int) {
            _course.value = newCourse
        }

        private val _audioPlaying = MutableStateFlow(false)
        override val audioPlaying: StateFlow<Boolean> = _audioPlaying.asStateFlow()

        var startAudioCalled = false
        var stopAudioCalled = false

        override fun startAudio(scope: CoroutineScope) {
            startAudioCalled = true
            _audioPlaying.value = true
        }

        override fun stopAudio() {
            stopAudioCalled = true
            _audioPlaying.value = false
        }
    }

    @Test
    fun mediaItemDataIsDynamicAndUnsetDuration() {
        val fakeConnection = FakeCompassConnection()
        val looper = Looper.getMainLooper() ?: Looper.myLooper() ?: return
        val player = CompassPlayer(fakeConnection, looper)

        assertEquals(C.TIME_UNSET, player.duration)
        assertTrue(player.isCurrentMediaItemDynamic)
        assertFalse(player.isCurrentMediaItemSeekable)
        assertEquals("AudioNav Compass", player.currentMediaItem?.mediaMetadata?.title.toString())
    }

    @Test
    fun handleSetPlayWhenReadyStartsAndStopsAudio() {
        val fakeConnection = FakeCompassConnection()
        val looper = Looper.getMainLooper() ?: Looper.myLooper() ?: return
        val player = CompassPlayer(fakeConnection, looper)

        player.playWhenReady = true
        assertTrue(fakeConnection.startAudioCalled)
        assertTrue(player.isPlaying)

        player.playWhenReady = false
        assertTrue(fakeConnection.stopAudioCalled)
        assertFalse(player.isPlaying)
    }

    @Test
    fun playAndPauseMethodsControlAudio() {
        val fakeConnection = FakeCompassConnection()
        val looper = Looper.getMainLooper() ?: Looper.myLooper() ?: return
        val player = CompassPlayer(fakeConnection, looper)

        player.play()
        assertTrue(fakeConnection.startAudioCalled)
        assertTrue(player.isPlaying)

        player.pause()
        assertTrue(fakeConnection.stopAudioCalled)
        assertFalse(player.isPlaying)
    }

    @Test
    fun stopMethodStopsAudioAndResetsState() {
        val fakeConnection = FakeCompassConnection()
        val looper = Looper.getMainLooper() ?: Looper.myLooper() ?: return
        val player = CompassPlayer(fakeConnection, looper)

        player.play()
        assertTrue(fakeConnection.startAudioCalled)

        player.stop()
        assertTrue(fakeConnection.stopAudioCalled)
        assertFalse(player.isPlaying)
        assertEquals(Player.STATE_IDLE, player.playbackState)
    }
}
