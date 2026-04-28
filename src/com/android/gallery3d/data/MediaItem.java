/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
/*
 * Changes from Qualcomm Innovation Center, Inc. are provided under the following license:
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;

import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.util.C2paUtil;
import com.android.gallery3d.util.ThreadPool.Job;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.truepic.lensverify.data.c2padata.C2PAData;
import com.truepic.lensverify.utils.C2PAPresenter;

import java.io.File;

// MediaItem represents an image or a video item.
public abstract class MediaItem extends MediaObject {
    // NOTE: These type numbers are stored in the image cache, so it should not
    // not be changed without resetting the cache.
    public static final int TYPE_THUMBNAIL = 1;
    public static final int TYPE_MICROTHUMBNAIL = 2;

    public static final int CACHED_IMAGE_QUALITY = 95;

    public static final int IMAGE_READY = 0;
    public static final int IMAGE_WAIT = 1;
    public static final int IMAGE_ERROR = -1;

    public static final String MIME_TYPE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_HEIC = "image/heic";

    private static final int BYTESBUFFE_POOL_SIZE = 4;
    private static final int BYTESBUFFER_SIZE = 200 * 1024;
    private static final String TAG = "MediaItem";

    private static int sMicrothumbnailTargetSize = 200;
    private static final BytesBufferPool sMicroThumbBufferPool =
            new BytesBufferPool(BYTESBUFFE_POOL_SIZE, BYTESBUFFER_SIZE);

    private static int sThumbnailTargetSize = 640;

    // TODO: fix default value for latlng and change this.
    public static final double INVALID_LATLNG = 0f;

    public abstract Job<Bitmap> requestImage(int type);
    public abstract Job<BitmapRegionDecoder> requestLargeImage();

    public MediaItem(Path path, long version) {
        super(path, version);
    }

    public long getDateInMs() {
        return 0;
    }

    public String getName() {
        return null;
    }

    public void getLatLong(double[] latLong) {
        latLong[0] = INVALID_LATLNG;
        latLong[1] = INVALID_LATLNG;
    }

    public String[] getTags() {
        return null;
    }

    public Face[] getFaces() {
        return null;
    }

    // The rotation of the full-resolution image. By default, it returns the value of
    // getRotation().
    public int getFullImageRotation() {
        return getRotation();
    }

    public int getRotation() {
        return 0;
    }

    public long getSize() {
        return 0;
    }

    public abstract String getMimeType();

    public String getFilePath() {
        return "";
    }

    // Returns width and height of the media item.
    // Returns 0, 0 if the information is not available.
    public abstract int getWidth();
    public abstract int getHeight();

    // This is an alternative for requestImage() in PhotoPage. If this
    // is implemented, you don't need to implement requestImage().
    public ScreenNail getScreenNail() {
        return null;
    }

    private int mC2paFlag = C2PA_FLAG_UNDEFINED;

    public static final int C2PA_FLAG_UNDEFINED = 0;
    public static final int C2PA_FLAG_PROTECTED = 1;
    public static final int C2PA_FLAG_AI_EDITED = 2;
    public static final int C2PA_FLAG_NOT_EXIST = 3;
    public static final int C2PA_FLAG_INVALID = 4;

    public enum C2PAStatus {
        NON_C2PA,
        C2PA,
        C2PA_INVALID_HASH,
        C2PA_INVALID_SIGNATURE
    }

    public int checkC2pa() {
        if (!isUnder500MB(getFilePath())) {
            return C2PAStatus.NON_C2PA.ordinal();
        }
        if(!C2paUtil.isC2paEnabled()){
            return C2PAStatus.NON_C2PA.ordinal();
        }
        if (mC2paFlag != C2PAStatus.NON_C2PA.ordinal()) {
            return mC2paFlag;
        }
        C2paUtil c2paUtil = new C2paUtil(getFilePath());
        c2paUtil.validateImage();
        C2paUtil.C2PAStatus status = c2paUtil.getC2PAStatus();
        Log.d(TAG, "C2PA info, file path " + getFilePath() + ",status:" + status.toString());
        mC2paFlag = status.ordinal();
        return mC2paFlag;
    }

    private static final long LIMIT_500MB = 500L * 1024 * 1024; // 524,288,000 bytes

    public static boolean isUnder500MB(String path) {
        if (path == null || path.isEmpty()) return false;

        File f = new File(path);
        if (!f.exists() || !f.isFile()) return false;

        long size = f.length();
        return size >= 0 && size <= LIMIT_500MB;
    }

    public static int getTargetSize(int type) {
        switch (type) {
            case TYPE_THUMBNAIL:
                return sThumbnailTargetSize;
            case TYPE_MICROTHUMBNAIL:
                return sMicrothumbnailTargetSize;
            default:
                throw new RuntimeException(
                    "should only request thumb/microthumb from cache");
        }
    }

    public static BytesBufferPool getBytesBufferPool() {
        return sMicroThumbBufferPool;
    }

    public static void setThumbnailSizes(int size, int microSize) {
        sThumbnailTargetSize = size;
        if (sMicrothumbnailTargetSize != microSize) {
            sMicrothumbnailTargetSize = microSize;
        }
    }
}
