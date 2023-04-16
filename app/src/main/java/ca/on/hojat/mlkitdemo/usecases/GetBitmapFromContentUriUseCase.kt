package ca.on.hojat.mlkitdemo.usecases

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore.Images.Media
import androidx.exifinterface.media.ExifInterface
import ca.on.hojat.mlkitdemo.common.BitmapUtils
import ca.on.hojat.mlkitdemo.extensions.rotateBitmap
import java.io.IOException


object GetBitmapFromContentUriUseCase {

    @Throws(IOException::class)
    operator fun invoke(
        contentResolver: ContentResolver,
        imageUri: Uri?
    ): Bitmap? {

        val decodedBitmap =
            Media.getBitmap(contentResolver, imageUri) ?: return null
        val orientation = BitmapUtils.getExifOrientationTag(contentResolver, imageUri)

        var rotationDegrees = 0
        var flipX = false
        var flipY = false
        // See e.g. https://magnushoff.com/articles/jpeg-orientation/ for a detailed explanation on each
        // orientation.
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                flipX = true
            }

            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotationDegrees = 90
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                rotationDegrees = 90
                flipX = true
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotationDegrees = 180
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                flipY = true
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotationDegrees = -90
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                rotationDegrees = -90
                flipX = true
            }

            else -> {
                // No transformations necessary
                // in the other case.
            }
        }

        return decodedBitmap.rotateBitmap(rotationDegrees, flipX, flipY)
    }
}