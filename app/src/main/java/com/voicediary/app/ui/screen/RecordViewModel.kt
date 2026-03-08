package com.voicediary.app.ui.screen

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicediary.app.data.local.VoiceEntry
import com.voicediary.app.data.network.TextCorrectionService
import com.voicediary.app.data.recording.AudioPlayerManager
import com.voicediary.app.data.recording.RecordingManager
import com.voicediary.app.data.recording.SpeechRecognitionManager
import com.voicediary.app.data.repository.VoiceEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CorrectionState {
    IDLE,
    CORRECTING,
    DONE
}

data class RecordUiState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val filePath: String? = null,
    val isSaved: Boolean = false,
    val saveFailed: Boolean = false,
    val partialTranscript: String = "",
    val rawTranscript: String = "",
    val correctedText: String = "",
    val correctionState: CorrectionState = CorrectionState.IDLE,
    val correctionSkipped: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isPlaying: Boolean = false
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingManager: RecordingManager,
    private val speechRecognitionManager: SpeechRecognitionManager,
    private val textCorrectionService: TextCorrectionService,
    private val audioPlayerManager: AudioPlayerManager,
    private val repository: VoiceEntryRepository
) : ViewModel() {

    val type: String = savedStateHandle["type"] ?: "diary"

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        speechRecognitionManager.setListeners(
            onPartialResult = { partial ->
                _uiState.update { it.copy(partialTranscript = partial) }
            },
            onFinalResult = { accumulated ->
                _uiState.update { it.copy(
                    rawTranscript = accumulated,
                    partialTranscript = accumulated
                ) }
            }
        )
    }

    fun startRecording() {
        val path = recordingManager.startRecording(type)
        _uiState.value = RecordUiState(
            isRecording = true,
            elapsedSeconds = 0L,
            filePath = path,
            createdAt = System.currentTimeMillis()
        )
        startTimer()

        mainHandler.postDelayed({
            if (_uiState.value.isRecording) {
                speechRecognitionManager.startListening()
            }
        }, 300L)
    }

    fun stopRecording() {
        val finalTranscript = speechRecognitionManager.stopListening()
        timerJob?.cancel()
        val path = recordingManager.stopRecording()

        _uiState.update { it.copy(
            isRecording = false,
            filePath = path,
            rawTranscript = finalTranscript.ifBlank { it.rawTranscript },
            partialTranscript = finalTranscript.ifBlank { it.partialTranscript },
            correctionState = CorrectionState.CORRECTING
        ) }

        requestCorrection()
    }

    private fun requestCorrection() {
        viewModelScope.launch {
            val raw = _uiState.value.rawTranscript
            val result = textCorrectionService.correctText(raw)
            _uiState.update { it.copy(
                correctedText = result.correctedText,
                correctionState = CorrectionState.DONE,
                correctionSkipped = result.isOriginal && raw.isNotBlank()
            ) }
        }
    }

    fun updateCorrectedText(text: String) {
        _uiState.update { it.copy(correctedText = text) }
    }

    fun playAudio() {
        val path = _uiState.value.filePath ?: return
        _uiState.update { it.copy(isPlaying = true) }
        audioPlayerManager.play(path) {
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    fun stopPlayback() {
        audioPlayerManager.stop()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun saveEntry() {
        val state = _uiState.value
        val path = state.filePath ?: return

        viewModelScope.launch {
            try {
                val entry = VoiceEntry(
                    type = type,
                    rawTranscript = state.rawTranscript,
                    correctedText = state.correctedText.ifBlank { state.rawTranscript },
                    audioFilePath = path,
                    createdAt = state.createdAt,
                    reviewStatus = "unreviewed"
                )
                repository.insert(entry)
                _uiState.update { it.copy(isSaved = true, saveFailed = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(saveFailed = true) }
            }
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionManager.release()
        recordingManager.release()
        audioPlayerManager.release()
        mainHandler.removeCallbacksAndMessages(null)
    }
}
