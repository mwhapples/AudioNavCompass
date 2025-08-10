package app.audionav.compass

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.map

class MainViewModel(orientationSensor: FusedOrientationCompass) : ViewModel() {
    val heading = orientationSensor.orientationEvents.map { it.headingDegrees.toInt() }
}