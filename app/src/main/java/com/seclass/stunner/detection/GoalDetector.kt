package com.seclass.stunner.detection

import com.seclass.stunner.model.BallDetection

/**
 * Abstraction for goal-line detection logic.
 *
 * Receives per-frame ball detections from [BallDetector] and decides
 * whether a goal or miss has occurred.
 *
 * Two implementations:
 *   - [SimpleGoalDetector]     — single-frame Y threshold check (original)
 *   - [TrajectoryGoalDetector] — multi-frame crossing detection (new)
 *
 * Switch in MainActivity by changing which implementation is passed to DetectionRepository.
 */
interface GoalDetector {
    var goalLineY: Float

    /**
     * Called once per camera frame with the current ball detections.
     * Returns a [GoalDetectorResult] when a shot outcome is determined, null otherwise.
     */
    fun onFrame(detections: List<BallDetection>): GoalDetectorResult?

    /** Reset internal state between shots or sessions. */
    fun reset()
}

/**
 * The ball's position at the moment a shot outcome was determined.
 *
 * @param isGoal  true = goal, false = miss
 * @param cx      normalized X [0,1] at detection moment
 * @param cy      normalized Y [0,1] at detection moment
 */
data class GoalDetectorResult(
    val isGoal: Boolean,
    val cx: Float,
    val cy: Float
)
