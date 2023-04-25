package ca.on.hojat.mlkitdemo.common;

import android.media.Image;
import android.media.Image.Plane;

import ca.on.hojat.mlkitdemo.extensions.PlaneKt;

import java.nio.ByteBuffer;

/**
 * Utils functions for bitmap conversions.
 */
public class BitmapUtils {

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

        if (PlaneKt.areNV21(yuv420888planes, width, height)) {
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
            PlaneKt.unpack(yuv420888planes[0], width, height, out, 0, 1);
            // Unpack U.
            PlaneKt.unpack(yuv420888planes[1], width, height, out, imageSize + 1, 2);
            // Unpack V.
            PlaneKt.unpack(yuv420888planes[2], width, height, out, imageSize, 2);
        }

        return ByteBuffer.wrap(out);
    }
}
