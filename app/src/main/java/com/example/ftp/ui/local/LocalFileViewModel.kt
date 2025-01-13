package com.example.ftp.ui.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ftp.utils.thread.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocalFileViewModel : ViewModel() {

    fun saveDrawableAsJPG(function: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            function()
        }
    }


    val showMultiSelectIcon = SingleLiveEvent<Boolean>()
    val showSelectAll = SingleLiveEvent<Boolean>()
    val changeSelectCondition = SingleLiveEvent<Int>()
    val changeSelectType = SingleLiveEvent<Int>()
    val sortTypes = mutableListOf(
        "按时间升序",
        "按时间降序",
    )

    init {

    }
}