package com.emoji.ftp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.emoji.ftp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class SftpServerService : Service() {

    companion object{
        val MY_CHANNEL_ID = "file_transfer_server"
    }
    private val coroutineScope = CoroutineScope(Dispatchers.IO) // 在IO线程中运行协程


    override fun onCreate() {
        Timber.d("onCreate ")
        val channelName = "File Transfer Server"
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(MY_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager?.createNotificationChannel(channel)

        // 创建前台通知
        val notification: Notification = Notification.Builder(this, MY_CHANNEL_ID)
            .setContentTitle("File Transfer Server")
            .setContentText("Running in the background")
            .setSmallIcon(R.mipmap.app_sftp_icon)
            .build()

        // 将服务设置为前台服务
        startForeground(1, notification)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand ok")
        return START_STICKY
    }

    private var model: SftpServerModel? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): SftpServerService = this@SftpServerService
    }

    override fun onBind(intent: Intent?): IBinder {
        // 在后台执行任务
        coroutineScope.launch {
            model = SftpServerModel()
            model?.startServer()
        }
        Timber.d("onBind ok")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        model?.stopServer()
        Timber.d("onDestroy")
    }

}