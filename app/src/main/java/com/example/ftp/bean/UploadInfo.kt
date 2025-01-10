package com.example.ftp.bean

data class UploadInfo(
    val progress: Float,
    val currentCount: Int,
    val count: Int,
    val currentFileSizes: Long,
    val fileSizes: Long,
)