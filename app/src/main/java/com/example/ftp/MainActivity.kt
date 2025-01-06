package com.example.ftp

import android.graphics.Color
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.ftp.databinding.ActivityMainBinding
import com.example.ftp.utils.setStatusBarAndNavBar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置全屏
        setStatusBarAndNavBar(window, Color.WHITE, true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}