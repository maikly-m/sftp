package com.example.ftp.ui.home

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Instrumentation.ActivityResult
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.ftp.databinding.FragmentServerBinding
import com.example.ftp.utils.grantExternalStorage
import org.apache.ftpserver.FtpServer

class ServerFragment : Fragment() {

    private lateinit var viewModel: ServerViewModel
    private var _binding: FragmentServerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this).get(ServerViewModel::class.java)

        _binding = FragmentServerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initView()

        viewModel.text.observe(viewLifecycleOwner){
            if (!TextUtils.isEmpty(it)) {
                binding.textServer.text = it
            } else {
                binding.textServer.text = "null"
            }
        }

        return root
    }


    // 权限请求相关
    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // MANAGE_EXTERNAL_STORAGE 权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    startFtpServer() // 权限已获取，启动 FTP 服务器
                } else {

                }
            }
        }

    private fun initView() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 请求 MANAGE_EXTERNAL_STORAGE 权限
            if (!Environment.isExternalStorageManager()) {
                // 创建跳转到 "管理应用所有文件访问权限" 的意图
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.setData(Uri.parse("package:" + requireActivity().packageName));
                // 使用 ActivityResultLauncher 启动该意图
                try {
                    storagePermissionLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "无法打开文件访问权限设置", Toast.LENGTH_SHORT).show()
                }
            } else {
                startFtpServer() // 权限已获取，启动 FTP 服务器
            }
        } else {
            // 适用于Android 9及以下版本
            grantExternalStorage(requireActivity()){
                if (it) {
                    // ok
                    startFtpServer()
                } else {
                    
                }
            }
        }

    }


    // 启动 FTP 服务器
    private fun startFtpServer() {
        viewModel.startFtpServer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        viewModel.getFtpServer()?.stop()
    }
}