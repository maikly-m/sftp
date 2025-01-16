package com.example.ftp.ui.player

import android.content.Context
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import com.example.ftp.R
import com.example.ftp.databinding.FragmentFullPlayerBinding
import com.example.ftp.player.playSftpVideo
import com.example.ftp.ui.dialog.PlayListDialog
import com.example.ftp.utils.MySPUtil
import com.example.ftp.utils.musicSuffixType
import com.example.ftp.utils.recover
import com.example.ftp.utils.setFullScreen
import com.example.ftp.utils.showCustomAlertDialog
import com.example.ftp.utils.videoSuffixType
import timber.log.Timber

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
            showCustomAlertDialog(requireContext(), getString(R.string.text_tip),
                getString(R.string.text_quit_playing), {
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

        // 监听切换
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        // Player 处于空闲状态
                        Timber.d("ExoPlayer is idle")
                    }
                    Player.STATE_BUFFERING -> {
                        // Player 正在缓冲
                        viewModel.loading.postValue(true)
                        Timber.d("ExoPlayer is buffering")
                    }
                    Player.STATE_READY -> {
                        // Player 已准备好，可能会开始播放
                        viewModel.loading.postValue(false)

                        val size = viewModel.playList?.size?:0
                        if (player.currentMediaItemIndex < size ){
                            val p = viewModel.playList!![player.currentMediaItemIndex]
                            // 通过文件名来识别
                            p.substringAfterLast(".", "").let {
                                if (it in videoSuffixType){
                                    viewModel.mediaTypeChange.postValue(1)
                                }else if (it in musicSuffixType) {
                                    viewModel.mediaTypeChange.postValue(0)
                                }
                            }
                        }

                        Timber.d("ExoPlayer is ready")
                    }
                    Player.STATE_ENDED -> {
                        viewModel.loading.postValue(false)
                        // 播放结束
                        Timber.d("ExoPlayer playback ended")
                    }
                }
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                // 加载状态发生变化
                if (isLoading) {
                    //viewModel.loading.postValue(true)
                    Timber.d("ExoPlayer is loading")
                } else {
                    Timber.d("ExoPlayer finished loading")
                    //viewModel.loading.postValue(false)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                // 获取当前播放的 MediaItem 索引
                val currentIndex = player.currentMediaItemIndex
                Timber.d("Current MediaItem Index: $currentIndex")
                // 获取具体的 MediaItem
                val currentItem = player.currentMediaItem
                Timber.d("Current MediaItem: ${currentItem?.mediaId}")

                // 获取并判断媒体类型
                val mimeType = currentItem?.mediaMetadata?.mediaType?:-1
                if (mimeType == MediaMetadata.MEDIA_TYPE_MUSIC) {
                    Timber.d("ExoPlayer, 正在播放音频")
                    viewModel.loading.postValue(false)
                } else if (mimeType == MediaMetadata.MEDIA_TYPE_VIDEO) {
                    Timber.d("ExoPlayer, 正在播放视频")
                }
                viewModel.mediaTypeChange.postValue(mimeType)
                viewModel.index = currentIndex
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                // 捕获播放异常
                Timber.d("Player error occurred: ${error.message}")
                // 错误后重置播放器并重新加载媒体
                player.prepare()
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
            viewModel.index = it
            // play
            player.seekTo(it, 0)
        }
        viewModel.loading.observe(viewLifecycleOwner){
            // play
            if (it) {
                binding.clLoading.visibility = View.VISIBLE
            } else {
                binding.clLoading.visibility = View.GONE
            }
        }
        viewModel.mediaTypeChange.observe(viewLifecycleOwner){
            // play
            if (it == 1) {
                binding.llMusicPlay.visibility = View.GONE
            } else if (it == 0){
                binding.llMusicPlay.visibility = View.VISIBLE
            }else{
                binding.llMusicPlay.visibility = View.GONE
            }
        }
    }


}