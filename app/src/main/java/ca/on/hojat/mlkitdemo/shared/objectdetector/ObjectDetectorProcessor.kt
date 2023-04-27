package ca.on.hojat.mlkitdemo.shared.objectdetector

import android.content.Context
import android.util.Log
import ca.on.hojat.mlkitdemo.shared.GraphicOverlay
import ca.on.hojat.mlkitdemo.shared.VisionProcessorBase
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase
import java.io.IOException

/** A processor to run object detector.  */
class ObjectDetectorProcessor(context: Context, options: ObjectDetectorOptionsBase) :
    VisionProcessorBase<List<DetectedObject>>(context) {

    private val detector: ObjectDetector = ObjectDetection.getClient(options)

    override fun stop() {
        super.stop()
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Exception thrown while trying to close object detector!",
                e
            )
        }
    }

    override fun detectInImage(image: InputImage): Task<List<DetectedObject>> {
        return detector.process(image)
    }

    override fun onSuccess(results: List<DetectedObject>, graphicOverlay: GraphicOverlay) {
        for (result in results) {
            graphicOverlay.add(ObjectGraphic(graphicOverlay, result))
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Object detection failed!", e)
    }

    companion object {
        private const val TAG = "ObjectDetectorProcessor"
    }
}
