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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.android.ext.android.inject

const val COMPASS_SERVICE_CHANNEL_ID = "compass_service_channel_id"

class CompassService : LifecycleService(), CompassConnection.ActiveCompassConnection {
    private val compassConnection: CompassConnection.ActiveCompassConnection by inject()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val notification = NotificationCompat.Builder(this, COMPASS_SERVICE_CHANNEL_ID).setContentTitle("AudioNav Compass Service").setContentText("AudioNav Compass is running").setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE).build()
        ServiceCompat.startForeground(this, 1, notification, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)
        compassConnection.startAudio(lifecycleScope)
        return START_STICKY
    }
    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return CompassServiceBinder()
    }

    override val compassEvents: Flow<CompassEvent>
        get() = compassConnection.compassEvents
    override val course: StateFlow<Int>
        get() = compassConnection.course

    override fun updateCourse(newCourse: Int) = compassConnection.updateCourse(newCourse)

    override val audioPlaying: Flow<Boolean>
        get() = compassConnection.audioPlaying

    override fun startAudio(scope: CoroutineScope) {
        val intent = Intent(this, this::class.java)
        startService(intent)
    }

    override fun stopAudio() {
        compassConnection.stopAudio()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    inner class CompassServiceBinder : Binder() {
        fun getCompassConnection(): CompassConnection.ActiveCompassConnection = this@CompassService
    }
}

class CompassServiceProvider(val context: Context) : CompassProvider {
    override val compassConnection: Flow<CompassConnection> = callbackFlow {
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                trySend((binder as CompassService.CompassServiceBinder).getCompassConnection())
            }

            override fun onServiceDisconnected(p0: ComponentName?) {}
        }
        val intent = Intent(context, CompassService::class.java)
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        awaitClose { context.unbindService(conn) }
    }
}