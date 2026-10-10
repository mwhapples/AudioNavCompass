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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow

interface CompassEvent {
    data class Heading(val headingInDegrees: Float, val headingError: Float) : CompassEvent
}

sealed interface VoiceCommandStatus {
    data object Idle : VoiceCommandStatus
    data object Listening : VoiceCommandStatus
    data class CourseChanged(val delta: Int) : VoiceCommandStatus
    data class CommandNotRecognized(val command: String) : VoiceCommandStatus
    data object Error : VoiceCommandStatus
    data object Unavailable : VoiceCommandStatus
    data object PermissionDenied : VoiceCommandStatus
}

interface CompassSensor {
    val compassEvents: Flow<CompassEvent>
}

sealed interface CompassConnection {
    object NoCompassConnection : CompassConnection
    interface ActiveCompassConnection : CompassConnection {
        val compassEvents: Flow<CompassEvent>
        val headingInDegrees: Flow<Float>
            get() = compassEvents.filterIsInstance<CompassEvent.Heading>()
                .map { it.headingInDegrees }

        val course: StateFlow<Int>
        val voiceCommandStatus: StateFlow<VoiceCommandStatus>
        fun requestVoiceCommand()
        fun updateVoiceCommandStatus(status: VoiceCommandStatus)
        fun updateCourse(newCourse: Int)
        val deviationFromCourseDegrees: Flow<Float>
            get() = headingInDegrees
                .combine(course) { h, c ->
                    val deviation = (h - c) % 360
                    if (deviation > 180) {
                        deviation - 360
                    } else if (deviation > -180) {
                        deviation
                    } else {
                        deviation + 360
                    }
                }
        val audioPlaying: Flow<Boolean>
        fun startAudio(scope: CoroutineScope)
        fun stopAudio()
    }
}

interface CompassProvider {
    val compassConnection: Flow<CompassConnection>
}