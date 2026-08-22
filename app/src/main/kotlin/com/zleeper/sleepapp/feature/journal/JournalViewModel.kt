package com.zleeper.sleepapp.feature.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class JournalUiState(
    val nights: List<JournalNight> = emptyList(),
    val trends: JournalTrends? = null,
)

@HiltViewModel
class JournalViewModel @Inject constructor(repository: JournalRepository) : ViewModel() {
    val state: StateFlow<JournalUiState> = repository.history
        .map { nights -> JournalUiState(nights = nights, trends = JournalTrends.from(nights)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())
}
