package app.audionav.compass

import androidx.lifecycle.ViewModel
import com.google.android.gms.location.DeviceOrientation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MainViewModel(headingflow: Flow<DeviceOrientation>) : ViewModel() {
    val heading = headingflow.map { it.headingDegrees.toInt() }
}