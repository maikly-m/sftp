package com.example.ftp.ui.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ftp.utils.thread.SingleLiveEvent

class LocalFileViewModel : ViewModel() {


    val showMultiSelectIcon = SingleLiveEvent<Boolean>()
    val changeSelectCondition = SingleLiveEvent<Int>()

}