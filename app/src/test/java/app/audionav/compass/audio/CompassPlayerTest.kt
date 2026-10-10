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
import app.audionav.compass.VoiceCommandStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

@androidx.annotation.OptIn(UnstableApi::class)
class CompassPlayerTest {

    private class FakeCompassConnection : CompassConnection.ActiveCompassConnection {
        override val compassEvents: Flow<CompassEvent> = emptyFlow()
        private val _heading = MutableStateFlow(0f)
        override val headingInDegrees: Flow<Float> = _heading.asStateFlow()
        fun setHeading(heading: Float) {
            _heading.value = heading
        }
        private val _course = MutableStateFlow(0)
        override val course: StateFlow<Int> = _course.asStateFlow()
        override val voiceCommandStatus: StateFlow<VoiceCommandStatus> = MutableStateFlow(VoiceCommandStatus.Idle)
        override fun requestVoiceCommand() = Unit
        override fun updateVoiceCommandStatus(status: VoiceCommandStatus) = Unit
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
        val player = CompassPlayer(fakeConnection, onSeekToNext = {}, looper = looper)

        assertEquals(C.TIME_UNSET, player.duration)
        assertTrue(player.isCurrentMediaItemDynamic)
        assertFalse(player.isCurrentMediaItemSeekable)
        assertEquals("AudioNav Compass", player.currentMediaItem?.mediaMetadata?.title.toString())
    }

    @Test
    fun previousSeekCommandsSetCourseToCurrentHeading() = runBlocking {
        val fakeConnection = FakeCompassConnection()
        val looper = Looper.getMainLooper() ?: Looper.myLooper() ?: return@runBlocking
        var nextSeekCalled = false
        val player = CompassPlayer(fakeConnection, onSeekToNext = { nextSeekCalled = true }, looper = looper)

        assertTrue(player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS))
        assertTrue(player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))

        fakeConnection.setHeading(123.6f)
        player.seekToPrevious()
        withTimeout(1_000) { fakeConnection.course.first { it == 124 } }

        fakeConnection.setHeading(271.2f)
        player.seekToPreviousMediaItem()
        withTimeout(1_000) { fakeConnection.course.first { it == 271 } }
        assertFalse(nextSeekCalled)
    }

    @Test
    fun nextSeekCommandsStillTriggerCallback() {
        val fakeConnection = FakeCompassConnection()
        val looper = Looper.getMainLooper() ?: Looper.myLooper() ?: return
        var nextSeekCalled = 0
        val player = CompassPlayer(fakeConnection, onSeekToNext = { nextSeekCalled++ }, looper = looper)

        player.seekToNext()
        player.seekToNextMediaItem()

        assertEquals(2, nextSeekCalled)
    }

    @Test
    fun handleSetPlayWhenReadyStartsAndStopsAudio() {
        val fakeConnection = FakeCompassConnection()
        val looper = Looper.getMainLooper() ?: Looper.myLooper() ?: return
        val player = CompassPlayer(fakeConnection, onSeekToNext = {}, looper = looper)

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
        val player = CompassPlayer(fakeConnection, onSeekToNext = {}, looper = looper)

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
        val player = CompassPlayer(fakeConnection, onSeekToNext = {}, looper = looper)

        player.play()
        assertTrue(fakeConnection.startAudioCalled)

        player.stop()
        assertTrue(fakeConnection.stopAudioCalled)
        assertFalse(player.isPlaying)
        assertEquals(Player.STATE_IDLE, player.playbackState)
    }
}
