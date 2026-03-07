package com.voicediary.app.ui.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicediary.app.data.local.VoiceEntry
import com.voicediary.app.data.recording.RecordingManager
import com.voicediary.app.data.repository.VoiceEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordUiState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val filePath: String? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingManager: RecordingManager,
    private val repository: VoiceEntryRepository
) : ViewModel() {

    val type: String = savedStateHandle["type"] ?: "diary"

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun startRecording() {
        val path = recordingManager.startRecording(type)
        _uiState.value = RecordUiState(isRecording = true, elapsedSeconds = 0L, filePath = path)
        startTimer()
    }

    fun stopRecording() {
        timerJob?.cancel()
        val path = recordingManager.stopRecording()
        _uiState.value = _uiState.value.copy(isRecording = false, filePath = path)
    }

    fun saveEntry() {
        val state = _uiState.value
        val path = state.filePath ?: return

        viewModelScope.launch {
            val entry = VoiceEntry(
                type = type,
                rawTranscript = "",
                correctedText = "",
                audioFilePath = path,
                createdAt = System.currentTimeMillis(),
                reviewStatus = "unreviewed"
            )
            repository.insert(entry)
            _uiState.value = state.copy(isSaved = true)
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _uiState.value = _uiState.value.copy(
                    elapsedSeconds = _uiState.value.elapsedSeconds + 1
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingManager.release()
    }
}
