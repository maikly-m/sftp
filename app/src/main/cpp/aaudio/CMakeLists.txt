
add_library(myaaudio STATIC
        MyAAudio.h
        MyAAudio.cpp
)

find_library(aaudio-lib aaudio)

target_link_libraries(
        myaaudio
        PUBLIC
        ${aaudio-lib}
        PRIVATE android log)