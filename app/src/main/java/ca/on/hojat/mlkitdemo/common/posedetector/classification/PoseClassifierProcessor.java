package ca.on.hojat.mlkitdemo.common.posedetector.classification;

import com.google.mlkit.vision.pose.Pose;

/**
 * Accepts a stream of {@link Pose} for classification and Rep counting.
 */
public class PoseClassifierProcessor {
    private static final String TAG = "PoseClassifierProcessor";
    private static final String POSE_SAMPLES_FILE = "pose/fitness_pose_samples.csv";

    // Specify classes for which we want rep counting.
    // These are the labels in the given {@code POSE_SAMPLES_FILE}. You can set your own class labels
    // for your pose samples.
    private static final String PUSHUPS_CLASS = "pushups_down";
    private static final String SQUATS_CLASS = "squats_down";
    private static final String[] POSE_CLASSES = {
            PUSHUPS_CLASS, SQUATS_CLASS
    };

    private final boolean isStreamMode;

    private EMASmoothing emaSmoothing;
    private java.util.List<ca.on.hojat.mlkitdemo.common.posedetector.classification.RepetitionCounter> repCounters;
    private PoseClassifier poseClassifier;
    private String lastRepResult;

    @androidx.annotation.WorkerThread
    public PoseClassifierProcessor(android.content.Context context, boolean isStreamMode) {
        com.google.common.base.Preconditions.checkState(android.os.Looper.myLooper() != android.os.Looper.getMainLooper());
        this.isStreamMode = isStreamMode;
        if (isStreamMode) {
            emaSmoothing = new EMASmoothing();
            repCounters = new java.util.ArrayList<>();
            lastRepResult = "";
        }
        loadPoseSamples(context);
    }

    private void loadPoseSamples(android.content.Context context) {
        java.util.List<ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample> poseSamples = new java.util.ArrayList<>();
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(context.getAssets().open(POSE_SAMPLES_FILE)));
            String csvLine = reader.readLine();
            while (csvLine != null) {
                // If line is not a valid {@link PoseSample}, we'll get null and skip adding to the list.
                ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample poseSample = ca.on.hojat.mlkitdemo.common.posedetector.classification.PoseSample.getPoseSample(csvLine, ",");
                if (poseSample != null) {
                    poseSamples.add(poseSample);
                }
                csvLine = reader.readLine();
            }
        } catch (java.io.IOException e) {
            android.util.Log.e(TAG, "Error when loading pose samples.\n" + e);
        }
        poseClassifier = new PoseClassifier(poseSamples);
        if (isStreamMode) {
            for (String className : POSE_CLASSES) {
                repCounters.add(new ca.on.hojat.mlkitdemo.common.posedetector.classification.RepetitionCounter(className));
            }
        }
    }

    /**
     * Given a new {@link Pose} input, returns a list of formatted {@link String}s with Pose
     * classification results.
     *
     * <p>Currently it returns up to 2 strings as following:
     * 0: PoseClass : X reps
     * 1: PoseClass : [0.0-1.0] confidence
     */
    @androidx.annotation.WorkerThread
    public java.util.List<String> getPoseResult(com.google.mlkit.vision.pose.Pose pose) {
        com.google.common.base.Preconditions.checkState(android.os.Looper.myLooper() != android.os.Looper.getMainLooper());
        java.util.List<String> result = new java.util.ArrayList<>();
        ca.on.hojat.mlkitdemo.common.posedetector.classification.ClassificationResult classification = poseClassifier.classify(pose);

        // Update {@link RepetitionCounter}s if {@code isStreamMode}.
        if (isStreamMode) {
            // Feed pose to smoothing even if no pose found.
            classification = emaSmoothing.getSmoothedResult(classification);

            // Return early without updating repCounter if no pose found.
            if (pose.getAllPoseLandmarks().isEmpty()) {
                result.add(lastRepResult);
                return result;
            }

            for (ca.on.hojat.mlkitdemo.common.posedetector.classification.RepetitionCounter repCounter : repCounters) {
                int repsBefore = repCounter.getNumRepeats();
                int repsAfter = repCounter.addClassificationResult(classification);
                if (repsAfter > repsBefore) {
                    // Play a fun beep when rep counter updates.
                    android.media.ToneGenerator tg = new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP);
                    lastRepResult = String.format(
                            java.util.Locale.US, "%s : %d reps", repCounter.getClassName(), repsAfter);
                    break;
                }
            }
            result.add(lastRepResult);
        }

        // Add maxConfidence class of current frame to result if pose is found.
        if (!pose.getAllPoseLandmarks().isEmpty()) {
            String maxConfidenceClass = classification.getMaxConfidenceClass();
            String maxConfidenceClassResult = String.format(
                    java.util.Locale.US,
                    "%s : %.2f confidence",
                    maxConfidenceClass,
                    classification.getClassConfidence(maxConfidenceClass)
                            / poseClassifier.confidenceRange());
            result.add(maxConfidenceClassResult);
        }

        return result;
    }

}
