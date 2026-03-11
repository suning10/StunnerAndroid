package com.seclass.stunner.model

import android.graphics.RectF

/**
 * A single ball detection result from one camera frame.
 *
 * @param boundingBox  Normalized coordinates [0,1] relative to frame size (left, top, right, bottom)
 * @param confidence   Model confidence score [0,1]
 * @param timestampMs  Frame timestamp in milliseconds
 */
data class BallDetection(
    val boundingBox: RectF,
    val confidence: Float,
    val timestampMs: Long = System.currentTimeMillis()
)
