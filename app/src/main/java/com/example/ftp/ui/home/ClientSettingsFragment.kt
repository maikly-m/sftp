package com.example.ftp.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.ftp.R
import com.example.ftp.bean.ConnectInfo
import com.example.ftp.databinding.FragmentClientSettingsBinding
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.grantCamera
import com.example.ftp.utils.grantExternalStorage
import com.example.ftp.utils.gson.GsonUtil
import com.example.ftp.utils.isIPAddress
import com.example.ftp.utils.showToast
import timber.log.Timber

class ClientSettingsFragment : Fragment() {

    private var serverInfo: ConnectInfo? = null
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
        viewModel =
            ViewModelProvider(this).get(ClientSettingsViewModel::class.java)

        _binding = FragmentClientSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.layoutTitle.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.layoutTitle.tvName.text = "客户端"

        binding.layoutTitle.ivRight.visibility = View.VISIBLE
        binding.layoutTitle.ivRight.setImageResource(R.drawable.svg_more_icon)
        binding.layoutTitle.ivRight.setOnClickListener {
            findNavController().navigate(R.id.action_client_settings2client_settings_more)
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
                        showToast("无法打开文件访问权限设置")
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
        replaceCursorStyle(binding.etIp)
        replaceCursorStyle(binding.etPort)
        replaceCursorStyle(binding.etName)
        replaceCursorStyle(binding.etPw)

        binding.etIp.addTextChangedListener(
            beforeTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            onTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            afterTextChanged = {
                it?.toString()?.run {
                    viewModel.etIp = this
                }
            })

        binding.etPort.addTextChangedListener(
            beforeTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            onTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            afterTextChanged = {
                it?.toString()?.run {
                    viewModel.etPort = this
                }
            })
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

//        ivPwMask.setOnClickListener {
//            val textPassword = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
//            val textVisiblePassword = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
//            etPw.inputType = if (etPw.inputType == textPassword) {
//                ivPwMask.setImageResource(R.drawable.icon_eyes_solid)
//                textVisiblePassword
//            } else {
//                ivPwMask.setImageResource(R.drawable.icon_eyes_close)
//                textPassword
//            }
//        }


        binding.tvScan.setOnClickListener {
            // 扫码
            grantCamera(requireActivity()){
                if (it) {
                    findNavController().navigate(R.id.action_client_settings2scan_code)
                } else {

                }
            }
        }

        binding.tvConnect.setOnClickListener {

            var p = -1
            try {
                p = binding.etPort.text.toString().toInt()
            } catch (e: Exception) {
                Timber.e(e.message)
            }
            val info =  ConnectInfo(binding.etIp.text.toString(),
                p,
                binding.etName.text.toString(),
                binding.etPw.text.toString(),
            )

            if (isIPAddress(info.ip)){
                if (info.port < 0) {
                    showToast("端口要大于0")
                } else {
                    if (TextUtils.isEmpty(info.name)) {
                        showToast("用户名不能为空")
                    } else {
                        if (TextUtils.isEmpty(info.pw)) {
                            showToast("密码不能为空")
                        } else {
                            MySPUtil.getInstance().clientConnectInfo = info
                            findNavController().navigate(R.id.action_client_settings2client_browser)
                        }
                    }
                }

            }else{
                showToast("ip格式不正确")
            }
        }

        setFragmentResultListener("scan"){ _, bundle ->
            bundle.getString("scanResult")?.let {
                if (!TextUtils.isEmpty(it)){
                    Timber.d("scanResult ${it}")
                    val info = GsonUtil.jsonToBean(it, ConnectInfo::class.java)
                    binding.etIp.setText(info.ip)
                    binding.etPort.setText("${info.port}")
                    binding.etName.setText(info.name)
                    binding.etPw.setText(info.pw)
                    serverInfo = info
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
        MySPUtil.getInstance().clientConnectInfo?.run {
            if (!TextUtils.isEmpty(ip)){
                binding.etIp.setText(ip)
            }
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