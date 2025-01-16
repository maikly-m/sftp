package com.emoji.ftp.ui

import android.os.Environment
import android.os.FileObserver.CREATE
import android.os.FileObserver.DELETE
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.emoji.ftp.R
import com.emoji.ftp.bean.FileInfo
import com.emoji.ftp.observe.RecursiveFileObserver
import com.emoji.ftp.provider.GetProvider
import com.emoji.ftp.room.DatabaseInstance
import com.emoji.ftp.room.bean.FileTrack
import com.emoji.ftp.room.bean.FileTrackDao
import com.emoji.ftp.utils.MySPUtil
import com.emoji.ftp.utils.apkSuffixType
import com.emoji.ftp.utils.delFile
import com.emoji.ftp.utils.docSuffixType
import com.emoji.ftp.utils.excSuffixType
import com.emoji.ftp.utils.imageSuffixType
import com.emoji.ftp.utils.musicSuffixType
import com.emoji.ftp.utils.normalizeFilePath
import com.emoji.ftp.utils.otherSuffixType
import com.emoji.ftp.utils.pdfSuffixType
import com.emoji.ftp.utils.pptSuffixType
import com.emoji.ftp.utils.removeFileExtension
import com.emoji.ftp.utils.textSuffixType
import com.emoji.ftp.utils.thread.SingleLiveEvent
import com.emoji.ftp.utils.videoSuffixType
import com.emoji.ftp.utils.zipSuffixType
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

    var player: ExoPlayer? = null
    private var appSdcard: String = ""
    fun getSppSdcard() = appSdcard
    private lateinit var fileObserver: RecursiveFileObserver
    private var images: MutableList<FileTrack> = mutableListOf()
    private var videos: MutableList<FileTrack> = mutableListOf()
    private var musics: MutableList<FileTrack> = mutableListOf()
    private var texts: MutableList<FileTrack> = mutableListOf()
    private var apks: MutableList<FileTrack> = mutableListOf()
    private var zips: MutableList<FileTrack> = mutableListOf()
    private var docs: MutableList<FileTrack> = mutableListOf()
    private var excs: MutableList<FileTrack> = mutableListOf()
    private var ppts: MutableList<FileTrack> = mutableListOf()
    private var pdfs: MutableList<FileTrack> = mutableListOf()
    private var others: MutableList<FileTrack> = mutableListOf()

    private var fileTrackDao: FileTrackDao =
        DatabaseInstance.getDatabase(GetProvider.get().context).fileTrackDao()
    private val fileChangeFlow = MutableSharedFlow<String>()


    val fileInfos = mutableListOf(
        FileInfo("image", GetProvider.get().context.getString(R.string.text_image), 0, R.drawable.svg_image_icon),
        FileInfo("video", GetProvider.get().context.getString(R.string.text_video), 0, R.drawable.svg_media_icon),
        FileInfo("music", GetProvider.get().context.getString(R.string.text_music), 0, R.drawable.svg_music_icon),
        FileInfo("text", GetProvider.get().context.getString(R.string.text_txt), 0, R.drawable.svg_text_icon),
        FileInfo("apk", GetProvider.get().context.getString(R.string.text_apk), 0, R.drawable.svg_apk_icon),
        FileInfo("zip", GetProvider.get().context.getString(R.string.text_zip), 0, R.drawable.svg_zip_icon),
        FileInfo("doc", GetProvider.get().context.getString(R.string.text_doc), 0, R.drawable.svg_word_icon),
        FileInfo("exc", GetProvider.get().context.getString(R.string.text_exc), 0, R.drawable.svg_excel_icon),
        FileInfo("ppt", GetProvider.get().context.getString(R.string.text_ppt), 0, R.drawable.svg_ppt_icon),
        FileInfo("pdf", GetProvider.get().context.getString(R.string.text_pdf), 0, R.drawable.svg_pdf_icon),
        FileInfo("other",
            GetProvider.get().context.getString(R.string.text_other), 0, R.drawable.svg_file_unknown_icon),
    )
    val fileMap = hashMapOf(
        "image" to images,
        "video" to videos,
        "music" to musics,
        "text" to texts,
        "apk" to apks,
        "zip" to zips,
        "doc" to docs,
        "exc" to excs,
        "ppt" to ppts,
        "pdf" to pdfs,
        "other" to others,
    )

    val fileSuffix = mutableListOf<String>()

    private var listFileJob: Job? = null
    private val _listFile = SingleLiveEvent<Int>()
    val listFile: LiveData<Int> = _listFile

    private var getAllFileJob: Job? = null
    private val _getAllFile = SingleLiveEvent<Int>()
    val getAllFile: LiveData<Int> = _getAllFile
    val thumbPaths = mutableListOf<Pair<String, String>>()

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
            addAll(excSuffixType)
            addAll(pptSuffixType)
            addAll(pdfSuffixType)
            addAll(otherSuffixType)
        }
        appSdcard = "${GetProvider.get().context.filesDir}/sdcard/"
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
            "doc" -> {
                docSuffixType.forEach {
                    fileTrackDao.getByType(it)?.run {
                        f.addAll(this)
                    }
                }
            }
            "exc" -> {
                excSuffixType.forEach {
                    fileTrackDao.getByType(it)?.run {
                        f.addAll(this)
                    }
                }
            }
            "ppt" -> {
                pptSuffixType.forEach {
                    fileTrackDao.getByType(it)?.run {
                        f.addAll(this)
                    }
                }
            }
            "pdf" -> {
                pdfSuffixType.forEach {
                    fileTrackDao.getByType(it)?.run {
                        f.addAll(this)
                    }
                }
            }
            "other" -> {
                otherSuffixType.forEach {
                    // 匹配文件
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
        thumbPaths.clear()
        filesToAdd.forEach {
            fileTrackDao.insert(it)
            // 视频需要添加缩略图
            if (!TextUtils.isEmpty(it.thumbnailPath)){
                // 多线程处理
                thumbPaths.add(Pair(it.path, it.thumbnailPath))
            }
        }
        filesToRemove.forEach {
            fileTrackDao.delete(it)
            // 视频需要移除缩略图
            if (!TextUtils.isEmpty(it.thumbnailPath)){
                delFile(it.thumbnailPath)
            }
        }
        // 后面处理paths
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
                    if (file.extension.lowercase() in videoSuffixType) {
                        fileList.add(
                            FileTrack(
                                type = file.extension.lowercase(),
                                path = file.absolutePath,
                                name = file.name,
                                size = file.length(),
                                mTime = file.lastModified(),
                                thumbnailPath = normalizeFilePath(appSdcard+removeFileExtension(file.absolutePath)+".jpg")
                            )
                        )
                    } else {
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
            docs.clear()
            docs.addAll(getFileByType("doc"))
            ppts.clear()
            ppts.addAll(getFileByType("ppt"))
            pdfs.clear()
            pdfs.addAll(getFileByType("pdf"))
            others.clear()
            others.addAll(getFileByType("other"))

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
            fileInfos.find {
                it.type == "doc"
            }?.count = docs.size
            fileInfos.find {
                it.type == "ppt"
            }?.count = ppts.size
            fileInfos.find {
                it.type == "pdf"
            }?.count = pdfs.size
            fileInfos.find {
                it.type == "other"
            }?.count = others.size
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

    fun updateThumbs() {

//        thumbPaths.forEach { p ->
//            viewModelScope.launch(Dispatchers.IO) {
//                Timber.d("thumbPaths start ${p.first}, thread=${coroutineContext}")
//                saveVideoThumbnail(GetProvider.get().context, p.first, p.second)
//                Timber.d("thumbPaths end ${p.first}")
//            }
//        }

    }

}