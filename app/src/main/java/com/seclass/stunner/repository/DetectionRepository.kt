package com.seclass.stunner.repository

import android.graphics.Bitmap
import com.seclass.stunner.detection.BallDetector
import com.seclass.stunner.model.BallDetection
import com.seclass.stunner.model.GoalEvent
import com.seclass.stunner.model.GoalZone
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Owns the per-frame goal detection logic.
 *
 * Depends on [BallDetector] via interface — swap TFLite for YOLO here without touching anything else.
 *
 * Goal line is defined in normalized Y [0,1] relative to the camera frame.
 * A ball whose center crosses below [goalLineY] while moving toward the camera = GOAL.
 */
class DetectionRepository(
    private val detector: BallDetector,
    private val goalEventRepository: GoalEventRepository,
    private val apiRepository: ApiRepository
) {
    /** Normalized Y position of the goal line; set once during calibration. */
    var goalLineY: Float = 0.5f

    var deviceId: String? = null

    /**
     * Process a single camera frame.
     * Returns a [GoalEvent] if a goal or miss was definitively detected, null otherwise.
     *
     * TODO: Add Kalman filter / trajectory tracking across frames to handle fast shots
     *       that skip frames.
     */
    suspend fun processFrame(bitmap: Bitmap): GoalEvent? {
        val detections = detector.detect(bitmap)
        val ball = detections.maxByOrNull { it.confidence } ?: return null

        val cx = (ball.boundingBox.left + ball.boundingBox.right) / 2f
        val cy = (ball.boundingBox.top + ball.boundingBox.bottom) / 2f

        val crossedGoalLine = cy >= goalLineY
        val zone = if (crossedGoalLine) GoalZone.fromNormalized(cx, cy) else null
        val accuracy = computePlacementAccuracy(cx, cy)

        val event = GoalEvent(
            goal = crossedGoalLine,
            accuracy = accuracy,
            zone = zone?.name,
            deviceId = deviceId
        )
        goalEventRepository.insert(event)
        apiRepository.postGoalEvent(event) // fire-and-forget; failure is logged, not thrown
        return event
    }

    /**
     * Placement accuracy: 1.0 = dead center, 0.0 = at the furthest corner.
     * Does not account for player intent — purely positional.
     */
    private fun computePlacementAccuracy(cx: Float, cy: Float): Double {
        val dist = sqrt((cx - 0.5).pow(2) + (cy - 0.5).pow(2))
        val maxDist = sqrt(0.5.pow(2) + 0.5.pow(2)) // corner-to-center distance
        return (1.0 - dist / maxDist).coerceIn(0.0, 1.0)
    }
}
