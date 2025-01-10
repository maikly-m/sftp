package com.example.ftp.ui.sftp

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.bean.ConnectInfo
import com.example.ftp.bean.UploadInfo
import com.example.ftp.service.SftpClientService
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.delFile
import com.example.ftp.utils.ensureLocalDirectoryExists
import com.example.ftp.utils.getFileNameFromPath
import com.example.ftp.utils.normalizeFilePath
import com.example.ftp.utils.showToast
import com.example.ftp.utils.thread.SingleLiveEvent
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpProgressMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.Vector
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * sftp 不是线程安全的，不能同时下载，上传，或者list等操作
 * 需要同步操作，需要多个实例
 */
class ClientSftpViewModel : ViewModel() {

    private var connectInfo: ConnectInfo? = null

    // 创建自定义的 IO Dispatcher
    val customIODispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    private var currentPath: String = "/"
    private var lastCurrentPath: String = "/"

    val showMultiSelectIcon = SingleLiveEvent<Boolean>()
    val showSelectAll = SingleLiveEvent<Boolean>()
    val changeSelectType = SingleLiveEvent<Int>()

    private var uploadFileInputStreamJob: Job? = null
    fun getUploadFileInputStreamJob() = uploadFileInputStreamJob
    private val _uploadFileInputStream = SingleLiveEvent<Int>()
    private val _uploadFileProgress = SingleLiveEvent<UploadInfo>()
    val uploadFileProgress: LiveData<UploadInfo> = _uploadFileProgress
    val uploadFileInputStream: LiveData<Int> = _uploadFileInputStream
    private val uploadSrcQueue = ConcurrentLinkedQueue<String>()
    private val uploadDstQueue = ConcurrentLinkedQueue<String>()
    private val uploadSrcFilePaths = ConcurrentLinkedQueue<String>()
    private val uploadDstFilePaths = ConcurrentLinkedQueue<String>()
    private var uploadSize = AtomicLong(0)
    private var uploadFileInput: InterruptibleInputStream? = null
    fun uploadFileInputStreamJobCancel() {
        // 通过关闭流来处理
        uploadFileInput?.interrupted = true
    }

    private var listFileJob: Job? = null
    private val _listFile = SingleLiveEvent<Int>()
    val listFile: LiveData<Int> = _listFile
    var listFileData: Vector<ChannelSftp.LsEntry>? = null
    private val _listFileLoading = SingleLiveEvent<Int>()
    val listFileLoading: LiveData<Int> = _listFileLoading

    private var downloadFileJob: Job? = null
    fun getDownloadFileJob() = downloadFileJob
    private val _downloadFile = SingleLiveEvent<Int>()
    private val _downloadFileProgress = SingleLiveEvent<UploadInfo>()
    val downloadFileProgress: LiveData<UploadInfo> = _downloadFileProgress
    val downloadFile: LiveData<Int> = _downloadFile
    private val downloadSrcQueue = ConcurrentLinkedQueue<String>()
    private val downloadDstQueue = ConcurrentLinkedQueue<String>()
    private val downloadSrcFilePaths = ConcurrentLinkedQueue<String>()
    private val downloadDstFilePaths = ConcurrentLinkedQueue<String>()
    private var downloadSize = AtomicLong(0)
    private var downloadFileInput: InterruptibleOutputStream? = null
    fun downloadFileJobCancel() {
        // 通过关闭流来处理
        downloadFileInput?.interrupted = true
    }


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
        connectInfo = MySPUtil.getInstance().clientConnectInfo
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
                if (!uploadSrcFilePaths.contains(f.absolutePath)){
                    srcFilePath.add(f.absolutePath)
                }
                val sdcard = Environment.getExternalStorageDirectory().absolutePath
                val absoluteSelectPath = if (selectParentPath.startsWith(sdcard)) {
                    selectParentPath
                } else {
                    sdcard.removeSuffix("/") + selectParentPath
                }
                val p = currentPath.removeSuffix("/") + "/" + f.absolutePath.removePrefix(
                    absoluteSelectPath
                )
                if (!uploadDstFilePaths.contains(normalizeFilePath(p))){
                    dstFilePath.add(normalizeFilePath(p))
                    allSize[0] += f.length()
                }
            }
        }
    }

    private fun collectLocalFiles(selectParentPath: String,
                                   files: List<File>,
                                   block: () -> Unit): Unit {
        viewModelScope.launch(Dispatchers.IO) {
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
                    if (!uploadSrcFilePaths.contains(f.absolutePath)){
                        srcFilePath.add(f.absolutePath)
                    }
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
                    if (!uploadDstFilePaths.contains(normalizeFilePath(p))){
                        dstFilePath.add(normalizeFilePath(p))
                        allSize[0] += f.length()
                    }
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
                Timber.d("collectLocalFiles srcFilePath: ${it}")
            }
            dstFilePath.forEach {
                Timber.d("collectLocalFiles dstFilePath: ${it}")
            }
            allSize.forEach {
                Timber.d("collectLocalFiles size: ${it}")
            }

            // 加入队列
            if (srcFilePath.size != 0 && srcFilePath.size == dstFilePath.size){
                uploadSrcQueue.addAll(srcFilePath)
                uploadDstQueue.addAll(dstFilePath)
                uploadSrcFilePaths.addAll(srcFilePath)
                uploadDstFilePaths.addAll(dstFilePath)
                uploadSize.set(uploadSize.get()+allSize[0])
                Timber.d("collectLocalFiles all size: ${uploadSize.get()}")
                block()
            }
        }

    }

    fun uploadLocalFiles(
        sftpClientService: SftpClientService?,
        selectParentPath: String,
        files: List<File>
    ) {
        collectLocalFiles(selectParentPath, files){
            if (uploadFileInputStreamJob != null && uploadFileInputStreamJob?.isActive == true) {
                return@collectLocalFiles
            }
            uploadFileInputStreamJob = viewModelScope.launch(customIODispatcher) {
                var uploadedBytes: Long = 0
                var lastUploadedBytes: Long = 0
                var uploadedCount: Int = 0

                // 循环获取数据并移除
                Timber.d("Upload Start")
                // 使用分离的客户端上传
                connectInfo?.run {
                    sftpClientService?.upload(
                        serverIp = ip,
                        port = port,
                        user = name,
                        password = pw,
                    ){ model ->
                        while (true) {
                            val uploadSrc = uploadSrcQueue.poll() ?: break // 获取并移除队首元素
                            val uploadDst = uploadDstQueue.poll() ?: break // 获取并移除队首元素
                            val l = object : SftpProgressMonitor {
                                override fun init(op: Int, src: String?, dest: String?, max: Long) {
                                    UploadInfo(
                                        progress = -1f,
                                        currentCount = 0,
                                        count = 0,
                                        currentFileSizes = 0,
                                        fileSizes = 0
                                    ).run {
                                        _uploadFileProgress.postValue(this)
                                    }
                                }

                                override fun count(count: Long): Boolean {
                                    uploadedBytes += count
                                    // 回传进度
                                    if (uploadSize.get() > 0) {
                                        if ((uploadedBytes - lastUploadedBytes) > uploadSize.get() / 1000 &&
                                            (uploadedBytes - lastUploadedBytes) > 1024 * 1024
                                        ) {
                                            // 超过千分之一并且大小大于1M，就更新进度
                                            lastUploadedBytes = uploadedBytes
                                            UploadInfo(
                                                progress = (uploadedBytes * 100 / uploadSize.get()).toFloat(),
                                                currentCount = uploadedCount,
                                                count = uploadSrcFilePaths.size,
                                                currentFileSizes = uploadedBytes,
                                                fileSizes = uploadSize.get()
                                            ).run {
                                                _uploadFileProgress.postValue(this)
                                            }
                                        }
                                    }
                                    return true // Return false to cancel the transfer
                                }

                                override fun end() {
                                    val peek = uploadSrcQueue.peek()
                                    uploadedCount += 1
                                    if (peek == null) {
                                        // 最后一个
                                        UploadInfo(
                                            progress = 100f,
                                            currentCount = uploadedCount,
                                            count = uploadSrcFilePaths.size,
                                            currentFileSizes = uploadedBytes,
                                            fileSizes = uploadSize.get()
                                        ).run {
                                            _uploadFileProgress.postValue(this)
                                        }
                                        uploadSize.set(0)
                                        uploadSrcFilePaths.clear()
                                        uploadDstFilePaths.clear()
                                        uploadedCount = 0
                                        Timber.d("Upload finished")
                                    }
                                }
                            }
                            Timber.d("uploadLocalFiles uploadSrc = ${uploadSrc}")
                            InterruptibleInputStream(uploadSrc).run {
                                uploadFileInput = this
                                try {
                                    model.uploadFileInputStream(this, uploadDst, l)
                                    // sftpClientService?.getClient()?.uploadFileInputStream(this, uploadDst, l)
                                } catch (e: Exception) {
                                    Timber.d("uploadFileInterrupt e ${e.message}")
                                    if (e.message?.contains(InterruptibleInputStream.INTERRUPT_MSG) == true) {
                                        model.deleteFile(uploadDst)
                                        cancel(InterruptibleInputStream.INTERRUPT_MSG, e)
                                    } else {

                                    }
                                }
                            }
                        }

                    }
                }


            }
            uploadFileInputStreamJob?.invokeOnCompletion { throwable ->
                if (throwable == null) {
                    Timber.d("uploadLocalFiles ok")
                    _uploadFileInputStream.postValue(1)
                } else {
                    Timber.d("uploadLocalFiles throwable = ${throwable.message}")
                    if (throwable.message?.contains(InterruptibleInputStream.INTERRUPT_MSG) == true) {
                        // skip
                    } else {
                        _uploadFileInputStream.postValue(0)
                    }

                }
                uploadSize.set(0)
                uploadSrcFilePaths.clear()
                uploadDstFilePaths.clear()
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

    private suspend fun addRemoteChildrenFile(
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
                        addRemoteChildrenFile(
                            sftpClientService = sftpClientService,
                            dirName = dirName + "/" + i.filename,
                            srcFilePath = srcFilePath,
                            dstFilePath = dstFilePath,
                            allSize = allSize,
                        )
                    } else if (i.attrs.isReg) {
                        val n = dirName + "/" + i.filename
                        if (!downloadSrcFilePaths.contains(n)){
                            srcFilePath.add(n)
                        }
                        if (!downloadDstFilePaths.contains(n)){
                            dstFilePath.add(n)
                            allSize[0] += i.attrs.size
                        }
                    }
                }
            }
        }
    }

    private fun collectRemoteFiles(sftpClientService: SftpClientService?,files: List<ChannelSftp.LsEntry>, block: () -> Unit): Unit {
        viewModelScope.launch(Dispatchers.IO) {
            val srcFilePath: MutableList<String> = mutableListOf()
            val dstFilePath: MutableList<String> = mutableListOf()
            val allSize = MutableList(1) { 0L }
            if (currentPath == "/") {
                // 根目录不用加
            } else {
            }
            files.forEach {
                if (it.attrs.isReg) {
                    val n = currentPath.removeSuffix("/") + "/" + it.filename
                    if (!downloadSrcFilePaths.contains(n)){
                        srcFilePath.add(n)
                    }
                    if (!downloadDstFilePaths.contains(n)){
                        dstFilePath.add(n)
                        allSize[0] += it.attrs.size
                    }
                    // 文件
                } else if (it.attrs.isDir) {
                    // 文件夹
                    addRemoteChildrenFile(
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
                Timber.d("collectRemoteFiles srcFilePath: ${it}")
            }
            dstFilePath.forEach {
                Timber.d("collectRemoteFiles dstFilePath: ${it}")
            }
            allSize.forEach {
                Timber.d("collectRemoteFiles allSize: ${it}")
            }
            // 加入队列
            if (srcFilePath.size != 0 && srcFilePath.size == dstFilePath.size){
                downloadSrcQueue.addAll(srcFilePath)
                downloadDstQueue.addAll(dstFilePath)
                downloadSrcFilePaths.addAll(srcFilePath)
                downloadDstFilePaths.addAll(dstFilePath)
                downloadSize.set(downloadSize.get()+allSize[0])
                Timber.d("collectRemoteFiles all size: ${downloadSize.get()}")
                block()
            }
        }
    }

    fun downloadFile(
        sftpClientService: SftpClientService?,
        files: List<ChannelSftp.LsEntry>,
    ) {
        collectRemoteFiles(sftpClientService, files){
            if (downloadFileJob != null && downloadFileJob?.isActive == true) {
                return@collectRemoteFiles
            }
            downloadFileJob = viewModelScope.launch(customIODispatcher) {
                var downloadedBytes: Long = 0
                var lastDownloadedBytes: Long = 0
                var downloadedCount: Int = 0

                // 循环获取数据并移除
                Timber.d("Download Start")
                //按照配置下载到选定的目录
                val parentPaths = MySPUtil.getInstance().downloadSavePath
                // 所有文件都是基于sdcard创建的
                val sdcardPath = normalizeFilePath(Environment.getExternalStorageDirectory().absolutePath+"/"+ parentPaths)
                ensureLocalDirectoryExists(sdcardPath)
                // 使用分离的客户端下载
                connectInfo?.run {
                    sftpClientService?.download(
                        serverIp = ip,
                        port = port,
                        user = name,
                        password = pw,
                    ){ model ->
                        while (true) {
                            val downloadSrc = downloadSrcQueue.poll() ?: break // 获取并移除队首元素
                            val downloadDst = downloadDstQueue.poll() ?: break // 获取并移除队首元素
                            val l = object : SftpProgressMonitor {
                                override fun init(op: Int, src: String?, dest: String?, max: Long) {
                                    UploadInfo(
                                        progress = -1f,
                                        currentCount = 0,
                                        count = 0,
                                        currentFileSizes = 0,
                                        fileSizes = 0
                                    ).run {
                                        _downloadFileProgress.postValue(this)
                                    }
                                }

                                override fun count(count: Long): Boolean {
                                    downloadedBytes += count
                                    // 回传进度
                                    if (downloadSize.get() > 0) {
                                        if ((downloadedBytes - lastDownloadedBytes) > downloadSize.get() / 1000 &&
                                            (downloadedBytes - lastDownloadedBytes) > 1024 * 1024
                                        ) {
                                            // 超过千分之一并且大小大于1M，就更新进度
                                            lastDownloadedBytes = downloadedBytes
                                            UploadInfo(
                                                progress = (downloadedBytes * 100 / downloadSize.get()).toFloat(),
                                                currentCount = downloadedCount,
                                                count = downloadSrcFilePaths.size,
                                                currentFileSizes = downloadedBytes,
                                                fileSizes = downloadSize.get()
                                            ).run {
                                                _downloadFileProgress.postValue(this)
                                            }
                                        }
                                    }
                                    return true // Return false to cancel the transfer
                                }

                                override fun end() {
                                    val peek = downloadSrcQueue.peek()
                                    downloadedCount += 1
                                    if (peek == null) {
                                        // 最后一个
                                        UploadInfo(
                                            progress = 100f,
                                            currentCount = downloadedCount,
                                            count = downloadSrcFilePaths.size,
                                            currentFileSizes = downloadedBytes,
                                            fileSizes = downloadSize.get()
                                        ).run {
                                            _downloadFileProgress.postValue(this)
                                        }
                                        downloadSize.set(0)
                                        downloadSrcFilePaths.clear()
                                        downloadDstFilePaths.clear()
                                        downloadedCount = 0
                                        Timber.d("Download finished")
                                    }
                                }
                            }
                            // at last
                            val dstFilePath = normalizeFilePath("${sdcardPath}/${getFileNameFromPath(downloadDst)}")
                            Timber.d("downloadFile dstFilePath = ${dstFilePath}")

                            InterruptibleOutputStream(dstFilePath).run {
                                downloadFileInput = this
                                try {
                                    model.downloadFile(downloadSrc, this, l)
                                } catch (e: Exception) {
                                    if (e.message?.contains(InterruptibleOutputStream.INTERRUPT_MSG) == true) {
                                        // 删除本地文件
                                        delFile(dstFilePath)
                                        cancel(InterruptibleOutputStream.INTERRUPT_MSG, e)
                                    } else {

                                    }
                                }
                            }
                        }
                    }

            }
            downloadFileJob?.invokeOnCompletion { throwable ->
                if (throwable == null) {
                    Timber.d("downloadFileJob ok")
                    _downloadFile.postValue(1)
                } else {
                    Timber.d("downloadFileJob throwable = ${throwable.message}")
                    if (throwable.message?.contains(InterruptibleOutputStream.INTERRUPT_MSG) == true) {
                        // skip
                    } else {
                        _downloadFile.postValue(0)
                    }

                }
                downloadSize.set(0)
                downloadSrcFilePaths.clear()
                downloadDstFilePaths.clear()
            }
        }

        }
    }

    private fun deleteRemoteFile(
        sftpClientService: SftpClientService?,
        path: String,
    ) {
        if (deleteFileJob != null && deleteFileJob?.isActive == true) {
            return
        }
        deleteFileJob = viewModelScope.launch(Dispatchers.IO) {
            sftpClientService?.getClient()
                ?.deleteFile(path)
        }
        deleteFileJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                Timber.d("deleteRemoteFile ok")
            } else {
                Timber.d("deleteRemoteFile throwable = ${throwable.message}")
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

    override fun onCleared() {
        super.onCleared()
        // 关闭自定义线程池
        (customIODispatcher.executor as? ExecutorService)?.shutdown()
    }
}