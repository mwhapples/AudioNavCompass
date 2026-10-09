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

import android.hardware.SensorManager
import org.junit.Assert.assertEquals
import org.junit.Test

class RotationVectorCompassTest {
    @Test
    fun mapsMagnetometerAccuracyToHeadingError() {
        assertEquals(1f, headingError(SensorManager.SENSOR_STATUS_UNRELIABLE, 50f), 0f)
        assertEquals(0.66f, headingError(SensorManager.SENSOR_STATUS_ACCURACY_LOW, 50f), 0f)
        assertEquals(0.33f, headingError(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM, 50f), 0f)
        assertEquals(0f, headingError(SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 50f), 0f)
    }

    @Test
    fun invalidMagneticFieldOverridesAccuracy() {
        assertEquals(1f, headingError(SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 24.99f), 0f)
        assertEquals(1f, headingError(SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 65.01f), 0f)
        assertEquals(0f, headingError(SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 25f), 0f)
        assertEquals(0f, headingError(SensorManager.SENSOR_STATUS_ACCURACY_HIGH, 65f), 0f)
    }
}