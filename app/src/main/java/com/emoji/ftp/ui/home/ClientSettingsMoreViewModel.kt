package com.emoji.ftp.ui.home

import androidx.lifecycle.ViewModel
import com.emoji.ftp.utils.MySPUtil

class ClientSettingsMoreViewModel : ViewModel() {

    var etSavePath: String = "/"
    var etUploadPath: String = "/sftp"
    init {
        etSavePath = MySPUtil.getInstance().downloadSavePath
        etUploadPath = MySPUtil.getInstance().uploadSavePath
    }
}