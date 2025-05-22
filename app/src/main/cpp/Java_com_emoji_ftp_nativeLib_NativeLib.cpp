//
// Created by emoji on 2025/5/21.
//

#include <jni.h>
#include "Java_com_emoji_ftp_nativeLib_NativeLib.h"

#include "aaudio/MyAAudio.h"

extern "C"
JNIEXPORT jint JNICALL
Java_com_emoji_ftp_nativeLib_NativeLib_nativeFunc(JNIEnv *env, jobject thiz) {
    return 1;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_emoji_ftp_nativeLib_NativeLib_nativeAAudioTest(JNIEnv *env, jobject thiz) {
    MyAAudio myAAudio;
    myAAudio.test();
    return 1;
}

