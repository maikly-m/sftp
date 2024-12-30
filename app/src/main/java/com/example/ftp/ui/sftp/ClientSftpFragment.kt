package com.example.ftp.ui.sftp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.ftp.databinding.FragmentClientSftpBinding
import com.example.ftp.event.MessageEvent
import com.example.ftp.service.SftpClientService
import com.example.ftp.utils.grantExternalStorage
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class ClientSftpFragment : Fragment() {

    private var serverIp: String? = null
    private var sftpClientService: SftpClientService? = null
    private var isBound: Boolean = false
    private lateinit var viewModel: ClientSftpViewModel
    private var _binding: FragmentClientSftpBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        EventBus.getDefault().register(this)
        viewModel =
            ViewModelProvider(this).get(ClientSftpViewModel::class.java)

        _binding = FragmentClientSftpBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initView()

        return root
    }


    // 权限请求相关
    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // MANAGE_EXTERNAL_STORAGE 权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                start()
            } else {

            }
        }
    }

    private fun initView() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 请求 MANAGE_EXTERNAL_STORAGE 权限
            if (!Environment.isExternalStorageManager()) {
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
                    start() // 权限已获取，启动 FTP 服务器
                }
            } else {
                start() // 权限已获取，启动 FTP 服务器
            }
        } else {
            // 适用于Android 9及以下版本
            grantExternalStorage(requireActivity()){
                if (it) {
                    // ok
                    start()
                } else {

                }
            }
        }

    }

    private fun start() {
        // init client
        binding.etIp.text.toString().let {
            if (!TextUtils.isEmpty(it)) {
                serverIp = it
                startFtpClient()
            } else {
                Timber.d("ip is null")
            }
        }
        Timber.d("start ...")
        binding.btnUpload.setOnClickListener {
            openGallery()
        }

        binding.btnList.setOnClickListener {
            viewModel.listFile("/001")
        }
    }

    // 获取系统相册图片
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUris = mutableListOf<Uri>()

            result.data?.let { data ->
                if (data.clipData != null) {
                    // 多选模式
                    val count = data.clipData!!.itemCount
                    for (i in 0 until minOf(count, 4)) { // 限制最多 4 张
                        imageUris.add(data.clipData!!.getItemAt(i).uri)
                    }
                } else if (data.data != null) {
                    // 单选模式
                    imageUris.add(data.data!!)
                }
            }

            // 处理选中的图片
            handleSelectedImages(imageUris)
        }
    }

    private fun handleSelectedImages(imageUris: List<Uri>) {
        // 这里是处理选中图片的逻辑
        imageUris.forEach { uri ->
            Timber.d("Selected image URI: $uri")
            // 例如：显示在 ImageView 或上传到服务器
        }
        if (imageUris.isNotEmpty()){
            val s: String? = getRealPathFromURI(requireContext().contentResolver, imageUris[0])
            Timber.d("handleSelectedImages s=$s")
            // 检测文件权限
            binding.etIp.text.toString().let {
                if (!TextUtils.isEmpty(it)) {
                    Timber.d("handleSelectedImages ip=$it")
                    s?.run {
                        viewModel.uploadFile(s)
                    }
                } else {
                }
            }
        }
    }

    fun getRealPathFromURI(contentResolver: ContentResolver, uri: Uri): String? {
        // 如果是通过 MediaStore 获取的内容
        if (uri.scheme == "content") {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (it.moveToFirst()) {
                    return it.getString(columnIndex)
                }
            }
        }
        return null
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // 启用多选
        }
        pickImageLauncher.launch(Intent.createChooser(intent, "Select up to 1 images"))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireContext().unbindService(serviceConnection)
        EventBus.getDefault().unregister(this)
    }

    // 启动 FTP 服务器
    private fun startFtpClient() {
        if (isBound){
            return
        }
        // 绑定服务
        Timber.d("startFtpClient ..")
        val intent = Intent(requireContext(), SftpClientService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("serviceConnection ..")
            val binder = service as SftpClientService.LocalBinder
            sftpClientService = binder.getService()
            sftpClientService!!.connect(serverIp!!, 2222, "ftpuser", "12345")
            viewModel.setClient(sftpClientService!!)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sftpClientService = null
            isBound = false
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent) {
        // 处理事件
        Toast.makeText(requireContext(), "${event.message}", Toast.LENGTH_SHORT).show()
    }
}