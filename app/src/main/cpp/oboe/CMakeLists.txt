# oboe

# 加入 Oboe 子目录
add_subdirectory(oboe-1.9.3)

add_library(myoboe STATIC
        MyOboe.h
        MyOboe.cpp
        SineWavePlayer.h
        SineWavePlayer.cpp
        EchoTest.h
        EchoTest.cpp
        IOboe.h
        IOboe.cpp
)


target_link_libraries(myoboe
        PUBLIC
        oboe
        PRIVATE android
        log
)
