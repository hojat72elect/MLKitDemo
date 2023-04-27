package ca.on.hojat.mlkitdemo.feature_still_image.usecases

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore.Images.Media
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import ca.on.hojat.mlkitdemo.shared.extensions.rotateBitmap
import java.io.IOException

/**
 * You provide [ContentResolver] and [Uri]? for it; and will receive a [Bitmap]? which was hosted at that [Uri].
 */
object GetBitmapFromContentUriUseCase {

    @Throws(IOException::class)
    operator fun invoke(
        contentResolver: ContentResolver,
        imageUri: Uri?
    ): Bitmap? {

        val decodedBitmap =
            Media.getBitmap(contentResolver, imageUri) ?: return null
        val orientation = getExifOrientationTag(contentResolver, imageUri)

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


    private fun getExifOrientationTag(
        resolver: ContentResolver,
        imageUri: Uri?
    ): Int {
        // We only support parsing EXIF orientation tag from local file on the device.
        // See also:
        // https://android-developers.googleblog.com/2016/12/introducing-the-exifinterface-support-library.html
        if (ContentResolver.SCHEME_CONTENT != imageUri?.scheme && ContentResolver.SCHEME_FILE != imageUri?.scheme)
            return 0

        var exif: ExifInterface
        try {
            resolver.openInputStream(imageUri).use { inputStream ->
                if (inputStream == null)
                    return 0

                exif = ExifInterface(inputStream)
            }
        } catch (e: IOException) {
            Log.e(
                "BitmapContentUriUseCase",
                "failed to open file to read rotation meta data: $imageUri", e
            )
            return 0
        }

        return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
}
