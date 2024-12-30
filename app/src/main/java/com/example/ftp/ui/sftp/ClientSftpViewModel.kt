package com.example.ftp.ui.sftp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.service.SftpClientService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class ClientSftpViewModel : ViewModel() {

    private var sftpClient: SftpClientService? = null

    fun uploadFile(
        localFilePath: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            sftpClient?.getClient()?.uploadFile(localFilePath,  "/001/"+File(localFilePath).name)
        }

    }

    fun listFile(
        localFilePath: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            sftpClient?.getClient()?.listFiles(localFilePath)
        }

    }

    fun setClient(sftpClientService: SftpClientService): Unit {
        sftpClient = sftpClientService
    }
}