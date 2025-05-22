//
// Created by emoji on 2025/5/22.
//

#ifndef FTP_MYOBOE_H
#define FTP_MYOBOE_H
#include "SineWavePlayer.h"
#include "EchoTest.h"

class MyOboe {
public:
    MyOboe();
    ~MyOboe();
    std::shared_ptr<SineWavePlayer> sineWavePlayer;
    std::shared_ptr<EchoTest> echoTest;
    void test();

    void startEcho();
    void stopEcho();
};


#endif //FTP_MYOBOE_H
