//
// Created by emoji on 2025/5/22.
//

#include "Java_com_emoji_ftp_nativeLib_OpenglTest.h"

#include <jni.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <string>

#define LOG_TAG "NativeRenderer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

static EGLDisplay eglDisplay = EGL_NO_DISPLAY;
static EGLContext eglContext = EGL_NO_CONTEXT;
static EGLSurface eglSurface = EGL_NO_SURFACE;
static ANativeWindow *nativeWindow = nullptr;
static int width = 0;
static int height = 0;

extern "C"
JNIEXPORT void JNICALL
Java_com_emoji_ftp_nativeLib_OpenglTest_nativeInit(JNIEnv
                                                   *env,
                                                   jobject thiz, jobject
                                                   surface) {
    if (nativeWindow) {
        ANativeWindow_release(nativeWindow);
        nativeWindow = nullptr;
    }
    nativeWindow = ANativeWindow_fromSurface(env, surface);

    eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (eglDisplay == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay failed");
        return;
    }
    if (!
            eglInitialize(eglDisplay, nullptr, nullptr
            )) {
        LOGE("eglInitialize failed");
        return;
    }

    const EGLint configAttribs[] = {
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 16,
            EGL_STENCIL_SIZE, 8,
            EGL_NONE
    };

    EGLConfig eglConfig;
    EGLint numConfigs;
    if (!
                eglChooseConfig(eglDisplay, configAttribs, &eglConfig,
                                1, &numConfigs) || numConfigs < 1) {
        LOGE("eglChooseConfig failed");
        return;
    }

    eglSurface = eglCreateWindowSurface(eglDisplay, eglConfig, nativeWindow, nullptr);
    if (eglSurface == EGL_NO_SURFACE) {
        LOGE("eglCreateWindowSurface failed");
        return;
    }

    const EGLint contextAttribs[] = {
            EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL_NONE
    };
    eglContext = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, contextAttribs);
    if (eglContext == EGL_NO_CONTEXT) {
        LOGE("eglCreateContext failed");
        return;
    }

    if (!
            eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext
            )) {
        LOGE("eglMakeCurrent failed");
        return;
    }
    LOGI("EGL init success");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_emoji_ftp_nativeLib_OpenglTest_nativeStart(JNIEnv
                                                    *env,
                                                    jobject thiz
) {
// 简单渲染循环模拟：这里只演示一次绘制，用户可以自行扩展线程循环
    if (eglDisplay == EGL_NO_DISPLAY || eglContext == EGL_NO_CONTEXT ||
        eglSurface == EGL_NO_SURFACE) {
        LOGE("EGL not initialized");
        return;
    }
    EGLint surfaceWidth, surfaceHeight;
    eglQuerySurface(eglDisplay, eglSurface, EGL_WIDTH, &surfaceWidth
    );
    eglQuerySurface(eglDisplay, eglSurface, EGL_HEIGHT, &surfaceHeight
    );
    width = surfaceWidth;
    height = surfaceHeight;

    glViewport(0, 0, width, height);
    glClearColor(0.1f, 0.3f, 0.5f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);

// 渲染简单颜色三角形（或其他内容）
    static const GLfloat vertices[] = {
            0.0f, 0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
    };

    GLuint vbo, vao;
    glGenVertexArrays(1, &vao);
    glBindVertexArray(vao);

    glGenBuffers(1, &vbo);
    glBindBuffer(GL_ARRAY_BUFFER, vbo
    );
    glBufferData(GL_ARRAY_BUFFER,
                 sizeof(vertices), vertices, GL_STATIC_DRAW);

    GLuint vertexShader = glCreateShader(GL_VERTEX_SHADER);
    const char *vertexShaderSrc = "#version 300 es\n"
                                  "layout(location = 0) in vec3 aPosition;\n"
                                  "void main() {\n"
                                  "  gl_Position = vec4(aPosition, 1.0);\n"
                                  "}\n";
    glShaderSource(vertexShader,
                   1, &vertexShaderSrc, nullptr);
    glCompileShader(vertexShader);

    GLuint fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
    const char *fragmentShaderSrc = "#version 300 es\n"
                                    "precision mediump float;\n"
                                    "out vec4 fragColor;\n"
                                    "void main() {\n"
                                    "  fragColor = vec4(1.0, 0.5, 0.2, 1.0);\n"
                                    "}\n";
    glShaderSource(fragmentShader,
                   1, &fragmentShaderSrc, nullptr);
    glCompileShader(fragmentShader);

    GLuint shaderProgram = glCreateProgram();
    glAttachShader(shaderProgram, vertexShader
    );
    glAttachShader(shaderProgram, fragmentShader
    );
    glLinkProgram(shaderProgram);

    glUseProgram(shaderProgram);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 3 * sizeof(float), nullptr);

    glDrawArrays(GL_TRIANGLES,
                 0, 3);

    eglSwapBuffers(eglDisplay, eglSurface
    );

// 清理
    glDisableVertexAttribArray(0);
    glDeleteBuffers(1, &vbo);
    glDeleteVertexArrays(1, &vao);
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);
    glDeleteProgram(shaderProgram);

    LOGI("Frame rendered");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_emoji_ftp_nativeLib_OpenglTest_nativeResize(JNIEnv
                                                     *env,
                                                     jobject thiz, jint
                                                     w,
                                                     jint h
) {
    width = w;
    height = h;
    if (eglDisplay !=
        EGL_NO_DISPLAY && eglContext
                          != EGL_NO_CONTEXT) {
        glViewport(0, 0, width, height);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_emoji_ftp_nativeLib_OpenglTest_nativeStop(JNIEnv
                                                   *env,
                                                   jobject thiz
) {
// 可扩展：停止渲染线程等
}

extern "C"
JNIEXPORT void JNICALL
Java_com_emoji_ftp_nativeLib_OpenglTest_nativeRelease(JNIEnv
                                                      *env,
                                                      jobject thiz
) {
    if (eglDisplay != EGL_NO_DISPLAY) {
        eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT
        );
        if (eglSurface != EGL_NO_SURFACE) {
            eglDestroySurface(eglDisplay, eglSurface
            );
            eglSurface = EGL_NO_SURFACE;
        }
        if (eglContext != EGL_NO_CONTEXT) {
            eglDestroyContext(eglDisplay, eglContext
            );
            eglContext = EGL_NO_CONTEXT;
        }
        eglTerminate(eglDisplay);
        eglDisplay = EGL_NO_DISPLAY;
    }
    if (nativeWindow) {
        ANativeWindow_release(nativeWindow);
        nativeWindow = nullptr;
    }
    LOGI("EGL resources released");
}