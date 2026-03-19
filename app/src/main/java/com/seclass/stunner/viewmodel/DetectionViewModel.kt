package com.seclass.stunner.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seclass.stunner.model.BallDetection
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
        viewModelScope.launch {
            // Always detect — needed for live bounding-box overlay
            val detections = detectionRepository.detectBall(bitmap)
            val topBall = detections.maxByOrNull { it.confidence }
            _state.update { it.copy(ballBounds = topBall?.boundingBox) }

            // Goal evaluation is debounced
            val now = System.currentTimeMillis()
            val timeSinceLast = now - lastShotTimeMs
            if (timeSinceLast < shotDebounceMs) {
                android.util.Log.v(TAG, "Goal eval skipped — debounce active (${shotDebounceMs - timeSinceLast}ms remaining)")
                return@launch
            }

            val event = detectionRepository.evaluateGoal(detections) ?: return@launch
            lastShotTimeMs = System.currentTimeMillis()
            android.util.Log.i(TAG, "Event recorded — goal=${event.goal}  zone=${event.zone}  accuracy=%.2f".format(event.accuracy))

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

    /**
     * Debug only — simulates a ball travelling from top to bottom of frame,
     * crossing goalLineY, so you can verify goal detection / zone / overlay
     * without needing the real model.
     *
     * Feeds fake BallDetection frames at 80 ms intervals (≈ 12 fps).
     * The ball moves from cy=0.05 → cy=0.95 in 10 steps.
     */
    fun simulateShot() {
        detectionRepository.goalDetector.reset()
        lastShotTimeMs = 0L   // clear debounce so the shot registers immediately

        viewModelScope.launch {
            android.util.Log.i(TAG, "--- SIM SHOT START ---")
            val steps = listOf(0.05f, 0.15f, 0.25f, 0.35f, 0.45f, 0.55f, 0.65f, 0.75f, 0.85f, 0.95f)
            for (cy in steps) {
                val half = 0.08f
                val fake = BallDetection(
                    boundingBox = RectF(0.5f - half, cy - half, 0.5f + half, cy + half),
                    confidence  = 0.99f
                )
                // Update overlay so the ball visually moves down the screen
                _state.update { it.copy(ballBounds = fake.boundingBox) }

                val event = detectionRepository.evaluateGoal(listOf(fake))
                if (event != null) {
                    lastShotTimeMs = System.currentTimeMillis()
                    android.util.Log.i(TAG, "SIM event — goal=${event.goal}  zone=${event.zone}")
                    _state.update {
                        it.copy(
                            lastEvent    = event,
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
                    break
                }
                delay(80)
            }
            _state.update { it.copy(ballBounds = null) }
            android.util.Log.i(TAG, "--- SIM SHOT END ---")
        }
    }

    fun setGoalLine(normalizedY: Float) {
        detectionRepository.goalLineY = normalizedY
        _state.update { it.copy(goalLineY = normalizedY) }
    }

    companion object {
        private const val TAG = "Stunner/ViewModel"

        fun factory(repo: DetectionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DetectionViewModel(repo) as T
        }
    }
}
