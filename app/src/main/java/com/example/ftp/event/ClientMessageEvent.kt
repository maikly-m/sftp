package com.example.ftp.event

import java.io.File

sealed class ClientMessageEvent(val message: String) {
    class SftpConnected(message: String) : ClientMessageEvent(message)
    class SftpConnectFail(message: String) : ClientMessageEvent(message)
    class SftpDisconnect(message: String) : ClientMessageEvent(message)

    class UploadFileList(message: String, val list: List<File>, val currentPath: String) : ClientMessageEvent(message)
}

