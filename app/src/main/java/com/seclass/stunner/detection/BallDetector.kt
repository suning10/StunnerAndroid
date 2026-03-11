package com.seclass.stunner.detection

import android.content.Context
import android.graphics.Bitmap
import com.seclass.stunner.model.BallDetection

/**
 * Abstraction for ball detection backends.
 * Swap implementations without touching the rest of the app:
 *   - TFLiteBallDetector  (current)
 *   - YoloBallDetector    (future, OpenCV + YOLO via ONNX Runtime)
 */
interface BallDetector {
    fun initialize(context: Context)
    fun detect(bitmap: Bitmap): List<BallDetection>
    fun release()
}
