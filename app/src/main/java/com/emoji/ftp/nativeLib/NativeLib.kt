package com.emoji.ftp.nativeLib

class NativeLib {
    companion object {
        init {
            System.loadLibrary("test")
        }
    }

    external fun nativeFunc(): Int


    external fun nativeAAudioTest(): Int
}
