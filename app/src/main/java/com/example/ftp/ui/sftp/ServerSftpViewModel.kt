package com.example.ftp.ui.sftp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ftp.utils.thread.SingleLiveEvent

class ServerSftpViewModel : ViewModel() {

    val _text = SingleLiveEvent<String>()
    val text: LiveData<String> = _text

}