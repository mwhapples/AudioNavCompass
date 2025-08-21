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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class MainViewModel(compassProvider: CompassProvider) : ViewModel() {
    val compassConnection = compassProvider.compassConnection.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = CompassConnection.NoCompassConnection)
    @OptIn(ExperimentalCoroutinesApi::class)
    val heading: StateFlow<Float> = compassConnection.filterIsInstance<CompassConnection.ActiveCompassConnection>().flatMapLatest { it.headingInDegrees }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = 0f)
    @OptIn(ExperimentalCoroutinesApi::class)
    val course = compassConnection.filterIsInstance<CompassConnection.ActiveCompassConnection>().flatMapLatest { it.course }
    fun updateCourse(newCourse: Int) = compassConnection.value.let {
        if (it is CompassConnection.ActiveCompassConnection) {
            it.updateCourse(newCourse)
        }
    }
}