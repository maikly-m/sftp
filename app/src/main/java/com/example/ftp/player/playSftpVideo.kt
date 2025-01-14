package com.example.ftp.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.DownloadHelper.createMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.ui.PlayerView


@OptIn(UnstableApi::class)
fun playSftpVideo(
    context: Context,
    playerView: PlayerView,
    sftpHost: String,
    sftpPort: Int,
    sftpUsername: String,
    sftpPassword: String,
    videoPath: String
) : ExoPlayer{
    // 创建 ExoPlayer 实例
    val exoPlayer = ExoPlayer.Builder(context).build()

    // 绑定 PlayerView
    playerView.player = exoPlayer

    // 创建自定义 SFTP DataSource.Factory
    val sftpDataSourceFactory = SftpDataSourceFactory(
        host = sftpHost,
        port = sftpPort,
        username = sftpUsername,
        password = sftpPassword
    )
    // 创建 MediaSource
    val mediaSource = ProgressiveMediaSource.Factory(sftpDataSourceFactory)
        .createMediaSource(MediaItem.fromUri(Uri.parse(videoPath)))

    // 设置 MediaSource 给 ExoPlayer
    exoPlayer.setMediaSource(mediaSource)

    // 准备播放
    exoPlayer.prepare()
    exoPlayer.play()
    return exoPlayer
}
