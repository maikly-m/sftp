package com.example.ftp.utils.thread

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.concurrent.Volatile

class AppExecutors private constructor(
    private val diskIO: Executor = SingleThreadExecutor(),
    private val networkIO: Executor = Executors.newFixedThreadPool(
        THREAD_COUNT
    ),
    private val mainThread: Executor = MainThreadExecutor(),
    private val multiThread: Executor = Executors.newFixedThreadPool(
        THREAD_COUNT
    )
) {
    fun diskIO(): Executor {
        return diskIO
    }

    fun networkIO(): Executor {
        return networkIO
    }

    fun mainThread(): Executor {
        return mainThread
    }

    fun multiThread(): Executor {
        return multiThread
    }

    private class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }

    companion object {
        @Volatile
        private var sInstance: AppExecutors? = null

        private const val THREAD_COUNT = 3

        /**
         * 获取全局的线程池
         *
         * @return 全局的线程池
         */
        fun globalAppExecutors(): AppExecutors? {
            if (sInstance == null) {
                synchronized(AppExecutors::class.java) {
                    if (sInstance == null) {
                        sInstance = AppExecutors()
                    }
                }
            }
            return sInstance
        }
    }
}