package ca.on.hojat.mlkitdemo.shared.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import ca.on.hojat.mlkitdemo.shared.FrameMetadata
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Converts a NV21 format [ByteBuffer] to a [Bitmap].
 */
fun ByteBuffer.getBitmap(metadata: FrameMetadata): Bitmap? {

    rewind()
    val imageInBuffer = ByteArray(limit())
    get(imageInBuffer, 0, imageInBuffer.size) // this line is a bit weird..
    try {
        val image = YuvImage(imageInBuffer, ImageFormat.NV21, metadata.width, metadata.height, null)
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
        Log.e("VisionProcessorBase", e.toString())
    }
    return null
}
