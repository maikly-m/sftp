cmake_minimum_required(VERSION 3.14)
project(test)

add_subdirectory(aaudio)
add_subdirectory(oboe)
#add_subdirectory(opengl)

add_library(test SHARED
        main.h
        Java_com_emoji_ftp_nativeLib_NativeLib.h
        Java_com_emoji_ftp_nativeLib_OboeTest.h
        Java_com_emoji_ftp_nativeLib_OpenglTest.h

        main.cpp
        Java_com_emoji_ftp_nativeLib_NativeLib.cpp
        Java_com_emoji_ftp_nativeLib_OboeTest.cpp
        Java_com_emoji_ftp_nativeLib_OpenglTest.cpp
)

find_library(egl-lib EGL)
find_library(gles-lib GLESv3)

target_link_libraries(
        test
        PUBLIC
        myaaudio
        myoboe
        myopengl
        PRIVATE
        android
        log
        ${egl-lib}
        ${gles-lib})