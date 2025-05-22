//
// Created by emoji on 2025/5/22.
//

#include "EchoTest.h"


void EchoTest::openInputStream() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(1)
            ->setSampleRate(48000);

    builder.openStream(&inputStream);
    inputStream->start();
}

void EchoTest::openOutputStream() {
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(1)
            ->setSampleRate(48000);

    builder.openStream(&outputStream);
    outputStream->start();
}

void EchoTest::startEcho() {
    openInputStream();
    openOutputStream();

    isRunning = true;
    ioThread = std::thread([this]() {
        const int32_t bufferSize = 480;
        std::vector<float> buffer(bufferSize);

        while (isRunning) {
            int32_t framesRead = inputStream->read(buffer.data(), bufferSize, 1000 * 1000).value();
            if (framesRead > 0) {
                outputStream->write(buffer.data(), framesRead, 1000 * 1000);
            }
        }
    });
}

void EchoTest::stopEcho() {
    isRunning = false;
    if (ioThread.joinable()) ioThread.join();

    if (inputStream) {
        inputStream->stop();
        inputStream->close();
        inputStream = nullptr;
    }

    if (outputStream) {
        outputStream->stop();
        outputStream->close();
        outputStream = nullptr;
    }
}

EchoTest::~EchoTest() {
    stopEcho();
}

