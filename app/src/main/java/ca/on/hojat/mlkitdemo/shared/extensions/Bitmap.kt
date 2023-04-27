package ca.on.hojat.mlkitdemo.shared.extensions

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Rotates a bitmap if it is converted from a bytebuffer.
 *
 * @param rotationDegrees  the amount of rotation in terms of degrees (I guess the rotation is clock-wise)
 * @param flipX  Whether to be flipped along the X axis or not.
 * @param flipY  Whether to be flipped along the Y axis or not.
 */
fun Bitmap.rotateBitmap(rotationDegrees: Int, flipX: Boolean, flipY: Boolean): Bitmap {

    val matrix = Matrix()

    // Rotate the image back to straight.
    matrix.postRotate(rotationDegrees.toFloat())

    // Mirror the image along the X or Y axis.
    matrix.postScale(if (flipX) -1f else 1f, if (flipY) -1f else 1f)
    val rotatedBitmap = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)

    // Recycle the old bitmap if it has changed.
    if (rotatedBitmap != this)
        recycle()

    return rotatedBitmap
}