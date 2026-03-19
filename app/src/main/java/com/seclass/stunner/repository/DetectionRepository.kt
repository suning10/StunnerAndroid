package com.seclass.stunner.repository

import android.graphics.Bitmap
import com.seclass.stunner.detection.BallDetector
import com.seclass.stunner.detection.GoalDetector
import com.seclass.stunner.model.GoalEvent
import com.seclass.stunner.model.GoalZone
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Owns the per-frame detection pipeline.
 *
 * - [BallDetector]  — swappable ML backend (TFLite / YOLO)
 * - [GoalDetector]  — swappable goal logic (Simple / Trajectory)
 *
 * Switch either independently in MainActivity.
 */
class DetectionRepository(
    private val ballDetector: BallDetector,
    val goalDetector: GoalDetector,
    private val goalEventRepository: GoalEventRepository,
    private val apiRepository: ApiRepository
) {
    var goalLineY: Float = 0.5f
        set(value) {
            field = value
            goalDetector.goalLineY = value
        }

    var deviceId: String? = null

    /** Step 1: run ML detection only. Call every frame for live ball overlay. */
    fun detectBall(bitmap: Bitmap): List<com.seclass.stunner.model.BallDetection> =
        ballDetector.detect(bitmap)

    /** Step 2: evaluate trajectory and record a goal/miss event. Only call when not debounced. */
    suspend fun evaluateGoal(detections: List<com.seclass.stunner.model.BallDetection>): GoalEvent? {
        val result = goalDetector.onFrame(detections) ?: return null

        val zone = if (result.isGoal) GoalZone.fromNormalized(result.cx, result.cy) else null
        val accuracy = computePlacementAccuracy(result.cx, result.cy)

        val event = GoalEvent(
            goal = result.isGoal,
            accuracy = accuracy,
            zone = zone?.name,
            deviceId = deviceId
        )
        goalEventRepository.insert(event)
        apiRepository.postGoalEvent(event)
        return event
    }

    @Deprecated("Use detectBall + evaluateGoal separately", ReplaceWith("evaluateGoal(detectBall(bitmap))"))
    suspend fun processFrame(bitmap: Bitmap): GoalEvent? = evaluateGoal(detectBall(bitmap))

    private fun computePlacementAccuracy(cx: Float, cy: Float): Double {
        val dist = sqrt((cx - 0.5).pow(2) + (cy - 0.5).pow(2))
        val maxDist = sqrt(0.5.pow(2) + 0.5.pow(2))
        return (1.0 - dist / maxDist).coerceIn(0.0, 1.0)
    }
}
