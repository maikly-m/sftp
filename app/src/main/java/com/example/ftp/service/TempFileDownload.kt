package com.example.ftp.service

import com.example.ftp.provider.GetProvider
import com.example.ftp.utils.ToastUtil
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.SftpProgressMonitor
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class TempFileDownload( private val host: String,
                        private val port: Int,
                        private val username: String,
                        private val password: String) {

    private var session: Session? = null
    private var channelSftp: ChannelSftp? = null

    // 下载文件
    suspend fun downloadFile(
        src: String,
        dst: OutputStream,
        l: SftpProgressMonitor
    ): Boolean {
        return suspendCoroutine { continuation ->
            try {
                try {
                    connect()
                    channelSftp?.get(src, dst, l)
                    disconnect()
                    continuation.resume(true)
                } catch (e: SftpException) {
                    e.printStackTrace()
                    continuation.resumeWithException(e)
                } finally {
                    dst.close()
                }
            } catch (e: Exception) {
                // 捕获异常并通过 continuation 恢复异常状态
                continuation.resumeWithException(e)
            }
        }
    }

    private fun connect() {
        try {
            // 设置 SFTP 连接
            val jsch = JSch()
            session = jsch.getSession(username, host, port)
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

            session!!.connect()

            val channel = session!!.openChannel("sftp") as ChannelSftp
            channel.connect()
            channelSftp = channel
        } catch (e: Exception) {
            ToastUtil.tempSftpPlayerErrorToast = "连接服务器失败"
            // EventBus.getDefault().post(ClientMessageEvent.SftpConnectFail(ClientType.PlayerClient, ""))
            throw RuntimeException("Error connecting to SFTP server", e)
        }
    }

    private fun disconnect() {
        try {
            session?.disconnect()
            channelSftp?.disconnect()
        } catch (e: Exception) {
            ToastUtil.tempSftpPlayerErrorToast = "服务器连接断开"
            // EventBus.getDefault().post(ClientMessageEvent.SftpDisconnect(ClientType.PlayerClient, ""))
            throw RuntimeException("Error disconnect to SFTP server", e)
        }
    }
}