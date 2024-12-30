package com.example.ftp.ui.sftp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.ui.home.CommonNetFtp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClientSftpViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

    fun uploadFile(
        ftpServer: String,
        ftpPort: Int,
        username: String,
        password: String,
        localFilePath: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            CommonNetSftp().uploadFileByCommonNet(
                ftpServer = ftpServer,
                ftpPort = ftpPort,
                username = username,
                password = password,
                localFilePath = localFilePath
            )
        }

    }
}