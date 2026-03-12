package com.seclass.stunner.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.seclass.stunner.model.BallDetection
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector

/**
 * TensorFlow Lite implementation of [BallDetector].
 *
 * Uses the COCO SSD MobileNet model — "sports ball" label covers soccer balls.
 * Model file: app/src/main/assets/ssd_mobilenet_v2.tflite
 *
 * To swap to OpenCV + YOLO in the future:
 *   1. Create YoloBallDetector : BallDetector
 *   2. Pass it wherever TFLiteBallDetector is injected — nothing else changes.
 */
class TFLiteBallDetector : BallDetector {

    private var detector: ObjectDetector? = null

    companion object {
        private const val MODEL_FILE = "ssd_mobilenet_v2.tflite"
        private const val SPORTS_BALL_LABEL = "sports ball"
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val MAX_RESULTS = 5
    }

    override fun initialize(context: Context) {
        try {
            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(MAX_RESULTS)
                .setScoreThreshold(CONFIDENCE_THRESHOLD)
                .build()
            detector = ObjectDetector.createFromFileAndOptions(context, MODEL_FILE, options)
        } catch (e: Exception) {
            // Non-fatal on emulators — detection will return empty results
            android.util.Log.w("TFLiteBallDetector", "Detector init failed: ${e.message}")
        }
    }

    override fun detect(bitmap: Bitmap): List<BallDetection> {
        val d = detector ?: return emptyList()

        val results = d.detect(TensorImage.fromBitmap(bitmap))

        return results
            .filter { detection ->
                detection.categories.any {
                    it.label.equals(SPORTS_BALL_LABEL, ignoreCase = true)
                }
            }
            .map { detection ->
                val box = detection.boundingBox
                val score = detection.categories
                    .first { it.label.equals(SPORTS_BALL_LABEL, ignoreCase = true) }
                    .score

                // Normalize bounding box from pixel coords to [0,1]
                BallDetection(
                    boundingBox = RectF(
                        box.left   / bitmap.width,
                        box.top    / bitmap.height,
                        box.right  / bitmap.width,
                        box.bottom / bitmap.height
                    ),
                    confidence = score
                )
            }
    }

    override fun release() {
        detector?.close()
        detector = null
    }
}
