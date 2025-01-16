package com.emoji.ftp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.emoji.ftp.databinding.FragmentScanCodeBinding
import com.google.zxing.Result
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.ViewfinderView
import com.journeyapps.barcodescanner.camera.CameraSettings

class ScanCodeFragment : Fragment() {

    private lateinit var barcodeView: BarcodeView
    private var _binding: FragmentScanCodeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel =
            ViewModelProvider(this).get(ScanCodeViewModel::class.java)

        _binding = FragmentScanCodeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 获取扫描框
        val viewfinderView: ViewfinderView = binding.zxingBarcodeViewfinder
        barcodeView = binding.zxingBarcodeScanner

        // todo 不使用viewfinder
        // 设置扫描框的颜色，样式等
        // viewfinderView.setMaskColor(resources.getColor(android.R.color.holo_blue_light))
        // viewfinderView.setCameraPreview(barcodeView)

        // 配置扫描参数
        val cameraSettings = CameraSettings()
        cameraSettings.isAutoFocusEnabled = true  // 开启自动对焦
        barcodeView.cameraSettings = cameraSettings

        // 设置 BarcodeView 的扫描格式 (可以自定义支持的条形码格式)
        barcodeView.decodeContinuous { result ->
            // 处理扫描结果
            if (result != null) {
                handleResult(result.result)
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleResult(result: Result) {
        val scanResult = result.text
        setFragmentResult("scan",  Bundle().apply {
            putString("scanResult", scanResult)
        })
        findNavController().popBackStack()
    }

    override fun onResume() {
        super.onResume()
        // 启动扫描
        barcodeView.resume()
        // 状态栏显示黑色
        // setStatusBarAndNavBar(requireActivity().window, Color.WHITE, true)
    }

    override fun onPause() {
        super.onPause()
        // 停止扫描
        barcodeView.pause()
    }
}