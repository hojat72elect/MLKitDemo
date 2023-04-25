package ca.on.hojat.mlkitdemo.extensions

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

