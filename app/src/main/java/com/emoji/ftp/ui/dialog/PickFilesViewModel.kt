package com.emoji.ftp.ui.dialog

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emoji.ftp.R
import com.emoji.ftp.provider.GetProvider
import com.emoji.ftp.utils.MySPUtil
import com.emoji.ftp.utils.showToast
import com.emoji.ftp.utils.thread.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


class PickFilesViewModel : ViewModel() {

    private var currentPath: String = "/"
    private var lastCurrentPath: String = "/"

    private var listFileJob: Job? = null
    private val _listFile = SingleLiveEvent<Int>()
    val listFile: LiveData<Int> = _listFile
    var listFileData: MutableList<File>? = null

    val showMultiSelectIcon = SingleLiveEvent<Boolean>()
    val showSelectAll = SingleLiveEvent<Boolean>()
    val changeSelectType = SingleLiveEvent<Int>()

    val sortTypes = mutableListOf(
        GetProvider.get().context.getString(R.string.text_sort_by_name),
        GetProvider.get().context.getString(R.string.text_sort_by_type),
        GetProvider.get().context.getString(R.string.text_sort_by_size),
        GetProvider.get().context.getString(R.string.text_sort_descendant_by_size),
        GetProvider.get().context.getString(R.string.text_sort_by_time),
        GetProvider.get().context.getString(R.string.text_sort_descendant_by_time),
    )

    private val _listFileLoading = SingleLiveEvent<Int>()
    val listFileLoading: LiveData<Int> = _listFileLoading

    fun getCurrentFilePath() = currentPath

    init {
        changeSelectType.value = MySPUtil.getInstance().clientSortType
    }

    fun listFile(path: String) {
        if (_listFileLoading.value == 1){
            return
        }
        lastCurrentPath = currentPath
        currentPath = path
        _listFileLoading.postValue(1)
        if (listFileJob != null && listFileJob?.isActive == true){
            return
        }
        listFileJob = viewModelScope.launch(Dispatchers.IO) {
            val filePath: String
            if (path == "/"){
                filePath = Environment.getExternalStorageDirectory().absolutePath
            }else{
                filePath = Environment.getExternalStorageDirectory().absolutePath + currentPath
            }
            val s = File(filePath).listFiles()
            Timber.d("filePath ${filePath}")


            if (s!= null && s.isNotEmpty()){
                listFileData = mutableListOf()
                for (i in s){
                    if (i.isDirectory){
                        if (i.name.equals(".")){
                            // 当前目录
                            Timber.d("listFiles .")
                            continue
                        }
                        if (i.name.equals("..")){
                            // 父目录
                            Timber.d("listFiles ..")
                            continue
                        }
                        listFileData?.add(i)
                    }else if (i.isFile) {
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
            _listFileLoading.postValue(-1)
        }
    }



}