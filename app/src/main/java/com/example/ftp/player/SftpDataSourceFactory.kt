package com.example.ftp.player

import SftpDataSource
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import com.example.ftp.provider.GetProvider
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.InputStream

@UnstableApi
class SftpDataSourceFactory(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        return SftpDataSource(host, port, username, password)
    }
}
