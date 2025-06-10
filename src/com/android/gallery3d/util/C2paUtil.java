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
 * Copyright (c) 2024-2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.gallery3d.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.common.Ashmem;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.ServiceManager;
import android.os.SharedMemory;
import android.preference.PreferenceManager;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import vendor.qti.hardware.c2pa.C2PADataType;
import vendor.qti.hardware.c2pa.C2PADataTypePair;
import vendor.qti.hardware.c2pa.IC2PA;

import com.truepic.lensverify.data.c2padata.C2PAData;
import com.google.gson.Gson;

public class C2paUtil {
    private static final String TAG = "C2paUtil";
    private static boolean mIsSupported = false;

    private static Context mContext = null;
    private Map<FileDescriptor, SharedMemory> fd_mem =
            new HashMap<FileDescriptor, SharedMemory>();
    private static IC2PA mFactoryAidl = null;
    private String mJsonResult;
    private Map<String, byte[]> mThumbnails = new HashMap<>();
    private C2PAStatus mStatus;

    String mFilePath;
    public C2paUtil(String filePath) {
        mFilePath = filePath;
    }
    public enum C2PAStatus {
        NON_C2PA,
        C2PA,
        C2PA_INVALID_HASH,
        C2PA_INVALID_SIGNATURE
    }

    public static void connectC2PAService() {
        try {
            if(mFactoryAidl == null) {
                Log.d(TAG, "Call C2PA getService");
                String ISERVICE_INTERFACE = "vendor.qti.hardware.c2pa.IC2PA/default";
                if (ServiceManager.isDeclared(ISERVICE_INTERFACE)) {
                    IBinder binder = ServiceManager.waitForService(ISERVICE_INTERFACE);
                    mFactoryAidl = IC2PA.Stub.asInterface(binder);
                    Log.d(TAG, "get C2PA Service successfully");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "C2PA not supported: " + e);
        }
        if ( mFactoryAidl == null) {
            Log.e(TAG, "C2PA returned null");
        }
    }

    public  List<C2PADataTypePair> getInputConfigParams(int type) {
        List<C2PADataTypePair> configParams = new ArrayList<C2PADataTypePair>();
        C2PADataTypePair outPair = new C2PADataTypePair();
        C2PADataType c2PADataType;
        outPair.key = "INPLACE_UPDATE";
        c2PADataType = new C2PADataType();
        c2PADataType.setByteValue((byte) 1);
        outPair.value = c2PADataType;
        configParams.add(outPair);
        return configParams;
    }

    public String parseFileDescriptor(ParcelFileDescriptor parcelFileDescriptor) {
        try (FileInputStream fileInputStream = new FileInputStream(
                parcelFileDescriptor.getFileDescriptor())) {
            Log.d(TAG, "parseFileDescriptor size = " + fileInputStream.available());
            StringBuilder stringBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
            }
            String jsonString = stringBuilder.toString();
            return jsonString;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] parseFileDescriptorToBytes(ParcelFileDescriptor parcelFileDescriptor) {
        try (FileInputStream fileInputStream = new FileInputStream(
                parcelFileDescriptor.getFileDescriptor())) {
            Log.d(TAG, "parseFileDescriptorToBytes size = " + fileInputStream.available());
            return fileInputStream.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    private void parseOutputParams(List<C2PADataTypePair> outputParams) {
        String[] thumbnailLabel = {"THUMBNAIL_URI", "THUMBNAIL_ID",
                "THUMBNAIL_MANIFEST_URI", "THUMBNAIL_INSTANCE_ID",
                "THUMBNAIL_TITLE", "THUMBNAIL_FORMAT"};
        if (outputParams != null && outputParams.size() > 0) {
            Log.d(TAG, "parseOutputParams outputParams = " + outputParams.size());
            for (int iter = 0; iter < outputParams.size(); iter++) {
                Log.d(TAG, "parseOutputParams pair,ley = " + outputParams.get(iter).key + ",value:" + outputParams.get(iter).value + ",iter:" + iter);
                if ("VALIDATION_REPORT".equals(outputParams.get(iter).key)) {
                    Ashmem report = outputParams.get(iter).value.getFdValue();
                    if (report.size > 0) {
                        mJsonResult = parseFileDescriptor(report.fd);
                    }
                } else if ("THUMBNAIL_ARRAY".equals(outputParams.get(iter).key)) {
                    String label;
                    int listSize = outputParams.get(iter).value.getIntValue();
                    Log.i(TAG,"listSize:" + listSize);
                    for (int iter2 = 0; iter2 < listSize; iter2++) {
                        String thumbnailKey = "";
                        iter++;
                        for (int iter3 = 0; iter3 < thumbnailLabel.length && iter < outputParams.size(); iter3++) {
                            label = thumbnailLabel[iter3] + iter2;
                            Log.i(TAG,"label:" + label + ",key:" + outputParams.get(iter).key + ",iter:" + iter);
                            if(outputParams.get(iter).key.equals(thumbnailLabel[1] + iter2)){
                                Log.i(TAG," value:" + outputParams.get(iter).value.getStringValue());
                                thumbnailKey = outputParams.get(iter).value.getStringValue();
                            }
                            iter++;
                        }
                        label = "THUMBNAIL_FILE" + iter2;
                        Log.i(TAG,"THUMBNAIL_FILE, label:" + label + ",iter:" + iter);
                        if(outputParams.get(iter).key.equals(label)){
                            Ashmem file = outputParams.get(iter).value.getFdValue();
                            Log.i(TAG,"file.fd:" + file.fd );
                            if(!thumbnailKey.equals("")) {
                                mThumbnails.put(thumbnailKey, parseFileDescriptorToBytes(file.fd));
                            }
                        }
                    }
                }
            }
        }
    }
    public  String getValidateResult(){
        return mJsonResult;
    }

    public void validateImage(){
        try {
            Ashmem ashmem = new Ashmem();
            Log.i(TAG,"mIsSupported:" + mIsSupported);
            if(!mIsSupported){
                return;
            }
            int[] values = nativeGetHardwareBufferFd(mFilePath);
            if(values == null){
                return;
            }
            try {
                ashmem.fd = ParcelFileDescriptor.fromFd(values[0]);
                ashmem.size = values[1];
            } catch (IOException e) {
                Log.e(TAG, "ERROR: Failed to get file descriptor : ", e);
                return ;
            }
            List<C2PADataTypePair> configParams = getInputConfigParams(0);
            List<C2PADataTypePair> outputParams = new ArrayList<C2PADataTypePair>();
            Log.d(TAG,"signC2PA signC2PA now! ashmem size =  " + ashmem.size + ",mFactoryAidl:" + mFactoryAidl);
            if(mFactoryAidl == null){
                connectC2PAService();
            }
            Log.d(TAG,"start to validate media  " );
            int response =
                    mFactoryAidl.validateMedia(ashmem, configParams, outputParams);
            Log.d(TAG,"validateC2PA response = " + response);
            parseOutputParams(outputParams);
            nativeFreeFd(values[0]);
            mStatus = parseC2PAStatus(response);
        } catch (Exception e) {
            Log.e(TAG,"signC2PA failed " + e);
            e.printStackTrace();
        }
    }
    public  C2PAData getImageC2paData() {
        Gson gson = new Gson();
        if (mThumbnails != null && mThumbnails.size() > 0) {
            C2PAData data = gson.fromJson(mJsonResult, C2PAData.class);
            if(data != null) data.setThumbnailStore(mThumbnails);
            return data;
        } else {
            return gson.fromJson(mJsonResult, C2PAData.class);
        }
    }

    private C2PAStatus parseC2PAStatus(int result) {
        if(result == 0){
            return C2PAStatus.C2PA;
        }else if (result == 1){
            return C2PAStatus.C2PA_INVALID_HASH;
        }
        return C2PAStatus.NON_C2PA;
    }

    public C2PAStatus getC2PAStatus() {
        return mStatus;
    }
    public static void setContext(Context context){
        mContext = context;
    }

    public static boolean isC2paEnabled(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        return settings.getBoolean("c2pa_option", false);
    }

    private native int[] nativeGetHardwareBufferFd(String filePath);

    private native  void nativeFreeFd(int id);

    static {
        try {
            System.loadLibrary("jni_c2pautil");
            mIsSupported = true;
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, e.toString());
            mIsSupported = false;
        }
    }
}
