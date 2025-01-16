package com.emoji.ftp.ui.player

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.emoji.ftp.R
import com.emoji.ftp.utils.setStatusBarAndNavBar
import java.util.ArrayList

class FullPlayerActivity : AppCompatActivity() {
    private lateinit var viewModel: FullPlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =
            ViewModelProvider(this).get(FullPlayerViewModel::class.java)
        // 获取 Intent 和 Bundle
        intent.extras?.let {
            //      bundle.putInt("index", pp)
            //      bundle.putLong("seek",  player.currentPosition)//ms
            //      bundle.putStringArrayList("playList", playList)
            viewModel.index = it.getInt("index", 0)
            viewModel.seek = it.getLong("seek", 0)
            viewModel.seekPos.postValue(viewModel.index)
            viewModel.playList = it.getStringArrayList("playList")?:ArrayList()
        }
        // 设置全屏
        setStatusBarAndNavBar(window, Color.WHITE, true)

        enableEdgeToEdge()
        setContentView(R.layout.activity_full_player)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}