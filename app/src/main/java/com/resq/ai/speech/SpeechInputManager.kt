package com.resq.ai.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpeechUiState(
    val listening: Boolean = false,
    val transcript: String = "",
    val error: String? = null,
    val onDevice: Boolean = false,
    val session: Int = 0
)

class SpeechInputManager(private val context: Context) {
    private val _state = MutableStateFlow(SpeechUiState())
    val state: StateFlow<SpeechUiState> = _state.asStateFlow()
    private var recognizer: SpeechRecognizer? = null
    private var session = 0

    fun start() {
        stop()
        session += 1
        val onDeviceAvailable = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        if (!onDeviceAvailable && !SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = SpeechUiState(error = "No speech recognition service is installed. Type the emergency instead.", session = session)
            return
        }
        recognizer = if (onDeviceAvailable && Build.VERSION.SDK_INT >= 31) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(listener(onDeviceAvailable))
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        _state.value = SpeechUiState(listening = true, onDevice = onDeviceAvailable, session = session)
        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        if (_state.value.listening) _state.value = _state.value.copy(listening = false)
    }

    private fun listener(onDevice: Boolean) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { _state.value = _state.value.copy(listening = true, error = null) }
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() { _state.value = _state.value.copy(listening = false) }
        override fun onError(error: Int) {
            _state.value = _state.value.copy(listening = false, error = errorMessage(error), onDevice = onDevice)
        }
        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            _state.value = SpeechUiState(false, text, null, onDevice, session)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isNotBlank()) _state.value = _state.value.copy(transcript = text, onDevice = onDevice)
        }
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error. Type the emergency instead."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Offline speech model unavailable. Type the emergency instead."
        SpeechRecognizer.ERROR_NO_MATCH -> "Speech was not understood. Try again or type the emergency."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Try again or type the emergency."
        else -> "Speech recognition unavailable. Type the emergency instead."
    }

    fun close() = stop()
}
