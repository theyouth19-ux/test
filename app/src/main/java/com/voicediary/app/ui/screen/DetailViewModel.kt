package com.voicediary.app.ui.screen

import android.media.MediaPlayer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicediary.app.data.local.VoiceEntry
import com.voicediary.app.data.repository.VoiceEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DetailUiState(
    val entry: VoiceEntry? = null,
    val editedCorrectedText: String = "",
    val isLoading: Boolean = true,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val isRawTextExpanded: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VoiceEntryRepository
) : ViewModel() {

    private val entryId: Long = savedStateHandle["id"] ?: 0L

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            _uiState.update {
                it.copy(
                    entry = entry,
                    editedCorrectedText = entry?.correctedText.orEmpty(),
                    isLoading = false
                )
            }
        }
    }

    fun onCorrectedTextChange(text: String) {
        _uiState.update { it.copy(editedCorrectedText = text, isSaved = false) }
    }

    fun toggleRawTextExpanded() {
        _uiState.update { it.copy(isRawTextExpanded = !it.isRawTextExpanded) }
    }

    fun saveCorrectedText() {
        val entry = _uiState.value.entry ?: return
        viewModelScope.launch {
            val updated = entry.copy(correctedText = _uiState.value.editedCorrectedText)
            repository.update(updated)
            _uiState.update { it.copy(entry = updated, isSaved = true) }
        }
    }

    fun toggleReviewStatus() {
        val entry = _uiState.value.entry ?: return
        viewModelScope.launch {
            val newStatus = if (entry.reviewStatus == "reviewed") "unreviewed" else "reviewed"
            val updated = entry.copy(reviewStatus = newStatus)
            repository.update(updated)
            _uiState.update { it.copy(entry = updated) }
        }
    }

    fun deleteEntry() {
        val entry = _uiState.value.entry ?: return
        viewModelScope.launch {
            repository.delete(entry)
            // 오디오 파일 삭제
            try {
                val file = File(entry.audioFilePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) { }
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    // --- Audio playback ---

    fun togglePlayback() {
        if (_uiState.value.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val filePath = _uiState.value.entry?.audioFilePath ?: return
        val file = File(filePath)
        if (!file.exists()) return

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _uiState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                    stopProgressTracking()
                }
            }
            _uiState.update { it.copy(durationMs = mediaPlayer?.duration?.toLong() ?: 0L) }
        }

        mediaPlayer?.start()
        _uiState.update { it.copy(isPlaying = true) }
        startProgressTracking()
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        _uiState.update { it.copy(isPlaying = false) }
        stopProgressTracking()
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                _uiState.update { it.copy(currentPositionMs = pos) }
                delay(200L)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressTracking()
        try {
            mediaPlayer?.release()
        } catch (_: Exception) { }
        mediaPlayer = null
    }
}
