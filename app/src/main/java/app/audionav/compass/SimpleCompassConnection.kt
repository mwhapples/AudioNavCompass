/*
 * Copyright (C) 2025 Michael Whapples
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

import android.media.AudioFormat
import app.audionav.compass.audio.ToneGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

private const val SAMPLE_RATE = 44100
private const val CHANNEL_MASK = AudioFormat.CHANNEL_OUT_MONO

class SimpleCompassConnection(compassSensor: CompassSensor) :
    CompassConnection.ActiveCompassConnection {
    override val compassEvents: Flow<CompassEvent> = compassSensor.compassEvents
    private val _mutableCourse = MutableStateFlow(0)
    override val course: StateFlow<Int> = _mutableCourse.asStateFlow()
    override fun updateCourse(newCourse: Int) {
        val moddedCourse = newCourse % 360
        _mutableCourse.value = if (moddedCourse < 0) moddedCourse + 360 else moddedCourse
    }

    private val _toneGenerator = ToneGenerator()
    private val _lowBeep =
        _toneGenerator.createAudioTrack(
            _toneGenerator.createTone(
                523,
                50,
                rampUpMS = 10,
                rampDownMS = 10,
                sampleRate = SAMPLE_RATE
            ),
            sampleRate = SAMPLE_RATE,
            channelMask = CHANNEL_MASK
        )
    private val _highBeep = _toneGenerator.createAudioTrack(
        _toneGenerator.createTone(
            1046,
            50,
            rampUpMS = 10,
            rampDownMS = 10,
            sampleRate = SAMPLE_RATE
        ),
        sampleRate = SAMPLE_RATE,
        channelMask = CHANNEL_MASK
    )
    private val _mutablePlayingState = MutableStateFlow(false)
    override val audioPlaying: StateFlow<Boolean> = _mutablePlayingState.asStateFlow()

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class, ExperimentalTime::class)
    override fun startAudio(scope: CoroutineScope) {
        val audioDispatcher = newSingleThreadContext("AudioDispatcher")
        scope.launch(audioDispatcher) {
            val deviation = deviationFromCourseDegrees.stateIn(
                scope = this,
                started = SharingStarted.Eagerly,
                initialValue = 0
            )
            val silentAngle = 0
            var lastLowBeep = Clock.System.now()
            var lastHighBeep = lastLowBeep
            _mutablePlayingState.emit(true)
            try {
                while (audioPlaying.value) {
                    val currentTime = Clock.System.now()
                    val currentDelta = deviation.value.toInt()
                    val deltaMS =
                        (if (currentDelta == 0) 0 else abs((5000 + silentAngle) / currentDelta)).toDuration(
                            DurationUnit.MILLISECONDS
                        )
                    if (currentDelta < -silentAngle && currentTime - lastLowBeep > deltaMS) {
                        _lowBeep.stop()
                        _lowBeep.reloadStaticData()
                        _lowBeep.play()
                        lastLowBeep = currentTime
                    } else if (currentDelta > silentAngle && currentTime - lastHighBeep > deltaMS) {
                        _highBeep.stop()
                        _highBeep.reloadStaticData()
                        _highBeep.play()
                        lastHighBeep = currentTime
                    }
                    delay(10)
                }
            } catch (_: CancellationException) {
                // Do nothing, just stopping
            } finally {
                _mutablePlayingState.emit(false)
            }
        }.invokeOnCompletion { audioDispatcher.close() }
    }

    override fun stopAudio() {
        _mutablePlayingState.value = false
    }
}