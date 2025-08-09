package app.audionav.compass

import android.content.Context
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executors

class FusedOrientationCompass(context: Context) {
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
}