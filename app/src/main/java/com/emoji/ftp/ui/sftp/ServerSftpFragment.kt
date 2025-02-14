package com.emoji.ftp.ui.sftp

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.emoji.ftp.R
import com.emoji.ftp.bean.ConnectInfo
import com.emoji.ftp.databinding.FragmentServerSftpBinding
import com.emoji.ftp.service.SftpServerService
import com.emoji.ftp.utils.MySPUtil
import com.emoji.ftp.utils.generateQRCode
import com.emoji.ftp.utils.getLocalIpAddress
import com.emoji.ftp.utils.grantExternalStorage
import com.emoji.ftp.utils.gson.GsonUtil
import com.emoji.ftp.utils.showToast
import timber.log.Timber

class ServerSftpFragment : Fragment() {

    private var sftpServerService: SftpServerService? = null
    private var isBound: Boolean = false
    private lateinit var viewModel: ServerSftpViewModel
    private var _binding: FragmentServerSftpBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this).get(ServerSftpViewModel::class.java)

        _binding = FragmentServerSftpBinding.inflate(inflater, container, false)
        val root: View = binding.root


        binding.layoutTitle.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.layoutTitle.tvName.text = getString(R.string.text_server)

        initView()

        viewModel.text.observe(viewLifecycleOwner){
            if (!TextUtils.isEmpty(it)) {
                binding.llServer.visibility = View.VISIBLE
                // 获取到了ip
                 MySPUtil.getInstance().serverConnectInfo?.let { info ->
                     binding.etIp.text = it
                     binding.etPort.text = "${info.port}"
                     binding.etName.text = info.name
                     binding.etPw.text = info.pw
                     // 要生成二维码的内容
                     val qrContent = ConnectInfo(it, info.port, info.name, info.pw)
                     // 调用生成二维码的方法
                     val bitmap = generateQRCode(GsonUtil.toJson(qrContent))
                     if (bitmap != null) {
                         binding.ivCode.setImageBitmap(bitmap)
                     }
                 }
            } else {
                binding.tvCodeTip.text = getString(R.string.text_not_obtain_ip)
                binding.llServer.visibility = View.INVISIBLE
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
                    showToast(getString(R.string.text_not_access_file_permission_settings))
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

    override fun onResume() {
        super.onResume()
        // 屏幕常亮
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 关闭常亮
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // 启动 FTP 服务器
    private fun startFtpServer() {
        if (isBound){
            return
        }
        // 绑定服务
        Timber.d("startFtpServer ..")
        val intent = Intent(requireContext(), SftpServerService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireContext().unbindService(serviceConnection)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("serviceConnection ..")
            val binder = service as SftpServerService.LocalBinder
            sftpServerService = binder.getService()
            viewModel._text.postValue(getLocalIpAddress(requireContext()))
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sftpServerService = null
            isBound = false
        }
    }
}