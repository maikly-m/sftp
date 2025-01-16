package com.emoji.ftp.ui.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emoji.ftp.R
import com.emoji.ftp.bean.ConnectInfo
import com.emoji.ftp.bean.UploadInfo
import com.emoji.ftp.provider.GetProvider
import com.emoji.ftp.service.SftpClientService
import com.emoji.ftp.ui.sftp.InterruptibleInputStream
import com.emoji.ftp.utils.MySPUtil
import com.emoji.ftp.utils.formatTimeWithDay
import com.emoji.ftp.utils.normalizeFilePath
import com.emoji.ftp.utils.thread.SingleLiveEvent
import com.jcraft.jsch.SftpProgressMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class LocalFileViewModel : ViewModel() {

    private var currentPath: String = "/sftp" // 当前服务器目录
    private var connectInfo: ConnectInfo? = null
    // 创建自定义的 IO Dispatcher
    val customIODispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
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

    val showMultiSelectIcon = SingleLiveEvent<Boolean>()
    val showSelectAll = SingleLiveEvent<Boolean>()
    val changeSelectCondition = SingleLiveEvent<Int>()
    val changeSelectType = SingleLiveEvent<Int>()
    val sortTypes = mutableListOf(
        GetProvider.get().context.getString(R.string.text_sort_by_time),
        GetProvider.get().context.getString(R.string.text_sort_descendant_by_time),
    )

    init {
        connectInfo = MySPUtil.getInstance().clientConnectInfo
        currentPath = MySPUtil.getInstance().uploadSavePath
    }

    fun saveDrawableAsJPG(function: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            function()
        }
    }
    private fun addLocalChildrenFile(
        file: File,
        remoteParentPath: String,
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
                    remoteParentPath,
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
                val p = currentPath.removeSuffix("/") + "/" +remoteParentPath + "/${formatTimeWithDay(f.lastModified())}/" +f.name
                if (!uploadDstFilePaths.contains(normalizeFilePath(p))){
                    dstFilePath.add(normalizeFilePath(p))
                    allSize[0] += f.length()
                }
            }
        }
    }

    private fun collectLocalFiles(remoteParentPath: String,
                                  files: List<File>,
                                  block: () -> Unit): Unit {
        viewModelScope.launch(Dispatchers.IO) {
            val srcFilePath: MutableList<String> = mutableListOf()
            val dstFilePath: MutableList<String> = mutableListOf()
            val allSize = MutableList(1) { 0L }
            files.forEach { f ->
                if (f.isDirectory) {
                    // 文件夹
                    addLocalChildrenFile(
                        f,
                        remoteParentPath,
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
                    val p = currentPath.removeSuffix("/") + "/" +remoteParentPath + "/${formatTimeWithDay(f.lastModified())}/" +f.name
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
        remoteParentPath: String,// 基于服务器指定目录下，文件所在的相对位置
        files: List<File>
    ) {
        collectLocalFiles(remoteParentPath, files){
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
                                } catch (e: Exception) {
                                    Timber.d("uploadFileInterrupt e ${e}")
                                    sftpClientService.other(
                                        serverIp = ip,
                                        port = port,
                                        user = name,
                                        password = pw,
                                    ){ model ->
                                        model.deleteFile(uploadDst)
                                    }
                                    cancel(InterruptibleInputStream.INTERRUPT_MSG, InterruptedException(InterruptibleInputStream.INTERRUPT_MSG))
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
}