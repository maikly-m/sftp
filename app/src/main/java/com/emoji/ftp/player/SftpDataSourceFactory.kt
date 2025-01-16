package com.emoji.ftp.player

import SftpDataSource
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource

@UnstableApi
class SftpDataSourceFactory(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        // 这里可以根据协议来实现
        return SftpDataSource(host, port, username, password)
    }
}
