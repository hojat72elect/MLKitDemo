package ca.on.hojat.mlkitdemo.common

import android.graphics.Bitmap
import android.graphics.Canvas
import ca.on.hojat.mlkitdemo.common.GraphicOverlay.Graphic

/**
 * Draw camera image to background.
 */
class CameraImageGraphic(overlay: GraphicOverlay, private val bitmap: Bitmap) :
    Graphic(overlay) {


    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, transformationMatrix, null)
    }
}