package ca.on.hojat.mlkitdemo.shared.extensions

import android.media.Image.Plane

/**
 * Unpacks an image plane into a byte array.
 *
 * The input plane data will be copied in 'out', starting at
 * 'offset' and every pixel will be spaced by 'pixelStride'.
 * Note that there is no row padding on the output.
 */
fun Plane.unpack(
    width: Int,
    height: Int,
    out: ByteArray,
    offset: Int,
    pixelStride: Int
) {
    buffer.rewind()

    // Compute the size of the current plane.
    // We assume that it has the aspect ratio as the original image.

    val numRow = (buffer.limit() + rowStride - 1) / rowStride
    if (numRow == 0)
        return
    val scaleFactor = height / numRow
    val numCol = width / scaleFactor

    // Extract the data in the output buffer.

    // Extract the data in the output buffer.
    var outputPos = offset
    var rowStart = 0
    for (row in 0 until numRow) {
        var inputPos = rowStart
        for (col in 0 until numCol) {
            out[outputPos] = buffer[inputPos]
            outputPos += pixelStride
            inputPos += this.pixelStride
        }
        rowStart += rowStride
    }
}


/**
 * Is applied to an Array of [Plane]s and checks if the
 * UV plane of a YUV_420_888 image are in the NV21
 * format.
 */
fun Array<Plane>.areNV21(width: Int, height: Int): Boolean {

    val imageSize = width * height
    val uBuffer = get(1).buffer
    val vBuffer = get(2).buffer

    // Backup buffer properties.
    val vBufferPosition = vBuffer.position()
    val uBufferLimit = uBuffer.limit()

    // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
    vBuffer.position(vBufferPosition + 1)
    // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
    uBuffer.limit(uBufferLimit - 1)

    // Check that the buffers are equal and have the expected number of elements.
    val areNV21 =
        (vBuffer.remaining() == (2 * imageSize / 4 - 2)) && (vBuffer.compareTo(uBuffer) == 0)

    // Restore buffers to their initial state.
    vBuffer.position(vBufferPosition)
    uBuffer.limit(uBufferLimit)

    return areNV21
}
