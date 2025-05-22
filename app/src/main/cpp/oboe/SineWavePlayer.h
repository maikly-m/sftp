//
// Created by emoji on 2025/5/22.
//

#ifndef FTP_SINEWAVEPLAYER_H
#define FTP_SINEWAVEPLAYER_H

#include <cmath>
#include <oboe/Oboe.h>
#include "android/log.h"
#include "IOboe.h"

class SineWavePlayer: public oboe::AudioStreamCallback, IOboe  {
public:
    SineWavePlayer();
    ~SineWavePlayer() override;
public:
    float phase = 0.0f;
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *stream, void *audioData, int32_t numFrames) override;
    void start();
    void stop();
private:
    oboe::AudioStream *stream = nullptr;
};


#endif //FTP_SINEWAVEPLAYER_H
