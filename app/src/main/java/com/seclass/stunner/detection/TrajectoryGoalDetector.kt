package com.seclass.stunner.detection

import android.graphics.PointF
import com.seclass.stunner.model.BallDetection

/**
 * Multi-frame trajectory-based goal detector.
 *
 * Logic:
 *  - Tracks ball Y position across a sliding window of frames
 *  - GOAL  : ball was moving toward the goal line AND its Y crosses from above to below [goalLineY]
 *            (prevY < goalLineY && currY >= goalLineY)
 *  - MISS  : ball approached within [missProximityThreshold] of the goal line but then
 *            reversed direction (moving away) without crossing
 *  - No event returned while the ball is still approaching or not yet detected
 *
 * Pros:  validates actual ball movement; avoids false positives from stationary balls
 * Cons:  requires [minApproachFrames] frames before triggering; may delay detection slightly
 *
 * @param goalLineY              normalized Y of the goal line [0,1]
 * @param minApproachFrames      consecutive frames ball must move toward goal before counting
 * @param missProximityThreshold how close (in normalized Y) the ball must get to register a miss
 */
class TrajectoryGoalDetector(
    override var goalLineY: Float = 0.5f,
    private val minApproachFrames: Int = 3,
    private val missProximityThreshold: Float = 0.15f
) : GoalDetector {

    private val history = ArrayDeque<PointF>()
    private val maxHistory = 10

    override fun onFrame(detections: List<BallDetection>): GoalDetectorResult? {
        val ball = detections.maxByOrNull { it.confidence }

        if (ball == null) {
            // Ball lost — clear history so a new approach starts fresh
            history.clear()
            return null
        }

        val cx = (ball.boundingBox.left + ball.boundingBox.right) / 2f
        val cy = (ball.boundingBox.top  + ball.boundingBox.bottom) / 2f

        history.addLast(PointF(cx, cy))
        if (history.size > maxHistory) history.removeFirst()

        if (history.size < 2) return null

        val prevY = history[history.size - 2].y
        val currY = cy

        // GOAL: ball just crossed the goal line this frame
        if (prevY < goalLineY && currY >= goalLineY) {
            // Validate: ball was actually approaching (at least minApproachFrames moving toward line)
            if (isApproaching()) {
                reset()
                return GoalDetectorResult(isGoal = true, cx = cx, cy = cy)
            }
        }

        // MISS: ball came close to the line but is now moving away
        val nearLine = prevY >= (goalLineY - missProximityThreshold)
        val movingAway = currY < prevY
        if (nearLine && movingAway && isApproaching()) {
            reset()
            return GoalDetectorResult(isGoal = false, cx = cx, cy = cy)
        }

        return null
    }

    override fun reset() {
        history.clear()
    }

    /** Returns true if the last [minApproachFrames] positions show consistent movement toward goal. */
    private fun isApproaching(): Boolean {
        if (history.size < minApproachFrames) return false
        val recent = history.takeLast(minApproachFrames)
        return recent.zipWithNext().all { (a, b) -> b.y > a.y }
    }
}
