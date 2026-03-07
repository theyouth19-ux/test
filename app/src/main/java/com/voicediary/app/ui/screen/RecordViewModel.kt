package com.voicediary.app.ui.screen

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicediary.app.data.local.VoiceEntry
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

data class RecordUiState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val filePath: String? = null,
    val isSaved: Boolean = false,
    val partialTranscript: String = "",
    val rawTranscript: String = ""
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingManager: RecordingManager,
    private val speechRecognitionManager: SpeechRecognitionManager,
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
        // 1) MediaRecorder 먼저 시작 (VOICE_COMMUNICATION 소스)
        val path = recordingManager.startRecording(type)
        _uiState.value = RecordUiState(
            isRecording = true,
            elapsedSeconds = 0L,
            filePath = path
        )
        startTimer()

        // 2) 약간의 지연 후 SpeechRecognizer 시작 (마이크 안정화)
        mainHandler.postDelayed({
            if (_uiState.value.isRecording) {
                speechRecognitionManager.startListening()
            }
        }, 300L)
    }

    fun stopRecording() {
        // 1) SpeechRecognizer 먼저 정지 → 최종 텍스트 수집
        val finalTranscript = speechRecognitionManager.stopListening()

        // 2) 타이머 정지
        timerJob?.cancel()

        // 3) MediaRecorder 정지
        val path = recordingManager.stopRecording()

        _uiState.update { it.copy(
            isRecording = false,
            filePath = path,
            rawTranscript = finalTranscript.ifBlank { it.rawTranscript },
            partialTranscript = finalTranscript.ifBlank { it.partialTranscript }
        ) }
    }

    fun saveEntry() {
        val state = _uiState.value
        val path = state.filePath ?: return

        viewModelScope.launch {
            val entry = VoiceEntry(
                type = type,
                rawTranscript = state.rawTranscript,
                correctedText = state.rawTranscript, // 일단 rawTranscript를 복사 (추후 AI 교정)
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
