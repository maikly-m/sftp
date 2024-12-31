package com.example.ftp.event

sealed class ClientMessageEvent(val message: String) {
    class SftpConnected(message: String) : ClientMessageEvent(message)
    class SftpConnectFail(message: String) : ClientMessageEvent(message)
    class SftpDisconnect(message: String) : ClientMessageEvent(message)
}

