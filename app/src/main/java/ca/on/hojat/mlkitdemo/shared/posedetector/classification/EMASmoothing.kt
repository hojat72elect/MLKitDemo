package ca.on.hojat.mlkitdemo.shared.posedetector.classification

import android.os.SystemClock
import java.util.Deque
import java.util.concurrent.LinkedBlockingDeque

/**
 * Runs EMA smoothing over a window with given stream of pose classification results.
 */
class EMASmoothing(private val windowSize: Int, private val alpha: Float) {

    // This is a window of {@link ClassificationResult}s as outputted by the {@link PoseClassifier}.
    // We run smoothing over this window of size {@link windowSize}.
    private val window: Deque<ClassificationResult> = LinkedBlockingDeque(windowSize)

    private var lastInputMs = 0L

    constructor() : this(DEFAULT_WINDOW_SIZE, DEFAULT_ALPHA)

    fun getSmoothedResult(classificationResult: ClassificationResult): ClassificationResult {

        // Resets memory if the input is too far away from the previous one in time.
        val nowMs = SystemClock.elapsedRealtime()
        if (nowMs - lastInputMs > RESET_THRESHOLD_MS)
            window.clear()
        lastInputMs = nowMs

        // If we are at window size, remove the last (oldest) result.
        if (window.size == windowSize)
            window.pollLast()

        // Insert at the beginning of the window.
        window.addFirst(classificationResult)

        val allClasses = HashSet<String>()
        for (result in window)
            allClasses.addAll(result.allClasses)

        val smoothedResult = ClassificationResult()

        for (className in allClasses) {
            var factor = 1f
            var topSum = 0f
            var bottomSum = 0f
            for (result in window) {
                val value = result.getClassConfidence(className)

                topSum += factor * value
                bottomSum += factor

                factor *= (1 - alpha)
            }
            smoothedResult.putClassConfidence(className, topSum / bottomSum)
        }
        return smoothedResult
    }

    companion object {
        private const val DEFAULT_WINDOW_SIZE = 10
        private const val DEFAULT_ALPHA = 0.2f
        private const val RESET_THRESHOLD_MS = 100L
    }
}