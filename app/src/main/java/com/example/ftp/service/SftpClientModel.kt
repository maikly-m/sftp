package com.example.ftp.service

import android.text.TextUtils
import com.example.ftp.R
import com.example.ftp.event.ClientMessageEvent
import com.example.ftp.provider.GetProvider
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.SftpProgressMonitor
import okio.use
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Vector
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SftpClientModel(val type: ClientType) {

    private var _password: String = ""
    private var _username: String = ""
    private var _ftpPort: Int = 2222
    private var _ftpServer: String = ""

    private var session: Session? = null
    private var channelSftp: ChannelSftp? = null
    private var initLockInt = AtomicInteger(0)
    private val lock = Object()

    fun isConnected() = channelSftp?.isConnected ?: false
    fun isConnecting() = initLockInt.get() == 1

    fun connect(
        ftpServer: String,
        ftpPort: Int,
        username: String,
        password: String,
        reconnect: Boolean = false
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
            if (!reconnect) {
                EventBus.getDefault().post(
                    ClientMessageEvent.SftpConnected(
                        type, GetProvider.get().context.getString(
                            R.string.text_connect_success
                        )
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            channelSftp?.disconnect()
            session?.disconnect()
            channelSftp = null
            session = null
            if (e is JSchException) {
                if (e.cause?.message?.contains("java.net.SocketTimeoutException") == true) {
                    EventBus.getDefault().post(
                        ClientMessageEvent.SftpConnectFail(
                            type,
                            GetProvider.get().context.getString(
                                R.string.text_connect_fail
                            )
                        )
                    )
                } else if (e.cause?.message?.contains("java.net.NoRouteToHostException") == true) {
                    EventBus.getDefault().post(
                        ClientMessageEvent.SftpConnectFail(
                            type,
                            GetProvider.get().context.getString(
                                R.string.text_connect_ip_fail
                            )
                        )
                    )
                } else {
                    EventBus.getDefault().post(ClientMessageEvent.SftpConnectFail(type,
                        GetProvider.get().context.getString(
                        R.string.text_connect_fail
                    )))
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
        EventBus.getDefault().post(
            ClientMessageEvent.SftpDisconnect(
                type,
                GetProvider.get().context.getString(R.string.text_disconnect)
            )
        )
    }

    private fun checkConnect(block: () -> Unit) {
        synchronized(lock) {
            if (initLockInt.get() != 0) {
                Timber.d("init ...")
                block()
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
    }

    private fun reconnect() {
        if (!TextUtils.isEmpty(_username) && !TextUtils.isEmpty(_password)) {
            Timber.d("reconnect ...")
            connect(_ftpServer, _ftpPort, _username, _password, true)
        }
    }

    suspend fun mkdir(path: String): Unit {
        suspendCoroutine { continuation ->
            try {
                checkConnect {
                    try {
                        channelSftp?.mkdir(path)
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
    suspend fun uploadFileInputStream(
        inputStream: InputStream,
        remoteFilePath: String,
        l: SftpProgressMonitor
    ): Boolean {
        return suspendCoroutine { continuation ->
            try {
                checkConnect {
                    try {
                        // check dir
                        Timber.d("uploadFileInputStream dst=${remoteFilePath}")
                        channelSftp?.let {
                            ensureDirectoryExists(
                                it,
                                remoteFilePath.removeSuffix("/").substringBeforeLast("/")
                            )
                        }
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

    fun deleteFile(remotePath: String) {
        checkConnect {
            try {
                channelSftp?.rm(remotePath)
                Timber.d("已删除文件: $remotePath")
            } catch (e: SftpException) {
                e.printStackTrace()
                Timber.d("SFTP %s", "文件删除失败: ${e.message}")
            }
        }
    }

    // 删除文件夹（删）
    fun deleteDir(remotePath: String) {
        checkConnect {
            try {
                val entries = channelSftp?.ls(remotePath)
                entries?.let {
                    for (entry in entries) {
                        if (entry is ChannelSftp.LsEntry) {
                            val filePath = "$remotePath/${entry.filename}"
                            if (entry.filename == "." || entry.filename == "..") {
                                continue // 跳过当前目录和父目录
                            }
                            if (entry.attrs.isDir) {
                                // 递归删除子目录
                                deleteDir(filePath)
                            } else {
                                // 删除文件
                                channelSftp?.rm(filePath)
                                Timber.d("已删除文件: $filePath")
                            }
                        }
                    }
                }
                // 删除当前空目录
                channelSftp?.rmdir(remotePath)
                Timber.d("已删除目录: $remotePath")
            } catch (e: SftpException) {
                e.printStackTrace()
                Timber.d("SFTP %s", "文件夹删除失败: ${e.message}")
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
                        Timber.d("downloadFile dst=${dst}")
                        val file = File(dst)
                        // 检查父目录是否存在，不存在则创建
                        if (file.parentFile != null) {
                            if (!file.parentFile!!.exists()) {
                                val isDirCreated = file.parentFile!!.mkdirs()
                                if (!isDirCreated) {
                                    // 无法创建
                                    continuation.resumeWithException(Throwable("无法创建父目录 ${dst}"))
                                    return@checkConnect
                                } else {
                                    // 创建文件
                                    if (!file.exists()) {
                                        val isFileCreated = file.createNewFile()
                                        if (!isFileCreated) {
                                            // 无法创建
                                            continuation.resumeWithException(Throwable("无法创建文件 ${dst}"))
                                            return@checkConnect
                                        }
                                    }
                                }
                            } else {
                                // 创建文件
                                if (!file.exists()) {
                                    val isFileCreated = file.createNewFile()
                                    if (!isFileCreated) {
                                        // 无法创建
                                        continuation.resumeWithException(Throwable("无法创建文件 ${dst}"))
                                        return@checkConnect
                                    }
                                }
                            }
                            FileOutputStream(file).use {
                                channelSftp?.get(src, it, l)
                                continuation.resume(true)
                            }

                        } else {
                            continuation.resumeWithException(Throwable("父目录不存在 ${dst}"))
                        }
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

    // 下载文件
    suspend fun downloadFile(
        src: String,
        dst: OutputStream,
        l: SftpProgressMonitor
    ): Boolean {
        return suspendCoroutine { continuation ->
            try {
                checkConnect {
                    try {
                        Timber.d("downloadFile OutputStream")
                        channelSftp?.get(src, dst, l)
                        continuation.resume(true)
                    } catch (e: SftpException) {
                        e.printStackTrace()
                        continuation.resumeWithException(e)
                    } finally {
                        dst.close()
                    }
                }
            } catch (e: Exception) {
                // 捕获异常并通过 continuation 恢复异常状态
                continuation.resumeWithException(e)
            }
        }
    }

    private fun ensureDirectoryExists(channelSftp: ChannelSftp, remotePath: String) {
        val directories = remotePath.split("/").filter { it.isNotEmpty() } // 按斜杠分割路径并过滤空元素
        var currentPath = ""

        for (dir in directories) {
            currentPath = if (currentPath.isEmpty()) "/$dir" else "$currentPath/$dir"

            try {
                // 检查目录是否存在
                val attrs = channelSftp.lstat(currentPath)
                if (!attrs.isDir) {
                    throw SftpException(ChannelSftp.SSH_FX_FAILURE, "$currentPath 存在但不是文件夹")
                }
            } catch (e: SftpException) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    // 当前目录不存在，创建
                    channelSftp.mkdir(currentPath)
                    Timber.d("目录创建成功: $currentPath")
                } else {
                    // 处理其他异常
                    throw e
                }
            }
        }
    }

}