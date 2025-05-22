//
// Created by emoji on 2025/5/22.
//

#include "MyAAudio.h"
#include "cmath"
#include "android/log.h"


aaudio_data_callback_result_t myDataCallback(AAudioStream *stream,
                                             void *userData,
                                             void *audioData,
                                             int32_t numFrames) {
    float *output = static_cast<float *>(audioData);
    static float phase = 0.0f;
    float frequency = 440.0f;
    float sampleRate = AAudioStream_getSampleRate(stream);

    for (int i = 0; i < numFrames; ++i) {
        output[i] = sinf(2 * M_PI * frequency * phase / sampleRate);
        phase += 1.0f;
    }

    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void MyAAudio::test() {
    if (__builtin_available(android 8.1, *)) {
        // 使用 AAudio
    } else {
        // 使用 OpenSL ES 或 fallback
        return;
    }

    AAudioStream *stream = nullptr;

    aaudio_result_t result;
    AAudioStreamBuilder *builder;

// 创建 Builder
    AAudio_createStreamBuilder(&builder);

// 配置播放参数
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_FLOAT);
    AAudioStreamBuilder_setChannelCount(builder, 1);
    AAudioStreamBuilder_setSampleRate(builder, 48000);
    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_EXCLUSIVE);

// 设置回调（可选）
    AAudioStreamBuilder_setDataCallback(builder, myDataCallback, nullptr);

// 打开流
    result = AAudioStreamBuilder_openStream(builder, &stream);

// 检查错误
    if (result != AAUDIO_OK) {
        __android_log_print(ANDROID_LOG_ERROR, "AAudio", "Failed to open stream: %s", AAudio_convertResultToText(result));
    }

    AAudioStream_requestStart(stream);



//    AAudioStream_requestStop(stream);
//    AAudioStream_close(stream);
//    AAudioStreamBuilder_delete(builder);


}

