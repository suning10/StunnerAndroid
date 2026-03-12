package com.seclass.stunner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seclass.stunner.model.GoalEvent
import com.seclass.stunner.model.GoalZone
import com.seclass.stunner.repository.GoalEventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StatsViewModel(
    repository: GoalEventRepository
) : ViewModel() {

    val allEvents: StateFlow<List<GoalEvent>> = repository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Hit count per GoalZone across all recorded events. */
    val zoneCounts: StateFlow<IntArray> = allEvents.map { events ->
        IntArray(9).also { counts ->
            events.filter { it.goal && it.zone != null }
                .forEach { counts[GoalZone.valueOf(it.zone!!).ordinal]++ }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IntArray(9))

    val totalGoals: StateFlow<Int> = allEvents.map { events -> events.count { it.goal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalShots: StateFlow<Int> = allEvents.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        fun factory(repo: GoalEventRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StatsViewModel(repo) as T
        }
    }
}
