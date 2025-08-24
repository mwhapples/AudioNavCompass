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
package app.audionav.compass.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

private const val SAMPLE_RATE = 44100
private const val CHANNEL_MASK = AudioFormat.CHANNEL_OUT_MONO

class AndroidAudioOutput(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val audioSessionId = audioManager.generateAudioSessionId()
    private val _toneGenerator = ToneGenerator()
    private val _lowBeep =
        _toneGenerator.createAudioTrack(
            _toneGenerator.createTone(
                523,
                50,
                rampUpMS = 10,
                rampDownMS = 10,
                sampleRate = SAMPLE_RATE
            ),
            sampleRate = SAMPLE_RATE,
            channelMask = CHANNEL_MASK,
            sessionId = audioSessionId
        )
    private val _highBeep = _toneGenerator.createAudioTrack(
        _toneGenerator.createTone(
            1046,
            50,
            rampUpMS = 10,
            rampDownMS = 10,
            sampleRate = SAMPLE_RATE
        ),
        sampleRate = SAMPLE_RATE,
        channelMask = CHANNEL_MASK,
        sessionId = audioSessionId
    )
    @OptIn(ExperimentalTime::class)
    suspend fun playAudio(deviation: StateFlow<Float>, playingState: MutableStateFlow<Boolean>) {
        var audioFocusRequest: AudioFocusRequest? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build()
            when (audioManager.requestAudioFocus(audioFocusRequest)) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> return
            }
        }
        playingState.emit(true)
        val silentAngle = 0
        var lastLowBeep = Clock.System.now()
        var lastHighBeep = lastLowBeep
        try {
            while (playingState.value) {
                val currentTime = Clock.System.now()
                val currentDelta = deviation.value.toInt()
                val deltaMS =
                    (if (currentDelta == 0) 0 else abs((5000 + silentAngle) / currentDelta)).toDuration(
                        DurationUnit.MILLISECONDS
                    )
                if (currentDelta < -silentAngle && currentTime - lastLowBeep > deltaMS) {
                    _lowBeep.stop()
                    _lowBeep.reloadStaticData()
                    _lowBeep.play()
                    lastLowBeep = currentTime
                } else if (currentDelta > silentAngle && currentTime - lastHighBeep > deltaMS) {
                    _highBeep.stop()
                    _highBeep.reloadStaticData()
                    _highBeep.play()
                    lastHighBeep = currentTime
                }
                delay(10)
            }
        } catch (_: CancellationException) {
            // Do nothing, just stopping
        } finally {
            playingState.emit(false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        }
    }
}