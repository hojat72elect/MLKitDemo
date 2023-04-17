package ca.on.hojat.mlkitdemo.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.Image.Plane;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import ca.on.hojat.mlkitdemo.extensions.BitmapKt;

/**
 * Utils functions for bitmap conversions.
 */
public class BitmapUtils {

    /**
     * I have already converted this to a kotlin extension function but
     * I don't know why, whenever I migrate to that file, will have a
     * run-time error from ML-Kit.
     * Converts NV21 format byte buffer to bitmap.
     */
    @Nullable
    public static Bitmap getBitmap(ByteBuffer data, FrameMetadata metadata) {
        data.rewind();
        byte[] imageInBuffer = new byte[data.limit()];
        data.get(imageInBuffer, 0, imageInBuffer.length);
        try {
            YuvImage image = new YuvImage(imageInBuffer, ImageFormat.NV21, metadata.getWidth(), metadata.getHeight(), null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, metadata.getWidth(), metadata.getHeight()), 80, stream);

            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

            stream.close();
            return BitmapKt.rotateBitmap(bmp, metadata.getRotation(), false, false);
        } catch (Exception e) {
            Log.e("VisionProcessorBase", "Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * It's impossible to convert this into kotlin (I have already done that but we face a weird IndexOutOfBounds runtime error)
     * <p>
     * Converts YUV_420_888 to NV21 bytebuffer.
     *
     * <p>The NV21 format consists of a single byte array containing the Y, U and V values. For an
     * image of size S, the first S positions of the array contain all the Y values. The remaining
     * positions contain interleaved V and U values. U and V are subsampled by a factor of 2 in both
     * dimensions, so there are S/4 U values and S/4 V values. In summary, the NV21 array will contain
     * S Y values followed by S/4 VU values: YYYYYYYYYYYYYY(...)YVUVUVUVU(...)VU
     *
     * <p>YUV_420_888 is a generic format that can describe any YUV image where U and V are subsampled
     * by a factor of 2 in both dimensions. {@link Image#getPlanes} returns an array with the Y, U and
     * V planes. The Y plane is guaranteed not to be interleaved, so we can just copy its values into
     * the first part of the NV21 array. The U and V planes may already have the representation in the
     * NV21 format. This happens if the planes share the same buffer, the V buffer is one position
     * before the U buffer and the planes have a pixelStride of 2. If this is case, we can just copy
     * them to the NV21 array.
     */
    public static ByteBuffer yuv420ThreePlanesToNV21(Plane[] yuv420888planes, int width, int height) {
        int imageSize = width * height;
        byte[] out = new byte[imageSize + 2 * (imageSize / 4)];

        if (areUVPlanesNV21(yuv420888planes, width, height)) {
            // Copy the Y values.
            yuv420888planes[0].getBuffer().get(out, 0, imageSize);

            java.nio.ByteBuffer uBuffer = yuv420888planes[1].getBuffer();
            java.nio.ByteBuffer vBuffer = yuv420888planes[2].getBuffer();
            // Get the first V value from the V buffer, since the U buffer does not contain it.
            vBuffer.get(out, imageSize, 1);
            // Copy the first U value and the remaining VU values from the U buffer.
            uBuffer.get(out, imageSize + 1, 2 * imageSize / 4 - 1);
        } else {
            // Fallback to copying the UV values one by one, which is slower but also works.
            // Unpack Y.
            unpackPlane(yuv420888planes[0], width, height, out, 0, 1);
            // Unpack U.
            unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2);
            // Unpack V.
            unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2);
        }

        return ByteBuffer.wrap(out);
    }

    /**
     * Checks if the UV plane buffers of a YUV_420_888 image are in the NV21 format.
     */
    public static boolean areUVPlanesNV21(Plane[] planes, int width, int height) {
        int imageSize = width * height;

        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        // Backup buffer properties.
        int vBufferPosition = vBuffer.position();
        int uBufferLimit = uBuffer.limit();

        // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
        vBuffer.position(vBufferPosition + 1);
        // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
        uBuffer.limit(uBufferLimit - 1);

        // Check that the buffers are equal and have the expected number of elements.
        boolean areNV21 = (vBuffer.remaining() == (2 * imageSize / 4 - 2)) && (vBuffer.compareTo(uBuffer) == 0);

        // Restore buffers to their initial state.
        vBuffer.position(vBufferPosition);
        uBuffer.limit(uBufferLimit);

        return areNV21;
    }

    /**
     * Unpack an image plane into a byte array.
     *
     * <p>The input plane data will be copied in 'out', starting at 'offset' and every pixel will be
     * spaced by 'pixelStride'. Note that there is no row padding on the output.
     */
    public static void unpackPlane(Plane plane, int width, int height, byte[] out, int offset, int pixelStride) {
        ByteBuffer buffer = plane.getBuffer();
        buffer.rewind();

        // Compute the size of the current plane.
        // We assume that it has the aspect ratio as the original image.
        int numRow = (buffer.limit() + plane.getRowStride() - 1) / plane.getRowStride();
        if (numRow == 0) {
            return;
        }
        int scaleFactor = height / numRow;
        int numCol = width / scaleFactor;

        // Extract the data in the output buffer.
        int outputPos = offset;
        int rowStart = 0;
        for (int row = 0; row < numRow; row++) {
            int inputPos = rowStart;
            for (int col = 0; col < numCol; col++) {
                out[outputPos] = buffer.get(inputPos);
                outputPos += pixelStride;
                inputPos += plane.getPixelStride();
            }
            rowStart += plane.getRowStride();
        }
    }
}
