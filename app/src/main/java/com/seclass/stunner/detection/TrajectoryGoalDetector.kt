package com.seclass.stunner.detection

import android.graphics.PointF
import com.seclass.stunner.model.BallDetection

/**
 * Multi-frame trajectory-based goal detector.
 *
 * Three ways a GOAL can be detected (first one to fire wins):
 *  1. Y-crossing  : ball net-moves toward goalLineY across minApproachFrames, then crosses it
 *                   (works in both directions — handles any camera tilt)
 *  2. Proximity   : ball bounding-box area exceeds [proximityGoalArea] of the frame
 *                   (handles camera-at-goal where ball gets larger rather than moving in Y)
 *
 * MISS: ball approached within [missProximityThreshold] of goalLineY then reversed, OR
 *       ball was large but shrank again (moved away).
 *
 * @param goalLineY              normalized Y of the goal line [0,1]
 * @param minApproachFrames      frames needed to confirm net approach direction
 * @param missProximityThreshold how close (normalized Y) the ball must get to register a miss
 * @param missedFrameTolerance   consecutive missed detections allowed before history clears
 * @param proximityGoalArea      ball bounding-box area (fraction of frame) that triggers a goal
 *                               e.g. 0.10 = ball fills 10% of frame. Set to 1f to disable.
 */
class TrajectoryGoalDetector(
    override var goalLineY: Float = 0.5f,
    private val minApproachFrames: Int = 3,
    private val missProximityThreshold: Float = 0.15f,
    private val missedFrameTolerance: Int = 3,
    private val proximityGoalArea: Float = 0.12f
) : GoalDetector {

    private val history = ArrayDeque<PointF>()
    private val maxHistory = 10
    private var missedFrames = 0
    private var maxSeenArea = 0f   // track peak box area to detect when ball moves away

    companion object {
        private const val TAG = "Stunner/GoalDet"
    }

    override fun onFrame(detections: List<BallDetection>): GoalDetectorResult? {
        val ball = detections.maxByOrNull { it.confidence }

        if (ball == null) {
            missedFrames++
            if (missedFrames <= missedFrameTolerance) return null
            if (history.isNotEmpty()) {
                android.util.Log.d(TAG, "Ball lost — clearing history after $missedFrames missed frames")
            }
            history.clear()
            missedFrames = 0
            maxSeenArea = 0f
            return null
        }

        missedFrames = 0

        val cx = (ball.boundingBox.left  + ball.boundingBox.right)  / 2f
        val cy = (ball.boundingBox.top   + ball.boundingBox.bottom) / 2f
        val bw = ball.boundingBox.right  - ball.boundingBox.left
        val bh = ball.boundingBox.bottom - ball.boundingBox.top
        val area = bw * bh

        history.addLast(PointF(cx, cy))
        if (history.size > maxHistory) history.removeFirst()

        android.util.Log.v(TAG,
            "cy=%.3f  goalLineY=%.3f  area=%.3f  histSize=${history.size}  approaching=${netApproaching()}"
                .format(cy, goalLineY, area))

        // ── Proximity goal: ball fills enough of the frame ──────────────────
        if (proximityGoalArea < 1f) {
            if (area >= proximityGoalArea) {
                android.util.Log.i(TAG,
                    "*** GOAL (proximity) ***  cx=%.3f cy=%.3f area=%.3f >= threshold=%.3f"
                        .format(cx, cy, area, proximityGoalArea))
                val result = GoalDetectorResult(isGoal = true, cx = cx, cy = cy)
                reset()
                return result
            }
            // Miss via proximity: ball was large but is now shrinking (moving away)
            if (maxSeenArea >= proximityGoalArea * 0.7f && area < maxSeenArea * 0.6f) {
                android.util.Log.i(TAG,
                    "*** MISS (proximity) ***  area dropped %.3f→%.3f".format(maxSeenArea, area))
                val result = GoalDetectorResult(isGoal = false, cx = cx, cy = cy)
                reset()
                return result
            }
        }
        maxSeenArea = maxOf(maxSeenArea, area)

        // ── Y-crossing goal ──────────────────────────────────────────────────
        if (history.size >= 2) {
            val prevY = history[history.size - 2].y
            val approaching = netApproaching()

            // Bidirectional crossing: top→bottom OR bottom→top
            val crossedDownward = prevY < goalLineY && cy >= goalLineY
            val crossedUpward   = prevY > goalLineY && cy <= goalLineY

            if (crossedDownward || crossedUpward) {
                if (approaching) {
                    android.util.Log.i(TAG,
                        "*** GOAL (crossing) ***  cx=%.3f cy=%.3f crossed goalLineY=%.3f"
                            .format(cx, cy, goalLineY))
                    val result = GoalDetectorResult(isGoal = true, cx = cx, cy = cy)
                    reset()
                    return result
                } else {
                    android.util.Log.d(TAG,
                        "Line crossed but netApproaching=false — ignored  histSize=${history.size}")
                }
            }

            // Miss via Y: near line but moving away
            val nearLine   = minOf(kotlin.math.abs(cy - goalLineY), kotlin.math.abs(prevY - goalLineY)) < missProximityThreshold
            val movingAway = if (goalLineY > 0.5f) cy < prevY else cy > prevY
            if (nearLine && movingAway && approaching) {
                android.util.Log.i(TAG,
                    "*** MISS (crossing) ***  cx=%.3f cy=%.3f near goalLine=%.3f".format(cx, cy, goalLineY))
                val result = GoalDetectorResult(isGoal = false, cx = cx, cy = cy)
                reset()
                return result
            }
        }

        return null
    }

    override fun reset() {
        history.clear()
        missedFrames = 0
        maxSeenArea = 0f
    }

    /**
     * Returns true if the net Y movement over the last [minApproachFrames] positions
     * is toward the goal line.  Uses first→last delta (not strict per-pair monotonic)
     * so minor jitter doesn't break the chain.
     */
    private fun netApproaching(): Boolean {
        if (history.size < minApproachFrames) return false
        val recent   = history.takeLast(minApproachFrames)
        val netDelta = recent.last().y - recent.first().y   // positive = moving down
        val towardGoal = if (recent.last().y <= goalLineY) netDelta > 0f   // approaching from above
                         else                               netDelta < 0f  // approaching from below
        return towardGoal && kotlin.math.abs(netDelta) > 0.01f  // at least 1% movement
    }
}
