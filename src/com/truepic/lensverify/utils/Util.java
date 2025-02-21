/*
 * Copyright (c) 2024 Truepic
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.truepic.lensverify.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;

public class Util {

    /**
     * Scales bitmap according to the max size using sampling (more memory efficient)
     * @param bitmap to be scaled
     * @param maxSize maximum size of the longer edge
     * @return scaled bitmap
     */
    public static Bitmap getScaledBitmapFromBuffer(byte[] bitmap, int maxSize, int rotation) {
        Bitmap sampled = getSampledBitmap(bitmap, maxSize);
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap resized = Bitmap.createBitmap(sampled, 0, 0, sampled.getWidth(), sampled.getHeight(), matrix, true);
        sampled.recycle();
        return resized;
    }

    /**
     * Gets down scaled bitmap where longest axis is 1000px
     *
     * @param data image data
     * @param size to be used for sampling
     * @return sampled bitmap
     */
    public static Bitmap getSampledBitmap(byte[] data, int size) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        Rect bounds = getUpdatedBounds(options, size);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        return Bitmap.createScaledBitmap( bitmap, bounds.width(), bounds.height(), true);
    }

    private static Rect getUpdatedBounds(BitmapFactory.Options options, int size) {
        int width, height;
        if (options.outWidth > options.outHeight)
        {
            width = size;
            height = width * options.outHeight / options.outWidth;
        }
        else {
            height = size;
            width = height * options.outWidth / options.outHeight;
        }
        return new Rect(0, 0, width, height);
    }

    public static int getDegreesFromExifOrientation(int orientation) {
        switch (orientation) {
            case 1:
            case 2: // mirrored
                return 0;
            case 3:
            case 4: // mirrored
                return 180;
            case 5:
            case 6: // mirrored
                return 90;
            case 7:
            case 8: // mirrored
                return 270;
            default:
                return 0;
        }
    }
}
