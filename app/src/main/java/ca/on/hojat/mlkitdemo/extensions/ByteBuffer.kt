package ca.on.hojat.mlkitdemo.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import ca.on.hojat.mlkitdemo.common.FrameMetadata
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Converts NV21 format byte buffer to bitmap.
 *
 * If any errors happen while the [ByteBuffer] is being
 * converted to [Bitmap], the result of this function is
 * going to be a null.
 *
 * You just provide input in the form of [ByteBuffer] and also the configuration in form of
 * [FrameMetadata]. The output is a [Bitmap].
 */
fun ByteBuffer.getBitmap(metadata: FrameMetadata): Bitmap {
    try {
        rewind()
        val imageInBuffer = byteArrayOf(limit().toByte())
        get(imageInBuffer, 0, imageInBuffer.size)
        val image = YuvImage(
            imageInBuffer,
            ImageFormat.NV21,
            metadata.width,
            metadata.height,
            null
        )
        val stream = ByteArrayOutputStream()
        image.compressToJpeg(Rect(0, 0, metadata.width, metadata.height), 80, stream)

        val bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())

        stream.close()
        return bmp.rotateBitmap(
            rotationDegrees = metadata.rotation,
            flipX = false,
            flipY = false
        )
    } catch (e: Exception) {
        Log.e("VisionProcessorBase", "Error: ${e.message}")
        throw (e)
    }
//    return null
}