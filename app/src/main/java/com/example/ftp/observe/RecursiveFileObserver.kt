package com.example.ftp.observe

import android.os.Build
import android.os.FileObserver
import timber.log.Timber
import java.io.File

class RecursiveFileObserver(
    private val path: String,
    private val eventHandler: (String, Int) -> Unit
) {
    private val observers = mutableListOf<FileObserver>()

    fun startWatching() {
        stopWatching() // 防止重复调用导致资源浪费
        addObservers(File(path))
    }

    fun stopWatching() {
        observers.forEach { it.stopWatching() }
        observers.clear()
    }

    private fun addObservers(file: File) {
        if (!file.exists()) return
        Timber.d("addObservers ${file.absolutePath}")
        // 为当前目录创建一个 FileObserver
        val observer = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            object : FileObserver(file.absolutePath, CREATE or DELETE) {
                override fun onEvent(event: Int, path: String?) {
                    if (path != null) {
                        eventHandler("$file/$path", event)
                    }
                }
            }
        } else {
            object : FileObserver(file, CREATE or DELETE) {
                override fun onEvent(event: Int, path: String?) {
                    if (path != null) {
                        eventHandler("$file/$path", event)
                    }
                }
            }
        }
        observer.startWatching()
        observers.add(observer)

        // 如果是目录，则递归添加子目录的观察者
        if (file.isDirectory) {
            file.listFiles()?.forEach { subFile ->
                if (subFile.name.startsWith(".")){
                    // 不监控
                    return@forEach
                }
                if (subFile.isDirectory) {
                    addObservers(subFile)
                }
            }
        }
    }
}
