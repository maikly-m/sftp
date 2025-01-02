package com.example.ftp

import android.app.Application
import timber.log.Timber

class MyApp:Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree()) // Plant DebugTree for debug builds
        } else {
            // Plant a different tree for release builds (e.g., Crashlytics, Firebase)
            // Timber.plant(MyReleaseTree())
        }

        // 设置全局异常捕获器
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 打印异常信息到日志
            Timber.e("UncaughtException, Thread: ${thread.name}, Error: ${throwable.message}")

        }
    }
}