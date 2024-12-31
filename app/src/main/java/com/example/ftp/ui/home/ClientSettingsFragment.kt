package com.example.ftp.ui.home

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.ftp.R
import com.example.ftp.databinding.FragmentClientSettingsBinding
import com.example.ftp.event.ClientMessageEvent
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.grantExternalStorage
import com.example.ftp.utils.showToast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class ClientSettingsFragment : Fragment() {

    private var serverIp: String? = null
    private lateinit var viewModel: ClientSettingsViewModel
    private var _binding: FragmentClientSettingsBinding? = null

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
            ViewModelProvider(this).get(ClientSettingsViewModel::class.java)

        _binding = FragmentClientSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.layoutTitleBrowser.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        initView()

        return root
    }


    // 权限请求相关
    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // MANAGE_EXTERNAL_STORAGE 权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                configure()
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
                    configure() // 权限已获取，启动 FTP 服务器
                }
            } else {
                configure() // 权限已获取，启动 FTP 服务器
            }
        } else {
            // 适用于Android 9及以下版本
            grantExternalStorage(requireActivity()){
                if (it) {
                    // ok
                    configure()
                } else {

                }
            }
        }

        binding.btnConnect.setOnClickListener {
            // 保存配置
            if (!TextUtils.isEmpty(serverIp)){
                MySPUtil.getInstance().setServerIp(serverIp)
                findNavController().navigate(R.id.action_client_settings2client_sftp)
            }else{
                showToast("ip 没有填写")
            }
        }
    }

    private fun configure() {
        binding.etIp.text.toString().let {
            if (!TextUtils.isEmpty(it)) {
                serverIp = it
            } else {
                Timber.d("ip is null")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: ClientMessageEvent) {
        // 处理事件
    }
}