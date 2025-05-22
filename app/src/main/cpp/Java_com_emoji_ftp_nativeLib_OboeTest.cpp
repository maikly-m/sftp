//
// Created by emoji on 2025/5/22.
//

#include "Java_com_emoji_ftp_nativeLib_OboeTest.h"
#include "jni.h"
#include "oboe/MyOboe.h"

extern "C"
JNIEXPORT jlong JNICALL
Java_com_emoji_ftp_nativeLib_OboeTest_testStart(JNIEnv *env, jobject thiz) {
    auto *myOboe = new MyOboe;
    myOboe->test();
    return reinterpret_cast<jlong>(myOboe);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_emoji_ftp_nativeLib_OboeTest_testStop(JNIEnv *env, jobject thiz, jlong ptr) {
    if (ptr == 0){
        return;
    }
    auto *myOboe = reinterpret_cast<MyOboe *>(ptr);
    delete myOboe;
}


extern "C"
JNIEXPORT jlong JNICALL
Java_com_emoji_ftp_nativeLib_OboeTest_testStartEcho(JNIEnv *env, jobject thiz) {
    auto *myOboe = new MyOboe;
    myOboe->startEcho();
    return reinterpret_cast<jlong>(myOboe);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_emoji_ftp_nativeLib_OboeTest_testStopEcho(JNIEnv *env, jobject thiz, jlong ptr) {
    if (ptr == 0){
        return;
    }
    auto *myOboe = reinterpret_cast<MyOboe *>(ptr);
    myOboe->startEcho();
    delete myOboe;
}