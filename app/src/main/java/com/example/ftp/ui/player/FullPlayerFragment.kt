package com.example.ftp.ui.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import androidx.recyclerview.widget.RecyclerView
import com.example.ftp.R
import com.example.ftp.databinding.FragmentFullPlayerBinding
import com.example.ftp.databinding.ItemListFileBinding
import com.example.ftp.player.playSftpVideo
import com.example.ftp.provider.GetProvider
import com.example.ftp.ui.dialog.PlayListDialog
import com.example.ftp.ui.toReadableFileSize
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.formatTimeWithSimpleDateFormat
import com.example.ftp.utils.getIcon4File
import com.example.ftp.utils.normalizeFilePath
import com.example.ftp.utils.recover
import com.example.ftp.utils.setFullScreen
import com.example.ftp.utils.showCustomAlertDialog
import com.example.ftp.utils.showCustomFileInfoDialog
import com.example.ftp.utils.showCustomPlayerDialog
import com.jcraft.jsch.ChannelSftp
import timber.log.Timber
import java.util.Vector

class FullPlayerFragment : Fragment() {

    private var playListDialog: PlayListDialog? = null
    private var config: Pair<Int, Int>? = null
    private lateinit var mNetErrorRunnable: Runnable
    private var mCurrentPosition: Long = 0
    private lateinit var mNetworkCallback: ConnectivityManager.NetworkCallback
    private lateinit var items: java.util.ArrayList<String>
    private lateinit var player: ExoPlayer
    private lateinit var viewModel: FullPlayerViewModel
    private var _binding: FragmentFullPlayerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 切换横屏
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        config = setFullScreen(requireActivity().window)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel =
            ViewModelProvider(requireActivity()).get(FullPlayerViewModel::class.java)
        _binding = FragmentFullPlayerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 返回键处理
        // 监听返回键操作
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // 弹窗提示退出
            showCustomAlertDialog(requireContext(), "提示","退出播放?", {
                // 取消

            }){
                // 确定
                binding.playerView.post {
                    requireActivity().finish()
                }
            }
        }

        val info = MySPUtil.getInstance().clientConnectInfo
        // 创建player
        player = playSftpVideo(
            context = requireContext(),
            playerView = binding.playerView,
            sftpHost = info.ip,
            sftpPort = info.port,
            sftpUsername = info.name,
            sftpPassword = info.pw,
            videoPath = viewModel.playList
        )
        // 播放位置
        Timber.d("onCreateView viewModel.index=${viewModel.index},viewModel.seek=${viewModel.seek} ")
        player.seekTo(viewModel.index, viewModel.seek)

        items = (arguments?.getStringArrayList("items")?:ArrayList<String>()) as ArrayList<String>

        player.let {
            binding.playerView.player = it
            initData()
        }

        return root
    }

    override fun onStart() {
        super.onStart()
        player.play()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        player.release()
        val manager =
            requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        manager.unregisterNetworkCallback(mNetworkCallback)

        // 切换竖屏
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        config?.let {
            recover(requireActivity().window, it)
        }
    }

    private fun initData() {
        // Listening network change
        val manager =
            requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Timber.d("NetworkCallback onAvailable")
                binding.playerView.post {
                    player.play()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Timber.d("NetworkCallback onLost ")
                binding.playerView.post {
                    mCurrentPosition = player.currentPosition ?:0
                    Timber.d("NetworkCallback onLost mCurrentPosition=%s", mCurrentPosition)
                }
            }
        }
        manager.registerNetworkCallback(NetworkRequest.Builder().build(), mNetworkCallback)

        mNetErrorRunnable = Runnable {
            // mIvLoading.setVisibility(View.GONE)
        }

        // 设置 VisibilityListener
        binding.playerView.setControllerVisibilityListener(ControllerVisibilityListener { visibility ->
            if (visibility == View.VISIBLE) {
                // 控制器显示
                Timber.d("ExoPlayer, Controller is visible")
                binding.ivList.visibility = View.VISIBLE
            } else {
                // 控制器隐藏
                Timber.d("ExoPlayer, Controller is hidden")
                binding.ivList.visibility = View.GONE
            }
        })

        binding.ivList.setOnClickListener {
            // 弹窗
            if (playListDialog != null && playListDialog?.isVisible == true){
                return@setOnClickListener
            }
            playListDialog = PlayListDialog.newInstance()
            playListDialog?.show(this)
        }

        viewModel.seekPos.observe(viewLifecycleOwner){
            // play
            player.seekTo(it, 0)
            viewModel.index = it
        }
    }


}