cmake_minimum_required(VERSION 3.10.2)
project(test)


add_library(test SHARED
        main.h
        Java_com_emoji_ftp_nativeLib_NativeLib.h

        main.cpp
        Java_com_emoji_ftp_nativeLib_NativeLib.cpp
)

target_include_directories(test
        PUBLIC
        ${CMAKE_CURRENT_LIST_DIR}
)

target_link_libraries(
        test
        android
        log)