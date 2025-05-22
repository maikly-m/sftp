package com.emoji.ftp.nativeLib

class OboeTest {
    companion object {
        init {
            System.loadLibrary("test")
        }
    }

    external fun testStart() : Long
    external fun testStop(longPtr: Long)

    external fun testStartEcho() : Long
    external fun testStopEcho(longPtr: Long)
}