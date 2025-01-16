package com.emoji.ftp.bean

import androidx.annotation.DrawableRes
import java.io.Serializable

data class FileInfo(
    val type: String,
    val name: String,
    var count: Int,
    @DrawableRes
    val icon: Int,
):Serializable