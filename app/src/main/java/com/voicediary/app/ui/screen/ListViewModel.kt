package com.voicediary.app.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicediary.app.data.local.VoiceEntry
import com.voicediary.app.data.repository.VoiceEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ListFilter(val label: String) {
    ALL("전체"),
    DIARY("일기만"),
    MEMO("메모만"),
    UNREVIEWED("검토전만")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ListViewModel @Inject constructor(
    private val repository: VoiceEntryRepository
) : ViewModel() {

    private val _currentFilter = MutableStateFlow(ListFilter.ALL)
    val currentFilter: StateFlow<ListFilter> = _currentFilter

    val entries: StateFlow<List<VoiceEntry>> = _currentFilter
        .flatMapLatest { filter ->
            when (filter) {
                ListFilter.ALL -> repository.getAllEntries()
                ListFilter.DIARY -> repository.getEntriesByType("diary")
                ListFilter.MEMO -> repository.getEntriesByType("memo")
                ListFilter.UNREVIEWED -> repository.getEntriesByReviewStatus("unreviewed")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setFilter(filter: ListFilter) {
        _currentFilter.value = filter
    }

    fun deleteEntry(entry: VoiceEntry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }
}
