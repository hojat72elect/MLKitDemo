package ca.on.hojat.mlkitdemo.shared.posedetector.classification

import ca.on.hojat.mlkitdemo.shared.posedetector.classification.Utils.average
import ca.on.hojat.mlkitdemo.shared.posedetector.classification.Utils.l2Norm2D
import ca.on.hojat.mlkitdemo.shared.posedetector.classification.Utils.multiplyAll
import ca.on.hojat.mlkitdemo.shared.posedetector.classification.Utils.subtract
import ca.on.hojat.mlkitdemo.shared.posedetector.classification.Utils.subtractAll
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Generates embedding for given list of Pose landmarks.
 */
object PoseEmbedding {

    // Multiplier to apply to the torso to get minimal
    // body size. Picked this by experimentation.
    private const val TORSO_MULTIPLIER = 2.5f

    @JvmStatic
    fun getPoseEmbedding(landmarks: List<PointF3D>): List<PointF3D> {

        val normalizedLandmarks = normalize(landmarks)
        return getEmbedding(normalizedLandmarks)
    }

    private fun normalize(landmarks: List<PointF3D>): List<PointF3D> {
        val normalizedLandmarks = ArrayList(landmarks)
        // Normalize translation.
        val center = average(
            landmarks[PoseLandmark.LEFT_HIP],
            landmarks[PoseLandmark.RIGHT_HIP]
        )
        subtractAll(center, normalizedLandmarks)

        // Normalize scale.
        multiplyAll(normalizedLandmarks, 1 / getPoseSize(normalizedLandmarks))
        // Multiplication by 100 is not required, but makes it easier to debug.
        multiplyAll(normalizedLandmarks, 100f)
        return normalizedLandmarks

    }

    // Translation normalization should've been done
    // prior to calling this method.
    private fun getPoseSize(landmarks: List<PointF3D>): Float {

        // Note: This approach uses only 2D landmarks to compute pose size as using Z wasn't helpful
        // in our experimentation but you're welcome to tweak.
        val hipsCenter = average(
            landmarks[PoseLandmark.LEFT_HIP],
            landmarks[PoseLandmark.RIGHT_HIP]
        )

        val shouldersCenter = average(
            landmarks[PoseLandmark.LEFT_SHOULDER],
            landmarks[PoseLandmark.RIGHT_SHOULDER]
        )

        val torsoSize = l2Norm2D(subtract(hipsCenter, shouldersCenter))

        var maxDistance = torsoSize * TORSO_MULTIPLIER
        // torsoSize * TORSO_MULTIPLIER is the floor we want based on experimentation but actual size
        // can be bigger for a given pose depending on extension of limbs etc so we calculate that.
        for (landmark in landmarks) {
            val distance = l2Norm2D(subtract(hipsCenter, landmark))
            if (distance > maxDistance)
                maxDistance = distance
        }
        return maxDistance
    }

    private fun getEmbedding(landmarks: List<PointF3D>): List<PointF3D> {

        val embedding = mutableListOf<PointF3D>()

        // We use several pairwise 3D distances to form pose embedding. These were selected
        // based on experimentation for best results with our default pose classes as captured in the
        // pose samples csv. Feel free to play with this and add or remove for your use-cases.

        // We group our distances by number of joints between the pairs.
        // One joint.
        embedding.add(
            subtract(
                average(landmarks[PoseLandmark.LEFT_HIP], landmarks[PoseLandmark.RIGHT_HIP]),
                average(
                    landmarks[PoseLandmark.LEFT_SHOULDER],
                    landmarks[PoseLandmark.RIGHT_SHOULDER]
                )
            )
        )

        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_ELBOW]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_ELBOW]
            )
        )

        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_ELBOW],
                landmarks[PoseLandmark.LEFT_WRIST]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.RIGHT_ELBOW],
                landmarks[PoseLandmark.RIGHT_WRIST]
            )
        )

        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_HIP],
                landmarks[PoseLandmark.LEFT_KNEE]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.RIGHT_HIP],
                landmarks[PoseLandmark.RIGHT_KNEE]
            )
        )

        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_KNEE],
                landmarks[PoseLandmark.LEFT_ANKLE]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.RIGHT_KNEE],
                landmarks[PoseLandmark.RIGHT_ANKLE]
            )
        )

        // Two joints.
        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_WRIST]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_WRIST]
            )
        )

        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_HIP],
                landmarks[PoseLandmark.LEFT_ANKLE]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.RIGHT_HIP],
                landmarks[PoseLandmark.RIGHT_ANKLE]
            )
        )

        // Four joints.
        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_HIP],
                landmarks[PoseLandmark.LEFT_WRIST]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.RIGHT_HIP],
                landmarks[PoseLandmark.RIGHT_WRIST]
            )
        )

        // Five joints.
        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_SHOULDER], landmarks[PoseLandmark.LEFT_ANKLE]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.RIGHT_SHOULDER], landmarks[PoseLandmark.RIGHT_ANKLE]
            )
        )

        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_HIP],
                landmarks[PoseLandmark.LEFT_WRIST]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.RIGHT_HIP],
                landmarks[PoseLandmark.RIGHT_WRIST]
            )
        )

        // Cross body.
        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_ELBOW],
                landmarks[PoseLandmark.RIGHT_ELBOW]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_KNEE],
                landmarks[PoseLandmark.RIGHT_KNEE]
            )
        )

        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_WRIST],
                landmarks[PoseLandmark.RIGHT_WRIST]
            )
        )
        embedding.add(
            subtract(
                landmarks[PoseLandmark.LEFT_ANKLE],
                landmarks[PoseLandmark.RIGHT_ANKLE]
            )
        )

        return embedding
    }
}