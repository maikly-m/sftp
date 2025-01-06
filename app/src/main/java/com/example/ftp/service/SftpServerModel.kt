package com.example.ftp.service

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.text.TextUtils
import com.example.ftp.provider.GetProvider
import com.example.ftp.utils.MySPUtil
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.server.ServerBuilder
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.subsystem.SubsystemFactory
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.file.Paths

class SftpServerModel{

    private val sshd: SshServer

    init {

        // Properties
        val p = "${GetProvider.get().context.filesDir}/user/home"
        val f = File(p)
        if (!f.exists()){
            f.mkdirs()
        }
        System.setProperty("user.home", p)

        sshd = SshServerBuilder().build()

        val info = MySPUtil.getInstance().serverConnectInfo
        // 设置服务器端口
        sshd.port = if (info.port < 0) {
            2222
        } else {
            info.port
        }
        val userName = if (TextUtils.isEmpty(info.name)) {
            "ftpuser"
        } else {
            info.name
        }
        val userPw = if (TextUtils.isEmpty(info.pw)) {
            "12345"
        } else {
            info.pw
        }

        // 设置服务器认证
        sshd.passwordAuthenticator = PasswordAuthenticator { username, password, session ->
            // 在这里验证用户名和密码
            username == userName && password == userPw
        }

        // 设置 SSH 密钥生成器
        sshd.keyPairProvider = SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser"))

        // 设置 SFTP 子系统
        val system = mutableListOf<SubsystemFactory>()
        system.add(SftpSubsystemFactory())
        sshd.subsystemFactories?.run {
            for (i in this){
                system.add(i)
            }
        }
        sshd.subsystemFactories = system

    }

    // 启动服务器
    fun startServer() {
        try {
            sshd.start()
            Timber.d("SFTP Server started on port ${sshd.port}")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // 停止服务器
    fun stopServer() {
        try {
            sshd.stop()
            Timber.d("SFTP Server stopped")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
class SshServerBuilder : ServerBuilder() {

    // Your `build` method that creates the SSH server with all configurations
    override fun build(isFillWithDefaultValues: Boolean): SshServer {
        if (isFillWithDefaultValues) {
            fillWithDefaultValues()
        }

        val ssh = factory.create()

        // 设置文件系统工厂，使用虚拟文件系统
        val fileSystemFactory = VirtualFileSystemFactory(Paths.get("/storage/emulated/0"))
        // Configure the file system (use custom file system factory)
        ssh.fileSystemFactory = fileSystemFactory
        // Set other necessary configurations (e.g., key exchange, cipher settings)
        ssh.keyExchangeFactories = keyExchangeFactories
        ssh.signatureFactories = signatureFactories
        ssh.randomFactory = randomFactory
        ssh.cipherFactories = cipherFactories
        ssh.compressionFactories = compressionFactories
        ssh.macFactories = macFactories
        ssh.channelFactories = channelFactories
        ssh.forwardingFilter = forwardingFilter
        ssh.forwarderFactory = forwarderFactory
        ssh.globalRequestHandlers = globalRequestHandlers
        ssh.channelStreamWriterResolver = channelStreamPacketWriterResolver
        ssh.unknownChannelReferenceHandler = unknownChannelReferenceHandler

        return ssh
    }
}