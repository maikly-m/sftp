package com.emoji.ftp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.emoji.ftp.R
import com.emoji.ftp.databinding.FragmentClientSettingsMoreBinding
import com.emoji.ftp.ui.MainViewModel
import com.emoji.ftp.utils.MySPUtil
import com.emoji.ftp.utils.isFolderNameValid
import com.emoji.ftp.utils.isFullFolderNameValid
import com.emoji.ftp.utils.replaceCursorStyle
import com.emoji.ftp.utils.showToast
import timber.log.Timber

class ClientSettingsMoreFragment : Fragment() {

    private lateinit var mainViewModel: MainViewModel
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
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        _binding = FragmentClientSettingsMoreBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.layoutTitle.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.layoutTitle.tvName.text = getString(R.string.text_settings)

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

        // 替换闪烁的cursor
        replaceCursorStyle(requireContext(), binding.etUploadPath)
        binding.etUploadPath.addTextChangedListener(
            beforeTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            onTextChanged = { charSequence: CharSequence?, i: Int, i1: Int, i2: Int -> },
            afterTextChanged = {
                it?.toString()?.run {
                    viewModel.etUploadPath = this
                }
            }
        )
        binding.etUploadPath.setText(viewModel.etUploadPath)

        binding.tvSave.setOnClickListener {
            if (checkDownloadPath(viewModel)){
                if (checkUploadPath(viewModel)){
                    Timber.d("viewModel.etUploadPath ${viewModel.etUploadPath}")
                }
            }
        }

        return root
    }

    private fun checkUploadPath(viewModel: ClientSettingsMoreViewModel): Boolean{
        if (isFullFolderNameValid(viewModel.etUploadPath)) {
            if (viewModel.etUploadPath == "/") {
                MySPUtil.getInstance().uploadSavePath = viewModel.etUploadPath
                showToast(getString(R.string.text_save))
                return true
            }
            val path: String
            if (viewModel.etUploadPath.first() == '/') {
                path = viewModel.etUploadPath.substring(1)
            } else {
                path = viewModel.etUploadPath
            }

            val paths = path.split("/")
            paths.forEachIndexed { _, p ->
                Timber.d("paths it=${p}")
                isFolderNameValid(p).run {
                    if (!this) {
                        showToast(getString(R.string.text_upload_path_illegal))
                        return false
                    }
                }
            }
            if (paths.isEmpty()) {
                showToast(getString(R.string.text_upload_path_illegal))
            } else {
                MySPUtil.getInstance().uploadSavePath = viewModel.etUploadPath
                showToast(getString(R.string.text_save))
                return true
            }
        } else {
            showToast(getString(R.string.text_upload_path_illegal))
        }
        return false

    }

    private fun checkDownloadPath(viewModel: ClientSettingsMoreViewModel): Boolean{
        if (isFullFolderNameValid(viewModel.etSavePath)) {
            if (viewModel.etSavePath == "/") {
                MySPUtil.getInstance().downloadSavePath = viewModel.etSavePath
                // 修改监听位置
                mainViewModel.resetFileObserver()
                return true
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
                    if (!this) {
                        showToast(getString(R.string.text_download_path_illegal))
                        return false
                    }
                }
            }
            if (paths.isEmpty()) {
                showToast(getString(R.string.text_download_path_illegal))
            } else {
                MySPUtil.getInstance().downloadSavePath = viewModel.etSavePath
                // 修改监听位置
                mainViewModel.resetFileObserver()
                return true
            }
        } else {
            showToast(getString(R.string.text_download_path_illegal))
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}