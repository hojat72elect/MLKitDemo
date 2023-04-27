package ca.on.hojat.mlkitdemo.shared.posedetector.classification

import android.util.Pair
import ca.on.hojat.mlkitdemo.shared.posedetector.classification.PoseEmbedding.getPoseEmbedding
import ca.on.hojat.mlkitdemo.shared.posedetector.classification.Utils.maxAbs
import ca.on.hojat.mlkitdemo.shared.posedetector.classification.Utils.multiply
import ca.on.hojat.mlkitdemo.shared.posedetector.classification.Utils.multiplyAll
import ca.on.hojat.mlkitdemo.shared.posedetector.classification.Utils.subtract
import ca.on.hojat.mlkitdemo.shared.posedetector.classification.Utils.sumAbs
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.pose.Pose
import java.util.PriorityQueue

/**
 *
 * Classifies [Pose] based on given [PoseSample]s.
 *
 * Inspired by [K-Nearest Neighbors Algorithm](https://en.wikipedia.org/wiki/K-nearest_neighbors_algorithm) with outlier
 * filtering.
 */
class PoseClassifier(
    private val poseSamples: List<PoseSample>,
    private val maxDistanceTopK: Int,
    private val meanDistanceTopK: Int,
    private val axesWeights: PointF3D
) {

    constructor(poseSamples: List<PoseSample>) : this(
        poseSamples,
        MAX_DISTANCE_TOP_K,
        MEAN_DISTANCE_TOP_K,
        AXES_WEIGHTS
    )

    /**
     * Returns the max range of confidence values.
     *
     *
     * Since we calculate confidence by counting the [PoseSample]s that survived outlier-filtering by maxDistanceTopK and meanDistanceTopK, this range is the minimum of two.
     */
    fun confidenceRange(): Int {
        return minOf(maxDistanceTopK, meanDistanceTopK)
    }

    fun classify(pose: Pose) = classify(extractPoseLandmarks(pose))

    private fun classify(landmarks: List<PointF3D>): ClassificationResult {
        val result = ClassificationResult()
        // Return early if no landmarks detected.
        if (landmarks.isEmpty())
            return result


        // We do flipping on X-axis so we are horizontal (mirror) invariant.
        val flippedLandmarks: MutableList<PointF3D> = ArrayList(landmarks)
        multiplyAll(flippedLandmarks, PointF3D.from(-1f, 1f, 1f))

        val embedding = getPoseEmbedding(landmarks)
        val flippedEmbedding = getPoseEmbedding(flippedLandmarks)


        // Classification is done in two stages:
        //  * First we pick top-K samples by MAX distance. It allows to remove samples that are almost
        //    the same as given pose, but maybe has few joints bent in the other direction.
        //  * Then we pick top-K samples by MEAN distance. After outliers are removed, we pick samples
        //    that are closest by average.

        // Keeps max distance on top so we can pop it when top_k size is reached.
        val maxDistances = PriorityQueue<Pair<PoseSample, Float>>(
            maxDistanceTopK
        ) { o1, o2 ->
            -(o1.second!!).compareTo(o2.second!!)
        }
        // Retrieve top K poseSamples by least distance to remove outliers.
        for (poseSample in poseSamples) {
            val sampleEmbedding = poseSample.embedding

            var originalMax = 0f
            var flippedMax = 0f

            for (i in embedding.indices) {
                originalMax = maxOf(
                    originalMax, maxAbs(
                        multiply(
                            subtract(
                                embedding[i],
                                sampleEmbedding[i]
                            ), axesWeights
                        )
                    )
                )
                flippedMax = maxOf(
                    flippedMax, maxAbs(
                        multiply(
                            subtract(
                                flippedEmbedding[i],
                                sampleEmbedding[i]
                            ), axesWeights
                        )
                    )
                )
            }
            // Set the max distance as min of original and flipped max distance.
            maxDistances.add(Pair(poseSample, minOf(originalMax, flippedMax)))
            // We only want to retain top n so pop the highest distance.
            if (maxDistances.size > maxDistanceTopK) {
                maxDistances.poll()
            }
        }

        // Keeps higher mean distances on top so we can pop it when top_k size is reached.
        val meanDistances = PriorityQueue<Pair<PoseSample, Float>>(
            meanDistanceTopK
        ) { o1, o2 ->
            -(o1.second!!).compareTo(o2.second!!)
        }
        // Retrieve top K poseSamples by least mean distance to remove outliers.
        for (sampleDistances in maxDistances) {
            val poseSample = sampleDistances.first
            val sampleEmbedding = poseSample.embedding

            var originalSum = 0f
            var flippedSum = 0f

            for (i in embedding.indices) {
                originalSum += sumAbs(
                    multiply(
                        subtract(embedding[i], sampleEmbedding[i]),
                        axesWeights
                    )
                )
                flippedSum += sumAbs(
                    multiply(
                        subtract(flippedEmbedding[i], sampleEmbedding[i]),
                        axesWeights
                    )
                )
            }
            // Set the mean distance as min of original and flipped mean distances.
            val meanDistance = minOf(originalSum, flippedSum) / (embedding.size * 2)
            meanDistances.add(Pair(poseSample, meanDistance))
            // We only want to retain top k so pop the highest mean distance.
            if (meanDistances.size > meanDistanceTopK) {
                meanDistances.poll()
            }
        }
        for (sampleDistances in meanDistances) {
            val className = sampleDistances.first.className
            result.incrementClassConfidence(className)
        }
        return result
    }

    companion object {
        private const val MAX_DISTANCE_TOP_K = 30
        private const val MEAN_DISTANCE_TOP_K = 10

        // Note Z has a lower weight as it is generally less accurate than X & Y.
        private val AXES_WEIGHTS = PointF3D.from(1f, 1f, 0.2f)

        private fun extractPoseLandmarks(pose: Pose) = pose.allPoseLandmarks.map { poseLandmark ->
            poseLandmark.position3D
        }


    }
}