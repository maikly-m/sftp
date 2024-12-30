package com.example.ftp.ui.home

import it.sauronsoftware.ftp4j.FTPClient
import it.sauronsoftware.ftp4j.FTPCommunicationListener
import it.sauronsoftware.ftp4j.FTPDataTransferListener
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.io.File

class Ftp4J {
    fun uploadFileByFtp4J(
        ftpServer: String,
        ftpPort: Int,
        username: String,
        password: String,
        localFilePath: String,
    ) {
        val ftpClient = FTPClient()
        try {
            // 连接到 FTP 服务器
            ftpClient.connect(ftpServer, ftpPort)

            // 登录 FTP 服务器
            ftpClient.login(username, password)

            // 设置被动模式
            ftpClient.isPassive = true
            ftpClient.charset = "UTF-8"

            // 设置文件传输的监听器（可选，提供传输进度）
            ftpClient.type = FTPClient.TYPE_BINARY
            val dataTransferListener = object : FTPDataTransferListener {
                override fun started() {
                    Timber.d("started")
                }

                override fun transferred(bytes: Int) {
                    // 这里可以更新上传进度
                    Timber.d("Uploaded $bytes bytes")
                }

                override fun completed() {
                    Timber.d("Upload complete.")
                }

                override fun aborted() {
                    Timber.d("Upload aborted.")
                }

                override fun failed() {
                    Timber.d("Upload failed.")
                }
            }
            ftpClient.addCommunicationListener(object : FTPCommunicationListener {
                override fun sent(p0: String?) {
                    Timber.d("send: ${p0}.")
                }

                override fun received(p0: String?) {
                    Timber.d("received: ${p0}.")
                }

            })

//                ftpClient.list().forEach {
//                    Timber.d("ftpClient it.name=${it.name}")
//                    Timber.d("ftpClient it.size=${it.size}")
//                    Timber.d("ftpClient it.type=${it.type}")
//                }
            Timber.e("ftpClient -----------------")
            // 上传文件
            val localFile = File(localFilePath)
            // restartAt 断点续传的位置
            ftpClient.upload(localFile, 0, dataTransferListener)
            // 关闭 FTP 客户端连接
            Timber.d("logout")
            ftpClient.logout()
            ftpClient.disconnect(false)

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                ftpClient.logout()
                if (ftpClient.isConnected){
                    ftpClient.disconnect(false)
                }
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        }

    }
}