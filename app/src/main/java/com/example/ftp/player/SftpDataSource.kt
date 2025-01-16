import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.example.ftp.R
import com.example.ftp.provider.GetProvider
import com.example.ftp.service.CustomUserInfo
import com.example.ftp.utils.ToastUtil
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import timber.log.Timber
import java.io.File
import java.io.InputStream

@UnstableApi
class SftpDataSource(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String
) : BaseDataSource(true) {

    private var session: Session? = null
    private var channelSftp: ChannelSftp? = null
    private var inputStream: InputStream? = null
    private var opened = false
    private var totalBytes: Long = -1

    override fun open(dataSpec: DataSpec): Long {
        Timber.d("open dataSpec.length ${dataSpec.length}, dataSpec.position ${dataSpec.position}")
        try {
            // 如果已经打开，则直接返回
            if (opened) return if (dataSpec.length == -1L) totalBytes - dataSpec.position else dataSpec.length

            // 初始化 SFTP 连接
            if (session == null || channelSftp == null || !session!!.isConnected || !channelSftp!!.isConnected) {
                connect()
            }

            // 获取文件路径
            val filePath = dataSpec.uri.path ?: throw IllegalArgumentException("Invalid file path")

            Timber.d("filePath ${filePath}")
            // 获取文件长度（仅在首次调用时获取）
            if (totalBytes == -1L) {
                val stat = channelSftp!!.stat(filePath)
                totalBytes = stat.size
            }
            //Timber.d("totalBytes ${totalBytes}")
            //Timber.d("dataSpec.position ${dataSpec.position}")
            // 打开 InputStream
            inputStream = channelSftp!!.get(filePath)
            inputStream!!.skip(dataSpec.position) // 从指定位置开始读取

            // 设置状态为已打开
            opened = true

            // 返回需要读取的长度
            return if (dataSpec.length == -1L) totalBytes - dataSpec.position else dataSpec.length
        } catch (e: Exception) {
            throw RuntimeException("Error opening SFTP data source", e)
        }
    }

    override fun getUri(): Uri? {
        return null
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (inputStream == null) throw IllegalStateException("DataSource not opened")
        return try {
            //Timber.d("read offset ${offset}")
            //Timber.d("read readLength ${readLength}")
            val i = inputStream!!.read(buffer, offset, readLength)
            //Timber.d("read i ${i}")
            i
        } catch (e: Exception) {
            throw RuntimeException("Error reading SFTP data source", e)
        }
    }

    override fun close() {
        Timber.d("close ..")
        if (!opened) return
        try {
            disconnect()
            inputStream?.close()
        } catch (e: Exception) {
            // Ignore errors during close
        } finally {
            inputStream = null
            opened = false
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
            ToastUtil.tempSftpPlayerErrorToast =
                GetProvider.get().context.getString(R.string.text_fail_to_connect_server)
            // EventBus.getDefault().post(ClientMessageEvent.SftpConnectFail(ClientType.PlayerClient, ""))
            throw RuntimeException("Error connecting to SFTP server", e)
        }
    }
    private fun disconnect() {
        try {
            session?.disconnect()
            channelSftp?.disconnect()
        } catch (e: Exception) {
            ToastUtil.tempSftpPlayerErrorToast = GetProvider.get().context.getString(R.string.text_disconnect_server)
            // EventBus.getDefault().post(ClientMessageEvent.SftpDisconnect(ClientType.PlayerClient, ""))
            throw RuntimeException("Error disconnect to SFTP server", e)
        }
    }
}
