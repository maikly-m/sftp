package com.example.ftp.ui.sftp

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.service.SftpClientService
import com.example.ftp.utils.thread.SingleLiveEvent
import com.jcraft.jsch.ChannelSftp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.ArrayList
import java.util.Vector


class ClientSftpViewModel : ViewModel() {

    private var currentPath: String = "/"
    private var uploadFileInputStreamJob: Job? = null
    private val _uploadFileInputStream = SingleLiveEvent<Int>()
    val uploadFileInputStream: LiveData<Int> = _uploadFileInputStream

    private var listFileJob: Job? = null
    private val _listFile = SingleLiveEvent<Int>()
    val listFile: LiveData<Int> = _listFile
    var listFileData: Vector<ChannelSftp.LsEntry>? = null

    fun getCurrentFilePath() = currentPath

    fun uploadFile(
        sftpClientService: SftpClientService?,
        localFilePath: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            sftpClientService?.getClient()?.uploadFile(localFilePath,  "/001/"+File(localFilePath).name)
        }

    }

    fun uploadFileInputStream(
        sftpClientService: SftpClientService?,
        inputStreams: MutableList<InputStream>,
        remoteFilePaths: MutableList<String>,
    ) {
        if (uploadFileInputStreamJob != null && uploadFileInputStreamJob?.isActive == true){
            return
        }
        uploadFileInputStreamJob = viewModelScope.launch(Dispatchers.IO) {
            if (inputStreams.size == remoteFilePaths.size){
                for (i in inputStreams.indices){
                    Timber.d("uploadFileInputStream remoteFilePaths[${i}] =${remoteFilePaths[i]}")
                    sftpClientService?.getClient()?.uploadFileInputStream(inputStreams[i], remoteFilePaths[i])
                }
            }
        }
        uploadFileInputStreamJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                Timber.d("uploadFileInputStream ok")
                _uploadFileInputStream.postValue(1)
            } else {
                Timber.d("uploadFileInputStream throwable = ${throwable.message}")
                _uploadFileInputStream.postValue(0)
            }
        }
    }

    fun listFile(
        sftpClientService: SftpClientService?,
        absolutePath: String,
    ) {
        currentPath = absolutePath
        if (listFileJob != null && listFileJob?.isActive == true){
            return
        }
        listFileJob = viewModelScope.launch(Dispatchers.IO) {
            // todo 可以切换到该目录下，或者不切换也行
            // sftpClientService?.getClient()?.cd(currentPath)
            val s = sftpClientService?.getClient()?.listFiles(currentPath)
            if (s != null && s.size > 0){
                listFileData = Vector<ChannelSftp.LsEntry>()
                for (i in s){
                    if (i is ChannelSftp.LsEntry){
                        if (i.attrs.isDir && i.filename.equals(".")){
                            // 当前目录
                            continue
                        }
                        if (i.attrs.isDir && i.filename.equals("..")){
                            // 父目录
                            continue
                        }
                        listFileData?.add(i)
                    }
                }
            }else{
                listFileData = null
            }
        }
        listFileJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                Timber.d("listFileJob ok")
                _listFile.postValue(1)
            } else {
                listFileData = null
                Timber.d("listFileJob throwable = ${throwable.message}")
                _listFile.postValue(0)
            }
        }
    }
}