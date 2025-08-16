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

import android.content.Context
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.Executors

class FusedOrientationCompass(context: Context) : CompassSensor {
    private val client = LocationServices.getFusedOrientationProviderClient(context)
    val orientationEvents: Flow<DeviceOrientation> = callbackFlow {
        val request = DeviceOrientationRequest.Builder(DeviceOrientationRequest.OUTPUT_PERIOD_DEFAULT).build()
        val executor = Executors.newSingleThreadExecutor()
        val listener: (DeviceOrientation) -> Unit = { orientation: DeviceOrientation ->
            try {
                trySend(orientation)
            } catch (_: Throwable) {
                // Not much which can be done
            }
        }
        client.requestOrientationUpdates(request, executor, listener)
        awaitClose { client.removeOrientationUpdates(listener) }
    }
    override val compassEvents: Flow<CompassEvent> = orientationEvents.map { CompassEvent.Heading(it.headingDegrees, it.headingErrorDegrees) }
}