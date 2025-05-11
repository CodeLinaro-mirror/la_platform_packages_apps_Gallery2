/*
 * Copyright (c) 2025 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */
#include <jni.h>
#include <android/log.h>
#include <vndk/hardware_buffer.h>
#include <aidlcommonsupport/NativeHandle.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <map>
#include <utils/Log.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <errno.h>

using ::aidl::android::hardware::common::NativeHandle;

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "jni_c2pa_util", __VA_ARGS__)
#define SIZE_2MB 0x200000

#define T_ERROR(error_code)                                                      \
    ret = (error_code);                                                          \
    ALOGE("%s::%d err=0x%x errno:%d(%s)", __func__, __LINE__, error_code, errno, \
          strerror(errno));                                                      \
    goto exit;

#define T_CHECK_ERR(cond, error_code) \
    if (!(cond)) {                    \
        T_ERROR(error_code)           \
    }

#define T_CHECK(cond)                        \
    if (!(cond)) {                           \
        ALOGE("%s::%d", __func__, __LINE__); \
        goto exit;                           \
    }

#define LOGD_PRINT(...)      \
    do {                     \
        ALOGD(__VA_ARGS__);  \
        printf(__VA_ARGS__); \
        printf("\n");        \
    } while (0)
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jintArray JNICALL Java_com_android_gallery3d_util_C2paUtil_nativeGetHardwareBufferFd(JNIEnv *env, jobject obj, jstring jpath);
JNIEXPORT void JNICALL Java_com_android_gallery3d_util_C2paUtil_nativeFreeFd(JNIEnv *env, jobject obj, jint id);

#ifdef __cplusplus
}
#endif

struct AHardwareBufferDeleter {
    void operator()(AHardwareBuffer* buffer) const {
        if (buffer != nullptr) {
            AHardwareBuffer_release(buffer);
        }
    }
};

using HardwareBufferPtr = std::unique_ptr<AHardwareBuffer, AHardwareBufferDeleter>;

std::map<int, HardwareBufferPtr> mHwBufferList;

void addBufferToMap(int key, AHardwareBuffer* buffer) {
    if (buffer != nullptr) {
        mHwBufferList[key] = HardwareBufferPtr(buffer);
    }
}

bool removeBufferFromMap(int key) {
    auto it = mHwBufferList.find(key);
    if (it != mHwBufferList.end()) {
        mHwBufferList.erase(it);
        return true;
    }
    return false;
}

void JNICALL Java_com_android_gallery3d_util_C2paUtil_nativeFreeFd(JNIEnv *env, jobject obj, jint id){
    removeBufferFromMap(id);
}

jintArray JNICALL Java_com_android_gallery3d_util_C2paUtil_nativeGetHardwareBufferFd(JNIEnv *env, jobject obj, jstring jpath) {
    int32_t ret = 0;
    int32_t fd = -1, ashmemFd = -1, buffLen = 0;
    struct stat appStat = {};
    void *buffer = nullptr, *outBuffer = nullptr;
    size_t copied = 0, fdSize = 0;
    AHardwareBuffer *hwBuffer = nullptr;
    AHardwareBuffer_Desc bufDesc;
    NativeHandle nativeHandleAIDL;
    const native_handle_t* nativeHandle;
    int values[2] = {-1, -1};

    jintArray result = env->NewIntArray(2);
    const char *filePath = env->GetStringUTFChars(jpath, 0);
    T_CHECK(filePath != NULL);

    fd = open(filePath, O_RDONLY);
    T_CHECK_ERR(fd > 0, fd);
    ret = fstat(fd, &appStat);
    T_CHECK_ERR(ret == 0, -1);
    fdSize = appStat.st_size;
    T_CHECK_ERR(fdSize > 0, -1);

    buffLen = fdSize;
    bufDesc.width = (buffLen + (SIZE_2MB -1)) & (~(SIZE_2MB - 1));
    bufDesc.height = 1;
    bufDesc.layers = 1;
    bufDesc.format = AHARDWAREBUFFER_FORMAT_BLOB;

    ret = AHardwareBuffer_allocate(&bufDesc, &hwBuffer);
    T_CHECK_ERR(ret == 0 && hwBuffer != nullptr, -1);

    nativeHandle = AHardwareBuffer_getNativeHandle(hwBuffer);
    T_CHECK_ERR(nativeHandle != nullptr, -1);

    nativeHandleAIDL = android::dupToAidl(nativeHandle);
    T_CHECK_ERR(!android::isAidlNativeHandleEmpty(nativeHandleAIDL), -1);

    buffer = malloc(fdSize);
    T_CHECK_ERR(buffer != nullptr, -1);

    ret = read(fd, buffer, fdSize);
    T_CHECK_ERR(ret == fdSize, -1);
    ret = 0;

    LOGD_PRINT("Load file of size :%d %d", fdSize, nativeHandleAIDL.fds[0].get());
    ashmemFd = dup(nativeHandleAIDL.fds[0].get());
    T_CHECK_ERR(ashmemFd > 0, fd);

    outBuffer = mmap(NULL, fdSize, PROT_READ | PROT_WRITE, MAP_SHARED, ashmemFd, 0);
    T_CHECK_ERR(outBuffer != MAP_FAILED, -1);

    memcpy(outBuffer, buffer, fdSize);

    LOGD_PRINT("Successfully loaded file into ashmem memory");

    addBufferToMap(ashmemFd, hwBuffer);

    env->ReleaseStringUTFChars(jpath, filePath);
    values[0] = ashmemFd;
    values[1] = static_cast<int>(fdSize);
    env->SetIntArrayRegion(result, 0, 2, values);

    exit:
    if (ret != 0) {
        removeBufferFromMap(ashmemFd);
    }
    if (outBuffer != nullptr) {
        munmap(outBuffer, fdSize);
    }
    if (ret != 0 && ashmemFd >= 0) {
        close(ashmemFd);
    }
    if (buffer != NULL) {
        free(buffer);
    }
    if (fd > 0) {
        close(fd);
    }
    return result;
}