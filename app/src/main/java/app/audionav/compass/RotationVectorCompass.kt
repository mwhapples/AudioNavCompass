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
package app.audionav.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.Math.toDegrees
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

internal fun headingDegrees(rotationVector: FloatArray): Float {
    val rotationMatrix = FloatArray(9)
    val orientation = FloatArray(3)
    SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
    SensorManager.getOrientation(rotationMatrix, orientation)
    return ((toDegrees(orientation[0].toDouble()).toFloat() % 360f) + 360f) % 360f
}

internal fun headingError(accuracy: Int, magneticFieldMagnitude: Float): Float {
    val accuracyError = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> 0.66f
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> 0.33f
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> 0f
        else -> 1f
    }
    return if (magneticFieldMagnitude in 25f..65f) accuracyError else 1f
}

class RotationVectorCompass(context: Context) : CompassSensor {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override val compassEvents: Flow<CompassEvent> = callbackFlow {
        val magnetometerAccuracy = AtomicInteger(SensorManager.SENSOR_STATUS_UNRELIABLE)
        val magneticFieldMagnitude = AtomicReference(Float.NaN)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val values = event.values
                        magneticFieldMagnitude.set(sqrt(
                            values[0] * values[0] + values[1] * values[1] + values[2] * values[2]
                        ))
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val error = headingError(magnetometerAccuracy.get(), magneticFieldMagnitude.get())
                        trySend(CompassEvent.Heading(headingDegrees(event.values), error))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) magnetometerAccuracy.set(accuracy)
            }
        }
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (rotationVector != null) sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
        if (magnetometer != null) sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}