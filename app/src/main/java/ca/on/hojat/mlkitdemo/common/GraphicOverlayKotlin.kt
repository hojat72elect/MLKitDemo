package ca.on.hojat.mlkitdemo.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.common.base.Preconditions
import com.google.common.primitives.Ints

class GraphicOverlayKotlin(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val lock = Object()
    private val graphics = mutableListOf<Graphic>()

    // Matrix for transforming from image coordinates to overlay view coordinates.
    val transformationMatrix = Matrix()

    var imageWidth = 0
        private set
    var imageHeight = 0
        private set

    // The factor of overlay View size to image size.
    // Anything in the image coordinates need to be
    // scaled by this amount to fit with the area of
    // overlay View.
    var scaleFactor = 1f
        private set

    // The number of horizontal pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    var postScaleWidthOffset = 0f
        private set

    // The number of vertical pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    var postScaleHeightOffset = 0f
        private set
    var isImageFlipped = false
        private set
    private var needUpdateTransformation = true


    init {
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            needUpdateTransformation = true
        }
    }

    /**
     * Removes all graphics from the overlay.
     */
    fun clear() {
        synchronized(lock) { graphics.clear() }
        postInvalidate()
    }

    /**
     * Adds a graphic to the overlay.
     */
    fun add(graphic: Graphic) {
        synchronized(lock) { graphics.add(graphic) }
    }

    /**
     * Sets the source information of the image being processed by detectors, including size and
     * whether it is flipped, which informs how to transform image coordinates later.
     *
     * @param imageWidth  the width of the image sent to ML Kit detectors
     * @param imageHeight the height of the image sent to ML Kit detectors
     * @param isFlipped   whether the image is flipped. Should set it to true when the image is from the
     *                    front camera.
     */
    fun setImageSourceInfo(
        imageWidth: Int,
        imageHeight: Int,
        isFlipped: Boolean
    ) {
        Preconditions.checkState(
            imageWidth > 0,
            "image width must be positive"
        )
        Preconditions.checkState(
            imageHeight > 0, "image height must be positive"
        )
        synchronized(lock) {
            this.imageWidth = imageWidth
            this.imageHeight = imageHeight
            this.isImageFlipped = isFlipped
            needUpdateTransformation = true
        }
        postInvalidate()
    }

    private fun updateTransformationIfNeeded() {

        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) {
            return
        }
        val viewAspectRatio = (width / height).toFloat()
        val imageAspectRatio = (imageWidth / imageHeight).toFloat()
        postScaleWidthOffset = 0f
        postScaleHeightOffset = 0f
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = (width / imageWidth).toFloat()
            postScaleHeightOffset = (width / imageAspectRatio - height) / 2
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = (height / imageHeight).toFloat()
            postScaleWidthOffset = (height * imageAspectRatio - width) / 2
        }

        transformationMatrix.reset()
        transformationMatrix.setScale(scaleFactor, scaleFactor)
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset)

        if (isImageFlipped) {
            transformationMatrix.postScale(-1f, 1f, width / 2f, height / 2f)
        }

        needUpdateTransformation = false
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        synchronized(lock) {
            updateTransformationIfNeeded()

            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }

}

/**
 * Base class for a custom graphics object to be rendered
 * within the graphic overlay. Subclass this and implement
 * the [Graphic.draw(Canvas)] method to define the graphics
 * element. Add instances to the overlay using
 * [GraphicOverlay.add(Graphic)].
 */
abstract class Graphic(private val overlay: GraphicOverlayKotlin) {

    /**
     * Draw the graphic on the supplied canvas. Drawing should
     * use the following methods to convert to view coordinates
     * for the graphics that are drawn:
     *
     * 1- [Graphic.scale(float)] adjusts the size of the
     * supplied value from the image scale to the view scale.
     *
     * 2- [Graphic.translateX(float)] and [Graphic.translateY(float)]
     * adjust the coordinate from the image's coordinate system to
     * the view coordinate system.
     *
     *
     * @param canvas drawing canvas
     */
    abstract fun draw(canvas: Canvas)

    /**
     * Adjusts the supplied value from the image scale to
     * the view scale.
     */
    fun scale(imagePixel: Float) = imagePixel * overlay.scaleFactor

    /**
     * Returns the application context of the app.
     */
    fun getApplicationContext() = overlay.context.applicationContext

    /**
     * Adjusts the x coordinate from the image's
     * coordinate system to the view coordinate system.
     */
    fun translateX(x: Float): Float {
        return if (overlay.isImageFlipped) {
            overlay.width - (scale(x) - overlay.postScaleWidthOffset)
        } else {
            scale(x) - overlay.postScaleWidthOffset
        }
    }

    /**
     * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
     */
    fun translateY(y: Float) = scale(y) - overlay.postScaleHeightOffset

    /**
     * Returns a [Matrix] for transforming from image coordinates to overlay view coordinates.
     */
    fun getTransformationMatrix() = overlay.transformationMatrix

    fun postInvalidate() = overlay.postInvalidate()

    /**
     * Given the [zInImagePixel], update the color for the passed in [paint]. The color will be
     * more red if the [zInImagePixel] is smaller, or more blue ish vice versa. This is
     * useful to visualize the z value of landmarks via color for features like Pose and Face Mesh.
     *
     * @param paint                    the paint to update color with
     * @param canvas                   the canvas used to draw with paint
     * @param visualizeZ               if true, paint color will be changed.
     * @param rescaleZForVisualization if true, re-scale the z value with zMin and zMax to make
     *                                 color more distinguishable
     * @param zInImagePixel            the z value used to update the paint color
     * @param zMin                     min value of all z values going to be passed in
     * @param zMax                     max value of all z values going to be passed in
     */
    fun updatePaintColorByZValue(
        paint: Paint,
        canvas: Canvas,
        visualizeZ: Boolean,
        rescaleZForVisualization: Boolean,
        zInImagePixel: Float,
        zMin: Float,
        zMax: Float
    ) {

        if (!visualizeZ)
            return

        // When visualizeZ is true, sets up the paint to different colors based on z values.
        // Gets the range of z value.
        val zLowerBoundInScreenPixel: Float
        val zUpperBoundInScreenPixel: Float

        if (rescaleZForVisualization) {
            zLowerBoundInScreenPixel = minOf(-0.001f, scale(zMin))
            zUpperBoundInScreenPixel = maxOf(0.001f, scale(zMax))
        } else {
            // By default, assume the range of z value in screen pixel is [-canvasWidth, canvasWidth].
            val defaultRangeFactor = 1f
            zLowerBoundInScreenPixel = -defaultRangeFactor * canvas.width
            zUpperBoundInScreenPixel = defaultRangeFactor * canvas.width
        }

        val zInScreenPixel = scale(zInImagePixel)

        if (zInScreenPixel < 0) {
            // Sets up the paint to be red if the item is in front of the z origin.
            // Maps values within [zLowerBoundInScreenPixel, 0) to [255, 0) and use it to control the
            // color. The larger the value is, the more red it will be.
            var v = (zInScreenPixel / zLowerBoundInScreenPixel * 255).toInt()
            v = Ints.constrainToRange(v, 0, 255)
            paint.setARGB(255, 255, 255 - v, 255 - v)
        } else {
            // Sets up the paint to be blue if the item is behind the z origin.
            // Maps values within [0, zUpperBoundInScreenPixel] to [0, 255] and use it to control the
            // color. The larger the value is, the more blue it will be.
            var v = (zInScreenPixel / zUpperBoundInScreenPixel * 255).toInt()
            v = Ints.constrainToRange(v, 0, 255)
            paint.setARGB(255, 255 - v, 255 - v, 255)
        }
    }
}