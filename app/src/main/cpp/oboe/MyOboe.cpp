//
// Created by emoji on 2025/5/22.
//

#include "MyOboe.h"

void MyOboe::test() {
    sineWavePlayer = std::make_shared<SineWavePlayer>();
    sineWavePlayer->start();
}

void MyOboe::startEcho() {
    echoTest = std::make_shared<EchoTest>();
    echoTest->startEcho();
}

void MyOboe::stopEcho() {
    if (echoTest){
        echoTest->stopEcho();
    }
}

MyOboe::MyOboe() {

}

MyOboe::~MyOboe() {

}

