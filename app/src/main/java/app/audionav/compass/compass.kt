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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

interface CompassEvent {
    data class Heading(val headingInDegrees: Float, val headingErrorInDegrees: Float) : CompassEvent
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
    }
}

interface CompassProvider {
    val compassConnection: Flow<CompassConnection>
}