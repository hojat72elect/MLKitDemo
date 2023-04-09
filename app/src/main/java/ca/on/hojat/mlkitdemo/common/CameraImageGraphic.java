package ca.on.hojat.mlkitdemo.common;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import ca.on.hojat.mlkitdemo.common.GraphicOverlay.Graphic;

/**
 * Draw camera image to background.
 */
public class CameraImageGraphic extends Graphic {

    private final android.graphics.Bitmap bitmap;

    public CameraImageGraphic(GraphicOverlay overlay, Bitmap bitmap) {
        super(overlay);
        this.bitmap = bitmap;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, getTransformationMatrix(), null);
    }
}
