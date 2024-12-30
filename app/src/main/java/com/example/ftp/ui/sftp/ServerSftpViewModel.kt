package com.example.ftp.ui.sftp

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.provider.GetProvider
import com.example.ftp.service.SshServerBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.sshd.common.BaseBuilder
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.common.util.OsUtils
import org.apache.sshd.server.ServerBuilder
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.subsystem.SubsystemFactory
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class ServerSftpViewModel : ViewModel() {

    val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

}