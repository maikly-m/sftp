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

class SftpClientService : Service() {

    companion object{
        val MY_CHANNEL_ID = "file_transfer_client"
    }
    private val coroutineScope = CoroutineScope(Dispatchers.IO) // 在IO线程中运行协程


    override fun onCreate() {
        Timber.d("onCreate ")
        val channelName = "File Transfer Client"
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(MY_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager?.createNotificationChannel(channel)

        // 创建前台通知
        val notification: Notification = Notification.Builder(this, MY_CHANNEL_ID)
            .setContentTitle("File Transfer Client")
            .setContentText("Running in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        // 将服务设置为前台服务
        startForeground(10, notification)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand ok")
        return START_STICKY
    }

    private var model: SftpClientModel? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): SftpClientService = this@SftpClientService
    }

    override fun onBind(intent: Intent?): IBinder {
        Timber.d("onBind ok")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        model?.disconnect()
        Timber.d("onDestroy")
    }

    fun connect(serverIp: String, port: Int, user: String, password: String): Unit {
        coroutineScope.launch {
            model = SftpClientModel(ClientType.BaseClient)
            model!!.connect(serverIp, port, user, password)
        }
    }

    // 分离下载
    suspend fun download(serverIp: String, port: Int, user: String, password: String, block: suspend (model: SftpClientModel)->Unit): Unit {
        val downloadModel = SftpClientModel(ClientType.DownloadClient)
        downloadModel.connect(serverIp, port, user, password)
        block(downloadModel)
        downloadModel.disconnect()
        Timber.d("download finish")
    }

    // 分离上传
    suspend fun upload(serverIp: String, port: Int, user: String, password: String, block: suspend (model: SftpClientModel)->Unit): Unit {
        val uploadModel = SftpClientModel(ClientType.UploadClient)
        uploadModel.connect(serverIp, port, user, password)
        block(uploadModel)
        uploadModel.disconnect()
        Timber.d("upload finish")
    }

    suspend fun other(serverIp: String, port: Int, user: String, password: String, block: suspend (model: SftpClientModel)->Unit): Unit {
        val otherModel = SftpClientModel(ClientType.OtherClient)
        otherModel.connect(serverIp, port, user, password)
        block(otherModel)
        otherModel.disconnect()
        Timber.d("other finish")
    }

    fun getClient(): SftpClientModel? {
        return model
    }

}