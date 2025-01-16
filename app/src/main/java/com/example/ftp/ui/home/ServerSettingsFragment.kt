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
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.ftp.R
import com.example.ftp.bean.ConnectInfo
import com.example.ftp.databinding.FragmentServerSettingsBinding
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.getLocalIpAddress
import com.example.ftp.utils.grantExternalStorage
import com.example.ftp.utils.isConnectedToWifi
import com.example.ftp.utils.showToast
import timber.log.Timber

class ServerSettingsFragment : Fragment() {

    private var _binding: FragmentServerSettingsBinding? = null
    private lateinit var viewModel: ServerSettingsViewModel
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel =
            ViewModelProvider(this).get(ServerSettingsViewModel::class.java)

        _binding = FragmentServerSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.layoutTitle.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.layoutTitle.tvName.text = getString(R.string.text_server)

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
                        showToast(resources.getString(R.string.text_not_access_file_permission_settings))

                    }
                } else {
                    configure()
                }
            } else {
                configure()
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

        // 替换闪烁的cursor
        replaceCursorStyle(binding.etPort)
        replaceCursorStyle(binding.etName)
        replaceCursorStyle(binding.etPw)

        binding.etPort.addTextChangedListener(
            beforeTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            onTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            afterTextChanged = {
                it?.toString()?.run {
                    viewModel.etPort = this
                }
            }
           )
        binding.etName.addTextChangedListener(
            beforeTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            onTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            afterTextChanged = {
                it?.toString()?.run {
                    viewModel.etName = this
                }
            })
        binding.etPw.addTextChangedListener(
            beforeTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            onTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            afterTextChanged = {
                it?.toString()?.run {
                    viewModel.etPw = this
                }
            })



        binding.tvStart.setOnClickListener {
            // 检测wifi连接
            val connectWifi = isConnectedToWifi(requireContext())
            val ip = getLocalIpAddress(requireContext())
            if (!connectWifi || ip == "0.0.0.0"){
                showToast(getString(R.string.text_disconnect_wifi))
                return@setOnClickListener
            }

            var p = -1
            try {
                p = binding.etPort.text.toString().toInt()
            } catch (e: Exception) {
                Timber.e(e.message)
            }

            val info =  ConnectInfo("",
                p,
                binding.etName.text.toString(),
                binding.etPw.text.toString(),
            )

            if (info.port < 2000) {
                showToast(getString(R.string.text_port_must_greater_than_2000))
            } else {
                if (TextUtils.isEmpty(info.name)) {
                    showToast(getString(R.string.text_user_name_can_not_null))
                } else {
                    if (TextUtils.isEmpty(info.pw)) {
                        showToast(getString(R.string.text_pw_can_not_null))
                    } else {
                        MySPUtil.getInstance().serverConnectInfo = info
                        findNavController().navigate(R.id.action_server_settings2server_sftp)
                    }
                }
            }
        }
    }

    private fun replaceCursorStyle(et: EditText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above (API 29+)
            val cursorDrawable = resources.getDrawable(R.drawable.custom_cursor, null)
            et.textCursorDrawable = cursorDrawable
        } else {
            // For Android 9 and below
            val editorField = EditText::class.java.getDeclaredField("mEditor")
            editorField.isAccessible = true
            val editor = editorField.get(et)

            val cursorDrawable = resources.getDrawable(R.drawable.custom_cursor)
            val cursorField = editor.javaClass.getDeclaredField("mCursorDrawable")
            cursorField.isAccessible = true
            cursorField.set(editor, arrayOf(cursorDrawable, cursorDrawable))
        }
    }

    private fun configure() {
        // 从sp中配置
        MySPUtil.getInstance().serverConnectInfo?.run {
            if (port > 0){
                binding.etPort.setText("${port}")
            }
            if (!TextUtils.isEmpty(name)){
                binding.etName.setText(name)
            }
            if (!TextUtils.isEmpty(pw)){
                binding.etPw.setText(pw)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}