package com.example.ftp.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.ftp.R
import com.example.ftp.databinding.FragmentHomeBinding
import com.example.ftp.utils.setStatusBarAndNavBar
import timber.log.Timber

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.layoutTitle.ivBack.visibility = View.GONE
        binding.layoutTitle.tvName.text = "文件传输"

        binding.layoutTitle.ivRight.visibility = View.VISIBLE
        binding.layoutTitle.ivRight.setImageResource(R.drawable.svg_introduce_icon)
        binding.layoutTitle.ivRight.setOnClickListener {
            findNavController().navigate(R.id.action_home2introduce)
        }

        binding.llServer.setOnClickListener {
            findNavController().navigate(R.id.action_home2server_settings)
        }

        binding.llClient.setOnClickListener {
            findNavController().navigate(R.id.action_home2client_settings)
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