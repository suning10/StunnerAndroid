package com.seclass.stunner.model

import android.graphics.RectF

data class DetectionState(
    val isDetecting: Boolean = false,
    val lastEvent: GoalEvent? = null,
    val ballBounds: RectF? = null,   // current ball bounding box in frame (normalized)
    val goalLineY: Float = 0.5f,     // normalized Y where goal line sits; set during calibration
    val sessionGoals: Int = 0,
    val sessionShots: Int = 0,
    val showGoalOverlay: Boolean = false
) {
    val shotAccuracy: Float
        get() = if (sessionShots == 0) 0f else sessionGoals.toFloat() / sessionShots
}
