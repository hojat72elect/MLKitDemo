package ca.on.hojat.mlkitdemo.extensions

import android.graphics.Bitmap
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import ca.on.hojat.mlkitdemo.common.BitmapUtils.yuv420ThreePlanesToNV21
import ca.on.hojat.mlkitdemo.common.FrameMetadata

/**
 * Converts a YUV_420_888 image from CameraX API to a bitmap.
 *
 * (CameraX API defines the images as [ImageProxy] objects).
 */
@ExperimentalGetImage
fun ImageProxy.getBitmap(): Bitmap? {

    val frameMetadata = FrameMetadata.Builder()
        .setWidth(width)
        .setHeight(height)
        .setRotation(imageInfo.rotationDegrees)
        .build()

    val nv21Buffer = yuv420ThreePlanesToNV21(image!!.planes, width, height)
    return nv21Buffer.getBitmap(frameMetadata)
}