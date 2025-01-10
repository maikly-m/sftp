package com.example.ftp.ui

import android.os.Environment
import android.os.FileObserver.CREATE
import android.os.FileObserver.DELETE
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.R
import com.example.ftp.bean.FileInfo
import com.example.ftp.observe.RecursiveFileObserver
import com.example.ftp.provider.GetProvider
import com.example.ftp.room.DatabaseInstance
import com.example.ftp.room.bean.FileTrack
import com.example.ftp.room.bean.FileTrackDao
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.apkSuffixType
import com.example.ftp.utils.docSuffixType
import com.example.ftp.utils.imageSuffixType
import com.example.ftp.utils.musicSuffixType
import com.example.ftp.utils.normalizeFilePath
import com.example.ftp.utils.pdfSuffixType
import com.example.ftp.utils.pptSuffixType
import com.example.ftp.utils.textSuffixType
import com.example.ftp.utils.thread.SingleLiveEvent
import com.example.ftp.utils.videoSuffixType
import com.example.ftp.utils.zipSuffixType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@OptIn(FlowPreview::class)
class MainViewModel : ViewModel() {

    private lateinit var fileObserver: RecursiveFileObserver
    private var images: MutableList<FileTrack> = mutableListOf()
    private var videos: MutableList<FileTrack> = mutableListOf()
    private var musics: MutableList<FileTrack> = mutableListOf()
    private var texts: MutableList<FileTrack> = mutableListOf()
    private var apks: MutableList<FileTrack> = mutableListOf()
    private var zips: MutableList<FileTrack> = mutableListOf()

    private var fileTrackDao: FileTrackDao =
        DatabaseInstance.getDatabase(GetProvider.get().context).fileTrackDao()
    private val fileChangeFlow = MutableSharedFlow<String>()


    val fileInfos = mutableListOf(
        FileInfo("image", "图片", 0, R.drawable.svg_image_icon),
        FileInfo("video", "视频", 0, R.drawable.svg_media_icon),
        FileInfo("music", "音乐", 0, R.drawable.svg_music_icon),
        FileInfo("text", "文本", 0, R.drawable.svg_text_icon),
        FileInfo("apk", "APK", 0, R.drawable.svg_apk_icon),
        FileInfo("zip", "压缩包", 0, R.drawable.svg_zip_icon),
    )

    val fileMap = hashMapOf(
        "image" to images,
        "video" to videos,
        "music" to musics,
        "text" to texts,
        "apk" to apks,
        "zip" to zips,
    )

    val fileSuffix = mutableListOf<String>()

    private var listFileJob: Job? = null
    private val _listFile = SingleLiveEvent<Int>()
    val listFile: LiveData<Int> = _listFile

    private var getAllFileJob: Job? = null
    private val _getAllFile = SingleLiveEvent<Int>()
    val getAllFile: LiveData<Int> = _getAllFile

    init {
        resetFileObserver()
        // 3s刷新一次
        viewModelScope.launch {
            fileChangeFlow
                .debounce(3_000)
                .collect { message ->
                    Timber.d("FileObserver, listFile: $message")
                    listFile(message)
                }
        }
        fileObserver.startWatching()

        fileSuffix.run {
            addAll(imageSuffixType)
            addAll(videoSuffixType)
            addAll(musicSuffixType)
            addAll(textSuffixType)
            addAll(apkSuffixType)
            addAll(zipSuffixType)
            addAll(docSuffixType)
            addAll(pptSuffixType)
            addAll(pdfSuffixType)
        }

    }

    override fun onCleared() {
        super.onCleared()
        fileObserver.stopWatching()
    }

    private suspend fun getFileByType(type: String): MutableList<FileTrack> {
        val f = mutableListOf<FileTrack>()
        when (type) {
            "image" -> {
                imageSuffixType.forEach {
                    fileTrackDao.getByType(it)?.run {
                        f.addAll(this)
                    }
                }
            }

            "video" -> {
                videoSuffixType.forEach {
                    fileTrackDao.getByType(it)?.run {
                        f.addAll(this)
                    }
                }
            }

            "music" -> {
                musicSuffixType.forEach {
                    fileTrackDao.getByType(it)?.run {
                        f.addAll(this)
                    }
                }
            }

            "apk" -> {
                apkSuffixType.forEach {
                    fileTrackDao.getByType(it)?.run {
                        f.addAll(this)
                    }
                }
            }

            "text" -> {
                textSuffixType.forEach {
                    fileTrackDao.getByType(it)?.run {
                        f.addAll(this)
                    }
                }
            }

            "zip" -> {
                zipSuffixType.forEach {
                    fileTrackDao.getByType(it)?.run {
                        f.addAll(this)
                    }
                }
            }

            else -> {}
        }
        return f
    }

    fun listFile(absolutePath: String, recursive: Boolean = true) {
        if (listFileJob != null && listFileJob?.isActive == true) {
            return
        }
        listFileJob = viewModelScope.launch(Dispatchers.IO) {
            Timber.d("listFile start")
            val listFileData = scanFiles(File(absolutePath), fileSuffix, recursive)
            // 更新到数据库
            Timber.d("listFile ...")
            updateDatabaseFiles(absolutePath, listFileData)
            Timber.d("listFile end")
        }
        listFileJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                _listFile.postValue(1)
            } else {
                _listFile.postValue(0)
            }
        }
    }

    private suspend fun updateDatabaseFiles(
        absolutePath: String,
        newFilePaths: MutableList<FileTrack>
    ) {
        var daoFilePaths = fileTrackDao.getAll()
        val parentPath = normalizeFilePath(absolutePath)
        if (parentPath == Environment.getExternalStorageDirectory().absolutePath) {
        } else {
            // 选出指定数据
            daoFilePaths = daoFilePaths.filter { it.path.contains(parentPath) }
        }
        var paths = daoFilePaths.map { it.path }.toSet()
        val filesToAdd = newFilePaths.filterNot { it.path in paths }

        paths = newFilePaths.map { it.path }.toSet()
        val filesToRemove = daoFilePaths.filterNot { it.path in paths }
        filesToAdd.forEach {
            fileTrackDao.insert(it)
        }
        filesToRemove.forEach {
            fileTrackDao.delete(it)
        }
    }

    private fun scanFiles(
        directory: File,
        fileTypes: List<String>? = null,
        hideDot: Boolean = true,
        recursive: Boolean = true
    ): MutableList<FileTrack> {
        val fileList = mutableListOf<FileTrack>()
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory && recursive) {
                if (file.name.startsWith(".")) {
                    if (hideDot) {
                        return@forEach
                    }
                }
                fileList.addAll(scanFiles(file, fileTypes)) // 递归子目录
            } else {
                if (fileTypes == null || file.extension.lowercase() in fileTypes) {
                    fileList.add(
                        FileTrack(
                            type = file.extension.lowercase(),
                            path = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            mTime = file.lastModified()
                        )
                    )
                }
            }
        }
        return fileList
    }

    fun getAllFile() {
        if (getAllFileJob != null && getAllFileJob?.isActive == true) {
            return
        }
        getAllFileJob = viewModelScope.launch(Dispatchers.IO) {
            images.clear()
            images.addAll(getFileByType("image"))
            videos.clear()
            videos.addAll(getFileByType("video"))
            musics.clear()
            musics.addAll(getFileByType("music"))
            texts.clear()
            texts.addAll(getFileByType("text"))
            apks.clear()
            apks.addAll(getFileByType("apk"))
            zips.clear()
            zips.addAll(getFileByType("zip"))

            fileInfos.find {
                it.type == "image"
            }?.count = images.size
            fileInfos.find {
                it.type == "video"
            }?.count = videos.size
            fileInfos.find {
                it.type == "music"
            }?.count = musics.size
            fileInfos.find {
                it.type == "text"
            }?.count = texts.size
            fileInfos.find {
                it.type == "apk"
            }?.count = apks.size
            fileInfos.find {
                it.type == "zip"
            }?.count = zips.size
        }
        getAllFileJob?.invokeOnCompletion { throwable ->
            if (throwable == null) {
                _getAllFile.postValue(1)
            } else {
                _getAllFile.postValue(0)
            }
        }
    }

    fun resetFileObserver() {
        if (::fileObserver.isInitialized){
            fileObserver.stopWatching()
        }
        val parentPaths = MySPUtil.getInstance().downloadSavePath
        // 所有文件都是基于sdcard创建的
        val sdcardPath = normalizeFilePath(Environment.getExternalStorageDirectory().absolutePath+"/"+ parentPaths)

        fileObserver = RecursiveFileObserver(sdcardPath){ file, event ->
            when (event) {
                CREATE -> Timber.d("FileObserver, File created: $file")
                DELETE -> Timber.d("FileObserver, File deleted: $file")
                else -> {

                }
            }
            // 更新数据
            file.let {
                File(file).run {
                    if (isDirectory){
                        // listFile(file, false)
                    }else if (isFile) {
                        parentFile?.absolutePath?.let { p ->
                            viewModelScope.launch{
                                fileChangeFlow.emit(p)
                            }
                        }
                    }
                }
            }
        }
    }

}