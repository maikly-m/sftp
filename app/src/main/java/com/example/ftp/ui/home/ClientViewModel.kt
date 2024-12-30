package com.example.ftp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClientViewModel : ViewModel() {

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
            CommonNetFtp().uploadFileByCommonNet(
                ftpServer = ftpServer,
                ftpPort = ftpPort,
                username = username,
                password = password,
                localFilePath = localFilePath
            )
        }

    }


}