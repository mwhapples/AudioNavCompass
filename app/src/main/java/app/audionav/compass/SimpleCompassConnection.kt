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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SimpleCompassConnection(compassSensor: CompassSensor) : CompassConnection.ActiveCompassConnection {
    override val compassEvents: Flow<CompassEvent> = compassSensor.compassEvents
    private val _mutableCourse = MutableStateFlow(0)
    override val course: StateFlow<Int> = _mutableCourse.asStateFlow()
    override fun updateCourse(newCourse: Int) {
        val moddedCourse = newCourse % 360
        _mutableCourse.value = if (moddedCourse < 0) moddedCourse + 360 else moddedCourse
    }
}