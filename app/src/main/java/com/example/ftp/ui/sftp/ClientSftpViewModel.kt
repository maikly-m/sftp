package com.example.ftp.ui.sftp

import android.os.Environment
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.service.SftpClientService
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.ensureLocalDirectoryExists
import com.example.ftp.utils.getFileNameFromPath
import com.example.ftp.utils.normalizeFilePath
import com.example.ftp.utils.showToast
import com.example.ftp.utils.thread.SingleLiveEvent
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpProgressMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Vector


class ClientSftpViewModel : ViewModel() {

    private var currentPath: String = "/"
    private var lastCurrentPath: String = "/"

    val showMultiSelectIcon = SingleLiveEvent<Boolean>()
    val showSelectAll = SingleLiveEvent<Boolean>()
    val changeSelectType = SingleLiveEvent<Int>()

    private var uploadFileInputStreamJob: Job? = null
    private val _uploadFileInputStream = SingleLiveEvent<Int>()
    private val _uploadFileProgress = SingleLiveEvent<Float>()
    val uploadFileProgress: LiveData<Float> = _uploadFileProgress
    val uploadFileInputStream: LiveData<Int> = _uploadFileInputStream

    private var listFileJob: Job? = null
    private val _listFile = SingleLiveEvent<Int>()
    val listFile: LiveData<Int> = _listFile
    var listFileData: Vector<ChannelSftp.LsEntry>? = null
    private val _listFileLoading = SingleLiveEvent<Int>()
    val listFileLoading: LiveData<Int> = _listFileLoading

    private var downloadFileJob: Job? = null
    private val _downloadFile = SingleLiveEvent<Int>()
    val downloadFile: LiveData<Int> = _downloadFile
    private val _downloadFileProgress = SingleLiveEvent<Float>()
    val downloadFileProgress: LiveData<Float> = _downloadFileProgress

    private var deleteFileJob: Job? = null
    private val _deleteFile = SingleLiveEvent<Int>()
    val deleteFile: LiveData<Int> = _deleteFile

    private var renameFileJob: Job? = null
    private val _renameFile = SingleLiveEvent<Int>()
    val renameFile: LiveData<Int> = _renameFile

    private var mkdirJob: Job? = null
    private val _mkdir = SingleLiveEvent<Int>()
    val mkdir: LiveData<Int> = _mkdir

    fun getCurrentFilePath() = currentPath

    val sortTypes = mutableListOf(
        "按名称",
        "按类型",
        "按大小升序",
        "按大小降序",
        "按时间升序",
        "按时间降序",
    )

    init {
        changeSelectType.value = MySPUtil.getInstance().serverSortType
    }

    fun uploadFileInputStream(
        sftpClientService: SftpClientService?,
        inputStreams: MutableList<InputStream>,
        remoteFilePaths: MutableList<String>,
        allSize: Long,
        size: Int,
    ) {
        if (uploadFileInputStreamJob != null && uploadFileInputStreamJob?.isActive == true) {
            return
        }
        uploadFileInputStreamJob = viewModelScope.launch(Dispatchers.IO) {
            if (inputStreams.size == remoteFilePaths.size) {
                var uploadedBytes: Long = 0
                var lastUploadedBytes: Long = 0
                var totalBytes: Long = 0
                if (allSize > 0) {
                    totalBytes = allSize
                } else {

                }

                for (i in inputStreams.indices) {
                    val l = object : SftpProgressMonitor {
                        override fun init(op: Int, src: String?, dest: String?, max: Long) {
                            if (i == 0) {
                                Timber.d("Upload Start")
                            }
                        }

                        override fun count(count: Long): Boolean {
                            uploadedBytes += count
                            // 回传进度
                            if (totalBytes > 0) {
                                if ((uploadedBytes - lastUploadedBytes) > totalBytes / 1000 &&
                                    (uploadedBytes - lastUploadedBytes) > 1024 * 1024
                                ) {
                                    // 超过千分之一并且大小大于1M，就更新进度
                                    lastUploadedBytes = uploadedBytes
                                    _uploadFileProgress.postValue((uploadedBytes * 100 / totalBytes).toFloat())
                                }
                            }
                            return true // Return false to cancel the transfer
                        }

                        override fun end() {
                            // 文件大小拿不到的时候，按照文件占比来回传进度
                            if (totalBytes == 0L) {
                                _uploadFileProgress.postValue(100f / size * (i + 1))
                            }
                            if (i == inputStreams.size - 1) {
                                // 最后一个
                                _uploadFileProgress.postValue(100f)
                                Timber.d("Upload finished")
                            }

                        }
                    }
                    sftpClientService?.getClient()
                        ?.uploadFileInputStream(inputStreams[i], remoteFilePaths[i], l)
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

    private fun addLocalChildrenFile(
        file: File,
        selectParentPath: String,
        srcFilePath: MutableList<String>,
        dstFilePath: MutableList<String>,
        allSize: MutableList<Long>,
    ): Unit {
        if (!file.isDirectory) {
            return
        }
        val s = file.listFiles()
        s?.forEach { f ->
            if (f.isDirectory) {
                // 文件夹
                addLocalChildrenFile(
                    f,
                    selectParentPath,
                    srcFilePath,
                    dstFilePath,
                    allSize
                )
            } else if (f.isFile) {
                // 本地文件
                srcFilePath.add(f.absolutePath)
                val sdcard = Environment.getExternalStorageDirectory().absolutePath
                val absoluteSelectPath = if (selectParentPath.startsWith(sdcard)) {
                    selectParentPath
                } else {
                    sdcard.removeSuffix("/") + selectParentPath
                }
                val p = currentPath.removeSuffix("/") + "/" + f.absolutePath.removePrefix(
                    absoluteSelectPath
                )
                dstFilePath.add(normalizeFilePath(p))
                allSize[0] += f.length()
            }
        }
    }

    fun uploadLocalFiles(
        sftpClientService: SftpClientService?,
        selectParentPath: String,
        files: List<File>
    ) {
        if (uploadFileInputStreamJob != null && uploadFileInputStreamJob?.isActive == true) {
            return
        }
        uploadFileInputStreamJob = viewModelScope.launch(Dispatchers.IO) {
            val srcFilePath: MutableList<String> = mutableListOf()
            val dstFilePath: MutableList<String> = mutableListOf()
            val allSize = MutableList(1) { 0L }
            if (selectParentPath == "/") {
                // 根目录不用加
            } else {

            }
            files.forEach { f ->
                if (f.isDirectory) {
                    // 文件夹
                    addLocalChildrenFile(
                        f,
                        selectParentPath,
                        srcFilePath,
                        dstFilePath,
                        allSize
                    )
                } else if (f.isFile) {
                    // 本地文件
                    srcFilePath.add(f.absolutePath)
                    // 远程文件
                    val sdcard = Environment.getExternalStorageDirectory().absolutePath
                    val absoluteSelectPath = if (selectParentPath.startsWith(sdcard)) {
                        selectParentPath
                    } else {
                        sdcard.removeSuffix("/") + selectParentPath
                    }
                    val p = currentPath.removeSuffix("/") + "/" + f.absolutePath.removePrefix(
                        absoluteSelectPath
                    )
                    dstFilePath.add(normalizeFilePath(p))
                    allSize[0] += f.length()
                }
            }
            // 升序排序，先创建文件夹
            srcFilePath.sortBy {
                it.length
            }
            dstFilePath.sortBy {
                it.length
            }

            srcFilePath.forEach {
                Timber.d("uploadLocalFiles srcFilePath: ${it}")
            }
            dstFilePath.forEach {
                Timber.d("uploadLocalFiles dstFilePath: ${it}")
            }
            allSize.forEach {
                Timber.d("uploadLocalFiles allSize: ${it}")
            }

            if (srcFilePath.size == dstFilePath.size) {
                var uploadedBytes: Long = 0
                var lastUploadedBytes: Long = 0
                var totalBytes: Long = allSize[0]

                for (i in srcFilePath.indices) {
                    val l = object : SftpProgressMonitor {
                        override fun init(op: Int, src: String?, dest: String?, max: Long) {
                            if (i == 0) {
                                Timber.d("Upload Start")
                            }
                        }

                        override fun count(count: Long): Boolean {
                            uploadedBytes += count
                            // 回传进度
                            if (totalBytes > 0) {
                                if ((uploadedBytes - lastUploadedBytes) > totalBytes / 1000 &&
                                    (uploadedBytes - lastUploadedBytes) > 1024 * 1024
                                ) {
                                    // 超过千分之一并且大小大于1M，就更新进度
                                    lastUploadedBytes = uploadedBytes
                                    _uploadFileProgress.postValue((uploadedBytes * 100 / totalBytes).toFloat())
                                }
                            }
                            return true // Return false to cancel the transfer
                        }

                        override fun end() {
                            if (i == srcFilePath.size - 1) {
                                // 最后一个
                                _uploadFileProgress.postValue(100f)
                                Timber.d("Upload finished")
                            }

                        }
                    }
                    Timber.d("uploadLocalFiles srcFilePath[${i}]: ${srcFilePath[i]}")
                    sftpClientService?.getClient()
                        ?.uploadFileInputStream(FileInputStream(srcFilePath[i]), dstFilePath[i], l)
                }
            }

        }
        uploadFileInputStreamJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                Timber.d("uploadLocalFiles ok")
                _uploadFileInputStream.postValue(1)
            } else {
                Timber.d("uploadLocalFiles throwable = ${throwable.message}")
                _uploadFileInputStream.postValue(0)
            }
        }
    }

    fun listFile(
        sftpClientService: SftpClientService?,
        absolutePath: String,
    ) {
        if (listFileJob != null && listFileJob?.isActive == true) {
            return
        }
        lastCurrentPath = currentPath
        currentPath = absolutePath
        _listFileLoading.postValue(1)
        listFileJob = viewModelScope.launch(Dispatchers.IO) {
            // todo 可以切换到该目录下，或者不切换也行
            // sftpClientService?.getClient()?.cd(currentPath)
            val s = sftpClientService?.getClient()?.listFiles(currentPath)
            if (s != null && s.size > 0) {
                listFileData = Vector<ChannelSftp.LsEntry>()
                for (i in s) {
                    if (i is ChannelSftp.LsEntry) {
                        if (i.attrs.isDir && i.filename.equals(".")) {
                            // 当前目录
                            continue
                        }
                        if (i.attrs.isDir && i.filename.equals("..")) {
                            // 父目录
                            continue
                        }
                        listFileData?.add(i)
                    }
                }
            } else {
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
                if (throwable.message?.contains("Permission denied") == true) {
                    showToast("Permission denied")
                    // 复原
                    currentPath = lastCurrentPath
                }
                _listFile.postValue(0)
            }
            _listFileLoading.postValue(-1)
        }
    }

    private suspend fun addChildrenFile(
        sftpClientService: SftpClientService?,
        dirName: String,
        srcFilePath: MutableList<String>,
        dstFilePath: MutableList<String>,
        allSize: MutableList<Long>,
    ): Unit {
        val s = sftpClientService?.getClient()?.listFiles(dirName)
        if (s != null && s.size > 0) {
            for (i in s) {
                if (i is ChannelSftp.LsEntry) {
                    if (i.attrs.isDir && i.filename.equals(".")) {
                        // 当前目录
                        continue
                    }
                    if (i.attrs.isDir && i.filename.equals("..")) {
                        // 父目录
                        continue
                    }
                    if (i.attrs.isDir) {
                        addChildrenFile(
                            sftpClientService = sftpClientService,
                            dirName = dirName + "/" + i.filename,
                            srcFilePath = srcFilePath,
                            dstFilePath = dstFilePath,
                            allSize = allSize,
                        )
                    } else if (i.attrs.isReg) {
                        allSize[0] += i.attrs.size
                        srcFilePath.add(dirName + "/" + i.filename)
                        dstFilePath.add(dirName + "/" + i.filename)
                    }
                }
            }
        }
    }

    fun downloadFile(
        sftpClientService: SftpClientService?,
        files: List<ChannelSftp.LsEntry>,
    ) {
        if (downloadFileJob != null && downloadFileJob?.isActive == true) {
            return
        }
        downloadFileJob = viewModelScope.launch(Dispatchers.IO) {
            val srcFilePath: MutableList<String> = mutableListOf()
            val dstFilePath: MutableList<String> = mutableListOf()
            val allSize = MutableList(1) { 0L }
            if (currentPath == "/") {
                // 根目录不用加
            } else {
            }
            files.forEach {
                if (it.attrs.isReg) {
                    srcFilePath.add(currentPath.removeSuffix("/") + "/" + it.filename)
                    dstFilePath.add(currentPath.removeSuffix("/") + "/" + it.filename)
                    allSize[0] += it.attrs.size
                    // 文件
                } else if (it.attrs.isDir) {
                    // 文件夹
                    addChildrenFile(
                        sftpClientService,
                        currentPath.removeSuffix("/") + "/" + it.filename,
                        srcFilePath,
                        dstFilePath,
                        allSize
                    )
                }
            }
            // 升序排序，先创建文件夹
            srcFilePath.sortBy {
                it.length
            }
            dstFilePath.sortBy {
                it.length
            }

            srcFilePath.forEach {
                Timber.d("downloadFile srcFilePath: ${it}")
            }
            dstFilePath.forEach {
                Timber.d("downloadFile dstFilePath: ${it}")
            }
            allSize.forEach {
                Timber.d("downloadFile allSize: ${it}")
            }
            //todo 检测是否覆盖相同的文件

            if (srcFilePath.size == dstFilePath.size) {
                var uploadedBytes: Long = 0
                var lastUploadedBytes: Long = 0
                var totalBytes: Long = 0
                if (allSize[0] > 0) {
                    totalBytes = allSize[0]
                } else {

                }

                //按照配置下载到选定的目录
                val parentPaths = MySPUtil.getInstance().downloadSavePath
                // 所有文件都是基于sdcard创建的
                val sdcardPath = normalizeFilePath(Environment.getExternalStorageDirectory().absolutePath+"/"+ parentPaths)
                ensureLocalDirectoryExists(sdcardPath)
                Timber.d("downloadFile sdcardPath: ${sdcardPath}")
                for (i in srcFilePath.indices) {

                    val l = object : SftpProgressMonitor {
                        override fun init(op: Int, src: String?, dest: String?, max: Long) {
                            if (i == 0) {
                                Timber.d("Download Start")
                            }
                        }

                        override fun count(count: Long): Boolean {
                            uploadedBytes += count
                            // 回传进度
                            if (totalBytes > 0) {
                                if ((uploadedBytes - lastUploadedBytes) > totalBytes / 1000 &&
                                    (uploadedBytes - lastUploadedBytes) > 1024 * 1024
                                ) {
                                    // 超过千分之一并且大小大于1M，就更新进度
                                    lastUploadedBytes = uploadedBytes
                                    _downloadFileProgress.postValue((uploadedBytes * 100 / totalBytes).toFloat())
                                }
                            }
                            return true // Return false to cancel the transfer
                        }

                        override fun end() {
                            if (i == srcFilePath.size - 1) {
                                // 最后一个
                                _downloadFileProgress.postValue(100f)
                                Timber.d("Download finished")
                            }

                        }
                    }
                    dstFilePath[i] = "${sdcardPath}/${getFileNameFromPath(dstFilePath[i])}"
                    sftpClientService?.getClient()?.downloadFile(srcFilePath[i], normalizeFilePath(dstFilePath[i]), l)
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

    fun deleteFiles(
        sftpClientService: SftpClientService?,
        files: List<ChannelSftp.LsEntry>,
    ) {
        if (deleteFileJob != null && deleteFileJob?.isActive == true) {
            return
        }
        deleteFileJob = viewModelScope.launch(Dispatchers.IO) {
            files.forEach {
                if (it.attrs.isReg) {
                    sftpClientService?.getClient()
                        ?.deleteFile(currentPath.removeSuffix("/") + "/" + it.filename)
                } else if (it.attrs.isDir) {
                    sftpClientService?.getClient()
                        ?.deleteDir(currentPath.removeSuffix("/") + "/" + it.filename)
                }
            }
        }
        deleteFileJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                Timber.d("deleteFiles ok")
                _deleteFile.postValue(1)
            } else {
                Timber.d("deleteFiles throwable = ${throwable.message}")
                _deleteFile.postValue(0)
            }
        }
    }

    fun renameFile(
        sftpClientService: SftpClientService?,
        file: ChannelSftp.LsEntry,
        name: String
    ) {
        if (renameFileJob != null && renameFileJob?.isActive == true) {
            return
        }
        renameFileJob = viewModelScope.launch(Dispatchers.IO) {

            sftpClientService?.getClient()?.renameFile(
                normalizeFilePath(currentPath + "/" + file.filename),
                normalizeFilePath("$currentPath/$name")
            )
        }
        renameFileJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                Timber.d("renameFile ok")
                _renameFile.postValue(1)
            } else {
                Timber.d("renameFile throwable = ${throwable.message}")
                _renameFile.postValue(0)
            }
        }
    }

    fun mkdir(
        sftpClientService: SftpClientService?,
        name: String
    ) {
        if (mkdirJob != null && mkdirJob?.isActive == true) {
            return
        }
        mkdirJob = viewModelScope.launch(Dispatchers.IO) {
            sftpClientService?.getClient()?.mkdir(normalizeFilePath("$currentPath/$name"))
        }
        mkdirJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                Timber.d("renameFile ok")
                _mkdir.postValue(1)
            } else {
                Timber.d("renameFile throwable = ${throwable.message}")
                _mkdir.postValue(0)
            }
        }
    }
}