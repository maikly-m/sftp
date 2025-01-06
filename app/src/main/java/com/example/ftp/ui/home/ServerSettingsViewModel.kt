package com.example.ftp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ServerSettingsViewModel : ViewModel() {

    var etIp: String? = null
    var etPort: String? = null
    var etName: String? = null
    var etPw: String? = null
    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text
}