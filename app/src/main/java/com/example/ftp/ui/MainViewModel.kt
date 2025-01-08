package com.example.ftp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.R
import com.example.ftp.bean.FileInfo
import com.example.ftp.provider.GetProvider
import com.example.ftp.room.DatabaseInstance
import com.example.ftp.room.bean.FileTrack
import com.example.ftp.room.bean.FileTrackDao
import com.example.ftp.utils.apkSuffixType
import com.example.ftp.utils.docSuffixType
import com.example.ftp.utils.imageSuffixType
import com.example.ftp.utils.musicSuffixType
import com.example.ftp.utils.pdfSuffixType
import com.example.ftp.utils.pptSuffixType
import com.example.ftp.utils.textSuffixType
import com.example.ftp.utils.thread.SingleLiveEvent
import com.example.ftp.utils.videoSuffixType
import com.example.ftp.utils.zipSuffixType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class MainViewModel : ViewModel() {

    private var images: MutableList<FileTrack> = mutableListOf()
    private var videos: MutableList<FileTrack> = mutableListOf()
    private var musics: MutableList<FileTrack> = mutableListOf()
    private var texts: MutableList<FileTrack> = mutableListOf()
    private var apks: MutableList<FileTrack> = mutableListOf()
    private var zips: MutableList<FileTrack> = mutableListOf()

    private var fileTrackDao: FileTrackDao = DatabaseInstance.getDatabase(GetProvider.get().context).fileTrackDao()

    val fileInfos = mutableListOf(
        FileInfo("image", "图片",0, R.drawable.svg_image_icon),
        FileInfo("video", "视频",0, R.drawable.svg_media_icon),
        FileInfo("music", "音乐",0, R.drawable.svg_music_icon),
        FileInfo("text", "文本",0, R.drawable.svg_text_icon),
        FileInfo("apk", "APK",0, R.drawable.svg_apk_icon),
        FileInfo("zip", "压缩包",0, R.drawable.svg_zip_icon),
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

    suspend fun getFileByType(type: String): MutableList<FileTrack> {
        val f = mutableListOf<FileTrack>()
        when (type){
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

    fun listFile(absolutePath: String) {
        if (listFileJob != null && listFileJob?.isActive == true) {
            return
        }
        listFileJob = viewModelScope.launch(Dispatchers.IO) {
            Timber.d("listFile start")
            val listFileData = scanFiles(File(absolutePath), fileSuffix)
            // 更新到数据库
            Timber.d("listFile ...")
            fileTrackDao.deleteAll()
            listFileData.forEach {
                fileTrackDao.insert(it)
            }
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
    private fun scanFiles(directory: File, fileTypes: List<String>? = null, hideDot: Boolean = true): MutableList<FileTrack> {
        val fileList = mutableListOf<FileTrack>()
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (file.name.startsWith(".")){
                    if (hideDot) {
                        return@forEach
                    }
                }
                fileList.addAll(scanFiles(file, fileTypes)) // 递归子目录
            } else {
                if (fileTypes == null || file.extension.lowercase() in fileTypes) {
                    fileList.add(FileTrack(
                        type = file.extension.lowercase(),
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        mTime = file.lastModified()
                    ))
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

}