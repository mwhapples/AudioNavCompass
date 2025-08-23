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
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

class ToneGenerator {
    private fun calculateAmplitude(sample: Int, fullAmplitude: IntRange, rampUpStep: Double, rampDownStep: Double): Double = when {
        fullAmplitude.isEmpty() -> 1.0
        sample < fullAmplitude.min() -> rampUpStep * sample
        sample > fullAmplitude.max() -> 1.0 - (rampDownStep * (sample - fullAmplitude.max()))
        else -> 1.0
    }.coerceIn(0.0, 1.0)
    fun createTone(frequency: Int, duration: Int, rampUpMS: Int = 5, rampDownMS: Int = 5, sampleRate: Int = 44100): ShortArray {
        val numOfSamples = sampleRate * duration / 1000
        val rampUpEnd = (rampUpMS * sampleRate / 1000).coerceAtMost(numOfSamples / 2)
        val rampUpStep = 1.0 / rampUpEnd
        val rampDownStart = numOfSamples - (rampDownMS * sampleRate / 1000).coerceAtMost(numOfSamples / 2)
        val rampDownStep = 1.0 / (numOfSamples - rampDownStart)
        return (0..<numOfSamples).map { (calculateAmplitude(it, rampUpEnd..rampDownStart, rampUpStep, rampDownStep) * 32000 * sin(PI * 2.0 * frequency * it / sampleRate)).toInt()
            .toShort() }.toShortArray()
    }
    fun createAudioTrack(buffer: ShortArray, sampleRate: Int, channelMask: Int, sessionId: Int): AudioTrack {
        val numOfBytes = buffer.size * 2
        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED).build()
        val audioFormat = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(channelMask).setSampleRate(sampleRate).build()
        return AudioTrack(
            audioAttributes,
            audioFormat,
            numOfBytes,
            AudioTrack.MODE_STATIC,
            sessionId
        ).apply {
            write(buffer, 0, buffer.size)
        }
    }
}