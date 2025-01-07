package com.example.ftp.ui.home

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.ftp.databinding.FragmentClientSettingsMoreBinding
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.isFolderNameValid
import com.example.ftp.utils.isFullFolderNameValid
import com.example.ftp.utils.normalizeFilePath
import com.example.ftp.utils.replaceCursorStyle
import com.example.ftp.utils.showToast
import timber.log.Timber

class ClientSettingsMoreFragment : Fragment() {

    private var _binding: FragmentClientSettingsMoreBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel =
            ViewModelProvider(this).get(ClientSettingsMoreViewModel::class.java)

        _binding = FragmentClientSettingsMoreBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.layoutTitle.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.layoutTitle.tvName.text = "设置"

        // 替换闪烁的cursor
        replaceCursorStyle(requireContext(), binding.etSavePath)
        binding.etSavePath.addTextChangedListener(
            beforeTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            onTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            afterTextChanged = {
                it?.toString()?.run {
                    viewModel.etSavePath = this
                }
            }
        )
        binding.etSavePath.setText(viewModel.etSavePath)

        binding.tvSave.setOnClickListener {
            if (isFullFolderNameValid(viewModel.etSavePath)){
                if (viewModel.etSavePath == "/"){
                    MySPUtil.getInstance().downloadSavePath = viewModel.etSavePath
                    showToast("已保存")
                    return@setOnClickListener
                }
                val path: String
                if (viewModel.etSavePath.first() == '/') {
                    path = viewModel.etSavePath.substring(1)
                } else {
                    path = viewModel.etSavePath
                }

                val paths = path.split("/")
                Timber.d("etSavePath ${viewModel.etSavePath}")
                Timber.d("path ${path}")
                Timber.d("paths it=${paths.size}")
                paths.forEachIndexed { _, p ->
                    Timber.d("paths it=${p}")
                    isFolderNameValid(p).run {
                        if (!this){
                            showToast("路径不合法，请修改")
                            return@setOnClickListener
                        }
                    }
                }
                if (paths.isEmpty()){
                    showToast("路径不合法，请修改")
                }else{
                    MySPUtil.getInstance().downloadSavePath = viewModel.etSavePath
                    showToast("已保存")
                }
            }else{
                showToast("路径不合法，请修改")
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}