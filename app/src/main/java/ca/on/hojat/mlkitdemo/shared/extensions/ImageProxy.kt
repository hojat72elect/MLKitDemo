package ca.on.hojat.mlkitdemo.shared.extensions

import android.graphics.Bitmap
import android.media.Image.Plane
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import ca.on.hojat.mlkitdemo.shared.FrameMetadata
import java.nio.ByteBuffer

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

/**
 * Converts YUV_420_888 to NV21 bytebuffer.
 *
 * The NV21 format consists of a single byte array containing the Y, U and V values. For an
 * image of size S, the first S positions of the array contain all the Y values. The remaining
 * positions contain interleaved V and U values. U and V are subsampled by a factor of 2 in both
 * dimensions, so there are S/4 U values and S/4 V values. In summary, the NV21 array will contain
 * S Y values followed by S/4 VU values: YYYYYYYYYYYYYY(...)YVUVUVUVU(...)VU
 *
 * YUV_420_888 is a generic format that can describe any YUV image where U and V are subsampled
 * by a factor of 2 in both dimensions. {@link Image#getPlanes} returns an array with the Y, U and
 * V planes. The Y plane is guaranteed not to be interleaved, so we can just copy its values into
 * the first part of the NV21 array. The U and V planes may already have the representation in the
 * NV21 format. This happens if the planes share the same buffer, the V buffer is one position
 * before the U buffer and the planes have a pixelStride of 2. If this is case, we can just copy
 * them to the NV21 array.
 */
private fun yuv420ThreePlanesToNV21(
    yuv420888planes: Array<Plane>,
    width: Int,
    height: Int
): ByteBuffer {
    val imageSize = width * height
    val out = ByteArray(imageSize + 2 * (imageSize / 4))

    if (yuv420888planes.areNV21(width, height)) {
        // Copy the Y values.
        yuv420888planes[0].buffer[out, 0, imageSize]

        val uBuffer = yuv420888planes[1].buffer
        val vBuffer = yuv420888planes[2].buffer

        // Get the first V value from the V buffer, since the U buffer does not contain it.
        vBuffer[out, imageSize, 1]

        // Copy the first U value and the remaining VU values from the U buffer.
        uBuffer[out, imageSize + 1, 2 * imageSize / 4 - 1]
    } else {
        // Fallback to copying the UV values one by one, which is slower but also works.
        // Unpack Y.
        yuv420888planes[0].unpack(width, height, out, 0, 1)
        // Unpack U.
        yuv420888planes[1].unpack(width, height, out, imageSize + 1, 2)
        // Unpack V.
        yuv420888planes[2].unpack(width, height, out, imageSize, 2)
    }

    return ByteBuffer.wrap(out)
}
