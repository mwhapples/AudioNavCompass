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

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

class ToneGenerator {
    fun createTone(frequency: Int, duration: Int, rampUpMS: Int = 0, rampDownMS: Int = 0, sampleRate: Int = 44100): FloatArray {
        val numOfSamples = sampleRate * duration / 1000
        return (0..<numOfSamples).map { sin(2.0 * PI * it * frequency / sampleRate).toFloat() }.toFloatArray()
    }
    fun createAudioTrack(buffer: FloatArray, sampleRate: Int, channelMask: Int, sessionId: Int = AudioManager.AUDIO_SESSION_ID_GENERATE): AudioTrack {
        val numOfBytes = buffer.size * 4
        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED).build()
        val audioFormat = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(channelMask).setSampleRate(sampleRate).build()
        return AudioTrack(
            audioAttributes,
            audioFormat,
            numOfBytes,
            AudioTrack.MODE_STATIC,
            sessionId
        ).apply {
            write(buffer, 0, buffer.size, AudioTrack.WRITE_NON_BLOCKING)
        }
    }
}