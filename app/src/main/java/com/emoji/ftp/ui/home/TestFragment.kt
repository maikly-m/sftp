package com.emoji.ftp.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.emoji.ftp.R
import com.emoji.ftp.databinding.FragmentHomeBinding
import com.emoji.ftp.databinding.FragmentTestBinding
import com.emoji.ftp.nativeLib.OboeTest
import com.emoji.ftp.utils.setStatusBarAndNavBar
import timber.log.Timber

class TestFragment : Fragment() {

    private var _binding: FragmentTestBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var testStart = 0L;
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val testViewModel =
            ViewModelProvider(this).get(TestViewModel::class.java)

        _binding = FragmentTestBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val oboeTest = OboeTest()

        binding.oboeStart.setOnClickListener {
            testStart = oboeTest.testStartEcho()
        }

        binding.oboeStop.setOnClickListener {
            oboeTest.testStopEcho(testStart)
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        // 状态栏显示黑色
        setStatusBarAndNavBar(requireActivity().window, Color.WHITE, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}