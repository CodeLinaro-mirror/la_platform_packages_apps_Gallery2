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

package com.android.gallery3d.util;

import android.hardware.common.Ashmem;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.ServiceManager;
import android.os.SharedMemory;
import android.system.ErrnoException;
import android.system.OsConstants;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
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
import com.truepic.lensverify.data.c2padata.ManifestStore;
import com.truepic.lensverify.data.c2padata.ValidationStatus;

public class C2paUtil {
    private static final String TAG = "C2paUtil";
    private Map<FileDescriptor, SharedMemory> fd_mem =
            new HashMap<FileDescriptor, SharedMemory>();
    private static IC2PA mFactoryAidl = null;
    private String mJsonResult;
    private Map<String, byte[]> mThumbnails = new HashMap<>();

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
    public  Ashmem getDataInAshmemObj(ByteBuffer pData) {
        int ret = 0;
        int rDataSize = 0;
        Ashmem rAshmem;
        SharedMemory sharedFd = null;
        ByteBuffer bbf = null;
        String s;
        byte[] recieveSM;

        rAshmem = new Ashmem();

        Log.i(TAG, "getDataInAshmem:Enter.");
        if (pData == null) {
            Log.e(TAG, "getDataInAshmem: ERROR: Null ptr passed");
            return null;
        }

        rDataSize = pData.array().length - pData.arrayOffset();
        sharedFd = createSharedMemory(rDataSize);
        Log.i(TAG, "SharedFd : " + Integer.toString(sharedFd.getFileDescriptor().getInt$()));
        try {
            bbf = sharedFd.map(OsConstants.PROT_READ|OsConstants.PROT_WRITE, 0, rDataSize);
        } catch (ErrnoException e) {
            Log.e(TAG, "getDataInAshmem: ERROR: Failed to map Sharedmemory : ", e);
            sharedFd.close();
            return null;
        }
        pData.flip();
        pData.position(pData.arrayOffset() + pData.position());
        bbf.put(pData.array(), pData.position(), rDataSize);

        try {
            rAshmem.fd = ParcelFileDescriptor.dup(sharedFd.getFileDescriptor());
            rAshmem.size = rDataSize;
            fd_mem.put(rAshmem.fd.getFileDescriptor(), sharedFd);
            unmapSharedMemory(rAshmem.fd, bbf);
        } catch (IOException e) {
            Log.e(TAG, "getDataInAshmem: ERROR: Failed to get file descriptor : ", e);
            sharedFd.unmap(bbf);
            sharedFd.close();
            fd_mem.remove(rAshmem.fd);
            return null;
        }

        Log.i(TAG, "getDataInAshmem:Exit.");
        return rAshmem;
    }

    private SharedMemory createSharedMemory(int size) {
        SharedMemory sFD = null;
        try {
            sFD = SharedMemory.create("", size);
        } catch (ErrnoException e) {
            Log.e(TAG, "createSharedMemory: ERROR: Failed to create Sharedmemory : ", e);
        }
        if (sFD == null || sFD.getSize() != size) {
            Log.e(TAG, "createSharedMemory: ERROR: Failed to allocate shared memory");
            sFD.close();
            return null;
        }
        return sFD;
    }

    private void unmapSharedMemory(ParcelFileDescriptor pFd, ByteBuffer bBuf) {
        FileDescriptor fd = pFd.getFileDescriptor();
        if (!fd_mem.containsKey(fd)) {
            Log.e(TAG, "unmapSharedMemory: ERROR: FD not found in cached map");
            return;
        }
        fd_mem.get(fd).unmap(bBuf);
    }

    public  ByteBuffer readFileToByteBuffer(File file) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        ByteBuffer byteBuffer;
        try {
            long longLength = raf.length();
            int length = (int) longLength;
            if (length != longLength) throw new IOException("File size >= 2 GB");
            Log.d(TAG,"signC2PA readFileToByteBuffer buffer size =  " + length);
            byte[] data = new byte[length];
            raf.readFully(data);
            byteBuffer = ByteBuffer.allocate(data.length);
            byteBuffer.put(data);
        } finally {
            raf.close();
        }
        return byteBuffer;
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
        outPair.key = "MEDIA_TYPE";
        c2PADataType = new C2PADataType();
        c2PADataType.setIntValue(type);
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
                            if(outputParams.get(iter).key.equals(thumbnailLabel[2] + iter2)){
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
            File file = new File(mFilePath);
            ByteBuffer buffer = readFileToByteBuffer(file);
            Ashmem ashmem = getDataInAshmemObj(buffer);
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

    public C2PAStatus getC2PAStatus(C2PAData data) {
        try {
            boolean isInvalidHash = false;
            if(data != null) {
                for (ManifestStore manifestStore : data.getManifestStore()) {
                    for (ValidationStatus validationStatus : manifestStore.getValidationStatuses()) {
                        if (validationStatus.code.contains("signingCredential.") && !validationStatus.success) {
                            return C2PAStatus.C2PA_INVALID_SIGNATURE;
                        }

                        if (!validationStatus.success) {
                            isInvalidHash = true;
                        }
                    }
                }
            }
            return isInvalidHash ? C2PAStatus.C2PA_INVALID_HASH : C2PAStatus.C2PA;
        } catch (Exception ex) {
            return C2PAStatus.NON_C2PA;
        }
    }
}
