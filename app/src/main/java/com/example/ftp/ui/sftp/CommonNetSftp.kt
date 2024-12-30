package com.example.ftp.ui.sftp

import android.util.Log
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import timber.log.Timber
import java.io.FileInputStream


class CommonNetSftp {

    suspend fun uploadFileByCommonNet(
        ftpServer: String,
        ftpPort: Int,
        username: String,
        password: String,
        localFilePath: String,
    ) {
        Timber.d("uploadFileByCommonNet ..")
        val jsch = JSch()
        val session: Session = jsch.getSession(username, ftpServer, ftpPort)
        // 设置密码
        session.setPassword(password)
        // 关闭严格的主机密钥检查（只用于测试环境）
        session.setConfig("StrictHostKeyChecking", "no")

        try {
            session.connect()
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            //listFiles(channel, "/001")
            uploadFile(channel, localFilePath, "test.txt")
            // 断开连接
            channel.disconnect()
            session.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 上传文件（增）
    fun uploadFile(channelSftp: ChannelSftp, localFilePath: String, remoteFilePath: String) {
        try {
            val fileInputStream = FileInputStream(localFilePath)
            channelSftp.put(fileInputStream, remoteFilePath)
            fileInputStream.close()
            Timber.d("SFTP %s", "文件上传成功")
        } catch (e: SftpException) {
            e.printStackTrace()
            Timber.d("SFTP %s", "文件上传失败: ${e.message}")
        }
    }

    // 删除文件（删）
    fun deleteFile(channelSftp: ChannelSftp,remoteFilePath: String) {
        try {
            channelSftp.rm(remoteFilePath)
            Timber.d("SFTP %s", "文件删除成功")
        } catch (e: SftpException) {
            e.printStackTrace()
            Timber.d("SFTP %s", "文件删除失败: ${e.message}")
        }
    }

    // 重命名文件（改）
    fun renameFile(channelSftp: ChannelSftp,oldFilePath: String, newFilePath: String) {
        try {
            channelSftp.rename(oldFilePath, newFilePath)
            Timber.d("SFTP%s", "文件重命名成功")
        } catch (e: SftpException) {
            e.printStackTrace()
            Timber.d("SFTP%s", "文件重命名失败: ${e.message}")
        }
    }

    // 列出目录内容（查）
    fun listFiles(channelSftp: ChannelSftp, remoteDirectory: String) {
        try {
            val fileList = channelSftp.ls(remoteDirectory)
            fileList?.forEach { file ->
                Timber.d("SFTP, 文件/目录: ${file.toString()}")
            }
        } catch (e: SftpException) {
            e.printStackTrace()
            Timber.d("SFTP, 列出目录失败: ${e.message}")
        }
    }

}