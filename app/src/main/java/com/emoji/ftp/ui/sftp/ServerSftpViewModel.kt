package com.emoji.ftp.ui.sftp

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.emoji.ftp.utils.thread.SingleLiveEvent

class ServerSftpViewModel : ViewModel() {

    val _text = SingleLiveEvent<String>()
    val text: LiveData<String> = _text

}