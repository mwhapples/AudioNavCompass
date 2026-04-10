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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class MainViewModel(compassProvider: CompassProvider) : ViewModel() {
    private inline fun <reified T : CompassConnection> updateCompass(value: CompassConnection, block: (T) -> Unit) {
        if (value is T) {
            block(value)
        }
    }
    val compassConnection = compassProvider.compassConnection.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = CompassConnection.NoCompassConnection)
    @OptIn(ExperimentalCoroutinesApi::class)
    val heading = compassConnection.filterIsInstance<CompassConnection.ActiveCompassConnection>().flatMapLatest { it.headingInDegrees }.shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)
    @OptIn(ExperimentalCoroutinesApi::class)
    val headingError = compassConnection.filterIsInstance<CompassConnection.ActiveCompassConnection>().flatMapLatest { it.compassEvents.filterIsInstance<CompassEvent.Heading>() }.map { it.headingError }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = 1f)
    @OptIn(ExperimentalCoroutinesApi::class)
    val course = compassConnection.filterIsInstance<CompassConnection.ActiveCompassConnection>().flatMapLatest { it.course }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = 0)
    fun updateCourse(newCourse: Int) = updateCompass<CompassConnection.ActiveCompassConnection>(compassConnection.value) { it.updateCourse(newCourse) }
    @OptIn(ExperimentalCoroutinesApi::class)
    val audioPlaying = compassConnection.filterIsInstance<CompassConnection.ActiveCompassConnection>().flatMapLatest { it.audioPlaying }
    fun updateAudioPlayingState(newState: Boolean) = updateCompass<CompassConnection.ActiveCompassConnection>(compassConnection.value) { if (newState) it.startAudio(viewModelScope) else it.stopAudio() }
}