//
// Created by emoji on 2025/5/22.
//

#include "SineWavePlayer.h"

oboe::DataCallbackResult
SineWavePlayer::onAudioReady(oboe::AudioStream *stream, void *audioData, int32_t numFrames) {
    auto *floatData = static_cast<float *>(audioData);
    float frequency = 440.0f; // A4
    float sampleRate = stream->getSampleRate();

    for (int i = 0; i < numFrames; ++i) {
        float sample = sinf(2 * M_PI * phase);
        phase += frequency / sampleRate;
        if (phase >= 1.0f) phase -= 1.0f;
        floatData[i] = sample;
    }
    // __android_log_print(ANDROID_LOG_DEBUG, "Oboe", "floatData[0] %f", floatData[0] );
    return oboe::DataCallbackResult::Continue;
}

void SineWavePlayer::start() {
    oboe::AudioStreamBuilder builder;
    builder.setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(1)
            ->setSampleRate(48000)
            ->setCallback(this)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setDirection(oboe::Direction::Output);

    oboe::Result result = builder.openStream(&stream);
    if (result != oboe::Result::OK || stream == nullptr) {
        // 处理错误
        __android_log_print(ANDROID_LOG_ERROR, "Oboe", "打开音频流失败: %s", oboe::convertToText(result));
    }

    if (stream) {
        result = stream->start();
        if (result != oboe::Result::OK) {
            __android_log_print(ANDROID_LOG_ERROR, "Oboe", "启动失败: %s", oboe::convertToText(result));
        }
    }
}

void SineWavePlayer::stop() {
    if (stream){
        stream->requestStop();   // 停止播放
        stream->close();         // 释放资源
        stream = nullptr;
    }
}

SineWavePlayer::SineWavePlayer() = default;

SineWavePlayer::~SineWavePlayer() {
    stop();
}
