package com.example.ftp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ftp.utils.MySPUtil

class ClientSettingsMoreViewModel : ViewModel() {

    var etSavePath: String = "/"
    init {
        etSavePath = MySPUtil.getInstance().downloadSavePath
    }
}