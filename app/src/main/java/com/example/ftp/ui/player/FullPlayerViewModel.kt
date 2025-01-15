package com.example.ftp.ui.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ftp.utils.thread.SingleLiveEvent
import java.util.ArrayList

class FullPlayerViewModel : ViewModel() {

    val loading = SingleLiveEvent<Boolean>()
    val seekPos = SingleLiveEvent<Int>()
    var playList: ArrayList<String>? = null
    var seek: Long = 0
    var index: Int = 0

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text
}