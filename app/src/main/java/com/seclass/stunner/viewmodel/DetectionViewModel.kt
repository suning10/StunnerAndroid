package com.seclass.stunner.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seclass.stunner.model.DetectionState
import com.seclass.stunner.model.GoalZone
import com.seclass.stunner.repository.DetectionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetectionViewModel(
    private val detectionRepository: DetectionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DetectionState())
    val state: StateFlow<DetectionState> = _state.asStateFlow()

    // Hit counts per zone for the current session, indexed by GoalZone.ordinal
    private val _sessionZoneCounts = MutableStateFlow(IntArray(9) { 0 })
    val sessionZoneCounts: StateFlow<IntArray> = _sessionZoneCounts.asStateFlow()

    // Debounce: ignore new shots within 3 seconds of the last one
    private var lastShotTimeMs = 0L
    private val shotDebounceMs = 3_000L

    fun processFrame(bitmap: Bitmap) {
        if (System.currentTimeMillis() - lastShotTimeMs < shotDebounceMs) return

        viewModelScope.launch {
            val event = detectionRepository.processFrame(bitmap) ?: return@launch
            lastShotTimeMs = System.currentTimeMillis()

            _state.update {
                it.copy(
                    lastEvent = event,
                    sessionGoals = if (event.goal) it.sessionGoals + 1 else it.sessionGoals,
                    sessionShots = it.sessionShots + 1,
                    showGoalOverlay = event.goal
                )
            }

            if (event.goal) {
                event.zone?.let { zoneName ->
                    val index = GoalZone.valueOf(zoneName).ordinal
                    val updated = _sessionZoneCounts.value.copyOf()
                    updated[index]++
                    _sessionZoneCounts.value = updated
                }
                delay(2_000)
                _state.update { it.copy(showGoalOverlay = false) }
            }
        }
    }

    fun setGoalLine(normalizedY: Float) {
        detectionRepository.goalLineY = normalizedY
        _state.update { it.copy(goalLineY = normalizedY) }
    }

    companion object {
        fun factory(repo: DetectionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DetectionViewModel(repo) as T
        }
    }
}
