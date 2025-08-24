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

import app.audionav.compass.audio.AndroidAudioOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlin.time.ExperimentalTime

class SimpleCompassConnection(compassSensor: CompassSensor, val audioOutput: AndroidAudioOutput) :
    CompassConnection.ActiveCompassConnection {
    override val compassEvents: Flow<CompassEvent> = compassSensor.compassEvents
    private val _mutableCourse = MutableStateFlow(0)
    override val course: StateFlow<Int> = _mutableCourse.asStateFlow()
    override fun updateCourse(newCourse: Int) {
        val moddedCourse = newCourse % 360
        _mutableCourse.value = if (moddedCourse < 0) moddedCourse + 360 else moddedCourse
    }

    private val _mutablePlayingState = MutableStateFlow(false)
    override val audioPlaying: StateFlow<Boolean> = _mutablePlayingState.asStateFlow()

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class, ExperimentalTime::class)
    override fun startAudio(scope: CoroutineScope) {
        val audioDispatcher = newSingleThreadContext("AudioDispatcher")
        scope.launch(audioDispatcher) {
            val deviation = deviationFromCourseDegrees.stateIn(
                scope = this,
                started = SharingStarted.Eagerly,
                initialValue = 0f
            )
            audioOutput.playAudio(deviation, _mutablePlayingState)
        }.invokeOnCompletion { audioDispatcher.close() }
    }

    override fun stopAudio() {
        _mutablePlayingState.value = false
    }
}