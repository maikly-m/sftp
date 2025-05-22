
add_library(myopengl STATIC
        MyOpenglTest.h
        MyOpenglTest.cpp
)

find_library(egl-lib EGL)
find_library(gles-lib GLESv3)

target_link_libraries(
        myopengl
        ${egl-lib}
        ${gles-lib}
        PRIVATE android log
)