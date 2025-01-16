package com.emoji.ftp.service


sealed class ClientType {
    data object BaseClient : ClientType()
    data object UploadClient : ClientType()
    data object DownloadClient : ClientType()
    data object OtherClient : ClientType()

    data object PlayerClient : ClientType()
}