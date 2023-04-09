package ca.on.hojat.mlkitdemo.common.posedetector.classification;

import static ca.on.hojat.mlkitdemo.common.posedetector.classification.Utils.maxAbs;
import static ca.on.hojat.mlkitdemo.common.posedetector.classification.Utils.multiply;
import static ca.on.hojat.mlkitdemo.common.posedetector.classification.Utils.multiplyAll;
import static ca.on.hojat.mlkitdemo.common.posedetector.classification.Utils.subtract;
import static ca.on.hojat.mlkitdemo.common.posedetector.classification.Utils.sumAbs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseEmbedding.getPoseEmbedding;

/**
 * Classifies {link Pose} based on given {@link PoseSample}s.
 *
 * <p>Inspired by K-Nearest Neighbors Algorithm with outlier filtering.
 * <a href="https://en.wikipedia.org/wiki/K-nearest_neighbors_algorithm">...</a>
 */
public class PoseClassifier {
    private static final int MAX_DISTANCE_TOP_K = 30;
    private static final int MEAN_DISTANCE_TOP_K = 10;
    // Note Z has a lower weight as it is generally less accurate than X & Y.
    private static final com.google.mlkit.vision.common.PointF3D AXES_WEIGHTS = com.google.mlkit.vision.common.PointF3D.from(1, 1, 0.2f);

    private final java.util.List<ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample> poseSamples;
    private final int maxDistanceTopK;
    private final int meanDistanceTopK;
    private final com.google.mlkit.vision.common.PointF3D axesWeights;

    public PoseClassifier(java.util.List<ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample> poseSamples) {
        this(poseSamples, MAX_DISTANCE_TOP_K, MEAN_DISTANCE_TOP_K, AXES_WEIGHTS);
    }

    public PoseClassifier(java.util.List<ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample> poseSamples, int maxDistanceTopK, int meanDistanceTopK, com.google.mlkit.vision.common.PointF3D axesWeights) {
        this.poseSamples = poseSamples;
        this.maxDistanceTopK = maxDistanceTopK;
        this.meanDistanceTopK = meanDistanceTopK;
        this.axesWeights = axesWeights;
    }

    private static java.util.List<com.google.mlkit.vision.common.PointF3D> extractPoseLandmarks(com.google.mlkit.vision.pose.Pose pose) {
        java.util.List<com.google.mlkit.vision.common.PointF3D> landmarks = new java.util.ArrayList<>();
        for (com.google.mlkit.vision.pose.PoseLandmark poseLandmark : pose.getAllPoseLandmarks()) {
            landmarks.add(poseLandmark.getPosition3D());
        }
        return landmarks;
    }

    /**
     * Returns the max range of confidence values.
     *
     * <p><Since we calculate confidence by counting {@link PoseSample}s that survived
     * outlier-filtering by maxDistanceTopK and meanDistanceTopK, this range is the minimum of two.
     */
    public int confidenceRange() {
        return min(maxDistanceTopK, meanDistanceTopK);
    }

    public ca.on.hojat.mlkitdemo.common.posedetector.classification.ClassificationResult classify(com.google.mlkit.vision.pose.Pose pose) {
        return classify(extractPoseLandmarks(pose));
    }

    public ca.on.hojat.mlkitdemo.common.posedetector.classification.ClassificationResult classify(java.util.List<com.google.mlkit.vision.common.PointF3D> landmarks) {
        ca.on.hojat.mlkitdemo.common.posedetector.classification.ClassificationResult result = new ca.on.hojat.mlkitdemo.common.posedetector.classification.ClassificationResult();
        // Return early if no landmarks detected.
        if (landmarks.isEmpty()) {
            return result;
        }

        // We do flipping on X-axis so we are horizontal (mirror) invariant.
        java.util.List<com.google.mlkit.vision.common.PointF3D> flippedLandmarks = new java.util.ArrayList<>(landmarks);
        multiplyAll(flippedLandmarks, com.google.mlkit.vision.common.PointF3D.from(-1, 1, 1));

        java.util.List<com.google.mlkit.vision.common.PointF3D> embedding = getPoseEmbedding(landmarks);
        java.util.List<com.google.mlkit.vision.common.PointF3D> flippedEmbedding = getPoseEmbedding(flippedLandmarks);


        // Classification is done in two stages:
        //  * First we pick top-K samples by MAX distance. It allows to remove samples that are almost
        //    the same as given pose, but maybe has few joints bent in the other direction.
        //  * Then we pick top-K samples by MEAN distance. After outliers are removed, we pick samples
        //    that are closest by average.

        // Keeps max distance on top so we can pop it when top_k size is reached.
        java.util.PriorityQueue<android.util.Pair<ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample, Float>> maxDistances = new java.util.PriorityQueue<>(maxDistanceTopK, (o1, o2) -> -Float.compare(o1.second, o2.second));
        // Retrieve top K poseSamples by least distance to remove outliers.
        for (ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample poseSample : poseSamples) {
            java.util.List<com.google.mlkit.vision.common.PointF3D> sampleEmbedding = poseSample.getEmbedding();

            float originalMax = 0;
            float flippedMax = 0;
            for (int i = 0; i < embedding.size(); i++) {
                originalMax = max(originalMax, maxAbs(multiply(subtract(embedding.get(i), sampleEmbedding.get(i)), axesWeights)));
                flippedMax = max(flippedMax, maxAbs(multiply(subtract(flippedEmbedding.get(i), sampleEmbedding.get(i)), axesWeights)));
            }
            // Set the max distance as min of original and flipped max distance.
            maxDistances.add(new android.util.Pair<>(poseSample, min(originalMax, flippedMax)));
            // We only want to retain top n so pop the highest distance.
            if (maxDistances.size() > maxDistanceTopK) {
                maxDistances.poll();
            }
        }

        // Keeps higher mean distances on top so we can pop it when top_k size is reached.
        java.util.PriorityQueue<android.util.Pair<ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample, Float>> meanDistances = new java.util.PriorityQueue<>(meanDistanceTopK, (o1, o2) -> -Float.compare(o1.second, o2.second));
        // Retrive top K poseSamples by least mean distance to remove outliers.
        for (android.util.Pair<ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample, Float> sampleDistances : maxDistances) {
            ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample poseSample = sampleDistances.first;
            java.util.List<com.google.mlkit.vision.common.PointF3D> sampleEmbedding = poseSample.getEmbedding();

            float originalSum = 0;
            float flippedSum = 0;
            for (int i = 0; i < embedding.size(); i++) {
                originalSum += sumAbs(multiply(subtract(embedding.get(i), sampleEmbedding.get(i)), axesWeights));
                flippedSum += sumAbs(multiply(subtract(flippedEmbedding.get(i), sampleEmbedding.get(i)), axesWeights));
            }
            // Set the mean distance as min of original and flipped mean distances.
            float meanDistance = min(originalSum, flippedSum) / (embedding.size() * 2);
            meanDistances.add(new android.util.Pair<>(poseSample, meanDistance));
            // We only want to retain top k so pop the highest mean distance.
            if (meanDistances.size() > meanDistanceTopK) {
                meanDistances.poll();
            }
        }

        for (android.util.Pair<ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample, Float> sampleDistances : meanDistances) {
            String className = sampleDistances.first.getClassName();
            result.incrementClassConfidence(className);
        }

        return result;
    }
}
