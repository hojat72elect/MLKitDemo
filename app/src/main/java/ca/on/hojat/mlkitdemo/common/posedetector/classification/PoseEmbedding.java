/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.on.hojat.mlkitdemo.common.posedetector.classification;

import static ca.on.hojat.mlkitdemo.common.posedetector.classification.Utils.average;
import static ca.on.hojat.mlkitdemo.common.posedetector.classification.Utils.l2Norm2D;
import static ca.on.hojat.mlkitdemo.common.posedetector.classification.Utils.multiplyAll;
import static ca.on.hojat.mlkitdemo.common.posedetector.classification.Utils.subtract;
import static ca.on.hojat.mlkitdemo.common.posedetector.classification.Utils.subtractAll;

/**
 * Generates embedding for given list of Pose landmarks.
 */
public class PoseEmbedding {
    // Multiplier to apply to the torso to get minimal body size. Picked this by experimentation.
    private static final float TORSO_MULTIPLIER = 2.5f;

    private PoseEmbedding() {
    }

    public static java.util.List<com.google.mlkit.vision.common.PointF3D> getPoseEmbedding(java.util.List<com.google.mlkit.vision.common.PointF3D> landmarks) {
        java.util.List<com.google.mlkit.vision.common.PointF3D> normalizedLandmarks = normalize(landmarks);
        return getEmbedding(normalizedLandmarks);
    }

    private static java.util.List<com.google.mlkit.vision.common.PointF3D> normalize(java.util.List<com.google.mlkit.vision.common.PointF3D> landmarks) {
        java.util.List<com.google.mlkit.vision.common.PointF3D> normalizedLandmarks = new java.util.ArrayList<>(landmarks);
        // Normalize translation.
        com.google.mlkit.vision.common.PointF3D center = average(
                landmarks.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP), landmarks.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP));
        subtractAll(center, normalizedLandmarks);

        // Normalize scale.
        multiplyAll(normalizedLandmarks, 1 / getPoseSize(normalizedLandmarks));
        // Multiplication by 100 is not required, but makes it easier to debug.
        multiplyAll(normalizedLandmarks, 100);
        return normalizedLandmarks;
    }

    // Translation normalization should've been done prior to calling this method.
    private static float getPoseSize(java.util.List<com.google.mlkit.vision.common.PointF3D> landmarks) {
        // Note: This approach uses only 2D landmarks to compute pose size as using Z wasn't helpful
        // in our experimentation but you're welcome to tweak.
        com.google.mlkit.vision.common.PointF3D hipsCenter = average(
                landmarks.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP), landmarks.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP));

        com.google.mlkit.vision.common.PointF3D shouldersCenter = average(
                landmarks.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER),
                landmarks.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER));

        float torsoSize = l2Norm2D(subtract(hipsCenter, shouldersCenter));

        float maxDistance = torsoSize * TORSO_MULTIPLIER;
        // torsoSize * TORSO_MULTIPLIER is the floor we want based on experimentation but actual size
        // can be bigger for a given pose depending on extension of limbs etc so we calculate that.
        for (com.google.mlkit.vision.common.PointF3D landmark : landmarks) {
            float distance = l2Norm2D(subtract(hipsCenter, landmark));
            if (distance > maxDistance) {
                maxDistance = distance;
            }
        }
        return maxDistance;
    }

    private static java.util.List<com.google.mlkit.vision.common.PointF3D> getEmbedding(java.util.List<com.google.mlkit.vision.common.PointF3D> lm) {
        java.util.List<com.google.mlkit.vision.common.PointF3D> embedding = new java.util.ArrayList<>();

        // We use several pairwise 3D distances to form pose embedding. These were selected
        // based on experimentation for best results with our default pose classes as captued in the
        // pose samples csv. Feel free to play with this and add or remove for your use-cases.

        // We group our distances by number of joints between the pairs.
        // One joint.
        embedding.add(subtract(
                average(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP)),
                average(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER))
        ));

        embedding.add(subtract(
                lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER), lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_ELBOW)));
        embedding.add(subtract(
                lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ELBOW)));

        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_ELBOW), lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_WRIST)));
        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ELBOW), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_WRIST)));

        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP), lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE)));
        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE)));

        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE), lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE)));
        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE)));

        // Two joints.
        embedding.add(subtract(
                lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER), lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_WRIST)));
        embedding.add(subtract(
                lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_WRIST)));

        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP), lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE)));
        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE)));

        // Four joints.
        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP), lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_WRIST)));
        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_WRIST)));

        // Five joints.
        embedding.add(subtract(
                lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER), lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE)));
        embedding.add(subtract(
                lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE)));

        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP), lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_WRIST)));
        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_WRIST)));

        // Cross body.
        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_ELBOW), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ELBOW)));
        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE)));

        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_WRIST), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_WRIST)));
        embedding.add(subtract(lm.get(com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE), lm.get(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE)));

        return embedding;
    }
}
