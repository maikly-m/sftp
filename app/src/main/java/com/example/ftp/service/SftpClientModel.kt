package com.example.ftp.service

import android.text.TextUtils
import com.example.ftp.event.ClientMessageEvent
import com.example.ftp.provider.GetProvider
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.SftpProgressMonitor
import com.jcraft.jsch.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Vector
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SftpClientModel {

    private var _password: String = ""
    private var _username: String = ""
    private var _ftpPort: Int = 2222
    private var _ftpServer: String = ""

    private val coroutineScope = CoroutineScope(Dispatchers.IO) // 在IO线程中运行协程
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
        if (initLockInt.get() != 0) {
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
        // 关闭严格的主机密钥检查（只用于测试环境）no
        // yes no ask
        // session!!.setConfig("StrictHostKeyChecking", "no")
        session!!.setConfig("StrictHostKeyChecking", "ask")
        // 设置用户交互信息处理
        session!!.userInfo = CustomUserInfo()
        // 创建 KnownHosts 实例，设置自定义路径
        val homeDir = "${GetProvider.get().context.filesDir}/user/home"
        val fileDir = File(homeDir)
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        }
        val known_hosts_path = "${homeDir}/known_hosts"
        val known_host_file = File(known_hosts_path)
        if (!known_host_file.exists() || known_host_file.isDirectory) {
            known_host_file.createNewFile()
        }
        jsch.setKnownHosts(known_hosts_path)

        // 10s
        session!!.timeout = 10_000
        try {
            session!!.connect()
            channelSftp = session!!.openChannel("sftp") as ChannelSftp
            channelSftp!!.connect()
            EventBus.getDefault().post(ClientMessageEvent.SftpConnected("连接成功"))
        } catch (e: Exception) {
            e.printStackTrace()
            channelSftp?.disconnect()
            session?.disconnect()
            channelSftp = null
            session = null
            if (e is JSchException) {
                e.cause?.message?.contains("java.net.SocketTimeoutException").let {
                    EventBus.getDefault().post(ClientMessageEvent.SftpConnectFail("连接异常"))
                }
                e.cause?.message?.contains("java.net.NoRouteToHostException").let {
                    EventBus.getDefault().post(ClientMessageEvent.SftpConnectFail("连接的IP异常"))
                }
            }
        } finally {
            initLockInt.set(0)
        }
    }

    fun disconnect() {
        channelSftp?.disconnect()
        session?.disconnect()
        channelSftp = null
        session = null
        initLockInt.set(0)
        EventBus.getDefault().post(ClientMessageEvent.SftpDisconnect("连接断开"))
    }

    private fun checkConnect(block: () -> Unit) {
        if (initLockInt.get() != 0) {
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
        if (!TextUtils.isEmpty(_username) && !TextUtils.isEmpty(_password)) {
            Timber.d("reconnect ...")
            connect(_ftpServer, _ftpPort, _username, _password)
        }
    }

    suspend fun pwd(): String {
        return suspendCoroutine { continuation ->
            try {
                checkConnect {
                    try {
                        val path = channelSftp?.pwd() ?: ""
                        Timber.d("pwd ${path}")
                        continuation.resume(path)
                    } catch (e: SftpException) {
                        e.printStackTrace()
                        continuation.resumeWithException(e)
                    }
                }
            } catch (e: Exception) {
                // 捕获异常并通过 continuation 恢复异常状态
                continuation.resumeWithException(e)
            }
        }
    }

    suspend fun cd(path: String) {
        suspendCoroutine { continuation ->
            try {
                checkConnect {
                    try {
                        channelSftp?.cd(path)
                        Timber.d("cd ${path}")
                        continuation.resume(path)
                    } catch (e: SftpException) {
                        e.printStackTrace()
                        continuation.resumeWithException(e)
                    }
                }
            } catch (e: Exception) {
                // 捕获异常并通过 continuation 恢复异常状态
                continuation.resumeWithException(e)
            }
        }
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

    // 上传文件（增）
    suspend fun uploadFileInputStream(
        inputStream: InputStream,
        remoteFilePath: String,
        l: SftpProgressMonitor
    ): Boolean {
        return suspendCoroutine { continuation ->
            try {
                checkConnect {
                    try {
                        channelSftp?.put(inputStream, remoteFilePath, l)
                        continuation.resume(true)
                    } catch (e: SftpException) {
                        e.printStackTrace()
                        continuation.resumeWithException(e)
                    } finally {
                        inputStream.close()
                    }
                }
            } catch (e: Exception) {
                // 捕获异常并通过 continuation 恢复异常状态
                continuation.resumeWithException(e)
            }
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
    suspend fun listFiles(remoteDirectory: String): Vector<*>? {
        return suspendCoroutine { continuation ->
            try {
                checkConnect {
                    Timber.d("SFTP, listFiles remoteDirectory=${remoteDirectory}")
                    try {
                        val list = channelSftp?.ls(remoteDirectory)
                        continuation.resume(list)
                    } catch (e: SftpException) {
                        e.printStackTrace()
                        continuation.resumeWithException(e)
                    }
                }
            } catch (e: Exception) {
                // 捕获异常并通过 continuation 恢复异常状态
                continuation.resumeWithException(e)
            }
        }

    }

    // 下载文件
    suspend fun downloadFile(
        src: String,
        dst: String,
        l: SftpProgressMonitor
    ): Boolean {
        return suspendCoroutine { continuation ->
            try {
                checkConnect {
                    try {
                        channelSftp?.get(src, dst, l)
                        continuation.resume(true)
                    } catch (e: SftpException) {
                        e.printStackTrace()
                        continuation.resumeWithException(e)
                    } finally {
                    }
                }
            } catch (e: Exception) {
                // 捕获异常并通过 continuation 恢复异常状态
                continuation.resumeWithException(e)
            }
        }
    }

    inner class CustomUserInfo : UserInfo {
        override fun getPassphrase(): String? {
            return null
        }

        override fun getPassword(): String? {
            return null
        }

        override fun promptPassword(message: String?): Boolean {
            return false
        }

        override fun promptPassphrase(message: String?): Boolean {
            return false
        }

        override fun promptYesNo(message: String?): Boolean {
            // 通过返回 true 来接受主机的公钥
            Timber.d("promptYesNo: ${message}")
            return true  // 允许接受公钥
        }

        override fun showMessage(message: String?) {
            // 用于显示消息
            Timber.d("showMessage: ${message}")
        }
    }
}