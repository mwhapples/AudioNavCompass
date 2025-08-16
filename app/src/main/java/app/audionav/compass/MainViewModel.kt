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
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

class MainViewModel(compassProvider: CompassProvider) : ViewModel() {
    val compassConnection = compassProvider.compassConnection.shareIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val heading = compassConnection.filterIsInstance<CompassConnection.ActiveCompassConnection>().flatMapConcat { it.compassEvents }.filterIsInstance<CompassEvent.Heading>().map { it.headingInDegrees.toInt() }
}