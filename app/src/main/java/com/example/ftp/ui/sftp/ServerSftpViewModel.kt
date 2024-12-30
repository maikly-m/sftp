package com.example.ftp.ui.sftp

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.provider.GetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.sshd.common.BaseBuilder
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.common.util.OsUtils
import org.apache.sshd.server.ServerBuilder
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.subsystem.SubsystemFactory
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class ServerSftpViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

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
        // 设置服务器端口
        sshd.port = 2222

        // 设置服务器认证
        sshd.passwordAuthenticator = PasswordAuthenticator { username, password, session ->
            // 在这里验证用户名和密码
            username == "ftpuser" && password == "12345"
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sshd.start()

                // 获取本机的 IP 地址（这里以 InetAddress.getLocalHost() 为例）
                val ipAddress = getLocalIpAddress(GetProvider.get().context)
                _text.postValue(ipAddress)

                Timber.d("SFTP Server started on port ${sshd.port}")
            } catch (e: IOException) {
                e.printStackTrace()
            }
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


    fun getLocalIpAddress(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo: WifiInfo = wifiManager.connectionInfo
        val ipAddress = connectionInfo.ipAddress
        return intToIp(ipAddress)
    }

    // 将 int 类型的 IP 地址转换为字符串格式
    fun intToIp(i: Int): String {
        return (i and 0xFF).toString() + "." +
                ((i shr 8) and 0xFF) + "." +
                ((i shr 16) and 0xFF) + "." +
                (i shr 24 and 0xFF)
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