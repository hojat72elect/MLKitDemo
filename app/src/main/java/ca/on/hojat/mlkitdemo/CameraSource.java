package ca.on.hojat.mlkitdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.Log;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;

import ca.on.hojat.mlkitdemo.common.FrameMetadata;
import ca.on.hojat.mlkitdemo.common.GraphicOverlay;
import ca.on.hojat.mlkitdemo.common.VisionImageProcessor;

/**
 * Manages the camera and allows UI updates on top of it (e.g. overlaying extra Graphics or
 * displaying extra information). This receives preview frames from the camera at a specified rate,
 * sending those frames to child classes' detectors / classifiers as fast as it is able to process.
 */
public class CameraSource {

    public static final int CAMERA_FACING_BACK = CameraInfo.CAMERA_FACING_BACK;
    public static final int CAMERA_FACING_FRONT = CameraInfo.CAMERA_FACING_FRONT;

    private static final String TAG = "MIDemoApp:CameraSource";

    private final GraphicOverlay graphicOverlay;
    private final FrameProcessingRunnable processingRunnable;
    private final Object processorLock = new Object();
    /**
     * Map to convert between a byte array, received from the camera, and its associated byte buffer.
     * We use byte buffers internally because this is a more efficient way to call into native code
     * later (avoids a potential copy).
     *
     * <p><b>Note:</b> uses IdentityHashMap here instead of HashMap because the behavior of an array's
     * equals, hashCode and toString methods is both useless and unexpected. IdentityHashMap enforces
     * identity ('==') check on the keys.
     */
    private final IdentityHashMap<byte[], ByteBuffer> bytesToByteBuffer = new IdentityHashMap<>();
    protected Activity activity;
    private Camera camera;
    /**
     * Rotation of the device, and thus the associated preview images captured from the device.
     */
    private int rotationDegrees;
    private com.google.android.gms.common.images.Size previewSize;
    // This instance needs to be held onto to avoid GC of its underlying resources. Even though it
    // isn't used outside of the method that creates it, it still must have hard references maintained
    // to it.
    /**
     * Dedicated thread and associated runnable for calling into the detector with frames, as the
     * frames become available from the camera.
     */
    private Thread processingThread;
    private VisionImageProcessor frameProcessor;

    public CameraSource(Activity activity, GraphicOverlay overlay) {
        this.activity = activity;
        graphicOverlay = overlay;
        graphicOverlay.clear();
        processingRunnable = new FrameProcessingRunnable();
    }

    /**
     * Closes the camera and stops sending frames to the underlying frame detector.
     */
    public synchronized void stop() {
        processingRunnable.setActive();
        if (processingThread != null) {
            try {
                // Wait for the thread to complete to ensure that we can't have multiple threads
                // executing at the same time (i.e., which would happen if we called start too
                // quickly after stop).
                processingThread.join();
            } catch (InterruptedException e) {
                android.util.Log.d(TAG, "Frame processing thread interrupted on release.");
            }
            processingThread = null;
        }

        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallbackWithBuffer(null);
            try {
                camera.setPreviewTexture(null);
                camera.setPreviewDisplay(null);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Failed to clear camera preview: " + e);
            }
            camera.release();
            camera = null;
        }

        // Release the reference to any image buffers, since these will no longer be in use.
        bytesToByteBuffer.clear();
    }

    /**
     * Stores a preview size and a corresponding same-aspect-ratio picture size. To avoid distorted
     * preview images on some devices, the picture size must be set to a size that is the same aspect
     * ratio as the preview size or the preview may end up being distorted. If the picture size is
     * null, then there is no picture size with the same aspect ratio as the preview size.
     */
    public static class SizePair {
        public final com.google.android.gms.common.images.Size preview;
        @Nullable
        public final com.google.android.gms.common.images.Size picture;

        public SizePair(com.google.android.gms.common.images.Size previewSize, @Nullable com.google.android.gms.common.images.Size pictureSize) {
            preview = previewSize;
            picture = pictureSize;
        }
    }

    /**
     * This runnable controls access to the underlying receiver, calling it to process frames when
     * available from the camera. This is designed to run detection on frames as fast as possible
     * (i.e., without unnecessary context switching or waiting on the next frame).
     *
     * <p>While detection is running on a frame, new frames may be received from the camera. As these
     * frames come in, the most recent frame is held onto as pending. As soon as detection and its
     * associated processing is done for the previous frame, detection on the mostly recently received
     * frame will immediately start on the same thread.
     */
    private class FrameProcessingRunnable implements Runnable {

        // This lock guards all of the member variables below.
        private final Object lock = new Object();
        private boolean active = true;

        // These pending variables hold the state associated with the new frame awaiting processing.
        private ByteBuffer pendingFrameData;

        FrameProcessingRunnable() {
        }

        /**
         * Marks the runnable as active/not active. Signals any blocked threads to continue.
         */
        void setActive() {
            synchronized (lock) {
                this.active = false;
                lock.notifyAll();
            }
        }

        /**
         * As long as the processing thread is active, this executes detection on frames continuously.
         * The next pending frame is either immediately available or hasn't been received yet. Once it
         * is available, we transfer the frame info to local variables and run detection on that frame.
         * It immediately loops back for the next frame without pausing.
         *
         * <p>If detection takes longer than the time in between new frames from the camera, this will
         * mean that this loop will run without ever waiting on a frame, avoiding any context switching
         * or frame acquisition time latency.
         *
         * <p>If you find that this is using more CPU than you'd like, you should probably decrease the
         * FPS setting above to allow for some idle time in between frames.
         */
        @SuppressLint("InlinedApi")
        @SuppressWarnings({"GuardedBy", "ByteBufferBackingArray"})
        @Override
        public void run() {
            ByteBuffer data;

            while (true) {
                synchronized (lock) {
                    while (active && (pendingFrameData == null)) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            lock.wait();
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Frame processing loop terminated.", e);
                            return;
                        }
                    }

                    if (!active) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return;
                    }

                    // Hold onto the frame data locally, so that we can use this for detection
                    // below.  We need to clear pendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.
                    data = pendingFrameData;
                    pendingFrameData = null;
                }

                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.

                try {
                    synchronized (processorLock) {
                        frameProcessor.processByteBuffer(
                                data,
                                new FrameMetadata.Builder()
                                        .setWidth(previewSize.getWidth())
                                        .setHeight(previewSize.getHeight())
                                        .setRotation(rotationDegrees)
                                        .build(),
                                graphicOverlay);
                    }
                } catch (Exception t) {
                    Log.e(TAG, "Exception thrown from receiver.", t);
                } finally {
                    camera.addCallbackBuffer(data.array());
                }
            }
        }
    }
}
