package com.example.ftp.player

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.example.ftp.utils.ToastUtil
import com.example.ftp.utils.showToast
import timber.log.Timber
import java.io.IOException


@OptIn(UnstableApi::class)
fun playSftpVideo(
    context: Context,
    playerView: PlayerView,
    sftpHost: String,
    sftpPort: Int,
    sftpUsername: String,
    sftpPassword: String,
    videoPath: List<String>?
) : ExoPlayer{

    // 创建自定义 SFTP DataSource.Factory
    val sftpDataSourceFactory = SftpDataSourceFactory(
        host = sftpHost,
        port = sftpPort,
        username = sftpUsername,
        password = sftpPassword
    )
    val mode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
    val renderersFactory: RenderersFactory =
        DefaultRenderersFactory(context).setExtensionRendererMode(mode)
    //val renderersFactory = DefaultRenderersFactory(context).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
    // val exoPlayer = ExoPlayer.Builder(context, renderersFactory).build()

    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
            DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
            10000, // 缓冲区的最大容量
            2000 // 预缓冲时间
        ).build()

    // 创建 ExoPlayer 实例
    val exoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setMediaSourceFactory(ProgressiveMediaSource.Factory(sftpDataSourceFactory))
        .setLoadControl(loadControl)
        .build()

    // 绑定 PlayerView
    playerView.player = exoPlayer

//    val m = mutableListOf<MediaItem>()
//    m.add(MediaItem.fromUri(Uri.parse(videoPath)))
//    exoPlayer.setMediaItems(m)

    exoPlayer.addListener(object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            when (error) {
                is ExoPlaybackException -> {
                    when (error.type) {
                        ExoPlaybackException.TYPE_SOURCE -> {
                            Timber.e("ExoPlayer", "Source error: ${error.sourceException.message}")
                            // 数据异常
                            if (!TextUtils.isEmpty(ToastUtil.tempSftpPlayerErrorToast)) {
                                showToast(ToastUtil.tempSftpPlayerErrorToast)
                                ToastUtil.tempSftpPlayerErrorToast = ""
                            } else {
                                showToast("文件不存在或格式不支持")
                            }
                            exoPlayer.seekToNextMediaItem()

                            handleSourceError(error.sourceException)
                        }
                        ExoPlaybackException.TYPE_RENDERER -> {
                            Timber.e("ExoPlayer", "Renderer error: ${error.rendererException.message}")
                            handleRendererError(error.rendererException)
                        }
                        ExoPlaybackException.TYPE_UNEXPECTED -> {
                            Timber.e("ExoPlayer", "Unexpected error: ${error.unexpectedException.message}")
                            handleUnexpectedError(error.unexpectedException)
                        }
                        ExoPlaybackException.TYPE_REMOTE -> {
                            Timber.e("ExoPlayer", "Remote error")
                            handleRemoteError()
                        }
                        else -> {
                            Timber.e("ExoPlayer", "Unknown error")
                        }
                    }
                }
                else -> {
                    Timber.e("ExoPlayer", "Unknown playback error: ${error.message}")
                }
            }
        }

        private fun handleRemoteError() {


        }

        private fun handleUnexpectedError(unexpectedException: RuntimeException) {


        }

        private fun handleRendererError(rendererException: Exception) {


        }

        private fun handleSourceError(sourceException: IOException) {

        }
    })
    videoPath?.forEach {
        exoPlayer.addMediaItem(MediaItem.fromUri(Uri.parse(it)))
    }
    // test
    //exoPlayer.addMediaItem(MediaItem.fromUri(Uri.parse("test")))

    // 准备播放
    exoPlayer.prepare()
    exoPlayer.play()
    return exoPlayer
}
