//
// Created by emoji on 2025/5/22.
//

#ifndef FTP_ECHOTEST_H
#define FTP_ECHOTEST_H

#include <oboe/Oboe.h>
#include <thread>
#include <atomic>
#include <vector>
#include "IOboe.h"

class EchoTest : IOboe{
    oboe::AudioStream *inputStream;
    oboe::AudioStream *outputStream;
    std::atomic<bool> isRunning = false;
    std::thread ioThread;
public:
    ~EchoTest();
    void openInputStream();
    void openOutputStream();
    void startEcho();
    void stopEcho();
};


#endif //FTP_ECHOTEST_H
