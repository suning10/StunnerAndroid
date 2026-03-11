package com.seclass.stunner.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.seclass.stunner.model.BallDetection

/**
 * TensorFlow Lite implementation of [BallDetector].
 *
 * Uses the COCO SSD MobileNet model which includes "sports ball" (class 32).
 * Place the model file at: app/src/main/assets/ssd_mobilenet_v2.tflite
 *
 * To swap to OpenCV + YOLO in the future:
 *   1. Create YoloBallDetector : BallDetector
 *   2. Pass it wherever TFLiteBallDetector is injected — nothing else changes.
 */
class TFLiteBallDetector : BallDetector {

    private var isInitialized = false

    // TODO: lateinit var interpreter: Interpreter
    // TODO: lateinit var options: ObjectDetector.ObjectDetectorOptions

    companion object {
        private const val MODEL_FILE = "ssd_mobilenet_v2.tflite"
        private const val SPORTS_BALL_CLASS = 32   // COCO label index
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val INPUT_SIZE = 300          // SSD MobileNet input: 300×300
    }

    override fun initialize(context: Context) {
        // TODO:
        //   val options = ObjectDetector.ObjectDetectorOptions.builder()
        //       .setMaxResults(5)
        //       .setScoreThreshold(CONFIDENCE_THRESHOLD)
        //       .build()
        //   interpreter = ObjectDetector.createFromFileAndOptions(context, MODEL_FILE, options)
        isInitialized = true
    }

    override fun detect(bitmap: Bitmap): List<BallDetection> {
        if (!isInitialized) return emptyList()

        // TODO:
        //   val image = TensorImage.fromBitmap(bitmap)
        //   val results = interpreter.detect(image)
        //   return results
        //       .filter { it.categories.any { c -> c.index == SPORTS_BALL_CLASS } }
        //       .map { detection ->
        //           BallDetection(
        //               boundingBox = detection.boundingBox.toNormalized(bitmap.width, bitmap.height),
        //               confidence  = detection.categories.first().score
        //           )
        //       }

        return emptyList()
    }

    override fun release() {
        // TODO: interpreter.close()
        isInitialized = false
    }
}
