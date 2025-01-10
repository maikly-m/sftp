package com.example.ftp.event

import com.example.ftp.service.ClientType
import java.io.File

sealed class ClientMessageEvent(val message: String) {
    class SftpConnected(val clientType: ClientType, message: String) : ClientMessageEvent(message)
    class SftpConnectFail(val clientType: ClientType, message: String) : ClientMessageEvent(message)
    class SftpDisconnect(val clientType: ClientType, message: String) : ClientMessageEvent(message)

    class UploadFileList(message: String, val list: List<File>, val currentPath: String) : ClientMessageEvent(message)
}

