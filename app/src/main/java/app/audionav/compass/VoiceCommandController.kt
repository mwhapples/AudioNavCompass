/*
 * Copyright (C) 2026 Michael Whapples
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 */
package app.audionav.compass

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

internal object VoiceCommandParser {
    private val commandPattern = Regex("^\\s*change course (?:by )?([+-]?\\d+) degrees\\s*$", RegexOption.IGNORE_CASE)

    fun parseCourseDelta(transcript: String): Int? {
        val number = commandPattern.matchEntire(transcript)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        return number.takeIf { it in -180..180 }
    }
}

internal class VoiceCommandController(
    private val context: Context,
    private val compassConnection: CompassConnection.ActiveCompassConnection,
    private val onStatusChanged: (VoiceCommandStatus) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening(): Boolean {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            !SpeechRecognizer.isRecognitionAvailable(context)
        ) {
            onStatusChanged(if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) VoiceCommandStatus.PermissionDenied else VoiceCommandStatus.Unavailable)
            return false
        }

        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            speechRecognizer = it
            it.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) {
                    onStatusChanged(VoiceCommandStatus.Error)
                }
                override fun onResults(results: Bundle?) {
                    val transcripts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val delta = transcripts?.firstNotNullOfOrNull(VoiceCommandParser::parseCourseDelta)
                    if (delta != null) {
                        compassConnection.updateCourse(compassConnection.course.value + delta)
                        onStatusChanged(VoiceCommandStatus.CourseChanged(delta))
                    } else {
                        onStatusChanged(VoiceCommandStatus.CommandNotRecognized(transcripts?.firstOrNull().orEmpty()))
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
        return try {
            onStatusChanged(VoiceCommandStatus.Listening)
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            })
            true
        } catch (_: RuntimeException) {
            onStatusChanged(VoiceCommandStatus.Error)
            false
        }
    }

    fun release() {
        check(Looper.myLooper() == Looper.getMainLooper())
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}