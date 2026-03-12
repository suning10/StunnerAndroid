package com.seclass.stunner.detection

import com.seclass.stunner.model.BallDetection

/**
 * Original single-frame goal detector.
 *
 * Logic: if the highest-confidence ball detection has its center below [goalLineY] → GOAL,
 * otherwise → MISS. No cross-frame tracking.
 *
 * Pros:  simple, zero latency, no state
 * Cons:  triggers on every frame a ball is visible; no trajectory validation
 */
class SimpleGoalDetector(override var goalLineY: Float = 0.5f) : GoalDetector {

    override fun onFrame(detections: List<BallDetection>): GoalDetectorResult? {
        val ball = detections.maxByOrNull { it.confidence } ?: return null

        val cx = (ball.boundingBox.left + ball.boundingBox.right) / 2f
        val cy = (ball.boundingBox.top  + ball.boundingBox.bottom) / 2f

        return GoalDetectorResult(
            isGoal = cy >= goalLineY,
            cx = cx,
            cy = cy
        )
    }

    override fun reset() { /* stateless — nothing to reset */ }
}
