package ca.on.hojat.mlkitdemo.shared.posedetector.classification

import android.util.Log
import com.google.common.base.Splitter
import com.google.mlkit.vision.common.PointF3D

/**
 * Reads Pose samples from a csv file.
 */
class PoseSample(val className: String, landmarks: List<PointF3D>) {

    val embedding: List<PointF3D> = PoseEmbedding.getPoseEmbedding(landmarks)

    companion object {
        private const val TAG = "PoseSample"
        private const val NUM_LANDMARKS = 33
        private const val NUM_DIMS = 3

        @JvmStatic
        fun getPoseSample(csvLine: String, separator: String): PoseSample? {
            val tokens = Splitter.onPattern(separator).splitToList(csvLine)
            // Format is expected to be Name,Class,X1,Y1,Z1,X2,Y2,Z2...
            // + 2 is for Name & Class.
            if (tokens.size != (NUM_LANDMARKS * NUM_DIMS) + 2) {
                Log.e(TAG, "Invalid number of tokens for PoseSample")
                return null
            }
            val className = tokens[1]
            val landmarks = mutableListOf<PointF3D>()
            // Read from the third token, first 2 tokens are name and class.
            var i = 2
            while (i < tokens.size) {
                val exceptions = listOf(NullPointerException::class, NumberFormatException::class)
                try {
                    landmarks.add(
                        PointF3D.from(
                            tokens[i].toFloat(),
                            tokens[i + 1].toFloat(),
                            tokens[i + 2].toFloat()
                        )
                    )
                } catch (e: Throwable) {
                    if (e::class in exceptions) {
                        Log.e(
                            TAG,
                            "Invalid value " + tokens[i] + " for landmark position."
                        )
                        return null
                    }
                }
                i += NUM_DIMS
            }
            return PoseSample(className, landmarks)
        }
    }
}