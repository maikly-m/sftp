package com.example.ftp.ui.home

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
import org.apache.ftpserver.DataConnectionConfigurationFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.ftplet.DefaultFtplet
import org.apache.ftpserver.ftplet.FtpException
import org.apache.ftpserver.ftplet.FtpRequest
import org.apache.ftpserver.ftplet.FtpSession
import org.apache.ftpserver.ftplet.FtpletContext
import org.apache.ftpserver.ftplet.FtpletResult
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter


class ServerViewModel : ViewModel() {
    private val logger: Logger = LoggerFactory.getLogger(ServerViewModel::class.java)


    private var server: FtpServer? = null

    fun getFtpServer(): FtpServer? {
        return server
    }

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

    init {

    }

    fun startFtpServer() {

        viewModelScope.launch(Dispatchers.IO) {

            // test
            // saveFileToSDCard("/storage/emulated/0/001")

            val serverFactory = FtpServerFactory()

            // 设置监听端口
            val factory = ListenerFactory()
            factory.port = 2121 // 设置端口

            // 配置被动模式端口范围
            val dataConfigFactory = DataConnectionConfigurationFactory()
            dataConfigFactory.passivePorts = "5000-20000" // 被动模式端口范围
            // 将数据连接配置应用到监听器
            factory.dataConnectionConfiguration = dataConfigFactory.createDataConnectionConfiguration()

            serverFactory.addListener("default", factory.createListener())

            // 注册 FileUploadFtplet
            val ftplet = FileUploadFtplet()
            serverFactory.ftplets = mapOf("uploadListener" to ftplet)

            // 创建用户
            val userManagerFactory = PropertiesUserManagerFactory()


            // 配置用户权限
            // 允许读写权限
            val authorities = mutableListOf<Authority>()
            authorities.add(WritePermission()) // 允许写入
            authorities.add(ConcurrentLoginPermission(10, 10)) // 多用户同时登陆

            userManagerFactory.passwordEncryptor = SaltedPasswordEncryptor()
            val userManager = userManagerFactory.createUserManager()
            val user = BaseUser()
            user.authorities = authorities
            user.name = "ftpuser"
            user.password = "12345"
            user.homeDirectory = "/storage/emulated/0/001" // 设置文件存储目录
            user.maxIdleTime = 60
            userManager.save(user)

            serverFactory.userManager = userManager

            // 启动服务器
            server = serverFactory.createServer()
            server!!.start()

            // 获取本机的 IP 地址（这里以 InetAddress.getLocalHost() 为例）
            val ipAddress = getLocalIpAddress(GetProvider.get().context)
            _text.postValue(ipAddress)

            Timber.e("startFtpServer ok")
        }

    }

    private fun saveFileToSDCard(path: String) {
        // 获取外部存储的路径（对于 Android 10+，使用 getExternalFilesDir）
        val file = File(path, "my_test_file.txt")
        // 检查目录是否存在，如果不存在则创建
        if (!file.exists()) {
            file.createNewFile()
        }
        // 写入数据
        val fileOutputStream = FileOutputStream(file)
        val writer = BufferedWriter(OutputStreamWriter(fileOutputStream))
        writer.write("Hello, this is a test file saved on SD card!")
        writer.close()
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

class FileUploadFtplet : DefaultFtplet() {

    override fun onUploadStart(session: FtpSession?, request: FtpRequest?): FtpletResult {
        Timber.d("onUploadStart")
        return super.onUploadStart(session, request)
    }

    override fun onUploadEnd(session: FtpSession?, request: FtpRequest?): FtpletResult {
        Timber.d("onUploadEnd")
        return super.onUploadEnd(session, request)
    }
    // 上传文件时调用
    @Throws(FtpException::class)
    override fun init(context: FtpletContext?) {
        // 初始化方法，如果需要时可以使用
    }

    @Throws(FtpException::class)
    override fun destroy() {
        // 销毁方法，如果需要时可以使用
    }
}