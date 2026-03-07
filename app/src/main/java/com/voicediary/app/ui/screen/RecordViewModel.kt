package com.voicediary.app.ui.screen

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicediary.app.data.local.VoiceEntry
import com.voicediary.app.data.network.TextCorrectionService
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
    val partialTranscript: String = "",
    val rawTranscript: String = "",
    val correctedText: String = "",
    val correctionState: CorrectionState = CorrectionState.IDLE,
    val showRawTranscript: Boolean = false
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingManager: RecordingManager,
    private val speechRecognitionManager: SpeechRecognitionManager,
    private val textCorrectionService: TextCorrectionService,
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
            filePath = path
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

        // 녹음 종료 후 자동으로 AI 교정 시작
        requestCorrection()
    }

    private fun requestCorrection() {
        viewModelScope.launch {
            val raw = _uiState.value.rawTranscript
            val result = textCorrectionService.correctText(raw)
            _uiState.update { it.copy(
                correctedText = result.correctedText,
                correctionState = CorrectionState.DONE
            ) }
        }
    }

    fun toggleShowRawTranscript() {
        _uiState.update { it.copy(showRawTranscript = !it.showRawTranscript) }
    }

    fun saveEntry() {
        val state = _uiState.value
        val path = state.filePath ?: return

        viewModelScope.launch {
            val entry = VoiceEntry(
                type = type,
                rawTranscript = state.rawTranscript,
                correctedText = state.correctedText.ifBlank { state.rawTranscript },
                audioFilePath = path,
                createdAt = System.currentTimeMillis(),
                reviewStatus = "unreviewed"
            )
            repository.insert(entry)
            _uiState.update { it.copy(isSaved = true) }
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
        mainHandler.removeCallbacksAndMessages(null)
    }
}
