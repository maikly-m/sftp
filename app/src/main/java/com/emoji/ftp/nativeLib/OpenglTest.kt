package com.emoji.ftp.nativeLib

import android.view.Surface

class OpenglTest {
    companion object {
        init {
            System.loadLibrary("test")
        }
    }

    private val nativeHandle: Long = 0

    fun start(previewSurface: Surface) {
        nativeInit(previewSurface)
        nativeStart()
    }

    fun resize(width: Int, height: Int) {
        nativeResize(width, height)
    }

    fun stop() {
        nativeStop()
        nativeRelease()
    }

    private external fun nativeInit(previewSurface: Surface)
    private external fun nativeStart()
    private external fun nativeResize(width: Int, height: Int)
    private external fun nativeStop()
    private external fun nativeRelease()
}