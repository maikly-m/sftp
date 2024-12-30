package com.example.ftp.ui.home

import android.widget.Toast
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class CommonNetFtp {
    suspend fun uploadFileByCommonNet(
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
            Timber.d("Connected to FTP server.")

            // 登录
            val success = ftpClient.login(username, password)
            if (success) {
                Timber.d("Logged in successfully.")
            } else {
                Timber.d("Failed to login.")
                return
            }

            // 设置为被动模式
            ftpClient.enterLocalPassiveMode()
            ftpClient.controlEncoding = "UTF-8"
            // 切换工作目录（可选）
            // ftpClient.changeWorkingDirectory("/")
            // delay(500)
            // 显示当前目录文件
            val files = ftpClient.listFiles()
            for (file in files) {
                Timber.d("File: ${file.name}")
            }
            //下载

//            // 要下载的远程文件路径
//            val remoteFilePath = files[files.size-1].name
//            // 本地保存的文件路径
//            val localFilePath = "/storage/emulated/0/001/_test_${files[files.size-1].name}"
//
//            FileOutputStream(localFilePath).use { outputStream ->
//                val success =
//                    ftpClient.retrieveFile(remoteFilePath, outputStream)
//                if (success) {
//                    Timber.d("File downloaded successfully!")
//                } else {
//                    Timber.d("Failed to download the file.")
//                }
//            }

            FileInputStream(localFilePath).use { inputStream ->
                val s = ftpClient.storeFile("text.txt", inputStream)
                if (s) {
                    Timber.d("File uploaded successfully.")
                } else {
                    Timber.d("Failed to upload file.")
                }
            }

            // 登出并断开连接
            ftpClient.logout()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (ftpClient.isConnected) {
                ftpClient.disconnect()
            }
        }

    }

}