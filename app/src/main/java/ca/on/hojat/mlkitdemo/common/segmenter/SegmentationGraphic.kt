package ca.on.hojat.mlkitdemo.common.segmenter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import androidx.annotation.ColorInt
import ca.on.hojat.mlkitdemo.common.GraphicOverlay
import com.google.mlkit.vision.segmentation.SegmentationMask
import java.nio.ByteBuffer

/** Draw the mask from SegmentationResult in preview.  */
class SegmentationGraphic(overlay: GraphicOverlay, segmentationMask: SegmentationMask) :
    GraphicOverlay.Graphic(overlay) {
    private val mask: ByteBuffer
    private val maskWidth: Int
    private val maskHeight: Int
    private val isRawSizeMaskEnabled: Boolean
    private val scaleX: Float
    private val scaleY: Float

    /** Draws the segmented background on the supplied canvas.  */
    override fun draw(canvas: Canvas) {
        val bitmap = Bitmap.createBitmap(
            maskColorsFromByteBuffer(mask), maskWidth, maskHeight, Bitmap.Config.ARGB_8888
        )
        if (isRawSizeMaskEnabled) {
            val matrix = Matrix(transformationMatrix)
            matrix.preScale(scaleX, scaleY)
            canvas.drawBitmap(bitmap, matrix, null)
        } else {
            canvas.drawBitmap(bitmap, transformationMatrix, null)
        }
        bitmap.recycle()
        // Reset byteBuffer pointer to beginning, so that the mask can be redrawn if screen is refreshed
        mask.rewind()
    }

    /** Converts byteBuffer floats to ColorInt array that can be used as a mask.  */
    @ColorInt
    private fun maskColorsFromByteBuffer(byteBuffer: ByteBuffer): IntArray {
        @ColorInt val colors = IntArray(maskWidth * maskHeight)
        for (i in 0 until maskWidth * maskHeight) {
            val backgroundLikelihood = 1 - byteBuffer.float
            if (backgroundLikelihood > 0.9) {
                colors[i] = Color.argb(128, 255, 0, 255)
            } else if (backgroundLikelihood > 0.2) {
                // Linear interpolation to make sure when backgroundLikelihood is 0.2, the alpha is 0 and
                // when backgroundLikelihood is 0.9, the alpha is 128.
                // +0.5 to round the float value to the nearest int.
                val alpha = (182.9 * backgroundLikelihood - 36.6 + 0.5).toInt()
                colors[i] = Color.argb(alpha, 255, 0, 255)
            }
        }
        return colors
    }

    init {
        mask = segmentationMask.buffer
        maskWidth = segmentationMask.width
        maskHeight = segmentationMask.height
        isRawSizeMaskEnabled =
            maskWidth != overlay.imageWidth || maskHeight != overlay.imageHeight
        scaleX = overlay.imageWidth * 1f / maskWidth
        scaleY = overlay.imageHeight * 1f / maskHeight
    }
}
