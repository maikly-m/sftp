package com.example.ftp.ui.sftp

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.service.SftpClientService
import com.example.ftp.utils.showToast
import com.example.ftp.utils.thread.SingleLiveEvent
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpProgressMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream
import java.util.Vector


class ClientSftpViewModel : ViewModel() {

    private var currentPath: String = "/"
    private var lastCurrentPath: String = "/"
    private var uploadFileInputStreamJob: Job? = null
    private var downloadFileJob: Job? = null
    private val _uploadFileInputStream = SingleLiveEvent<Int>()
    val uploadFileInputStream: LiveData<Int> = _uploadFileInputStream

    private var listFileJob: Job? = null
    private val _listFile = SingleLiveEvent<Int>()
    val listFile: LiveData<Int> = _listFile
    var listFileData: Vector<ChannelSftp.LsEntry>? = null

    private val _uploadFileProgress = SingleLiveEvent<Float>()
    val uploadFileProgress: LiveData<Float> = _uploadFileProgress

    private val _downloadFile = SingleLiveEvent<Int>()
    val downloadFile: LiveData<Int> = _downloadFile
    private val _downloadFileProgress = SingleLiveEvent<Float>()
    val downloadFileProgress: LiveData<Float> = _downloadFileProgress

    private var listFileLoading = false

    fun getCurrentFilePath() = currentPath

    fun uploadFileInputStream(
        sftpClientService: SftpClientService?,
        inputStreams: MutableList<InputStream>,
        remoteFilePaths: MutableList<String>,
        allSize: Long,
        size: Int,
    ) {
        if (uploadFileInputStreamJob != null && uploadFileInputStreamJob?.isActive == true){
            return
        }
        uploadFileInputStreamJob = viewModelScope.launch(Dispatchers.IO) {
            if (inputStreams.size == remoteFilePaths.size){
                var uploadedBytes: Long = 0
                var lastUploadedBytes: Long = 0
                var totalBytes: Long = 0
                if (allSize > 0){
                    totalBytes = allSize
                }else{

                }

                for (i in inputStreams.indices){
                    val l = object : SftpProgressMonitor {
                        override fun init(op: Int, src: String?, dest: String?, max: Long) {
                            if (i == 0){
                                Timber.d("Upload Start")
                            }
                        }

                        override fun count(count: Long): Boolean {
                            uploadedBytes += count
                            // 回传进度
                            if (totalBytes > 0){
                                if ((uploadedBytes - lastUploadedBytes) > totalBytes/1000 &&
                                    (uploadedBytes - lastUploadedBytes) > 1024*1024){
                                    // 超过千分之一并且大小大于1M，就更新进度
                                    lastUploadedBytes = uploadedBytes
                                    _uploadFileProgress.postValue((uploadedBytes*100/totalBytes).toFloat())
                                }
                            }
                            return true // Return false to cancel the transfer
                        }

                        override fun end() {
                            // 文件大小拿不到的时候，按照文件占比来回传进度
                            if (totalBytes == 0L){
                                _uploadFileProgress.postValue(100f/size * (i+1))
                            }
                            if (i == inputStreams.size-1){
                                // 最后一个
                                _uploadFileProgress.postValue(100f)
                                Timber.d("Upload finished")
                            }

                        }
                    }
                    sftpClientService?.getClient()?.uploadFileInputStream(inputStreams[i], remoteFilePaths[i], l)
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
        if (listFileLoading){
            Timber.d("listFile loading..")
            return
        }
        listFileLoading = true
        lastCurrentPath = currentPath
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
                if (throwable.message?.contains("Permission denied") == true){
                    showToast("Permission denied")
                    // 复原
                    currentPath = lastCurrentPath
                }
                _listFile.postValue(0)
            }
            listFileLoading = false
        }
    }

    suspend fun listFile4Check(
        sftpClientService: SftpClientService?,
        dir: String,
    ) : Vector<ChannelSftp.LsEntry>{
        val listFileData = Vector<ChannelSftp.LsEntry>()
        val s = sftpClientService?.getClient()?.listFiles(dir)
        if (s != null && s.size > 0){
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
                    listFileData.add(i)
                }
            }
        }else{

        }
        return listFileData
    }

    fun downloadFile(
        sftpClientService: SftpClientService?,
        srcFilePath: MutableList<String>,
        dstFilePath: MutableList<String>,
        allSize: Long,
        size: Int
    ) {
        if (downloadFileJob != null && downloadFileJob?.isActive == true){
            return
        }
        downloadFileJob = viewModelScope.launch(Dispatchers.IO) {
            if (srcFilePath.size == dstFilePath.size){
                var uploadedBytes: Long = 0
                var lastUploadedBytes: Long = 0
                var totalBytes: Long = 0
                if (allSize > 0){
                    totalBytes = allSize
                }else{

                }

                for (i in srcFilePath.indices){
                    val l = object : SftpProgressMonitor {
                        override fun init(op: Int, src: String?, dest: String?, max: Long) {
                            if (i == 0){
                                Timber.d("Download Start")
                            }
                        }

                        override fun count(count: Long): Boolean {
                            uploadedBytes += count
                            // 回传进度
                            if (totalBytes > 0){
                                if ((uploadedBytes - lastUploadedBytes) > totalBytes/1000 &&
                                    (uploadedBytes - lastUploadedBytes) > 1024*1024){
                                    // 超过千分之一并且大小大于1M，就更新进度
                                    lastUploadedBytes = uploadedBytes
                                    _downloadFileProgress.postValue((uploadedBytes*100/totalBytes).toFloat())
                                }
                            }
                            return true // Return false to cancel the transfer
                        }

                        override fun end() {
                            // 文件大小拿不到的时候，按照文件占比来回传进度
                            if (totalBytes == 0L){
                                _downloadFileProgress.postValue(100f/size * (i+1))
                            }
                            if (i == srcFilePath.size-1){
                                // 最后一个
                                _downloadFileProgress.postValue(100f)
                                Timber.d("Download finished")
                            }

                        }
                    }
                    sftpClientService?.getClient()?.downloadFile(srcFilePath[i], dstFilePath[i], l)
                }
            }
        }
        downloadFileJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                Timber.d("downloadFile ok")
                _downloadFile.postValue(1)
            } else {
                Timber.d("downloadFile throwable = ${throwable.message}")
                _downloadFile.postValue(0)
            }
        }
    }
}