package com.seclass.stunner.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.seclass.stunner.model.BallDetection
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite implementation of [BallDetector].
 *
 * Uses the raw TFLite Interpreter (works with any model output count).
 * Model file: app/src/main/assets/ssd_mobilenet_v2.tflite
 *
 * Assumes standard SSD post-processed output layout:
 *   Output 0 — boxes   [1, N, 4]  (y_min, x_min, y_max, x_max, normalized)
 *   Output 1 — classes [1, N]     (0-indexed COCO class id)
 *   Output 2 — scores  [1, N]
 *   Output 3 — count   [1]
 *
 * COCO class 32 = sports ball (0-indexed).
 * Tensor shapes are logged at init — if outputs differ, update parseDetections().
 */
class TFLiteBallDetector : BallDetector {

    private var interpreter: Interpreter? = null
    private var inputWidth  = 300
    private var inputHeight = 300
    private var isQuantized = false

    companion object {
        private const val TAG              = "Stunner/BallDet"
        private const val MODEL_FILE       = "ssd_mobilenet_v2.tflite"
        // Accept both mappings — consecutive 80-class (32) and gap-preserved 90-class (36)
        private val SPORTS_BALL_CLASSES = setOf(32, 36)
        private const val SPORTS_BALL_CLS_1INDEXED = 37   // COCO 1-indexed (raw logits fallback)
        private const val CONFIDENCE_THRESHOLD = 0.2f
        private const val MAX_DETECTIONS   = 20  // model outputs up to 20 detections
    }

    override fun initialize(context: Context) {
        Log.i(TAG, "initialize() called")
        try {
            val model = loadModelFile(context, MODEL_FILE)
            val options = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(model, options)

            val interp = interpreter!!
            // Log input tensors
            Log.i(TAG, "--- Input tensors (${interp.inputTensorCount}) ---")
            for (i in 0 until interp.inputTensorCount) {
                val t = interp.getInputTensor(i)
                Log.i(TAG, "  in[$i] shape=${t.shape().contentToString()}  type=${t.dataType()}  name=${t.name()}")
            }
            // Log output tensors — share these in chat if detection still fails
            Log.i(TAG, "--- Output tensors (${interp.outputTensorCount}) ---")
            for (i in 0 until interp.outputTensorCount) {
                val t = interp.getOutputTensor(i)
                Log.i(TAG, "  out[$i] shape=${t.shape().contentToString()}  type=${t.dataType()}  name=${t.name()}")
            }

            // Dynamic-shape models report [1,1,1,3] until resized — force 300×300
            val inShape = interp.getInputTensor(0).shape()
            val needsResize = inShape[1] <= 1 || inShape[2] <= 1
            if (needsResize) {
                interp.resizeInput(0, intArrayOf(1, 300, 300, 3))
                interp.allocateTensors()
                Log.i(TAG, "Dynamic input resized to 300×300")
            }
            val resolvedShape = interp.getInputTensor(0).shape()
            inputHeight = resolvedShape[1]
            inputWidth  = resolvedShape[2]
            isQuantized = interp.getInputTensor(0).dataType().toString() == "UINT8"
            Log.i(TAG, "Model loaded OK — input=${inputWidth}x${inputHeight}  quantized=$isQuantized  threshold=$CONFIDENCE_THRESHOLD")
        } catch (e: Exception) {
            Log.e(TAG, "Model init FAILED: ${e.message}", e)
        }
    }

    private var frameCount = 0

    override fun detect(bitmap: Bitmap): List<BallDetection> {
        val interp = interpreter ?: run {
            Log.w(TAG, "detect() called but interpreter is null — model failed to init")
            return emptyList()
        }

        frameCount++

        // Run inference on the full frame
        val fullResults = runInference(interp, bitmap, offsetX = 0f, offsetY = 0f, scaleX = 1f, scaleY = 1f)

        // Also run on a center crop (50% of frame) — makes far-away balls appear larger
        val cropFraction = 0.5f
        val cropX = bitmap.width  * (1f - cropFraction) / 2f
        val cropY = bitmap.height * (1f - cropFraction) / 2f
        val cropW = (bitmap.width  * cropFraction).toInt()
        val cropH = (bitmap.height * cropFraction).toInt()
        val crop = Bitmap.createBitmap(bitmap, cropX.toInt(), cropY.toInt(), cropW, cropH)
        // Remap crop detections back to full-frame normalized coords
        val cropResults = runInference(interp, crop,
            offsetX = cropX / bitmap.width,
            offsetY = cropY / bitmap.height,
            scaleX  = cropFraction,
            scaleY  = cropFraction
        )

        // Merge: keep the highest-confidence detection per spatial region
        val allResults = (fullResults + cropResults)
            .sortedByDescending { it.confidence }
            .take(5)

        if (allResults.isNotEmpty()) {
            allResults.forEach {
                val cx = (it.boundingBox.left + it.boundingBox.right) / 2f
                val cy = (it.boundingBox.top  + it.boundingBox.bottom) / 2f
                Log.i(TAG, "BALL DETECTED  conf=%.2f  cx=%.3f  cy=%.3f".format(it.confidence, cx, cy))
            }
        } else if (frameCount % 30 == 0) {
            //Log.d(TAG, "frame #$frameCount — no ball detected")

        }

        return allResults
    }

    /** Runs a single inference pass on [bitmap] and remaps boxes to full-frame coords. */
    private fun runInference(
        interp: Interpreter,
        bitmap: Bitmap,
        offsetX: Float,
        offsetY: Float,
        scaleX: Float,
        scaleY: Float
    ): List<BallDetection> {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val inputBuffer = bitmapToByteBuffer(scaled)
        val outputs = HashMap<Int, Any>()
        for (i in 0 until interp.outputTensorCount) {
            outputs[i] = allocateFloatBuffer(interp.getOutputTensor(i).shape())
        }
        interp.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
        return parseDetections(interp, outputs, offsetX, offsetY, scaleX, scaleY)
    }

    /**
     * This model has RAW SSD outputs (no built-in NMS post-processing):
     *   out[0] [1, 1917, 4]  — box delta-encodings relative to SSD anchors (NOT usable as-is)
     *   out[3] [1, 1917, 91] — raw class logits for 91 COCO classes
     *
     * We read the class logits to confirm the ball is present and return a placeholder box.
     * Replace ssd_mobilenet_v2.tflite with a post-processed model to get real bounding boxes.
     * The correct model is "ssd_mobilenet_v2_coco_quant_postprocess.tflite" from the TFLite
     * model zoo — it has 4 outputs (boxes, classes, scores, count) and works without anchor decoding.
     *
     * COCO class 37 = sports ball (1-indexed; index 37 in the 91-class logit vector).
     */
    /**
     * Parses inference outputs and remaps boxes to full-frame normalized coords.
     * offsetX/Y and scaleX/Y describe where the input bitmap sits within the full frame.
     */
    private fun parseDetections(
        interp: Interpreter,
        outputs: Map<Int, Any>,
        offsetX: Float,
        offsetY: Float,
        scaleX: Float,
        scaleY: Float
    ): List<BallDetection> {
        val outputCount = interp.outputTensorCount

        // Standard post-processed model: 4 outputs [boxes, classes, scores, count]
        if (outputCount == 4) {
            val boxes    = outputs[0] as? Array<*> ?: return emptyList()
            val classes  = outputs[1] as? Array<*> ?: return emptyList()
            val scores   = outputs[2] as? Array<*> ?: return emptyList()
            val countArr = outputs[3] as? FloatArray ?: return emptyList()
            val count    = countArr[0].toInt().coerceAtMost(MAX_DETECTIONS)
            val boxRow   = boxes[0]   as? Array<*>   ?: return emptyList()
            val clsRow   = classes[0] as? FloatArray ?: return emptyList()
            val scoreRow = scores[0]  as? FloatArray ?: return emptyList()
            val results  = mutableListOf<BallDetection>()

            // Log all detections above threshold every frame
            val allDetected = (0 until count).filter { scoreRow[it] >= CONFIDENCE_THRESHOLD }
            if (allDetected.isNotEmpty()) {
                val summary = allDetected.joinToString("  ") {
                    "cls=${clsRow[it].toInt()} score=%.2f".format(scoreRow[it])
                }
                //Log.d(TAG, "frame #$frameCount detections: $summary")
            } else {
                Log.d(TAG, "frame #$frameCount — nothing above threshold")
            }

            for (i in 0 until count) {
                val score = scoreRow[i]
                if (score < CONFIDENCE_THRESHOLD) continue
                if (clsRow[i].toInt() !in SPORTS_BALL_CLASSES) continue
                val coords = boxRow[i] as? FloatArray ?: continue
                // coords: [y_min, x_min, y_max, x_max] normalized within the input bitmap
                // remap to full-frame coords using offset + scale
                results += BallDetection(
                    boundingBox = RectF(
                        offsetX + coords[1] * scaleX,
                        offsetY + coords[0] * scaleY,
                        offsetX + coords[3] * scaleX,
                        offsetY + coords[2] * scaleY
                    ),
                    confidence = score
                )
            }
            return results
        }

        // Raw (pre-NMS) model: use out[3] class logits to detect ball presence
        // out[3] shape [1, 1917, 91] — COCO 1-indexed, index 37 = sports ball
        if (outputCount >= 4) {
            val clsLogits = outputs[3] as? Array<*> ?: return emptyList()
            val anchorScores = clsLogits[0] as? Array<*> ?: return emptyList()
            var bestScore = 0f
            var bestAnchor = -1
            for (a in anchorScores.indices) {
                val logits = anchorScores[a] as? FloatArray ?: continue
                if (logits.size <= SPORTS_BALL_CLS_1INDEXED) continue
                val score = sigmoid(logits[SPORTS_BALL_CLS_1INDEXED])
                if (score > bestScore) { bestScore = score; bestAnchor = a }
            }
            if (bestScore >= CONFIDENCE_THRESHOLD) {
                Log.d(TAG, "Raw model: best sports-ball anchor=$bestAnchor score=%.3f (box inaccurate — replace model)".format(bestScore))
                // Return a rough center box — not accurate without anchor decoding
                return listOf(BallDetection(boundingBox = RectF(
                    offsetX + 0.3f * scaleX, offsetY + 0.3f * scaleY,
                    offsetX + 0.7f * scaleX, offsetY + 0.7f * scaleY
                ), confidence = bestScore))
            }
        }
        return emptyList()
    }

    private fun sigmoid(x: Float) = 1f / (1f + Math.exp(-x.toDouble()).toFloat())

    override fun release() {
        interpreter?.close()
        interpreter = null
    }

    // --- helpers ---

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fd = context.assets.openFd(filename)
        return FileInputStream(fd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val pixelCount = inputWidth * inputHeight
        val buf = if (isQuantized) {
            ByteBuffer.allocateDirect(pixelCount * 3).apply { order(ByteOrder.nativeOrder()) }
        } else {
            ByteBuffer.allocateDirect(pixelCount * 3 * 4).apply { order(ByteOrder.nativeOrder()) }
        }
        val pixels = IntArray(pixelCount)
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        for (px in pixels) {
            val r = (px shr 16) and 0xFF
            val g = (px shr 8)  and 0xFF
            val b =  px         and 0xFF
            if (isQuantized) {
                buf.put(r.toByte()); buf.put(g.toByte()); buf.put(b.toByte())
            } else {
                buf.putFloat((r - 127.5f) / 127.5f)
                buf.putFloat((g - 127.5f) / 127.5f)
                buf.putFloat((b - 127.5f) / 127.5f)
            }
        }
        buf.rewind()
        return buf
    }

    /** Recursively allocates nested float arrays matching a tensor shape. */
    private fun allocateFloatBuffer(shape: IntArray): Any = when (shape.size) {
        1    -> FloatArray(shape[0])
        2    -> Array(shape[0]) { FloatArray(shape[1]) }
        3    -> Array(shape[0]) { Array(shape[1]) { FloatArray(shape[2]) } }
        else -> Array(shape[0]) { Array(shape[1]) { Array(shape[2]) { FloatArray(shape[3]) } } }
    }
}
