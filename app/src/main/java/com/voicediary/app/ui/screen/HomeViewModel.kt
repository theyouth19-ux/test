package com.voicediary.app.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicediary.app.data.local.VoiceEntry
import com.voicediary.app.data.repository.VoiceEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: VoiceEntryRepository
) : ViewModel() {

    val entries: StateFlow<List<VoiceEntry>> = repository.getAllEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deleteEntry(entry: VoiceEntry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}
