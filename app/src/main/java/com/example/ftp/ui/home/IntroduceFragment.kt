package com.example.ftp.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.ftp.R
import com.example.ftp.databinding.FragmentIntroduceBinding
import com.example.ftp.utils.setStatusBarAndNavBar
import org.jsoup.Jsoup

class IntroduceFragment : Fragment() {

    private var _binding: FragmentIntroduceBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel =
            ViewModelProvider(this).get(IntroduceViewModel::class.java)

        _binding = FragmentIntroduceBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initView()

        return root
    }

    private fun initView() {
        val tvTitle: TextView = binding.layoutTitle.tvName
        val ivBack: ImageView = binding.layoutTitle.ivBack

        // 让 WebView 支持 JavaScript 等特性
        val webSettings = binding.web.settings
        webSettings.javaScriptEnabled = true
        webSettings.setSupportZoom(false)

        val content ="<b>文件客户端使用:</b>" +
                "<p>客户端和服务端连接到同一个路由器的时候，在客户端页面，输入sftp地址，端口号，用户名和密码，即可连接到sftp服务端；" +
                "或者通过扫码连接App提供的服务端。</p>" + "<b>文件服务端使用:</b><p>客户端和服务端连接到同一个路由器的时候,就可以给客户端提供服务。</p>" +
                "<b>客户端和服务端搭配用法:</b> " +
                "<ol><li>App客户端和App服务端:<p>客户端和服务端连接到同一个路由器或者客户端开启热点让服务端连接</p></li>" +
                "<li>App客户端和其他sftp服务端:<p>客户端和服务端连接到同一个路由器</p></li>" +
                "<li>其他sftp客户端和App服务端:<p>客户端和服务端连接到同一个路由器</p></li>" +
                "</ol>"

        // 加载 HTML 内容, 处理base64不自动换行问题
        val htmlContent = "<html>" +
                "<head>" +
                "<style>" +
                "body {" +
                "  font-family: Arial, sans-serif;" +
                "  word-wrap: break-word;" +
                "  overflow-wrap: break-word;" +
                "  word-break: break-all;" +
                "  max-width: 100%;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "</body>" +
                " ${content}" +
                "</html>"
        binding.web.loadDataWithBaseURL(
            null,
            Jsoup.parse(htmlContent).html(),
            "text/html",
            "UTF-8",
            null
        )

        ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        tvTitle.text = getString(R.string.text_instruction)
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