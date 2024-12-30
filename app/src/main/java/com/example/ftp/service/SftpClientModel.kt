package com.example.ftp.service

import android.text.TextUtils
import com.example.ftp.event.MessageEvent
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger

class SftpClientModel {

    private var _password: String = ""
    private var _username: String = ""
    private var _ftpPort: Int = 2222
    private var _ftpServer: String = ""

    // private val coroutineScope = CoroutineScope(Dispatchers.IO) // 在IO线程中运行协程
    private var session: Session? = null
    private var channelSftp: ChannelSftp? = null
    private var initLockInt = AtomicInteger(0)

    fun connect(
        ftpServer: String,
        ftpPort: Int,
        username: String,
        password: String
    ) {
        Timber.d("SftpClientModel connect start")
        if (initLockInt.get() != 0){
            return
        }
        initLockInt.set(1)
        _ftpServer = ftpServer
        _ftpPort = ftpPort
        _username = username
        _password = password
        val jsch = JSch()
        session = jsch.getSession(username, ftpServer, ftpPort)
        // 设置密码
        session!!.setPassword(password)
        // 关闭严格的主机密钥检查（只用于测试环境）
        session!!.setConfig("StrictHostKeyChecking", "no")
        // 10s
        session!!.timeout = 10_000
        try {
            session!!.connect()
            channelSftp = session!!.openChannel("sftp") as ChannelSftp
            channelSftp!!.connect()
            Timber.d("SftpClientModel connect")
        } catch (e: Exception) {
            Timber.e("SftpClientModel connect error")
            e.printStackTrace()
            channelSftp?.disconnect()
            session?.disconnect()
            channelSftp = null
            session = null
            if (e is JSchException){
                e.cause?.message?.contains("java.net.SocketTimeoutException").let {
                    EventBus.getDefault().post(MessageEvent("连接异常"))
                }
            }
        }finally {
            initLockInt.set(0)
        }
    }

    fun disconnect() {
        channelSftp?.disconnect()
        session?.disconnect()
        channelSftp = null
        session = null
        initLockInt.set(0)
    }

    // 上传文件（增）
    fun uploadFile(localFilePath: String, remoteFilePath: String) {
        checkConnect {
            try {
                val fileInputStream = FileInputStream(localFilePath)
                channelSftp?.put(fileInputStream, remoteFilePath)
                fileInputStream.close()
                Timber.d("SFTP %s", "文件上传成功")
            } catch (e: SftpException) {
                e.printStackTrace()
                Timber.d("SFTP %s", "文件上传失败: ${e.message}")
            }
        }
    }

    private fun checkConnect(block: () -> Unit) {
        if (initLockInt.get() != 0){
            Timber.d("init ...")
            return
        }
        if (channelSftp == null) {
            // reconnect
            reconnect()
        } else if (channelSftp?.isConnected == false) {
            // reconnect
            reconnect()
        } else {
            //continue
        }
        block()
    }

    private fun reconnect() {
        if (!TextUtils.isEmpty(_username) && !TextUtils.isEmpty(_password)){
            Timber.d("reconnect ...")
            connect(_ftpServer, _ftpPort, _username, _password)
        }
    }

    // 删除文件（删）
    fun deleteFile(remoteFilePath: String) {
        checkConnect {
            try {
                channelSftp?.rm(remoteFilePath)
                Timber.d("SFTP %s", "文件删除成功")
            } catch (e: SftpException) {
                e.printStackTrace()
                Timber.d("SFTP %s", "文件删除失败: ${e.message}")
            }
        }
    }

    // 重命名文件（改）
    fun renameFile(oldFilePath: String, newFilePath: String) {
        checkConnect {
            try {
                channelSftp?.rename(oldFilePath, newFilePath)
                Timber.d("SFTP%s", "文件重命名成功")
            } catch (e: SftpException) {
                e.printStackTrace()
                Timber.d("SFTP%s", "文件重命名失败: ${e.message}")
            }
        }
    }

    // 列出目录内容（查）
    fun listFiles(remoteDirectory: String) {
        checkConnect {
            Timber.d("SFTP, listFiles")
            try {
                val fileList = channelSftp?.ls(remoteDirectory)
                fileList?.forEach { file ->
                    Timber.d("SFTP, 文件/目录: ${file}")
                }
            } catch (e: SftpException) {
                e.printStackTrace()
                Timber.d("SFTP, 列出目录失败: ${e.message}")
            }
        }
    }

}