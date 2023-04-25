package ca.on.hojat.mlkitdemo

import android.app.Activity
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.util.Log
import ca.on.hojat.mlkitdemo.common.FrameMetadata
import ca.on.hojat.mlkitdemo.common.GraphicOverlay
import ca.on.hojat.mlkitdemo.common.VisionImageProcessor
import com.google.android.gms.common.images.Size
import java.nio.ByteBuffer


/**
 * Manages the camera and allows UI updates on top of it
 * (e.g. overlaying extra Graphics or displaying extra
 * information). This receives preview frames from the camera
 * at a specified rate, sending those frames to child classes'
 * detectors / classifiers as fast as it is able to process.
 *
 * @param rotationDegrees  Rotation of the device, and thus the
 * associated preview images captured from the device.
 */
class CameraSourceKotlin(
    private val activity: Activity,
    private val graphicOverlay: GraphicOverlay,
    private val rotationDegrees: Int,
    private val previewSize: Size,
    private val frameProcessor: VisionImageProcessor
) {

    private val processingRunnable = FrameProcessingRunnable()
    private val processorLock = Object()

    // This instance needs to be held onto to avoid GC of its underlying resources. Even though it
    // isn't used outside of the method that creates it, it still must have hard references maintained
    // to it.
    private var camera: Camera? = null

    private var processingThread: Thread? = null

    init {
        graphicOverlay.clear()

    }


    /**
     * Closes the camera and stops sending frames to the
     * underlying frame detector.
     */
    @Synchronized
    fun stop() {
        processingRunnable.setActive()
        if (processingThread != null) {
            try {
                // Wait for the thread to complete to ensure that we can't have multiple threads
                // executing at the same time (i.e., which would happen if we called start too
                // quickly after stop).
                processingThread?.join()
            } catch (e: InterruptedException) {
                Log.d(TAG, "Frame processing thread interrupted on release.")
            }
            processingThread = null
        }
        if (camera != null) {
            camera!!.stopPreview()
            camera!!.setPreviewCallbackWithBuffer(null)
            try {
                camera!!.setPreviewTexture(null)
                camera!!.setPreviewDisplay(null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear camera preview: $e")
            }
            camera!!.release()
            camera = null
        }
    }


    companion object {
        const val CAMERA_FACING_BACK = CameraInfo.CAMERA_FACING_BACK
        const val CAMERA_FACING_FRONT = CameraInfo.CAMERA_FACING_FRONT

        private const val TAG = "MIDemoApp:CameraSource"
    }

    /**
     * Stores a preview size and a corresponding
     * same-aspect-ratio picture size. To avoid distorted
     * preview images on some devices, the picture size must
     * be set to a size that is the same aspect
     * ratio as the preview size or the preview may end up
     * being distorted. If the picture size is null, then
     * there is no picture size with the same aspect ratio
     * as the preview size.
     */
    data class SizePair(
        private val preview: Size,
        private val picture: Size
    )

    /**
     * This runnable controls access to the underlying receiver,
     * calling it to process frames when available from the
     * camera. This is designed to run detection on frames as
     * fast as possible (i.e., without unnecessary context
     * switching or waiting on the next frame).
     *
     * While detection is running on a frame, new frames may be
     * received from the camera. As these frames come in, the
     * most recent frame is held onto as pending. As soon as
     * detection and its associated processing is done for the
     * previous frame, detection on the most recently received
     * frame will immediately start on the same thread.
     */
    private inner class FrameProcessingRunnable() : Runnable {

        // This lock guards all of the member variables below.
        private val lock = Object()
        private var active = true

        // These pending variables hold the state
        // associated with the new frame awaiting processing.
        private var pendingFrameData: ByteBuffer? = null

        /**
         * Marks the runnable as active/not active. Signals
         * any blocked threads to continue.
         */
        fun setActive() {
            synchronized(lock) {
                active = false
                lock.notifyAll()
            }
        }

        /**
         * As long as the processing thread is active, this
         * executes detection on frames continuously.
         * The next pending frame is either immediately
         * available or hasn't been received yet. Once it is
         * available, we transfer the frame info to local
         * variables and run detection on that frame.
         * It immediately loops back for the next frame without
         * pausing.
         *
         * If detection takes longer than the time in between
         * new frames from the camera, this will mean that this
         * loop will run without ever waiting on a frame,
         * avoiding any context switching or frame acquisition
         * time latency.
         *
         * If you find that this is using more CPU than you'd
         * like, you should probably decrease the FPS setting
         * to allow for some idle time in between frames.
         */
        override fun run() {
            var data: ByteBuffer

            while (true) {
                synchronized(lock) {
                    while (active && pendingFrameData == null) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            lock.wait()
                        } catch (e: InterruptedException) {
                            Log.d(
                                TAG,
                                "Frame processing loop terminated.",
                                e
                            )
                            return
                        }
                    }
                    if (active.not()) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return
                    }

                    // Hold onto the frame data locally, so that we can use this for detection
                    // below.  We need to clear pendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.
                    data = pendingFrameData!!
                    pendingFrameData = null
                }

                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.


                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.
                try {
                    synchronized(processorLock) {
                        frameProcessor.processByteBuffer(
                            data,
                            FrameMetadata.Builder()
                                .setWidth(previewSize.width)
                                .setHeight(previewSize.height)
                                .setRotation(rotationDegrees)
                                .build(),
                            graphicOverlay
                        )
                    }
                } catch (t: Exception) {
                    Log.e(TAG, "Exception thrown from receiver.", t)
                } finally {
                    camera?.addCallbackBuffer(data.array())
                }

            }
        }
    }
}