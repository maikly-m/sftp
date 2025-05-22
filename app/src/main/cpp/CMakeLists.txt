cmake_minimum_required(VERSION 3.14)
project(test)

add_subdirectory(aaudio)
add_subdirectory(oboe)

add_library(test SHARED
        main.h
        Java_com_emoji_ftp_nativeLib_NativeLib.h
        Java_com_emoji_ftp_nativeLib_OboeTest.h

        main.cpp
        Java_com_emoji_ftp_nativeLib_NativeLib.cpp
        Java_com_emoji_ftp_nativeLib_OboeTest.cpp
)

target_link_libraries(
        test
        PUBLIC
        myaaudio
        myoboe
        PRIVATE android log)